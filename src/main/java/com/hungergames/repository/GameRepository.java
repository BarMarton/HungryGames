package com.hungergames.repository;

import com.hungergames.model.Game;
import com.hungergames.model.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findTopByStatusOrderByIdDesc(GameStatus status);
    Page<Game> findAllByOrderByIdDesc(Pageable pageable);
    Optional<Game> findTopByOrderByIdDesc();
}
