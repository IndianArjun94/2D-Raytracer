package net.arjun.justwalkforward.game.objects;

import net.arjun.justwalkforward.game.raytracing.RayAction;

public class TestSquare extends GameObject {
    public Hitbox hitbox;

    public TestSquare(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.hitbox = new Hitbox(x,y,width,height);
    }

    @Override
    public RayAction onRayHit() {
        return RayAction.BOUNCE;
    }
}
