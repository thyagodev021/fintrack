package com.fintrack.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fintrack.model.Transaction;
import com.fintrack.model.TransactionType;
import com.fintrack.repository.FileTransactionRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FinanceServiceTest {

    @TempDir
    Path tempDir;

    private Path file;
    private FinanceService service;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("dados").resolve("transactions.csv");
        service = new FinanceService(new FileTransactionRepository(file));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Test
    void geraIdsSequenciaisEPersisteEntreInstancias() {
        service.add(TransactionType.INCOME, money("3000"), "Salário", "", LocalDate.of(2026, 10, 5));
        Transaction second = service.add(TransactionType.EXPENSE, money("50"), "Lazer", "Cinema, pipoca",
                LocalDate.of(2026, 10, 6));

        assertEquals(2, second.id());

        // Nova instância lendo o mesmo arquivo: dados devem ter sido salvos.
        FinanceService other = new FinanceService(new FileTransactionRepository(file));
        List<Transaction> all = other.list();
        assertEquals(2, all.size());
        assertEquals("Cinema, pipoca", all.get(1).description());
    }

    @Test
    void resumoCalculaTotaisECategoriasOrdenadas() {
        LocalDate d = LocalDate.of(2026, 10, 10);
        service.add(TransactionType.INCOME, money("3000"), "Salário", "", d);
        service.add(TransactionType.EXPENSE, money("100"), "Lazer", "", d);
        service.add(TransactionType.EXPENSE, money("400"), "Moradia", "", d);
        service.add(TransactionType.EXPENSE, money("50.50"), "Lazer", "", d);
        // Outro mês: não deve entrar no resumo de outubro.
        service.add(TransactionType.EXPENSE, money("999"), "Moradia", "", LocalDate.of(2026, 9, 30));

        Summary s = service.summarize(YearMonth.of(2026, 10));

        assertEquals(money("3000.00"), s.income());
        assertEquals(money("550.50"), s.expense());
        assertEquals(money("2449.50"), s.balance());
        assertEquals(List.of("Moradia", "Lazer"), List.copyOf(s.expenseByCategory().keySet()));
        assertEquals(money("150.50"), s.expenseByCategory().get("Lazer"));
    }

    @Test
    void removeExistenteERetornaFalseParaInexistente() {
        Transaction t = service.add(TransactionType.EXPENSE, money("10"), "Lazer", "", LocalDate.of(2026, 10, 1));

        assertTrue(service.remove(t.id()));
        assertFalse(service.remove(t.id()));
        assertTrue(service.list().isEmpty());
    }

    @Test
    void listaPorMesFiltraECadastraEmOrdemDeData() {
        service.add(TransactionType.EXPENSE, money("10"), "A", "", LocalDate.of(2026, 10, 20));
        service.add(TransactionType.EXPENSE, money("20"), "B", "", LocalDate.of(2026, 10, 2));
        service.add(TransactionType.EXPENSE, money("30"), "C", "", LocalDate.of(2026, 11, 1));

        List<Transaction> october = service.list(YearMonth.of(2026, 10));

        assertEquals(2, october.size());
        assertEquals("B", october.get(0).category());
    }
}
