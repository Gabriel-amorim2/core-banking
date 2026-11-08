
# ADR-005: Transferência síncrona com lock pessimista
 
- **Status:** Aceito
- **Data:** 2026-11-07
- **Decisores:** Gabriel
## Contexto
 
O desenho original previa transferências **assíncronas**: a API cria a transação `PENDING`, publica `transfer.requested` e um worker executa depois. Isso é útil em escala, mas exige broker, workers, retries e consulta de status pelo cliente.
 
Além disso, duas transferências simultâneas saindo da mesma conta podem ler o mesmo saldo e, juntas, deixá-lo negativo (*race condition* / *lost update*).
 
## Decisão
 
No MVP, a transferência é **síncrona** e executada em **uma única transação de banco**:
 
1. Abre a transação.
2. Trava as duas contas com `SELECT ... FOR UPDATE`, **sempre em ordem crescente de `id`**.
3. Valida status das contas, moeda e saldo.
4. Grava a transação, os lançamentos no ledger e os novos saldos.
5. Marca a transação como `COMPLETED` e faz o commit.
6. Responde `201 Created` com o resultado final.
Qualquer erro → rollback total; a requisição retorna erro e nada fica gravado parcialmente.
 
A execução assíncrona via broker fica para a Fase B/C, quando houver integração com fraude e agendamento.
 
## Alternativas consideradas
 
| Alternativa | Prós | Contras |
|---|---|---|
| **Síncrona + lock pessimista (escolhida)** | Simples; resposta final imediata; impossível saldo negativo por concorrência | Throughput limitado por conta muito movimentada ("hot account"); a requisição espera o lock |
| Síncrona + lock otimista (`@Version`) | Sem espera de lock | Muitos conflitos e retries em contas disputadas; lógica de retry na aplicação |
| Assíncrona via fila | Absorve picos; desacopla | Precisa de broker, workers, idempotência no consumidor e polling de status: complexo demais para o MVP |
| `UPDATE ... SET balance = balance - x WHERE balance >= x` | Atômico em um comando | Funciona para débito, mas não cobre bem o ledger com `balance_after` e as validações de status |
 
## Consequências
 
**Positivas**
 
- Correção garantida pelo banco: duas transferências na mesma conta são serializadas.
- A ordem fixa dos locks evita **deadlock** entre transferências cruzadas (A→B e B→A ao mesmo tempo).
- O cliente recebe o status final na mesma resposta.
**Negativas / trade-offs**
 
- Contas com altíssimo volume podem virar gargalo.
- Transações precisam ser curtas: nada de chamadas HTTP externas dentro do lock.
- `lock_timeout` deve ser configurado para não segurar conexões indefinidamente.
## Quando revisitar
 
- Entrar a verificação de fraude em tempo real antes da execução.
- Surgirem contas "quentes" (ex.: conta de liquidação) com contenção mensurável.
- Integração com o `scheduler-service`, que pode se beneficiar de execução via fila.
 

