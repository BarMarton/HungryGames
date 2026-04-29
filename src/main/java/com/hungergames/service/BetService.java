package com.hungergames.service;

import com.hungergames.dto.BetResponse;
import com.hungergames.dto.PlaceBetRequest;
import com.hungergames.model.*;
import com.hungergames.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetService {

    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final NPCRepository npcRepository;
    private final UserService userService;

    @Transactional
    public BetResponse placeBet(PlaceBetRequest req) {
        Game game = gameRepository.findById(req.getGameId())
                .orElseThrow(() -> new NoSuchElementException("Game not found: " + req.getGameId()));

        if (game.getStatus() != GameStatus.BETTING) {
            throw new IllegalStateException("Bets can only be placed while the game is in the BETTING phase");
        }

        NPC npc = npcRepository.findByGameId(game.getId())
        .stream()
        .filter(n -> n.getPicId() != null && n.getPicId().equals(req.getPicId()))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("NPC not found for picId: " + req.getPicId()));

        if (!npc.getGame().getId().equals(game.getId())) {
            throw new IllegalArgumentException("NPC " + req.getPicId() + " does not belong to game " + req.getGameId());
        }

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUserId()));

        if (user.getBalance() < req.getAmount()) {
            throw new IllegalStateException("Insufficient balance: have " + user.getBalance()
                    + ", need " + req.getAmount());
        }

        userService.deductBalance(user.getId(), req.getAmount());

        game.setTotalPool(game.getTotalPool() + req.getAmount());
        gameRepository.save(game);

        Bet bet = new Bet();
        bet.setUser(user);
        bet.setGame(game);
        bet.setNpc(npc);
        bet.setAmount(req.getAmount());
        bet = betRepository.save(bet);

        log.info("Bet placed: user={} game={} npc={} amount={}", user.getUsername(), game.getId(), npc.getName(), req.getAmount());
        return BetResponse.fromEntity(bet);
    }

    @Transactional
    public void processPayouts(Long gameId, Long winnerNpcId) {
        List<Bet> allBets = betRepository.findByGameId(gameId);

        log.info("=== PAYOUT START ===");
        log.info("GameId: {}, WinnerNpcId: {}", gameId, winnerNpcId);

        log.info("Total bets: {}", allBets.size());

        if (allBets.isEmpty()) {
            log.info("Game {} had no bets. No payouts.", gameId);
            return;
        }

        double totalPool = betRepository.sumAmountByGameId(gameId);
        double totalBetsOnWinner = winnerNpcId != null
                ? betRepository.sumAmountByGameIdAndNpcId(gameId, winnerNpcId)
                : 0.0;
        
        log.info("TotalPool: {}", totalPool);
        log.info("TotalBetsOnWinner: {}", totalBetsOnWinner);

        for (Bet bet : allBets) {

            log.info("Checking bet: user={}, npcId={}, amount={}",
                bet.getUser().getId(),
                bet.getNpc().getId(),
                bet.getAmount()
            );
            double payout = 0.0;
            if (winnerNpcId != null
                    && bet.getNpc().getId().equals(winnerNpcId)
                    && totalBetsOnWinner > 0) {
                payout = ((bet.getAmount() / totalBetsOnWinner) * totalPool)*1.05;

                log.info("WINNER BET! Calculated payout: {}", payout);
            }
            bet.setPayout(payout);
            bet.setSettled(true);
            betRepository.save(bet);

            if (payout > 0) {
                log.info("ADDING BALANCE to user {} amount {}", bet.getUser().getId(), payout);
                userService.addBalance(bet.getUser().getId(), payout);
                log.info("Payout: user={} amount={} (bet {} on winner NPC {})",
                        bet.getUser().getUsername(), payout, bet.getAmount(), winnerNpcId);
            }
        }

        log.info("Payouts settled for game {}. Pool={}, betsOnWinner={}", gameId, totalPool, totalBetsOnWinner);
    }

    @Transactional(readOnly = true)
    public List<BetResponse> getBetsByUser(Long userId) {
        return betRepository.findByUserId(userId).stream()
                .map(BetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BetResponse> getBetsByGame(Long gameId) {
        return betRepository.findByGameId(gameId).stream()
                .map(BetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BetResponse getBet(Long betId) {
        return BetResponse.fromEntity(betRepository.findById(betId)
                .orElseThrow(() -> new NoSuchElementException("Bet not found: " + betId)));
    }

    @Transactional(readOnly = true)
    public double getOdds(Long gameId, Long npcId) {
        double totalPool = betRepository.sumAmountByGameId(gameId);
        double npcPool = betRepository.sumAmountByGameIdAndNpcId(gameId, npcId);
        if (npcPool == 0) return 0;
        return totalPool / npcPool;
    }
}
