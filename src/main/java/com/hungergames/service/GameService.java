package com.hungergames.service;

import com.hungergames.dto.GameResponse;
import com.hungergames.dto.GameStatusMessage;
import com.hungergames.dto.NpcDto;
import com.hungergames.engine.GameEngine;
import com.hungergames.engine.LiveNpc;
import com.hungergames.model.Game;
import com.hungergames.model.GameStatus;
import com.hungergames.model.NPC;
import com.hungergames.repository.GameRepository;
import com.hungergames.repository.NPCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final NPCRepository npcRepository;
    private final BetService betService;
    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messaging;

    @Value("${game.npc-count:10}")
    private int npcCount;

    @Value("${game.grid-size:100}")
    private int gridSize;


    @Transactional
    public Game openBettingPhase(int bettingDurationSeconds) {
        Game game = new Game();
        game.setStatus(GameStatus.BETTING);
        game.setBettingStartedAt(Instant.now());
        game.setBettingEndsAt(Instant.now().plusSeconds(bettingDurationSeconds));
        game = gameRepository.save(game);

        List<NPC> npcs = spawnNpcs(game);
        log.info("Opened betting for game {} with {} NPCs. Betting closes at {}",
                game.getId(), npcs.size(), game.getBettingEndsAt());

        broadcastStatus(game, null, null);
        return game;
    }


    @Transactional
    public void startGame(BiConsumer<Long, Long> onGameEnd) {
        System.out.println("Elkeztdődött a kecskesex");
        Game game = gameRepository.findTopByStatusOrderByIdDesc(GameStatus.BETTING)
                .orElseThrow(() -> new IllegalStateException("No game in BETTING state found"));

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setGameStartedAt(Instant.now());
        game = gameRepository.save(game);

        List<NPC> npcs = npcRepository.findByGameId(game.getId());
        log.info("Starting simulation for game {} with {} NPCs", game.getId(), npcs.size());

        broadcastStatus(game, null, null);
        gameEngine.startGame(game, npcs, onGameEnd);
    }


    @Transactional
    public void finishGame(Long gameId, Long winnerNpcId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Game not found: " + gameId));

        game.setStatus(GameStatus.FINISHED);
        game.setGameEndedAt(Instant.now());
        game.setWinnerNpcId(winnerNpcId);
        gameRepository.save(game);

        Map<Long, LiveNpc> finalStates = gameEngine.getFinalNpcStates();
        List<NPC> npcs = npcRepository.findByGameId(gameId);
        int totalNpcs = npcs.size();

        for (NPC npc : npcs) {
            LiveNpc live = finalStates.get(npc.getId());
            if (live != null) {
                npc.setFinalHp(live.getCurrentHp());
                npc.setFinalX(live.getX());
                npc.setFinalY(live.getY());
                npc.setAlive(live.isAlive());
                if (npc.getId().equals(winnerNpcId)) {
                    npc.setFinishPosition(totalNpcs);
                }
            }
            npcRepository.save(npc);
        }

        betService.processPayouts(gameId, winnerNpcId);

        String winnerName = npcs.stream()
                .filter(n -> n.getId().equals(winnerNpcId))
                .map(NPC::getName)
                .findFirst().orElse("Unknown");

        broadcastStatus(game, winnerNpcId, winnerName);
        log.info("Game {} finished. Winner: {} ({})", gameId, winnerName, winnerNpcId);
    }


    // @Transactional(readOnly = true)
    // public GameResponse getCurrentGame() {
    //     Optional<Game> inProgress = gameRepository.findTopByStatusOrderByIdDesc(GameStatus.IN_PROGRESS);
    //     if (inProgress.isPresent()) {
    //         return buildGameResponse(inProgress.get(), true);
    //     }
    //     Optional<Game> betting = gameRepository.findTopByStatusOrderByIdDesc(GameStatus.BETTING);
    //     if (betting.isPresent()) {
    //         return buildGameResponse(betting.get(), false);
    //     }
    //     throw new NoSuchElementException("No active game found");
    // }

    @Transactional(readOnly = true)
    public GameResponse getCurrentGame() {
        Game latestGame = gameRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new NoSuchElementException("No active game found"));

        return buildGameResponse(latestGame, latestGame.getStatus() == GameStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Game not found: " + id));
        return buildGameResponse(game, game.getStatus() == GameStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public Page<GameResponse> getGames(Pageable pageable) {
        return gameRepository.findAllByOrderByIdDesc(pageable)
                .map(g -> buildGameResponse(g, false));
    }

    @Transactional(readOnly = true)
    public List<NpcDto> getNpcsForGame(Long gameId) {
        if (gameEngine.isRunning()) {
            Optional<Game> live = gameRepository.findTopByStatusOrderByIdDesc(GameStatus.IN_PROGRESS);
            if (live.isPresent() && live.get().getId().equals(gameId)) {
                return gameEngine.getCurrentNpcStates();
            }
        }
        return npcRepository.findByGameId(gameId).stream()
                .map(NpcDto::fromEntity)
                .collect(Collectors.toList());
    }

    private List<NPC> spawnNpcs(Game game) {
        Random rng = new Random();
        Set<String> usedPositions = new HashSet<>();
        List<NPC> npcs = new ArrayList<>();

        String[] names = {"minecraft makka", "Kovács Dániel OwO", "Kocsán László", "Barabás Márton von Marci", "Téapó","Milef when sees this", "Dragon sex", "Lakatos Radiátor"};

        for (int i = 0; i < npcCount; i++) {
            String posKey;
            int x, y;
            do {
                x = rng.nextInt(gridSize);
                y = rng.nextInt(gridSize);
                posKey = x + "," + y;
            } while (usedPositions.contains(posKey));
            usedPositions.add(posKey);

            NPC npc = new NPC();
            npc.setGame(game);
            npc.setName(names[i]);
            npc.setMaxHp(50 + rng.nextInt(101));
            npc.setDmg(5 + rng.nextInt(26));
            npc.setSpeed(1 + rng.nextInt(10));
            npc.setFinalHp(npc.getMaxHp());
            npc.setFinalX(x);
            npc.setFinalY(y);
            npc.setAlive(true);
            npc.setPicId(i+1);
            npcs.add(npcRepository.save(npc));
        }

        return npcs;
    }


    private GameResponse buildGameResponse(Game game, boolean liveNpcs) {
        GameResponse resp = GameResponse.fromEntity(game);

        List<NpcDto> npcDtos;
        if (liveNpcs && gameEngine.isRunning()) {
            npcDtos = gameEngine.getCurrentNpcStates();
        } else {
            npcDtos = npcRepository.findByGameId(game.getId()).stream()
                    .map(NpcDto::fromEntity)
                    .collect(Collectors.toList());
        }
        resp.setNpcs(npcDtos);
        return resp;
    }

    private void broadcastStatus(Game game, Long winnerNpcId, String winnerNpcName) {
        GameStatusMessage msg = new GameStatusMessage(
                "STATUS",
                game.getId(),
                game.getStatus(),
                game.getBettingEndsAt(),
                winnerNpcId,
                winnerNpcName
        );
        messaging.convertAndSend("/topic/game.status", msg);
    }
}
