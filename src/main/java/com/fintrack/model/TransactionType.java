package com.fintrack.model;

/** Tipo de lançamento financeiro. */
public enum TransactionType {
    INCOME("Receita"),
    EXPENSE("Despesa");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
