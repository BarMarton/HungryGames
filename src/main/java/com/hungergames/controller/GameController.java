package com.hungergames.controller;

import com.hungergames.dto.BetResponse;
import com.hungergames.dto.GameResponse;
import com.hungergames.dto.NpcDto;
import com.hungergames.service.BetService;
import com.hungergames.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final BetService betService;

    @GetMapping
    public Page<GameResponse> listGames(
            @PageableDefault(size = 10) Pageable pageable) {
        return gameService.getGames(pageable);
    }

    @GetMapping("/current")
    public GameResponse getCurrentGame() {
        return gameService.getCurrentGame();
    }

    @GetMapping("/{id}")
    public GameResponse getGame(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @GetMapping("/{id}/npcs")
    public List<NpcDto> getNpcs(@PathVariable Long id) {
        return gameService.getNpcsForGame(id);
    }

    @GetMapping("/{id}/bets")
    public List<BetResponse> getBets(@PathVariable Long id) {
        return betService.getBetsByGame(id);
    }

    /**
     * Returns a map of npcId → odds multiplier.
     * Odds = totalPool / totalBetsOnNpc.
     * Odds of 0 means nobody has bet on that NPC yet.
     */
    @GetMapping("/{id}/odds")
    public Map<Long, Double> getOdds(@PathVariable Long id) {
        List<NpcDto> npcs = gameService.getNpcsForGame(id);
        Map<Long, Double> odds = new java.util.LinkedHashMap<>();
        for (NpcDto npc : npcs) {
            odds.put(npc.getId(), betService.getOdds(id, npc.getId()));
        }
        return odds;
    }
}
