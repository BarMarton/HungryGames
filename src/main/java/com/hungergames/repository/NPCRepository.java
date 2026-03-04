package com.hungergames.repository;

import com.hungergames.model.NPC;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NPCRepository extends JpaRepository<NPC, Long> {
    List<NPC> findByGameId(Long gameId);
    List<NPC> findByGameIdAndAliveTrue(Long gameId);
}
