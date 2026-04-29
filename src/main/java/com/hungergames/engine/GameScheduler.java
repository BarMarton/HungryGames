package com.hungergames.engine;

import com.hungergames.service.GameService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
@RequiredArgsConstructor
public class GameScheduler {

    private final GameService gameService;

    @Value("${game.betting-duration-seconds:5}")
    private int bettingDurationSeconds;

    @Value("${game.between-games-pause-seconds:30}")
    private int betweenGamesPauseSeconds;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-lifecycle");
                t.setDaemon(true);
                return t;
            });

    @PostConstruct
    public void init() {
        log.info("GameScheduler starting. Betting duration={}s, pause between games={}s",
                bettingDurationSeconds, betweenGamesPauseSeconds);
        startBettingPhase();
    }

    private void startBettingPhase() {
        try {
            gameService.openBettingPhase(bettingDurationSeconds);
            log.info("Betting phase started for {} seconds", bettingDurationSeconds);

            // Schedule game start after the betting window closes
            scheduler.schedule(this::startGame, bettingDurationSeconds, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Failed to start betting phase, retrying in 10 seconds: {}", e.getMessage(), e);
            scheduler.schedule(this::startBettingPhase, 10, TimeUnit.SECONDS);
        }
    }

    private void startGame() {
        try {
            gameService.startGame(this::onGameEnd);
            log.info("Game simulation started");
        } catch (Exception e) {
            log.error("Failed to start game, retrying in 10 seconds: {}", e.getMessage(), e);
            scheduler.schedule(this::startGame, 10, TimeUnit.SECONDS);
        }
    }

    private void onGameEnd(Long gameId, Long winnerNpcId) {
        try {
            gameService.finishGame(gameId, winnerNpcId);
            log.info("Game {} finished. Winner NPC: {}. Next betting phase in {} seconds",
                    gameId, winnerNpcId, betweenGamesPauseSeconds);
        } catch (Exception e) {
            log.error("Error finishing game {}: {}", gameId, e.getMessage(), e);
        }

        scheduler.schedule(this::startBettingPhase, betweenGamesPauseSeconds, TimeUnit.SECONDS);
    }
}
