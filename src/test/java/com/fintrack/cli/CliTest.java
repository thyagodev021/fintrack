package com.fintrack.cli;

import static org.junit.jupiter.api.Assertions.*;

import com.fintrack.repository.FileTransactionRepository;
import com.fintrack.service.FinanceService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outBuffer;
    private ByteArrayOutputStream errBuffer;
    private Cli cli;

    @BeforeEach
    void setUp() {
        outBuffer = new ByteArrayOutputStream();
        errBuffer = new ByteArrayOutputStream();
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
        FinanceService service = new FinanceService(new FileTransactionRepository(tempDir.resolve("t.csv")));
        cli = new Cli(service,
                new PrintStream(outBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errBuffer, true, StandardCharsets.UTF_8),
                clock);
    }

    private String out() {
        return outBuffer.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBuffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void fluxoCompletoAdicionarListarResumirRemover() {
        assertEquals(0, cli.run(new String[]{"add", "receita", "3.000,00", "Salário"}));
        assertEquals(0, cli.run(new String[]{"add", "despesa", "45,90", "Alimentação", "Almoço", "no", "centro"}));

        outBuffer.reset();
        assertEquals(0, cli.run(new String[]{"list"}));
        assertTrue(out().contains("Almoço no centro"));
        assertTrue(out().contains("15/10/2026")); // data padrão = hoje (relógio fixo)

        outBuffer.reset();
        assertEquals(0, cli.run(new String[]{"summary"}));
        assertTrue(out().contains("Resumo de 10/2026"));
        assertTrue(out().contains("2.954,10")); // saldo = 3000 - 45,90
        assertTrue(out().contains("100%"));

        assertEquals(0, cli.run(new String[]{"remove", "2"}));
        assertEquals(1, cli.run(new String[]{"remove", "2"}));
    }

    @Test
    void entradasInvalidasRetornamErroSemQuebrar() {
        assertEquals(1, cli.run(new String[]{"add", "despesa", "abc", "Lazer"}));
        assertTrue(err().contains("Valor inválido"));

        assertEquals(1, cli.run(new String[]{"add", "despesa", "10", "Lazer", "--date", "31/12/2026"}));
        assertEquals(1, cli.run(new String[]{"summary", "--month", "outubro"}));
        assertEquals(1, cli.run(new String[]{"voar"}));
    }

    @Test
    void parseMoneyAceitaFormatosBrasileirosEInternacionais() {
        assertEquals(new BigDecimal("1234.56"), Cli.parseMoney("1.234,56"));
        assertEquals(new BigDecimal("45.90"), Cli.parseMoney("45,90"));
        assertEquals(new BigDecimal("45.90"), Cli.parseMoney("45.90"));
    }
}
