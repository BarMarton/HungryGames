package com.hungergames.dto;

import com.hungergames.model.NPC;
import lombok.Data;


@Data
public class NpcDto {

    private Long id;
    private String name;
    private Long gameId;

    // Stats
    private int maxHp;
    private int currentHp;
    private int dmg;
    private int speed;

    // Position
    private int x;
    private int y;

    private boolean alive;
    private Integer finishPosition;

    public static NpcDto fromEntity(NPC npc) {
        NpcDto dto = new NpcDto();
        dto.id = npc.getId();
        dto.name = npc.getName();
        dto.gameId = npc.getGame().getId();
        dto.maxHp = npc.getMaxHp();
        dto.currentHp = npc.getFinalHp();
        dto.dmg = npc.getDmg();
        dto.speed = npc.getSpeed();
        dto.x = npc.getFinalX();
        dto.y = npc.getFinalY();
        dto.alive = npc.isAlive();
        dto.finishPosition = npc.getFinishPosition();
        return dto;
    }
}
