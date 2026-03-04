package com.hungergames.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "games")
@Getter @Setter @NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.BETTING;

    @Column(name = "betting_started_at")
    private Instant bettingStartedAt;

    @Column(name = "betting_ends_at")
    private Instant bettingEndsAt;

    @Column(name = "game_started_at")
    private Instant gameStartedAt;

    @Column(name = "game_ended_at")
    private Instant gameEndedAt;

    @Column(name = "total_pool", nullable = false)
    private double totalPool = 0.0;

    @Column(name = "winner_npc_id")
    private Long winnerNpcId;
}
