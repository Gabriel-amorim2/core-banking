
# Core Service — Plataforma Bancária
 
Serviço **core** da Plataforma Bancária: é o **dono do dinheiro**. Concentra, em um único monólito, as operações financeiras que precisam de consistência forte:
 
- **Contas** — abertura, consulta de saldo e status
- **Transferências** — movimentação entre contas, com reversão auditada
- **Transações** — registro e histórico de cada operação
- **Livro-razão (ledger)** — o "livro da verdade": lançamentos imutáveis em partidas dobradas
> As demais capacidades da plataforma (autenticação/MFA, cartões, pagamentos agendados, detecção de fraude, auditoria centralizada) são **serviços separados** e não fazem parte deste repositório.
 
---
 
## Sumário
 
- [Arquitetura](#arquitetura)
- [Por que um monólito?](#por-que-um-monólito)
- [Stack](#stack)
- [Modelo de dados](#modelo-de-dados)
- [Livro-razão (ledger)](#livro-razão-ledger)
- [Endpoints](#endpoints)
- [Fluxo de transferência](#fluxo-de-transferência)
- [Segurança](#segurança)
- [Como rodar](#como-rodar)
- [Testes](#testes)
- [Roadmap](#roadmap)
---
 
## Arquitetura
 
**Fronteira de responsabilidade**
 
| Serviço | É dono de | Não guarda |
|---|---|---|
| `auth-service` | Identidade: `users`, e-mail, senha, nome, **CPF** (`UNIQUE`), MFA | Saldo, contas |
| `core-service` | Dinheiro: contas, transações, ledger | CPF, nome, username |
 
O `core-service` referencia o usuário apenas por `user_id` (UUID), uma **referência lógica, sem foreign key**, porque a tabela `users` fica em outro banco. A regra "um CPF = um usuário" é garantida no `auth-service`.
 
---
 
## Por que um monólito?
 
Contas, transferências, transações e ledger mudam juntos e precisam de **transações ACID** no mesmo banco: um débito sem o crédito correspondente não pode existir. Manter esses módulos no mesmo processo e no mesmo PostgreSQL elimina transações distribuídas (sagas, 2PC) justamente na parte em que a consistência é mais crítica.
 
Internamente o código é organizado como **monólito modular**, com pacotes bem separados, para facilitar uma eventual extração no futuro:
 
```
src/main/java/.../core
├── account/        # contas e saldo
├── transfer/       # orquestração das transferências e reversões
├── transaction/    # registro e histórico das transações
├── ledger/         # lançamentos imutáveis (partidas dobradas)
└── shared/         # segurança, erros, configuração, eventos
```
 
---
 
## Stack
 
| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17+ |
| Framework | Spring Boot (Web, Data JPA, Security, Validation) |
| Banco | PostgreSQL |
| Migrações | Flyway  |
| Cache / locks | Redis |
| Mensageria | Kafka |
| Testes | JUnit 5, Mockito, Testcontainers |
| Observabilidade | Prometheus + Grafana, logs estruturados *(planejado)* |
 
---
 
## Modelo de dados
 
### `accounts`
 
| Coluna | Tipo | Observação |
|---|---|---|
| `id` | `uuid` | PK |
| `user_id` | `uuid` | Referência lógica ao `auth-service` (sem FK) |
| `currency` | `char(3)` | ISO 4217 (ex.: `BRL`) |
| `balance` | `numeric(19,4)` | Saldo derivado do ledger |
| `status` | `varchar` | `ACTIVE`, `BLOCKED`, `CLOSED` |
| `created_at` | `timestamptz` | |
 
### `transactions`
 
| Coluna | Tipo | Observação |
|---|---|---|
| `id` | `uuid` | PK |
| `from_account_id` | `uuid` | FK `accounts` |
| `to_account_id` | `uuid` | FK `accounts` |
| `amount` | `numeric(19,4)` | Sempre positivo |
| `currency` | `char(3)` | |
| `status` | `varchar` | `PENDING`, `COMPLETED`, `FAILED`, `REVERSED` |
| `type` | `varchar` | `TRANSFER`, `PAYMENT`, `REVERSAL` |
| `idempotency_key` | `varchar` | `UNIQUE`, evita transferência duplicada |
| `created_at` | `timestamptz` | |
| `metadata` | `jsonb` | Dados extras da requisição |
 
### `ledger_entries`
 
| Coluna | Tipo | Observação |
|---|---|---|
| `id` | `uuid` | PK |
| `transaction_id` | `uuid` | FK `transactions` |
| `account_id` | `uuid` | FK `accounts` |
| `direction` | `varchar` | `DEBIT` ou `CREDIT` |
| `amount` | `numeric(19,4)` | Sempre positivo |
| `currency` | `char(3)` | |
| `balance_after` | `numeric(19,4)` | Saldo da conta após o lançamento |
| `created_at` | `timestamptz` | |
 
> Valores monetários usam `numeric` no banco e `BigDecimal` no Java. **Nunca** `double` ou `float`.
 
---
 
## Livro-razão (ledger)
 
O ledger é a **fonte da verdade** do saldo. Regras:
 
1. **Append-only**: lançamentos nunca são alterados nem apagados. Corrigir algo significa criar novos lançamentos (estorno).
2. **Partidas dobradas**: toda transação gera ao menos um `DEBIT` e um `CREDIT`, e a soma dos débitos é igual à soma dos créditos.
3. **Saldo reconciliável**: `accounts.balance` é um valor em cache que sempre precisa bater com a soma dos lançamentos da conta.
4. **Reversão = nova transação** do tipo `REVERSAL`, com lançamentos invertidos. A transação original passa para `REVERSED`.
Exemplo de uma transferência de R$ 100,00 da conta A para a conta B:
 
| transaction | account | direction | amount |
|---|---|---|---|
| T1 | A | DEBIT | 100.00 |
| T1 | B | CREDIT | 100.00 |
 
---
 
## Endpoints
 
Todos exigem `Authorization: Bearer <JWT>` emitido pelo `auth-service`.
 
### Contas
 
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/accounts` | Abre uma conta para o usuário autenticado |
| `GET` | `/accounts/{id}` | Saldo e dados da conta |
| `GET` | `/accounts/{id}/statement` | Extrato a partir do ledger (paginado) |
 
### Transferências
 
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/transfers` | Cria e executa uma transferência |
| `GET` | `/transfers/{id}` | Status e detalhes |
| `POST` | `/transfers/{id}/reverse` | Estorna a transferência (auditado) |
 
**Exemplo: `POST /transfers`**
 
```http
POST /transfers
Authorization: Bearer <jwt>
Idempotency-Key: 3f6c1a2e-8b1d-4c1e-9a7f-2d5e0b9c4a11
Content-Type: application/json
 
{
  "fromAccountId": "6b1e...",
  "toAccountId": "9c2f...",
  "amount": "100.00",
  "currency": "BRL",
  "metadata": { "description": "Aluguel" }
}
```
 
```json
{
  "id": "a41d...",
  "status": "COMPLETED",
  "amount": "100.00",
  "currency": "BRL",
  "createdAt": "2026-11-07T18:00:00Z"
}
```
 
---
 
## Fluxo de transferência
 
No MVP a execução é **síncrona**, dentro de uma única transação de banco:
 
- As contas são travadas sempre na **mesma ordem** (por `id`), para evitar deadlock entre transferências cruzadas.
- Qualquer falha gera **rollback** completo, e nenhum lançamento parcial fica no ledger.
- Depois do commit, o evento `transfer.completed` será publicado para os demais serviços *(planejado, via outbox)*.
---
 
## Segurança
 
- Autenticação delegada ao `auth-service`: o core apenas **valida o JWT** e extrai o `user_id`.
- **Autorização por titularidade**: o usuário só movimenta e consulta contas cujo `user_id` é o dele.
- Idempotência obrigatória em `POST /transfers`.
- Dados pessoais (CPF, nome) **não** são armazenados no core.
- Segredos via variáveis de ambiente ou vault, nunca no repositório.
---
 
## Como rodar
 
### Pré-requisitos
 
- Java 17+
- Docker e Docker Compose
### Subindo o banco
 
```bash
docker compose up -d postgres
```
 
### Variáveis de ambiente
 
| Variável | Exemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/core_db` |
| `DB_USERNAME` | `core` |
| `DB_PASSWORD` | `core` |
| `JWT_ISSUER_URI` / `JWT_PUBLIC_KEY` | Emissor ou chave pública do `auth-service` |
 
### Executando
 
```bash
./mvnw spring-boot:run
```
 
A API sobe em `http://localhost:8080`.
 
---
 
## Testes
 
```bash
./mvnw test
```
 
- **Unitários**: regras de saldo, validações e montagem dos lançamentos.
- **Integração** com Testcontainers (PostgreSQL real), cobrindo:
  - transferência com saldo insuficiente → `FAILED`, sem lançamentos
  - requisição repetida com a mesma `Idempotency-Key` → uma única transação
  - transferências concorrentes na mesma conta → saldo nunca negativo
  - invariante do ledger: Σ débitos = Σ créditos
