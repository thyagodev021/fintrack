package com.fintrack.cli;

import com.fintrack.model.Transaction;
import com.fintrack.model.TransactionType;
import com.fintrack.service.FinanceService;
import com.fintrack.service.Summary;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interface de linha de comando. Recebe os argumentos e devolve o código de saída
 * (0 = sucesso, 1 = erro). Entrada/saída e relógio são injetados para facilitar testes.
 */
public final class Cli {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinanceService service;
    private final PrintStream out;
    private final PrintStream err;
    private final Clock clock;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    public Cli(FinanceService service, PrintStream out, PrintStream err, Clock clock) {
        this.service = service;
        this.out = out;
        this.err = err;
        this.clock = clock;
    }

    public int run(String[] args) {
        if (args.length == 0 || List.of("help", "--help", "-h", "ajuda").contains(args[0])) {
            printHelp();
            return 0;
        }

        List<String> rest = Arrays.asList(args).subList(1, args.length);
        try {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "add", "adicionar" -> add(Args.parse(rest));
                case "list", "listar" -> list(Args.parse(rest));
                case "summary", "resumo" -> summary(Args.parse(rest));
                case "remove", "remover" -> remove(Args.parse(rest));
                default -> {
                    err.println("Comando desconhecido: " + args[0]);
                    printHelp();
                    yield 1;
                }
            };
        } catch (IllegalArgumentException e) {
            err.println("Erro: " + e.getMessage());
            return 1;
        } catch (UncheckedIOException | IllegalStateException e) {
            err.println("Erro ao acessar os dados: " + e.getMessage());
            return 1;
        }
    }

    // ----- comandos -----

    private int add(Args args) {
        List<String> p = args.positional();
        if (p.size() < 3) {
            throw new IllegalArgumentException(
                    "Uso: add <despesa|receita> <valor> <categoria> [descrição] [--date AAAA-MM-DD]");
        }
        TransactionType type = parseType(p.get(0));
        BigDecimal amount = parseMoney(p.get(1));
        String description = p.size() > 3 ? String.join(" ", p.subList(3, p.size())) : "";
        LocalDate date = args.options().containsKey("date")
                ? parseDate(args.options().get("date"))
                : LocalDate.now(clock);

        Transaction t = service.add(type, amount, p.get(2), description, date);
        out.printf("%s #%d registrada: %s em %s (%s)%n",
                t.type().label(), t.id(), currency.format(t.amount()), t.category(),
                t.date().format(DATE_FORMAT));
        return 0;
    }

    private int list(Args args) {
        List<Transaction> transactions = args.options().containsKey("month")
                ? service.list(parseMonth(args.options().get("month")))
                : service.list();

        if (transactions.isEmpty()) {
            out.println("Nenhum lançamento encontrado.");
            return 0;
        }
        out.printf("%-4s %-10s %-8s %-16s %-26s %14s%n",
                "ID", "DATA", "TIPO", "CATEGORIA", "DESCRIÇÃO", "VALOR");
        for (Transaction t : transactions) {
            out.printf("%-4d %-10s %-8s %-16s %-26s %14s%n",
                    t.id(), t.date().format(DATE_FORMAT), t.type().label(),
                    truncate(t.category(), 16), truncate(t.description(), 26),
                    currency.format(t.amount()));
        }
        return 0;
    }

    private int summary(Args args) {
        YearMonth month = args.options().containsKey("month")
                ? parseMonth(args.options().get("month"))
                : YearMonth.now(clock);
        Summary s = service.summarize(month);

        out.printf("Resumo de %02d/%d%n", month.getMonthValue(), month.getYear());
        out.printf("  Receitas: %14s%n", currency.format(s.income()));
        out.printf("  Despesas: %14s%n", currency.format(s.expense()));
        out.printf("  Saldo:    %14s%n", currency.format(s.balance()));

        if (!s.expenseByCategory().isEmpty()) {
            out.println();
            out.println("Despesas por categoria:");
            s.expenseByCategory().forEach((category, value) -> {
                // Percentual da categoria sobre o total de despesas + barra visual.
                int percent = value.multiply(BigDecimal.valueOf(100))
                        .divide(s.expense(), 0, RoundingMode.HALF_UP).intValue();
                out.printf("  %-16s %14s %4d%%  %s%n",
                        truncate(category, 16), currency.format(value), percent,
                        "█".repeat(Math.max(1, percent / 5)));
            });
        }
        return 0;
    }

    private int remove(Args args) {
        if (args.positional().size() != 1) {
            throw new IllegalArgumentException("Uso: remove <id>");
        }
        int id;
        try {
            id = Integer.parseInt(args.positional().get(0));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O id deve ser um número inteiro.");
        }
        if (service.remove(id)) {
            out.println("Lançamento #" + id + " removido.");
            return 0;
        }
        err.println("Lançamento #" + id + " não encontrado.");
        return 1;
    }

    // ----- helpers de parsing -----

    static TransactionType parseType(String text) {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "despesa", "expense", "gasto" -> TransactionType.EXPENSE;
            case "receita", "income", "entrada" -> TransactionType.INCOME;
            default -> throw new IllegalArgumentException(
                    "Tipo inválido: \"" + text + "\". Use despesa ou receita.");
        };
    }

    /** Aceita "45.90", "45,90" e "1.234,56". */
    static BigDecimal parseMoney(String text) {
        String normalized = text.strip();
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else {
            normalized = normalized.replace(",", ".");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: \"" + text + "\".");
        }
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida. Use o formato AAAA-MM-DD.");
        }
    }

    private static YearMonth parseMonth(String text) {
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Mês inválido. Use o formato AAAA-MM.");
        }
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private void printHelp() {
        out.println("""
                FinTrack - controle de finanças pessoais

                Uso:
                  add <despesa|receita> <valor> <categoria> [descrição] [--date AAAA-MM-DD]
                  list [--month AAAA-MM]
                  summary [--month AAAA-MM]
                  remove <id>
                  help

                Exemplos:
                  add despesa 45,90 Alimentação "Almoço no centro"
                  add receita 3000 Salário --date 2026-10-05
                  summary --month 2026-10

                Os dados ficam em ~/.fintrack/transactions.csv
                (use a variável de ambiente FINTRACK_FILE para mudar o local).""");
    }

    /** Separa argumentos posicionais das opções no formato --nome valor. */
    private record Args(List<String> positional, Map<String, String> options) {
        static Args parse(List<String> tokens) {
            List<String> positional = new ArrayList<>();
            Map<String, String> options = new HashMap<>();
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.startsWith("--")) {
                    if (i + 1 >= tokens.size()) {
                        throw new IllegalArgumentException("A opção " + token + " precisa de um valor.");
                    }
                    options.put(token.substring(2), tokens.get(++i));
                } else {
                    positional.add(token);
                }
            }
            return new Args(positional, options);
        }
    }
}
