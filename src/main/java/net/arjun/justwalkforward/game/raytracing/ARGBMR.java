package net.arjun.justwalkforward.game.raytracing;

import net.arjun.justwalkforward.game.GameRenderer;

public class ARGBMR {
    public GameRenderer.ARGB argb;
    public GameRenderer.Material material;

    private ARGBMR(GameRenderer.ARGB argb, GameRenderer.Material material) {
        this.material = material;
        this.argb = argb;
    }

    public static ARGBMR argbmr(GameRenderer.ARGB argb, GameRenderer.Material material) {
        return new ARGBMR(argb, material);
    }
}
