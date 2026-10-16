package com.fintrack.repository;

import com.fintrack.model.Transaction;
import java.util.List;

/** Contrato de persistência. Permite trocar CSV por banco de dados no futuro. */
public interface TransactionRepository {

    /** Retorna uma lista mutável com todos os lançamentos. */
    List<Transaction> findAll();

    /** Substitui todos os lançamentos salvos pela lista informada. */
    void saveAll(List<Transaction> transactions);
}
