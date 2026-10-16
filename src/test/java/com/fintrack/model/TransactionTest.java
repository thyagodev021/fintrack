package com.fintrack.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TransactionTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);

    @Test
    void normalizaCategoriaDescricaoEValor() {
        Transaction t = new Transaction(1, DATE, TransactionType.EXPENSE,
                "  ALIMENTAÇÃO ", "  almoço \n  no centro ", new BigDecimal("10.5"));

        assertEquals("Alimentação", t.category());
        assertEquals("almoço no centro", t.description());
        assertEquals(new BigDecimal("10.50"), t.amount());
    }

    @Test
    void rejeitaValorZeroOuNegativo() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(1, DATE, TransactionType.INCOME, "Salário", "", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(1, DATE, TransactionType.INCOME, "Salário", "", new BigDecimal("-5")));
    }

    @Test
    void rejeitaCategoriaVazia() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(1, DATE, TransactionType.EXPENSE, "  ", "", BigDecimal.TEN));
    }
}
