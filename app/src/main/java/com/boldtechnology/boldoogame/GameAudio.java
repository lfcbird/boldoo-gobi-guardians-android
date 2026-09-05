package com.boldtechnology.boldoogame;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.util.EnumMap;
import java.util.Map;

public final class GameAudio {
    private final SoundPool soundPool;
    private final Map<GameEvent.Type, Integer> sounds = new EnumMap<>(GameEvent.Type.class);
    private final MediaPlayer ambient;
    private GameSettings settings;

    public GameAudio(Context context, GameSettings settings) {
        this.settings = settings;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(attributes).build();
        register(context, GameEvent.Type.JUMP, R.raw.sfx_jump);
        register(context, GameEvent.Type.WATER, R.raw.sfx_water);
        register(context, GameEvent.Type.TRAIL, R.raw.sfx_water);
        register(context, GameEvent.Type.TRASH, R.raw.sfx_stomp);
        register(context, GameEvent.Type.STOMP, R.raw.sfx_stomp);
        register(context, GameEvent.Type.HURT, R.raw.sfx_hurt);
        register(context, GameEvent.Type.LIFE_LOST, R.raw.sfx_hurt);
        register(context, GameEvent.Type.SHIELD, R.raw.sfx_ability);
        register(context, GameEvent.Type.CHECKPOINT, R.raw.sfx_checkpoint);
        register(context, GameEvent.Type.ABILITY, R.raw.sfx_ability);
        register(context, GameEvent.Type.BARRIER, R.raw.sfx_break);
        register(context, GameEvent.Type.BOSS_HIT, R.raw.sfx_break);
        register(context, GameEvent.Type.COMPLETE, R.raw.sfx_win);
        ambient = MediaPlayer.create(context, R.raw.ambient_gobi);
        if (ambient != null) {
            ambient.setLooping(true);
            ambient.setVolume(0.24f, 0.24f);
        }
    }

    private void register(Context context, GameEvent.Type event, int resource) {
        sounds.put(event, soundPool.load(context, resource, 1));
    }

    public void setSettings(GameSettings settings) {
        this.settings = settings;
        syncAmbient();
    }

    public void play(GameEvent.Type event) {
        if (!settings.soundEffects) return;
        Integer sound = sounds.get(event);
        if (sound != null) soundPool.play(sound, 0.72f, 0.72f, 1, 0, 1f);
    }

    public void resume() {
        syncAmbient();
    }

    public void pause() {
        if (ambient != null && ambient.isPlaying()) ambient.pause();
    }

    private void syncAmbient() {
        if (ambient == null) return;
        if (settings.ambientSound) {
            if (!ambient.isPlaying()) ambient.start();
        } else if (ambient.isPlaying()) {
            ambient.pause();
        }
    }

    public void release() {
        soundPool.release();
        if (ambient != null) ambient.release();
    }
}
