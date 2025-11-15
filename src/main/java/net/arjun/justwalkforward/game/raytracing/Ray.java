package net.arjun.justwalkforward.game.raytracing;

import java.awt.*;

public class Ray {
    public float degrees;
    public int strength;
    public float decay;
    public Color color;

    public int x;
    public int y;
    public float actualX;
    public float actualY;
    public int originalX;
    public int originalY;
    public int travel;
    public float intervalX;
    public float intervalY;

    private Ray(float degrees, int strength, float decay, Color color, int x, int y) {
        this.degrees = degrees;
        this.strength = strength;
        this.color = color;
        this.x = x;
        this.actualX = x;
        this.originalX = x;
        this.y = y;
        this.actualY = y;
        this.originalY = y;
        this.decay = decay;
        calculateInterval();
    }

    public void shift(int direction, int distance) {
        if (direction == 0) { // up
            originalY-=distance;
        } else if (direction == 1) { // right
            originalX+=distance;
        } else if (direction == 2) { // down
            originalY+=distance;
        } else if (direction == 3) { // left
            originalX-=distance;
        } else {
            System.err.println("Ray: Invalid Direction \\" + direction + "\\! Only 0, 1, 2, or 3 are valid directions.");
        }
    }

    public static Ray ray(float degrees, int strength, float decay, Color color, int x, int y) {
        if (degrees >= 0 && degrees <= 360) {
            return new Ray(degrees, strength, decay, color, x, y);
        } else {
            int quotient1 = (int) (degrees/360);
            float quotient2 = degrees/360;

            degrees = (quotient2-quotient1)*360;

            return new Ray(degrees, strength, decay, color, x, y);
        }
    }

    public void castActualCoords() {
        this.x = (int) actualX;
        this.y = (int) actualY;
    }

    private void calculateInterval() {
        double rad = Math.toRadians(degrees);
        intervalX = (float) Math.sin(rad);    // right = positive X
        intervalY = (float) -Math.cos(rad);   // down = positive Y
    }

    public void tick() {
        travel++;
        calcCoords();
    }

    public void reset() {
        travel = 0;
        x = originalX;
        y = originalY;
        actualX = originalX;
        actualY = originalY;
    }

    private void calcCoords() {
        actualX += intervalX;
        actualY += intervalY;
        x = (int) actualX;
        y = (int) actualY;
//        if (degrees == 0) { // straight up
//            y = originalY-travel;
//            actualY = y;
//        } else if (degrees == 180) { // straight down
//            y = originalY+travel;
//            actualY = y;
//        } else if (degrees == 90) { // straight right
//            x = originalX+travel;
//            actualX = x;
//        } else if (degrees == 270) { // straight left
//            x = originalX-travel;
//            actualX = x;
//        }
//
//        else if (degrees > 0 && degrees < 90) { // First Quadrant
//            actualX += intervalX;
//            actualY += -intervalY;
//            x = (int) actualX;
//            y = (int) actualY;
//        } else if (degrees > 90 && degrees < 180) { // Third Quadrant
//            actualX += intervalX;
//            actualY += intervalY;
//            x = (int) actualX;
//            y = (int) actualY;
//        } else if (degrees > 180 && degrees < 270) { // Second Quadrant
//            actualX += -intervalX;
//            actualY += intervalY;
//            x = (int) actualX;
//            y = (int) actualY;
//        } else if (degrees > 270 && degrees < 360) {
//            actualX += -intervalX;
//            actualY += -intervalY;
//            x = (int) actualX;
//            y = (int) actualY;
//        }
    }
}
