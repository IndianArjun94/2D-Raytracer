package net.arjun.justwalkforward.game;

import net.arjun.justwalkforward.game.raytracing.GPUManager;
import net.arjun.justwalkforward.game.raytracing.Ray;

import java.io.IOException;
import java.util.Random;

import static net.arjun.justwalkforward.game.GameRenderer.RGB.rgb;

public class GameUpdater implements Runnable {

    private final Random random = new Random();

    public GameRenderer renderer;
    public GameRenderer.InnerGameRenderer innerGameRenderer;

    public int[] initialPixels;
    public int[] raytracedPixels;

    public boolean running = false;

    public Thread updateThread;

    public int patternCounter = 0;

    public GameUpdater(GameRenderer renderer) {
        this.renderer = renderer;
        this.innerGameRenderer = renderer.innerGameRenderer;
        if (this.renderer == null) {
            System.err.println("Failed GameUpdater Init! renderer is null");
        } else if (this.innerGameRenderer == null) {
            System.err.println("Failed GameUpdater Init! innerGameRenderer is null");
        }

//        Allocate GPU-side Ray-Data arrays
        GPUManager.makeContextCurrent();
        GPUManager.allocVars();
        GPUManager.loadModule("build/resources/main/justwalkforward/raytracing/cuda/kernels/ray_util.ptx");
        GPUManager.loadFunction("tickAll", "ray_util.ptx");

    }

    public void addAllInitialTestRays() {
        GPUManager.makeContextCurrent();
        int max = (int)(Math.sqrt((renderer.WIDTH*renderer.WIDTH)+(renderer.HEIGHT*renderer.HEIGHT)));
        for (int i = 0; i < 100; i++) {
            innerGameRenderer.addTestRays();
            GPUManager.raysCount+=3600;
            int j = 0;
            for (Ray ray : innerGameRenderer.rays) {
                GPUManager.actualXs[j] = ray.actualX;
                GPUManager.actualYs[j] = ray.actualY;
                GPUManager.xIntervals[j] = ray.intervalX;
                GPUManager.yIntervals[j] = ray.intervalY;
//                ray.tick();


                j++;
            }
            GPUManager.sendVars();
            GPUManager.runTickAllKernel(1);
            System.out.println(i);
        }

        GPUManager.getVars();

        for (int i = 0; i < innerGameRenderer.rays.size(); i++) {
            innerGameRenderer.rays.get(i).actualX = GPUManager.actualXs[i];
            innerGameRenderer.rays.get(i).actualY = GPUManager.actualYs[i];
            innerGameRenderer.rays.get(i).intervalX = GPUManager.xIntervals[i];
            innerGameRenderer.rays.get(i).intervalY = GPUManager.yIntervals[i];
            innerGameRenderer.rays.get(i).castActualCoords();
            System.out.println(innerGameRenderer.rays.get(i).x + ", " + innerGameRenderer.rays.get(i).y);
        }

        System.out.println("Rays loaded!");
    }

    public void initUpdateSystem() throws IOException {
        this.initialPixels = new int[this.renderer.WIDTH*this.renderer.HEIGHT];
        this.raytracedPixels = new int[this.renderer.WIDTH*this.renderer.HEIGHT];
        startUpdateThread();
    }

    public void retryInit(GameRenderer renderer) {
        this.renderer = renderer;
        this.innerGameRenderer = renderer.innerGameRenderer;
        if (this.renderer == null) {
            System.err.println("Failed GameUpdater Init! renderer is null");
        } else if (this.innerGameRenderer == null) {
            System.err.println("Failed GameUpdater Init! innerGameRenderer is null");
        }
        GPUManager.makeContextCurrent();
        GPUManager.allocVars();
        GPUManager.loadModule("build/resources/main/justwalkforward/raytracing/cuda/kernels/ray_util.ptx");
        GPUManager.loadFunction("tickAll", "ray_util.ptx");
    }

