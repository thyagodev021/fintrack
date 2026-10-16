# FinTrack

Controle de finanças pessoais em linha de comando, escrito em **Java 21** sem
dependências externas. Registre receitas e despesas, veja o extrato e receba um
resumo mensal com o percentual de gasto por categoria.

## Requisitos
- JDK 21+
- Maven 3.9+

## Como rodar
```bash
mvn clean package                       # compila, roda os testes e gera target/fintrack.jar
java -jar target/fintrack.jar help
```

## Exemplos
```bash
java -jar target/fintrack.jar add receita 3000 Salário --date 2026-10-05
java -jar target/fintrack.jar add despesa 45,90 Alimentação "Almoço no centro"
java -jar target/fintrack.jar list --month 2026-10
java -jar target/fintrack.jar summary --month 2026-10
java -jar target/fintrack.jar remove 2
```

Os dados ficam em `~/.fintrack/transactions.csv`. Use a variável de ambiente
`FINTRACK_FILE` para escolher outro local.

## Arquitetura
```
cli/         interface de linha de comando (parsing e formatação)
service/     regras de negócio (cadastro, resumo mensal)
repository/  persistência em CSV (interface permite trocar por banco de dados)
model/       Transaction (record) e TransactionType (enum)
```

## Testes
```bash
mvn test
```

## Licença
MIT - veja [LICENSE](LICENSE).
