package net.arjun.justwalkforward.game.objects;

import net.arjun.justwalkforward.game.raytracing.RayAction;

public abstract class GameObject {
    public int x;
    public int y;
    public int width;
    public int height;

    public abstract RayAction onRayHit();
}
