package com.fintrack;

import com.fintrack.cli.Cli;
import com.fintrack.repository.FileTransactionRepository;
import com.fintrack.service.FinanceService;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;

/** Ponto de entrada: monta as peças (repositório -> serviço -> CLI) e executa. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Path dataFile = resolveDataFile();
        FinanceService service = new FinanceService(new FileTransactionRepository(dataFile));

        // Força UTF-8 na saída para exibir acentos e a barra do gráfico corretamente.
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);

        System.exit(new Cli(service, out, err, Clock.systemDefaultZone()).run(args));
    }

    private static Path resolveDataFile() {
        String custom = System.getenv("FINTRACK_FILE");
        if (custom != null && !custom.isBlank()) {
            return Path.of(custom);
        }
        return Path.of(System.getProperty("user.home"), ".fintrack", "transactions.csv");
    }
}
