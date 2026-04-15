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
    private int dmg;
    private final int speed;

    private final int max_hp;
    private final int regen;
    private int currentHp;
    private int x;
    private int y;
    private boolean alive = true;
    private final int moveCooldown;

    public LiveNpc(long id, String name, int maxHp, int dmg, int speed, int startX, int startY,int regen, int picID) {
        this.picId = picID;
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
        this.max_hp = maxHp;
        this.regen = regen;
        this.currentHp = max_hp;
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

    public void setDmg(int dmg) {
        this.dmg = dmg;
    }

    public void heal() {
        if (this.currentHp < this.maxHp && this.regen > 0) {
            this.currentHp = Math.min(this.maxHp, this.currentHp + this.regen);
        }
    }
}
