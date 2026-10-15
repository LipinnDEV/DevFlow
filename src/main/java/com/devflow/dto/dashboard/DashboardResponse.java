package com.devflow.dto.dashboard;

import com.devflow.enums.TaskPriority;
import com.devflow.enums.TaskStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalProjects,
        long completedProjects,
        long inProgressProjects,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        Map<TaskPriority, Long> tasksByPriority,
        Map<TaskStatus, Long> tasksByStatus,
        List<UserProductivityResponse> userProductivity
) {

    public static DashboardResponse empty() {
        return new DashboardResponse(0, 0, 0, 0, 0, 0,
                zeroedCounts(TaskPriority.class),
                zeroedCounts(TaskStatus.class),
                List.of());
    }

    /** Cria um mapa com todos os valores do enum zerados, para o JSON sempre listar todas as categorias. */
    public static <E extends Enum<E>> Map<E, Long> zeroedCounts(Class<E> type) {
        Map<E, Long> counts = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            counts.put(value, 0L);
        }
        return counts;
    }
}
