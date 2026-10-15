package com.devflow.controller;

import com.devflow.dto.dashboard.DashboardResponse;
import com.devflow.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Estatísticas de projetos e tarefas")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Estatísticas de projetos, tarefas e produtividade dos usuários")
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard();
    }
}
