# ADR-001: Core financeiro como monólito modular

- **Status:** Aceito
- **Data:** 2026-11-07
- **Decisores:** Gabriel
## Contexto

A arquitetura alvo da Plataforma Bancária é de microsserviços: autenticação/MFA, contas/transferências, pagamentos agendados, cartões, detecção de fraude e auditoria. No MVP, a prioridade é o **core financeiro**: contas, transferências, transações e livro-razão.

Esses quatro domínios têm uma característica em comum: **mudam juntos e precisam ser consistentes juntos**. Uma transferência precisa, de forma atômica:

1. validar e travar a conta de origem e a de destino;
2. registrar a transação;
3. gravar o débito e o crédito no ledger;
4. atualizar o saldo das duas contas.
   Se esses passos estiverem em serviços e bancos diferentes, uma falha no meio do caminho pode deixar dinheiro "sumido" ou "duplicado", a menos que se implementem mecanismos de consistência distribuída.

## Decisão

Vamos implementar **contas, transferências, transações e ledger em um único serviço (`core-service`)**, com **um único banco PostgreSQL**, organizado internamente como **monólito modular**:

```
core
├── account/
├── transfer/
├── transaction/
├── ledger/
└── shared/
```

Regras de modularização:

- cada módulo expõe uma interface pública (service/facade) e esconde seus repositórios;
- um módulo não acessa as tabelas de outro diretamente;
- dependências seguem uma direção: `transfer → transaction → ledger → account`.
  Os demais domínios (auth, cartões, agendamento, fraude, auditoria) continuam como **serviços separados** e se comunicam com o core por API e eventos.

## Alternativas consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| **Monólito modular (escolhida)** | Transação ACID local; simples de desenvolver, testar e implantar; sem latência de rede entre os módulos; refatoração barata enquanto o domínio ainda está se formando | Escala como uma unidade só; exige disciplina para manter as fronteiras entre os módulos |
| Um microsserviço por domínio (accounts, transfers, ledger) | Deploy e escala independentes | Exige saga ou 2PC para cada transferência; consistência eventual no saldo; muito mais infraestrutura e pontos de falha para um MVP |
| Monólito tradicional, sem fronteiras | Mais rápido no início | Acoplamento cresce rápido e dificulta extrair serviços no futuro |

## Consequências

**Positivas**

- Uma transferência é uma única `@Transactional` no banco: ou tudo acontece, ou nada.
- Sem sagas, compensações ou 2PC no caminho mais crítico do sistema.
- Testes de integração simples: um Postgres via Testcontainers cobre o fluxo inteiro.
- Um único artefato para implantar e observar.
  **Negativas / trade-offs**

- Os módulos não escalam de forma independente.
- Uma falha no core afeta todas as operações financeiras ao mesmo tempo.
- As fronteiras entre módulos dependem de disciplina. Para reforçá-las, recomenda-se usar ArchUnit ou Spring Modulith nos testes.
## Quando revisitar

- Um módulo passa a ter carga ou ritmo de deploy muito diferente dos outros (ex.: extrato com volume de leitura muito maior → candidato a réplica de leitura ou serviço de consulta).
- Times diferentes passam a ser donos de módulos diferentes.
- O banco único vira gargalo mesmo depois de índices, particionamento e réplicas de leitura.
 
