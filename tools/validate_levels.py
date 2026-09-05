#!/usr/bin/env python3
"""Validate every data-driven level before Android compilation."""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEVEL_DIR = ROOT / "app" / "src" / "main" / "assets" / "levels"
VALID_COLLECTIBLES = {"WATER", "TRAIL", "TRASH"}
VALID_HAZARDS = {"WIND", "SPIKES", "ROCK_ZONE"}
VALID_ABILITIES = {"DASH", "SHIELD", "GROUND_POUND"}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def validate_file(path: Path) -> dict:
    level = json.loads(path.read_text(encoding="utf-8"))
    label = f"{path.name}:"
    world_width = float(level["worldWidth"])
    require(level["id"] in (1, 2, 3), f"{label} invalid id")
    require(world_width >= 1280, f"{label} world must be at least one viewport")
    require(0 <= level["startX"] < level["goalX"] <= world_width, f"{label} start/goal bounds")
    require(level["parTime"] > 0, f"{label} parTime must be positive")
    require(level["platforms"], f"{label} must contain platforms")

    ids: set[str] = set()
    groups = ("platforms", "collectibles", "enemies", "hazards", "checkpoints", "abilities", "barriers")
    for group in groups:
        for item in level.get(group, []):
            item_id = item.get("id", "")
            require(item_id and item_id not in ids, f"{label} duplicate/missing id {item_id!r}")
            ids.add(item_id)
            x = float(item.get("x", 0))
            require(0 <= x <= world_width, f"{label} {item_id} x outside world")
            if "width" in item:
                require(float(item["width"]) > 0 and x + float(item["width"]) <= world_width,
                        f"{label} {item_id} width outside world")
            if "height" in item:
                require(float(item["height"]) > 0, f"{label} {item_id} invalid height")

    collectible_types = [item["type"] for item in level.get("collectibles", [])]
    require(set(collectible_types) <= VALID_COLLECTIBLES, f"{label} invalid collectible type")
    require(level.get("requiredWater", 0) <= collectible_types.count("WATER"), f"{label} missing required water")
    require(level.get("requiredMarkers", 0) <= collectible_types.count("TRAIL"), f"{label} missing required markers")
    require({item["type"] for item in level.get("hazards", [])} <= VALID_HAZARDS, f"{label} invalid hazard type")
    require({item["type"] for item in level.get("abilities", [])} <= VALID_ABILITIES, f"{label} invalid ability type")

    ground = sorted(
        (float(item["x"]), float(item["x"]) + float(item["width"]))
        for item in level["platforms"]
        if float(item["y"]) >= float(level["groundY"]) - 1
    )
    require(ground and ground[0][0] <= level["startX"], f"{label} no ground under player start")
    max_gap = max((ground[index + 1][0] - ground[index][1] for index in range(len(ground) - 1)), default=0)
    require(max_gap <= 250, f"{label} ground gap {max_gap:g} is not safely jumpable")
    return level


def main() -> None:
    files = sorted(LEVEL_DIR.glob("level_*.json"))
    require(len(files) == 3, f"expected exactly three level files, found {len(files)}")
    levels = [validate_file(path) for path in files]
    require([level["id"] for level in levels] == [1, 2, 3], "level ids must be sequential")
    print(f"Validated {len(levels)} levels and {sum(len(level['platforms']) for level in levels)} platforms.")


if __name__ == "__main__":
    main()
