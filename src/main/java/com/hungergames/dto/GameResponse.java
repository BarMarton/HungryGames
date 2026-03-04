package com.hungergames.dto;

import com.hungergames.model.Game;
import com.hungergames.model.GameStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class GameResponse {

    private Long id;
    private GameStatus status;
    private Instant bettingStartedAt;
    private Instant bettingEndsAt;
    private Instant gameStartedAt;
    private Instant gameEndedAt;
    private double totalPool;
    private Long winnerNpcId;

    private List<NpcDto> npcs;

    public static GameResponse fromEntity(Game game) {
        GameResponse r = new GameResponse();
        r.id = game.getId();
        r.status = game.getStatus();
        r.bettingStartedAt = game.getBettingStartedAt();
        r.bettingEndsAt = game.getBettingEndsAt();
        r.gameStartedAt = game.getGameStartedAt();
        r.gameEndedAt = game.getGameEndedAt();
        r.totalPool = game.getTotalPool();
        r.winnerNpcId = game.getWinnerNpcId();
        return r;
    }
}
