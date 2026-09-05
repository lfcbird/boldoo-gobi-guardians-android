package com.boldtechnology.boldoogame;

public final class GameEvent {
    public enum Type {
        JUMP, WATER, TRAIL, TRASH, STOMP, HURT, SHIELD,
        CHECKPOINT, ABILITY, BARRIER, BOSS_HIT, LIFE_LOST, COMPLETE
    }

    public final Type type;
    public final float x;
    public final float y;

    public GameEvent(Type type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
