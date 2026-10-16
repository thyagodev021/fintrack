package com.fintrack.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvUtilTest {

    @Test
    void idaEVoltaPreservaVirgulasEAspas() {
        List<String> original = List.of("1", "Almoço, com \"amigos\"", "", "45.90");
        assertEquals(original, CsvUtil.split(CsvUtil.join(original)));
    }

    @Test
    void campoVazioNoFinalEPreservado() {
        assertEquals(List.of("a", "b", ""), CsvUtil.split("a,b,"));
    }
}
