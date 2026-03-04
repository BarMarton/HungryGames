package com.hungergames.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "npcs")
@Getter @Setter @NoArgsConstructor
public class NPC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;


    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(nullable = false)
    private int dmg;


    @Column(nullable = false)
    private int speed;


    @Column(name = "final_hp", nullable = false)
    private int finalHp;

    @Column(name = "final_x", nullable = false)
    private int finalX;

    @Column(name = "final_y", nullable = false)
    private int finalY;

    @Column(nullable = false)
    private boolean alive = true;

    @Column(name = "finish_position")
    private Integer finishPosition;
}
