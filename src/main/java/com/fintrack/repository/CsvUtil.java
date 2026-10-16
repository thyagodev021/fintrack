package com.fintrack.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Utilitário mínimo de CSV com suporte a aspas e vírgulas dentro dos campos. */
public final class CsvUtil {

    private CsvUtil() {}

    /** Junta campos em uma linha CSV, sempre entre aspas ("" escapa aspas internas). */
    public static String join(List<String> fields) {
        return fields.stream()
                .map(f -> "\"" + f.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    /** Separa uma linha CSV em campos, respeitando aspas. */
    public static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"'); // aspas escapadas
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