    public synchronized void setInitialPixel(int x, int y, GameRenderer.RGB rgb) {
        initialPixels[(this.renderer.WIDTH*y)+x] = (255 << 24) | (rgb.r << 16) | (rgb.g << 8) | rgb.b;
    }

    public synchronized void setRaytracedPixel(int x, int y, GameRenderer.RGB rgb) {
        raytracedPixels[(this.renderer.WIDTH*y)+x] = (255 << 24) | (rgb.r << 16) | (rgb.g << 8) | rgb.b;
    }

    public synchronized void setRaytracedPixel(int x, int y, int pixel) {
        raytracedPixels[(this.renderer.WIDTH*y)+x] = pixel;
    }

    public synchronized int getInitialPixel(int x, int y) {
//        int a = (pixelValue >> 24) & 0xFF;
//        int r = (pixelValue >> 16) & 0xFF;
//        int g = (pixelValue >> 8) & 0xFF;
//        int b = pixelValue & 0xFF;

        return initialPixels[(this.renderer.WIDTH*y)+x];
    }

    public synchronized int getRaytracedPixel(int x, int y) {
//        int a = (pixelValue >> 24) & 0xFF;
//        int r = (pixelValue >> 16) & 0xFF;
//        int g = (pixelValue >> 8) & 0xFF;
//        int b = pixelValue & 0xFF;

        return raytracedPixels[(this.renderer.WIDTH * y) + x];
    }

    public synchronized void update() {
//        int time_int = (int) (System.currentTimeMillis()*5);
        int time_int = patternCounter;

        for (int y = 0; y < innerGameRenderer.getHeight(); y++) {
            for (int x = 0; x < innerGameRenderer.getWidth(); x++) {
                int r = (int) ((Math.sin(time_int / 1000.0) + 1) / 2 * 255);
                int g = (int) ((Math.sin(x / 50.0 + time_int / 2000.0) + 1) / 2 * 255);
                int b = (int) ((Math.cos(y / 50.0 + time_int / 1500.0) + 1) / 2 * 255);

                setInitialPixel(x,y,rgb(r,g,b));
            }
        }

        for (Ray ray : innerGameRenderer.rays) {
            if (ray.x >= 0 && ray.x < innerGameRenderer.getWidth() && ray.y >= 0 && ray.y < innerGameRenderer.getHeight()) { // in bounds
                int pixelAtPos = getInitialPixel(ray.x, ray.y);

                pixelAtPos =
                        (0xFF << 24)
                                | ((int)Math.min(255, (((pixelAtPos >> 16) & 0xFF)
                                + (ray.color.getRed()) / Math.max(1.0, 180.0 / ray.color.getRed()))) << 16)
                                | ((int)Math.min(255, (((pixelAtPos >> 8) & 0xFF)
                                + (ray.color.getGreen()) / Math.max(1.0, 180.0 / ray.color.getGreen()))) << 8)
                                | (int)Math.min(255, ((pixelAtPos & 0xFF)
                                + (ray.color.getBlue()) / Math.max(1.0, 180.0 / ray.color.getBlue())));

                innerGameRenderer.setPixel(ray.x,ray.y, pixelAtPos);

//                ray.tick();
            } else {
                ray.reset();
            }
        }
        patternCounter +=50;
    }

    private synchronized void startUpdateThread() {
        if (running) return;
        running = true;
        updateThread = new Thread(this, "updateGame");
        updateThread.setPriority(2);
        updateThread.start();
    }

    private synchronized void stopUpdateThread() {
        if (!running) return;
        running = false;
        try {
            updateThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        addAllInitialTestRays();
        final int targetUPS = 120;
        final double targetDelta = 1000.0 / targetUPS; // milliseconds per update (≈8.33ms)

        long lastTime = System.currentTimeMillis();

        while (running) {
            long now = System.currentTimeMillis();
            double delta = now - lastTime;
            if (delta >= targetDelta) {
                update(); // one update per tick
                innerGameRenderer.renderFrame = true;
                lastTime = now;
            } else {
                try {
                    Thread.sleep((long) Math.max(0, targetDelta - delta));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
