package com.huwng.alterna.vine.duck;

/**
 * Duck interface for GameRenderer to control camera tilt during vine sliding.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public interface VineGameRendererDuck {
    void vine$setVineTilt(float yaw);
}
