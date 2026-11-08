
# ADR-003: Livro-razão imutável em partidas dobradas

- **Status:** Aceito
- **Data:** 2026-11-07
- **Decisores:** Gabriel
## Contexto

O jeito mais simples de controlar saldo é uma coluna `balance` que recebe `UPDATE` a cada operação. O problema é que assim **não existe histórico confiável**: se o saldo estiver errado, não dá para provar como ele chegou ali, nem reconstruí-lo.

Um sistema bancário precisa responder, a qualquer momento:

- qual é o saldo, e **por quê**;
- qual era o saldo em uma data passada;
- se alguma operação criou ou destruiu dinheiro por engano.
## Decisão

Vamos manter um **livro-razão (`ledger_entries`)** como **fonte da verdade** do saldo, com estas regras:

1. **Append-only**: lançamentos nunca sofrem `UPDATE` nem `DELETE`. O usuário de banco da aplicação não recebe essas permissões na tabela.
2. **Partidas dobradas**: toda transação gera pelo menos um `DEBIT` e um `CREDIT`, e **Σ débitos = Σ créditos** em cada transação.
3. **Correção por estorno**: um erro é corrigido com uma nova transação do tipo `REVERSAL`, com lançamentos invertidos, nunca editando o passado.
4. **Saldo em cache**: `accounts.balance` existe para leitura rápida, é atualizado na mesma transação dos lançamentos e precisa sempre bater com a soma do ledger.
5. **`balance_after`** em cada lançamento, para facilitar extrato e auditoria.
## Alternativas consideradas

| Alternativa | Prós | Contras |
|---|---|---|
| **Ledger em partidas dobradas + saldo em cache (escolhida)** | Histórico completo e auditável; invariante simples de checar; leitura de saldo rápida | Mais escrita por operação; o cache precisa ser reconciliado |
| Só `UPDATE` em `balance` | Simples e rápido | Sem trilha de auditoria; impossível reconstruir ou provar o saldo |
| Ledger sem cache (saldo = `SUM` a cada leitura) | Nunca diverge | Leitura cara conforme o histórico cresce; difícil travar para validar saldo |
| Event sourcing completo | Histórico total de todo o domínio | Complexidade alta (projeções, versionamento de eventos) para um MVP |

## Consequências

**Positivas**

- Qualquer saldo pode ser explicado lançamento por lançamento.
- O extrato vem direto do ledger, sem tabela paralela.
- Um job de reconciliação pode verificar automaticamente: `balance == Σ créditos − Σ débitos` e `Σ débitos == Σ créditos` por transação.
- Base pronta para auditoria e para o futuro `audit-service`.
  **Negativas / trade-offs**

- Cada transferência grava pelo menos 4 linhas (1 transação + 2 lançamentos + saldo das 2 contas).
- A tabela cresce indefinidamente; no futuro pode exigir particionamento por data.
- Se o cache divergir (bug), é preciso processo de correção, sempre recalculando a partir do ledger.
## Quando revisitar

- O volume de lançamentos tornar consultas lentas mesmo com índices → particionamento ou snapshots de saldo periódicos.
- Surgir necessidade de multi-moeda com câmbio → lançamentos em contas de conversão.
