package com.boldtechnology.boldoogame;

public final class InputController {
    public boolean leftHeld;
    public boolean rightHeld;
    public boolean jumpHeld;
    public boolean abilityHeld;
    private boolean jumpPressed;
    private boolean jumpReleased;
    private boolean abilityPressed;

    public void setState(boolean left, boolean right, boolean jump, boolean ability) {
        if (jump && !jumpHeld) jumpPressed = true;
        if (!jump && jumpHeld) jumpReleased = true;
        if (ability && !abilityHeld) abilityPressed = true;
        leftHeld = left;
        rightHeld = right;
        jumpHeld = jump;
        abilityHeld = ability;
    }

    public boolean consumeJumpPressed() {
        boolean value = jumpPressed;
        jumpPressed = false;
        return value;
    }

    public boolean consumeJumpReleased() {
        boolean value = jumpReleased;
        jumpReleased = false;
        return value;
    }

    public boolean consumeAbilityPressed() {
        boolean value = abilityPressed;
        abilityPressed = false;
        return value;
    }

    public void clear() {
        setState(false, false, false, false);
        jumpPressed = false;
        jumpReleased = false;
        abilityPressed = false;
    }
}
