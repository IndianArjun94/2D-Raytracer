package net.arjun.justwalkforward.game.raytracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RayManager implements Runnable {
    public boolean running = false;
    public Thread thread;

    public final List<Ray> rays = Collections.synchronizedList(new ArrayList<>());

    public volatile boolean moveRequest = false;
    public volatile int direction; // up=0, right=1, down=2, left=3
    public volatile int distance; // by pixels

    public void requestMove(int direction, int amount) {
        this.direction = direction;
        this.distance = amount;
        this.moveRequest = true;
    }

    public synchronized void start() {
        thread = new Thread(this);
        running = true;
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            try { thread.join(); } catch (InterruptedException ignored) {}
        }
    }
    @Override
    public void run() {
//        TODO: add UPS logic from game updater

        final int targetUPS = 120;
        final long targetDelta = 1_000_000_000 / targetUPS; // milliseconds per update (≈8.33ms)

        long lastTime = System.nanoTime();

//        -----------------------------

        long lastUPSUpdateTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            while (now-lastTime >= targetDelta) {
                if (moveRequest) {
                    synchronized (rays) {
                        for (Ray ray : rays) {
                            ray.shift(direction, distance);
                        }
                    }
                }
                lastTime+=targetDelta;

                now = System.nanoTime();

                if (now - lastUPSUpdateTime >= 1_000_000_000L) {
                    lastUPSUpdateTime = now;
                }
            }
        }
    }
}
