package com.hungergames.repository;

import com.hungergames.model.Bet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BetRepository extends JpaRepository<Bet, Long> {

    List<Bet> findByUserId(Long userId);

    List<Bet> findByGameId(Long gameId);

    List<Bet> findByGameIdAndNpcId(Long gameId, Long npcId);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Bet b WHERE b.game.id = :gameId AND b.npc.id = :npcId")
    double sumAmountByGameIdAndNpcId(@Param("gameId") Long gameId, @Param("npcId") Long npcId);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Bet b WHERE b.game.id = :gameId")
    double sumAmountByGameId(@Param("gameId") Long gameId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);
    
    
}
