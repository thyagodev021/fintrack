package com.fintrack.repository;

import com.fintrack.model.Transaction;
import com.fintrack.model.TransactionType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Persiste os lançamentos em um arquivo CSV (UTF-8). */
public final class FileTransactionRepository implements TransactionRepository {

    private static final String HEADER = "id,date,type,category,description,amount";

    private final Path file;

    public FileTransactionRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Transaction> findAll() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.skip(1) // pula o cabeçalho
                    .filter(line -> !line.isBlank())
                    .map(this::parse)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + file, e);
        }
    }

    @Override
    public void saveAll(List<Transaction> transactions) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);

            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            transactions.stream().map(this::format).forEach(lines::add);

            // Grava em arquivo temporário e troca no final: evita corromper
            // os dados se o programa for interrompido no meio da escrita.
            Path temp = Files.createTempFile(dir, "fintrack", ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar " + file, e);
        }
    }

    private String format(Transaction t) {
        return CsvUtil.join(List.of(
                String.valueOf(t.id()),
                t.date().toString(),
                t.type().name(),
                t.category(),
                t.description(),
                t.amount().toPlainString()));
    }

    private Transaction parse(String line) {
        List<String> f = CsvUtil.split(line);
        if (f.size() != 6) {
            throw new IllegalStateException("Linha inválida no arquivo de dados: " + line);
        }
        return new Transaction(
                Integer.parseInt(f.get(0)),
                LocalDate.parse(f.get(1)),
                TransactionType.valueOf(f.get(2)),
                f.get(3),
                f.get(4),
                new BigDecimal(f.get(5)));
    }
}
