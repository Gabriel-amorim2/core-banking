# ADR-002: Separação entre identidade (auth) e dinheiro (core)

- **Status:** Aceito
- **Data:** 2026-11-07
- **Decisores:** Gabriel
## Contexto

Na primeira versão do modelo, dados do titular (nome, username, CPF) apareciam tanto do lado da identidade quanto do lado das contas. Isso cria:

- **duplicação de dados pessoais** (LGPD: mais lugares para proteger, corrigir e apagar);
- **dúvida sobre a fonte da verdade** (qual CPF vale se os dois divergirem?);
- **acoplamento**: mudar o cadastro do usuário exigiria mexer no core.
## Decisão

Vamos separar as responsabilidades em dois donos:

**`auth-service`: dono da identidade**

- Tabela `users`: `id (UUID)`, `email`, `password_hash`, `full_name`, `cpf`, `status`, `mfa_*`.
- O CPF é normalizado (só dígitos), validado (formato + dígito verificador) no cadastro e protegido por constraint `UNIQUE`.
- A regra **"um CPF = um usuário"** é garantida aqui.
  **`core-service`: dono do dinheiro**

- Tabela `accounts`: `id`, `user_id`, `currency`, `balance`, `status`, `created_at`.
- **Nenhum** dado pessoal: sem CPF, nome ou username.
- `user_id` é uma **referência lógica, sem foreign key**, porque `users` está em outro banco.
- O `user_id` vem do JWT emitido pelo `auth-service` (claim `sub`) e não do corpo da requisição.
## Alternativas consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| **Identidade só no auth, core só com `user_id` (escolhida)** | Uma única fonte da verdade; menos dados pessoais expostos; serviços independentes | Exibir o nome do titular exige consultar o auth; não existe FK garantindo que o `user_id` existe |
| Replicar CPF/nome no core | Consultas locais mais simples | Duplicação de PII; risco de divergência; maior superfície de LGPD |
| Banco compartilhado entre auth e core | FK real entre `accounts` e `users` | Acopla os dois serviços pelo schema; anula a separação em serviços |

## Consequências

**Positivas**

- O core lida apenas com dinheiro, o que reduz o escopo de dados sensíveis desse banco.
- Correções ou exclusão de dados pessoais acontecem num lugar só.
- O auth pode evoluir (ex.: mudar o provedor de MFA, migrar para Keycloak) sem afetar o core.
  **Negativas / trade-offs**

- Sem FK, a integridade do `user_id` depende da aplicação: o core confia no JWT assinado pelo auth.
- Se um usuário for desativado no auth, o core precisa ser avisado (evento `user.deactivated`, planejado) para bloquear as contas.
- Telas que mostram "nome do titular" precisam compor dados dos dois serviços (no gateway/BFF ou no front).
## Quando revisitar

- Surgir uma regra de negócio do core que dependa de um dado pessoal (ex.: limite por faixa de renda). Nesse caso, avaliar consultar o auth sob demanda antes de replicar o dado.
