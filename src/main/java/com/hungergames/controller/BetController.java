package com.hungergames.controller;

import com.hungergames.dto.BetResponse;
import com.hungergames.dto.PlaceBetRequest;
import com.hungergames.service.BetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;

    @PostMapping
    public ResponseEntity<BetResponse> placeBet(@Valid @RequestBody PlaceBetRequest request) {
        BetResponse bet = betService.placeBet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bet);
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
