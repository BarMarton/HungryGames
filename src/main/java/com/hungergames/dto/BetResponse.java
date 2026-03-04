package com.hungergames.dto;

import com.hungergames.model.Bet;
import lombok.Data;

import java.time.Instant;

@Data
public class BetResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long gameId;
    private Long npcId;
    private String npcName;
    private double amount;
    private Double payout;
    private boolean settled;
    private Instant placedAt;

    public static BetResponse fromEntity(Bet bet) {
        BetResponse r = new BetResponse();
        r.id = bet.getId();
        r.userId = bet.getUser().getId();
        r.username = bet.getUser().getUsername();
        r.gameId = bet.getGame().getId();
        r.npcId = bet.getNpc().getId();
        r.npcName = bet.getNpc().getName();
        r.amount = bet.getAmount();
        r.payout = bet.getPayout();
        r.settled = bet.isSettled();
        r.placedAt = bet.getPlacedAt();
        return r;
    }
}
