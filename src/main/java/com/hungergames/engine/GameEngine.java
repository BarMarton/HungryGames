package com.hungergames.engine;

import com.hungergames.dto.GameEventMessage;
import com.hungergames.dto.GameEventMessage.GameEvent;
import com.hungergames.dto.GameStateMessage;
import com.hungergames.dto.GameStatusMessage;
import com.hungergames.dto.NpcDto;
import com.hungergames.model.Game;
import com.hungergames.model.GameStatus;
import com.hungergames.model.NPC;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GameEngine {


    private static final int[] DX = {0, 0, -1, 1};   // NESW column deltas
    private static final int[] DY = {-1, 1, 0, 0};   // NESW row deltas


    private final SimpMessagingTemplate messaging;
    private final Random rng = new Random();

    @Value("${game.grid-size:100}")
    private int gridSize;

    @Value("${game.tick-interval-ms:200}")
    private int tickIntervalMs;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickFuture;

    private volatile Long currentGameId;
    private volatile List<LiveNpc> npcs;
    private volatile long tickCounter = 0;
    private volatile boolean running = false;

    private BiConsumer<Long, Long> onGameEnd;

    private volatile int deathRank = 0;

    private final Map<Long, LiveNpc> finalNpcStates = new ConcurrentHashMap<>();


    public GameEngine(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }



    public synchronized void startGame(Game game, List<NPC> dbNpcs,
                                       BiConsumer<Long, Long> onGameEnd) {
        if (running) {
            throw new IllegalStateException("A game is already running");
        }

        this.currentGameId = game.getId();
        this.onGameEnd = onGameEnd;
        this.tickCounter = 0;
        this.deathRank = 0;
        this.finalNpcStates.clear();

        List<LiveNpc> liveNpcs = new ArrayList<>();
        for (NPC npc : dbNpcs) {
            LiveNpc live = new LiveNpc(
                    npc.getId(), npc.getName(),
                    npc.getMaxHp(), npc.getDmg(), npc.getSpeed(),
                    npc.getFinalX(), npc.getFinalY()
            );
            liveNpcs.add(live);
        }
        this.npcs = liveNpcs;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-engine-" + game.getId());
            t.setDaemon(true);
            return t;
        });

        running = true;
        tickFuture = scheduler.scheduleAtFixedRate(
                this::tick, 0, tickIntervalMs, TimeUnit.MILLISECONDS);

        log.info("Game {} started with {} NPCs on a {}x{} grid, tick={}ms",
                game.getId(), dbNpcs.size(), gridSize, gridSize, tickIntervalMs);
    }

    public List<NpcDto> getCurrentNpcStates() {
        List<LiveNpc> snapshot = npcs;
        if (snapshot == null) return List.of();
        return snapshot.stream().map(this::toDto).collect(Collectors.toList());
    }

    public boolean isRunning() { return running; }

    private void tick() {
        try {
            tickCounter++;
            List<GameEvent> tickEvents = new ArrayList<>();

            synchronized (this) {
                for (LiveNpc npc : npcs) {
                    if (npc.isAlive() && npc.shouldMoveOnTick(tickCounter)) {
                        moveRandomly(npc);
                    }
                }

                resolveCombat(tickEvents);

                List<LiveNpc> alive = npcs.stream().filter(LiveNpc::isAlive).toList();
                if (alive.size() <= 1) {
                    Long winnerId = alive.isEmpty() ? null : alive.get(0).getId();

                    GameEvent over = new GameEvent();
                    over.setType(GameEventMessage.EventType.GAME_OVER);
                    over.setWinnerNpcId(winnerId);
                    if (winnerId != null) {
                        alive.get(0).setAlive(true);
                        over.setWinnerNpcName(alive.get(0).getName());
                        finalNpcStates.put(winnerId, alive.get(0));
                    }
                    tickEvents.add(over);

                    broadcastState();
                    broadcastEvents(tickEvents);
                    endGame(winnerId);
                    return;
                }
            }

            broadcastState();

            if (!tickEvents.isEmpty()) {
                broadcastEvents(tickEvents);
            }

        } catch (Exception e) {
            log.error("Error in game tick: {}", e.getMessage(), e);
        }
    }

    private void moveRandomly(LiveNpc npc) {
        int dir = rng.nextInt(4);
        int nx = npc.getX() + DX[dir];
        int ny = npc.getY() + DY[dir];
        // Clamp to grid
        npc.setX(Math.max(0, Math.min(gridSize - 1, nx)));
        npc.setY(Math.max(0, Math.min(gridSize - 1, ny)));
    }


    private void resolveCombat(List<GameEvent> events) {
        Map<Long, List<LiveNpc>> cellMap = new HashMap<>();
        for (LiveNpc npc : npcs) {
            if (!npc.isAlive()) continue;
            long cellKey = (long) npc.getX() * gridSize + npc.getY();
            cellMap.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(npc);
        }

        for (List<LiveNpc> combatants : cellMap.values()) {
            if (combatants.size() < 2) continue;

            combatants.sort(Comparator.comparingInt(LiveNpc::getSpeed).reversed());

            while (countAlive(combatants) > 1) {
                LiveNpc attacker = firstAlive(combatants);
                LiveNpc defender = secondAlive(combatants);
                if (attacker == null || defender == null) break;

                int dmg = attacker.getDmg();
                defender.takeDamage(dmg);

                GameEvent combatEvent = new GameEvent();
                combatEvent.setType(GameEventMessage.EventType.COMBAT);
                combatEvent.setAttackerId(attacker.getId());
                combatEvent.setAttackerName(attacker.getName());
                combatEvent.setDefenderId(defender.getId());
                combatEvent.setDefenderName(defender.getName());
                combatEvent.setDamage(dmg);
                combatEvent.setDefenderRemainingHp(defender.getCurrentHp());
                events.add(combatEvent);

                if (!defender.isAlive()) {
                    recordDeath(defender, events);
                    continue;
                }

                int retaliationDmg = defender.getDmg();
                attacker.takeDamage(retaliationDmg);

                GameEvent retEvent = new GameEvent();
                retEvent.setType(GameEventMessage.EventType.COMBAT);
                retEvent.setAttackerId(defender.getId());
                retEvent.setAttackerName(defender.getName());
                retEvent.setDefenderId(attacker.getId());
                retEvent.setDefenderName(attacker.getName());
                retEvent.setDamage(retaliationDmg);
                retEvent.setDefenderRemainingHp(attacker.getCurrentHp());
                events.add(retEvent);

                if (!attacker.isAlive()) {
                    recordDeath(attacker, events);
                }
            }
        }
    }

    private void recordDeath(LiveNpc npc, List<GameEvent> events) {
        deathRank++;
        finalNpcStates.put(npc.getId(), npc);

        GameEvent death = new GameEvent();
        death.setType(GameEventMessage.EventType.DEATH);
        death.setDeadNpcId(npc.getId());
        death.setDeadNpcName(npc.getName());
        death.setKillRank(deathRank);
        events.add(death);

        log.debug("NPC {} died (rank {})", npc.getName(), deathRank);
    }

    private int countAlive(List<LiveNpc> list) {
        return (int) list.stream().filter(LiveNpc::isAlive).count();
    }

    private LiveNpc firstAlive(List<LiveNpc> sorted) {
        return sorted.stream().filter(LiveNpc::isAlive).findFirst().orElse(null);
    }

    private LiveNpc secondAlive(List<LiveNpc> sorted) {
        return sorted.stream().filter(LiveNpc::isAlive).skip(1).findFirst().orElse(null);
    }

    private void broadcastState() {
        List<NpcDto> dtos = npcs.stream().map(this::toDto).collect(Collectors.toList());
        long alive = dtos.stream().filter(NpcDto::isAlive).count();
        GameStateMessage msg = new GameStateMessage("STATE", currentGameId, tickCounter, (int) alive, dtos);
        messaging.convertAndSend("/topic/game.state", msg);
    }

    private void broadcastEvents(List<GameEvent> events) {
        GameEventMessage msg = new GameEventMessage();
        msg.setGameId(currentGameId);
        msg.setTick(tickCounter);
        msg.setEvents(events);
        messaging.convertAndSend("/topic/game.events", msg);
    }


    private void endGame(Long winnerId) {
        running = false;
        if (tickFuture != null) tickFuture.cancel(false);
        if (scheduler != null) scheduler.shutdown();

        log.info("Game {} ended. Winner NPC id={}", currentGameId, winnerId);

        Long gameId = currentGameId;
        new Thread(() -> onGameEnd.accept(gameId, winnerId), "game-end-callback").start();
    }

    private NpcDto toDto(LiveNpc npc) {
        NpcDto dto = new NpcDto();
        dto.setId(npc.getId());
        dto.setName(npc.getName());
        dto.setGameId(currentGameId);
        dto.setMaxHp(npc.getMaxHp());
        dto.setCurrentHp(npc.getCurrentHp());
        dto.setDmg(npc.getDmg());
        dto.setSpeed(npc.getSpeed());
        dto.setX(npc.getX());
        dto.setY(npc.getY());
        dto.setAlive(npc.isAlive());
        return dto;
    }

    public Map<Long, LiveNpc> getFinalNpcStates() {
        return Collections.unmodifiableMap(finalNpcStates);
    }
}
