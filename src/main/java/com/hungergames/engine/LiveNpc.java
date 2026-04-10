package com.hungergames.engine;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LiveNpc {

    private final long id;
    private final String name;
    private final int picId;
    private final int maxHp;
    private final int dmg;
    private final int speed;

    private int currentHp;
    private int x;
    private int y;
    private boolean alive = true;
    private final int moveCooldown;

    public LiveNpc(long id, String name, int maxHp, int dmg, int speed, int startX, int startY, int picID) {
        this.picId = picID;
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.dmg = dmg;
        this.speed = speed;
        this.x = startX;
        this.y = startY;
        this.moveCooldown = Math.max(1, 11 - speed);
    }

    public boolean shouldMoveOnTick(long tick) {
        return tick % moveCooldown == 0;
    }

    public void takeDamage(int damage) {
        currentHp = Math.max(0, currentHp - damage);
        if (currentHp == 0) {
            alive = false;
        }
    }

    @Override
    public String toString() {
        return String.format("NPC[%d:%s hp=%d/%d dmg=%d spd=%d pos=(%d,%d)]",
                id, name, currentHp, maxHp, dmg, speed, x, y);
    }
}
