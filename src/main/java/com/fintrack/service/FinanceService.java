package com.fintrack.service;

import com.fintrack.model.Transaction;
import com.fintrack.model.TransactionType;
import com.fintrack.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Regras de negócio: cadastrar, remover, listar e resumir lançamentos. */
public final class FinanceService {

    private final TransactionRepository repository;

    public FinanceService(TransactionRepository repository) {
        this.repository = repository;
    }

    public Transaction add(TransactionType type, BigDecimal amount, String category,
                           String description, LocalDate date) {
        List<Transaction> all = repository.findAll();
        int nextId = all.stream().mapToInt(Transaction::id).max().orElse(0) + 1;

        Transaction transaction = new Transaction(nextId, date, type, category, description, amount);
        all.add(transaction);
        repository.saveAll(all);
        return transaction;
    }

    /** @return true se o lançamento existia e foi removido. */
    public boolean remove(int id) {
        List<Transaction> all = repository.findAll();
        boolean removed = all.removeIf(t -> t.id() == id);
        if (removed) {
            repository.saveAll(all);
        }
        return removed;
    }

    /** Lista todos os lançamentos, do mais antigo ao mais novo. */
    public List<Transaction> list() {
        return sorted(repository.findAll().stream());
    }

    /** Lista os lançamentos de um mês específico. */
    public List<Transaction> list(YearMonth month) {
        return sorted(repository.findAll().stream().filter(t -> t.month().equals(month)));
    }

    public Summary summarize(YearMonth month) {
        List<Transaction> transactions = list(month);

        BigDecimal income = total(transactions, TransactionType.INCOME);
        BigDecimal expense = total(transactions, TransactionType.EXPENSE);

        // Despesas agrupadas por categoria, da maior para a menor.
        Map<String, BigDecimal> byCategory = transactions.stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        return new Summary(month, income, expense, byCategory);
    }

    private static BigDecimal total(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.type() == type)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<Transaction> sorted(java.util.stream.Stream<Transaction> stream) {
        return stream.sorted(Comparator.comparing(Transaction::date).thenComparingInt(Transaction::id))
                .toList();
    }
}
