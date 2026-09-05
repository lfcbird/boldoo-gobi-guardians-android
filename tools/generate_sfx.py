#!/usr/bin/env python3
"""Generate the game's original, dependency-free PCM sound effects."""

from __future__ import annotations

import math
import random
import wave
from pathlib import Path

RATE = 22_050
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "res" / "raw"


def envelope(t: float, duration: float, attack: float = 0.012, release: float = 0.12) -> float:
    return min(1.0, t / attack) * min(1.0, max(0.0, duration - t) / release)


def save(name: str, duration: float, sample_fn) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    frames = bytearray()
    for index in range(int(RATE * duration)):
        t = index / RATE
        value = max(-1.0, min(1.0, sample_fn(t, duration)))
        sample = int(value * 32767)
        frames += sample.to_bytes(2, "little", signed=True)
    with wave.open(str(OUT / name), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(frames)


def tone(start: float, end: float, volume: float = 0.35):
    def sample(t: float, duration: float) -> float:
        frequency = start + (end - start) * t / duration
        phase = 2 * math.pi * (start * t + (end - start) * t * t / (2 * duration))
        return math.sin(phase) * envelope(t, duration) * volume
    return sample


def chord(notes: tuple[float, ...], volume: float = 0.18):
    def sample(t: float, duration: float) -> float:
        return sum(math.sin(2 * math.pi * note * t) for note in notes) * envelope(t, duration, 0.02, 0.2) * volume
    return sample


def noise_hit(seed: int, pitch: float = 75.0):
    rng = random.Random(seed)
    values = [rng.uniform(-1, 1) for _ in range(int(RATE * 0.8) + 1)]

    def sample(t: float, duration: float) -> float:
        noise = values[min(len(values) - 1, int(t * RATE))]
        body = math.sin(2 * math.pi * pitch * t) * math.exp(-t * 12)
        return (noise * 0.23 + body * 0.55) * envelope(t, duration, 0.004, 0.15)
    return sample


def ambient(t: float, duration: float) -> float:
    rng_phase = math.sin(t * 0.31) + math.sin(t * 0.47 + 1.2)
    wind = math.sin(2 * math.pi * 72 * t + math.sin(t * 0.8) * 2.2)
    breeze = math.sin(2 * math.pi * 118 * t + rng_phase)
    fade = min(1.0, t / 0.35, (duration - t) / 0.35)
    return (wind * 0.055 + breeze * 0.025) * max(0.0, fade)


def main() -> None:
    save("sfx_jump.wav", 0.25, tone(360, 690, 0.34))
    save("sfx_water.wav", 0.34, chord((660, 880, 1320), 0.11))
    save("sfx_stomp.wav", 0.29, noise_hit(11, 82))
    save("sfx_hurt.wav", 0.40, tone(260, 105, 0.38))
    save("sfx_checkpoint.wav", 0.62, chord((392, 523.25, 659.25), 0.10))
    save("sfx_ability.wav", 0.48, tone(240, 980, 0.30))
    save("sfx_break.wav", 0.43, noise_hit(27, 58))
    save("sfx_win.wav", 1.05, chord((523.25, 659.25, 783.99), 0.11))
    save("ambient_gobi.wav", 6.0, ambient)


if __name__ == "__main__":
    main()
