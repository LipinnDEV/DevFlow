package com.devflow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressCalculatorTest {

    @ParameterizedTest(name = "{1} de {0} tarefas concluídas = {2}%")
    @CsvSource({
            "0, 0, 0.0",
            "4, 0, 0.0",
            "4, 1, 25.0",
            "3, 1, 33.33",
            "3, 2, 66.67",
            "2, 2, 100.0"
    })
    @DisplayName("Deve calcular o percentual de conclusão com duas casas decimais")
    void deveCalcularPercentual(long total, long concluidas, double esperado) {
        assertThat(ProgressCalculator.percentage(total, concluidas)).isEqualTo(esperado);
    }
}
