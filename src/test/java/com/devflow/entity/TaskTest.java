package com.devflow.entity;

import com.devflow.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    @Test
    @DisplayName("Tarefa com prazo vencido e não concluída está atrasada")
    void prazoVencidoENaoConcluidaEstaAtrasada() {
        Task task = Task.builder().status(TaskStatus.EM_ANDAMENTO).deadline(LocalDate.now().minusDays(1)).build();
        assertThat(task.isOverdue()).isTrue();
    }

    @Test
    @DisplayName("Tarefa concluída nunca está atrasada")
    void tarefaConcluidaNaoEstaAtrasada() {
        Task task = Task.builder().status(TaskStatus.CONCLUIDA).deadline(LocalDate.now().minusDays(10)).build();
        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("Tarefa sem prazo não está atrasada")
    void semPrazoNaoEstaAtrasada() {
        Task task = Task.builder().status(TaskStatus.TODO).build();
        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("Tarefa com prazo hoje ou no futuro não está atrasada")
    void prazoFuturoNaoEstaAtrasada() {
        Task hoje = Task.builder().status(TaskStatus.TODO).deadline(LocalDate.now()).build();
        Task futuro = Task.builder().status(TaskStatus.TODO).deadline(LocalDate.now().plusDays(3)).build();
        assertThat(hoje.isOverdue()).isFalse();
        assertThat(futuro.isOverdue()).isFalse();
    }
}
