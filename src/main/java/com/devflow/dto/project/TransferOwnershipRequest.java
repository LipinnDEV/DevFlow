package com.devflow.dto.project;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(
        @NotNull(message = "O id do novo proprietário é obrigatório")
        Long newOwnerId
) {
}
