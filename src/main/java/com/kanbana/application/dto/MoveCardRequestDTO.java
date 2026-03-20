package com.kanbana.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class MoveCardRequestDTO {

    @NotNull(message = "Target column ID must not be null")
    private UUID targetColumnId;

    public MoveCardRequestDTO() {}

    public UUID getTargetColumnId() { return targetColumnId; }
    public void setTargetColumnId(UUID targetColumnId) { this.targetColumnId = targetColumnId; }
}
