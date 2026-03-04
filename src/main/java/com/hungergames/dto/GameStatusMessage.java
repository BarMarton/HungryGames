package com.hungergames.dto;

import com.hungergames.model.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStatusMessage {

    private String type = "STATUS";
    private Long gameId;
    private GameStatus status;
    private Instant bettingEndsAt;   // set during BETTING phase
    private Long winnerNpcId;        // set when FINISHED
    private String winnerNpcName;    // set when FINISHED
}
