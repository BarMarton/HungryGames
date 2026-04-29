package com.hungergames.controller;

import com.hungergames.dto.BetResponse;
import com.hungergames.dto.PlaceBetRequest;
import com.hungergames.repository.BetRepository;
import com.hungergames.service.BetService;
import com.hungergames.service.GameService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;
    private final GameService gameService;
    private final BetRepository betRepository;

    @PostMapping
    public ResponseEntity<?> placeBet(@Valid @RequestBody PlaceBetRequest request) {
        if (betRepository.existsByUserIdAndGameId(request.getUserId(), request.getGameId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ebben a körben már fogadtál!"));
        }

        BetResponse bet = betService.placeBet(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(bet);
    }

    @GetMapping("/has-bet")
    public ResponseEntity<Boolean> hasUserBetInCurrentGame(@RequestParam Long userId) {
        Long currentGameId = gameService.getCurrentGame().getId();
        boolean hasBet = betRepository.existsByUserIdAndGameId(userId, currentGameId);
        return ResponseEntity.ok(hasBet);
    }

    @GetMapping("/{id}")
    public BetResponse getBet(@PathVariable Long id) {
        return betService.getBet(id);
    }

    @GetMapping("/user/{userId}")
    public List<BetResponse> getBetsByUser(@PathVariable Long userId) {
        return betService.getBetsByUser(userId);
    }
}
