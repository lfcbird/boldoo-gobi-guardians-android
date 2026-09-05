package com.boldtechnology.boldoogame;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class LevelLoader {
    private final Context context;

    public LevelLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    public LevelData load(int levelId) {
        String fileName = String.format("levels/level_%02d.json", levelId);
        try (InputStream stream = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            LevelData level = parse(new JSONObject(json.toString()));
            LevelValidator.validate(level);
            return level;
        } catch (IOException | JSONException error) {
            throw new IllegalStateException("Could not load " + fileName, error);
        }
    }

    private LevelData parse(JSONObject json) throws JSONException {
        LevelData level = new LevelData();
        level.id = json.getInt("id");
        level.title = json.getString("title");
        level.titleMn = json.getString("titleMn");
        level.objectiveMn = json.getString("objectiveMn");
        level.environment = json.optString("environment", "OASIS");
        level.worldWidth = number(json, "worldWidth", 3200f);
        level.groundY = number(json, "groundY", 610f);
        level.startX = number(json, "startX", 90f);
        level.startY = number(json, "startY", level.groundY - Player.HEIGHT);
        level.goalX = number(json, "goalX", level.worldWidth - 160f);
        level.parTime = number(json, "parTime", 120f);
        level.requiredWater = json.optInt("requiredWater", 0);
        level.requiredMarkers = json.optInt("requiredMarkers", 0);

        JSONArray platforms = json.getJSONArray("platforms");
        for (int i = 0; i < platforms.length(); i++) {
            JSONObject item = platforms.getJSONObject(i);
            LevelData.Platform value = new LevelData.Platform();
            value.id = item.getString("id");
            value.baseX = value.x = number(item, "x", 0f);
            value.baseY = value.y = number(item, "y", 0f);
            value.width = number(item, "width", 100f);
            value.height = number(item, "height", 28f);
            value.moveX = number(item, "moveX", 0f);
            value.moveY = number(item, "moveY", 0f);
            value.speed = number(item, "speed", 0f);
            value.phase = number(item, "phase", 0f);
            level.platforms.add(value);
        }

        JSONArray collectibles = json.optJSONArray("collectibles");
        if (collectibles != null) for (int i = 0; i < collectibles.length(); i++) {
            JSONObject item = collectibles.getJSONObject(i);
            LevelData.Collectible value = new LevelData.Collectible();
            value.id = item.getString("id");
            value.type = item.optString("type", LevelData.Collectible.WATER);
            value.x = number(item, "x", 0f);
            value.y = number(item, "y", 0f);
            level.collectibles.add(value);
        }

        JSONArray enemies = json.optJSONArray("enemies");
        if (enemies != null) for (int i = 0; i < enemies.length(); i++) {
            JSONObject item = enemies.getJSONObject(i);
            LevelData.Enemy value = new LevelData.Enemy();
            value.id = item.getString("id");
            value.type = item.optString("type", LevelData.Enemy.SMOGLING);
            value.x = number(item, "x", 0f);
            value.y = number(item, "y", 0f);
            value.width = number(item, "width", value.isBoss() ? 180f : 78f);
            value.height = number(item, "height", value.isBoss() ? 150f : 78f);
            value.minX = number(item, "minX", value.x - 100f);
            value.maxX = number(item, "maxX", value.x + 100f);
            value.speed = number(item, "speed", 90f);
            value.health = value.maxHealth = item.optInt("health", value.isBoss() ? 3 : 1);
            level.enemies.add(value);
        }

        JSONArray hazards = json.optJSONArray("hazards");
        if (hazards != null) for (int i = 0; i < hazards.length(); i++) {
            JSONObject item = hazards.getJSONObject(i);
            LevelData.Hazard value = new LevelData.Hazard();
            value.id = item.getString("id");
            value.type = item.getString("type");
            value.x = number(item, "x", 0f);
            value.y = number(item, "y", 0f);
            value.width = number(item, "width", 100f);
            value.height = number(item, "height", 100f);
            value.strength = number(item, "strength", 0f);
            value.interval = number(item, "interval", 1.5f);
            value.timer = number(item, "delay", value.interval);
            level.hazards.add(value);
        }

        JSONArray checkpoints = json.optJSONArray("checkpoints");
        if (checkpoints != null) for (int i = 0; i < checkpoints.length(); i++) {
            JSONObject item = checkpoints.getJSONObject(i);
            LevelData.Checkpoint value = new LevelData.Checkpoint();
            value.id = item.getString("id");
            value.x = number(item, "x", 0f);
            value.y = number(item, "y", level.groundY);
            level.checkpoints.add(value);
        }

        JSONArray abilities = json.optJSONArray("abilities");
        if (abilities != null) for (int i = 0; i < abilities.length(); i++) {
            JSONObject item = abilities.getJSONObject(i);
            LevelData.AbilityPickup value = new LevelData.AbilityPickup();
            value.id = item.getString("id");
            value.type = item.getString("type");
            value.x = number(item, "x", 0f);
            value.y = number(item, "y", 0f);
            level.abilities.add(value);
        }

        JSONArray barriers = json.optJSONArray("barriers");
        if (barriers != null) for (int i = 0; i < barriers.length(); i++) {
            JSONObject item = barriers.getJSONObject(i);
            LevelData.Barrier value = new LevelData.Barrier();
            value.id = item.getString("id");
            value.x = number(item, "x", 0f);
            value.y = number(item, "y", 0f);
            value.width = number(item, "width", 70f);
            value.height = number(item, "height", 120f);
            value.health = item.optInt("health", 1);
            level.barriers.add(value);
        }
        return level;
    }

    private static float number(JSONObject object, String name, float fallback) {
        return (float) object.optDouble(name, fallback);
    }
}
