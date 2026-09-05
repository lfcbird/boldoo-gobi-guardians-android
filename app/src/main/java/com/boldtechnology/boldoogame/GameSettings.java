package com.boldtechnology.boldoogame;

public final class GameSettings {
    public boolean soundEffects = true;
    public boolean ambientSound = true;
    public boolean haptics = true;
    public boolean leftHanded = false;
    public float controlOpacity = 0.62f;

    public void cycleOpacity() {
        if (controlOpacity < 0.5f) controlOpacity = 0.62f;
        else if (controlOpacity < 0.75f) controlOpacity = 0.82f;
        else if (controlOpacity < 0.95f) controlOpacity = 1f;
        else controlOpacity = 0.42f;
    }
}
