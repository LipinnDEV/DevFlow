package com.devflow.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculo do percentual de conclusão de um projeto. */
public final class ProgressCalculator {

    private ProgressCalculator() {
    }

    /**
     * @param total     total de tarefas do projeto
     * @param completed tarefas concluídas
     * @return percentual de 0 a 100, com duas casas decimais (0 quando não há tarefas)
     */
    public static double percentage(long total, long completed) {
        if (total <= 0) {
            return 0.0;
        }
        double value = (completed * 100.0) / total;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
