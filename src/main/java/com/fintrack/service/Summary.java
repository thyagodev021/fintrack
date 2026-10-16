package com.fintrack.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/** Resumo financeiro de um mês. */
public record Summary(
        YearMonth month,
        BigDecimal income,
        BigDecimal expense,
        Map<String, BigDecimal> expenseByCategory) {

    public BigDecimal balance() {
        return income.subtract(expense);
    }
}
