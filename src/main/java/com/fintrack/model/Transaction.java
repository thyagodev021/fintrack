package com.fintrack.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Objects;

/**
 * Um lançamento financeiro (receita ou despesa).
 *
 * <p>É um {@code record}: imutável, com equals/hashCode/toString automáticos.
 * O construtor compacto valida e normaliza os dados na criação.
 */
public record Transaction(
        int id,
        LocalDate date,
        TransactionType type,
        String category,
        String description,
        BigDecimal amount) {

    public Transaction {
        Objects.requireNonNull(date, "A data é obrigatória.");
        Objects.requireNonNull(type, "O tipo é obrigatório.");
        Objects.requireNonNull(amount, "O valor é obrigatório.");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }

        // Normalização: "ALIMENTAÇÃO " e "alimentação" viram a mesma categoria.
        String c = category.strip();
        category = c.substring(0, 1).toUpperCase(Locale.ROOT) + c.substring(1).toLowerCase(Locale.ROOT);
        // Remove quebras de linha para não corromper o arquivo CSV.
        description = description == null ? "" : description.strip().replaceAll("\\s+", " ");
        // Dinheiro sempre com 2 casas decimais.
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public YearMonth month() {
        return YearMonth.from(date);
    }
}
