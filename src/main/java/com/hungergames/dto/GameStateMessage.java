package com.hungergames.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateMessage {

    private String type = "STATE";
    private Long gameId;
    private long tick;
    private int aliveCount;
    private List<NpcDto> npcs;
}
