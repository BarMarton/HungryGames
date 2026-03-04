package com.hungergames.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceBetRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long gameId;

    @NotNull
    private Long npcId;

    @NotNull
    @Min(value = 1, message = "Minimum bet is 1")
    private Double amount;
}
