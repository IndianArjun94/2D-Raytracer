package net.arjun.justwalkforward.game.raytracing;

import java.awt.*;

public class Ray {
    public float degrees;
    public float strength;
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

    private Ray(float degrees, float strength, Color color, int x, int y) {
        this.degrees = degrees;
        this.strength = strength;
        this.color = color;
        this.x = x;
        this.actualX = x;
        this.originalX = x;
        this.y = y;
        this.actualY = y;
        this.originalY = y;
        calculateInterval();
    }

    public static Ray ray(float degrees, float strength, Color color, int x, int y) {
        if (degrees >= 0 && degrees <= 360) {
            return new Ray(degrees, strength, color, x, y);
        } else {
            throw new RuntimeException("degree value not in degree bounds");
        }
    }

    public void castActualCoords() {
        this.x = (int) actualX;
        this.y = (int) actualY;
    }

    private void calculateInterval() {
        intervalX = (float) Math.abs(Math.sin(Math.toRadians(degrees)) * 1);
        intervalY = (float) Math.abs(Math.cos(Math.toRadians(degrees)) * 1);

        if (degrees > 0 && degrees < 90) { // First Quadrant
            intervalY*=-1;
        } else if (degrees > 90 && degrees < 180) { // Third Quadrant
//            Nothing!
        } else if (degrees > 180 && degrees < 270) { // Second Quadrant
            intervalX*=-1;
        } else if (degrees > 270 && degrees < 360) {
            intervalX*=-1;
            intervalY*=-1;
        }
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
