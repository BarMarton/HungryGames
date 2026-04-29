package com.hungergames.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GameEventMessage {

    public enum EventType {
        COMBAT,   // an NPC attacked another
        DEATH,    // an NPC was killed
        MOVE,     // informational – only sent if no other events this tick
        GAME_OVER, // game finished
        WEAPON_PICKUP //fővette a puskát lacika
    }

    @Data
    @NoArgsConstructor
    public static class GameEvent {
        private EventType type;

        // COMBAT fields
        private Long attackerId;
        private String attackerName;
        private Long defenderId;
        private String defenderName;
        private int damage;
        private int defenderRemainingHp;

        // DEATH fields
        private Long deadNpcId;
        private String deadNpcName;
        private int killRank;          // 1 = first to die, N = last (winner)

        // GAME_OVER fields
        private Long winnerNpcId;
        private String winnerNpcName;
    }

    private String type = "EVENTS";
    private Long gameId;
    private long tick;
    private List<GameEvent> events;
}
