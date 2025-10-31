package net.arjun.justwalkforward.game;

import jcuda.Pointer;
import jcuda.Sizeof;
import net.arjun.justwalkforward.game.raytracing.GPUManager;
import net.arjun.justwalkforward.game.raytracing.Ray;

import java.io.*;
import java.net.URL;
import java.util.Random;

import static jcuda.driver.JCudaDriver.*;
import static net.arjun.justwalkforward.game.raytracing.GPUManager.*;

public class GameUpdater implements Runnable {

    private final Random random = new Random();

    public GameRenderer renderer;
    public GameRenderer.InnerGameRenderer innerGameRenderer;

    public int[] initialPixels;
    public int[] raytracedPixels;

    public boolean running = false;

    public Thread updateThread;

    public int patternCounter = 0;

    public GameUpdater(GameRenderer renderer) throws IOException {
        this.renderer = renderer;
        this.innerGameRenderer = renderer.innerGameRenderer;
        if (this.renderer == null) {
            System.err.println("Failed GameUpdater Init! renderer is null");
            return;
        } else if (this.innerGameRenderer == null) {
            System.err.println("Failed GameUpdater Init! innerGameRenderer is null");
            return;
        }

//        Allocate GPU-side Ray-Data arrays
        loadKernels();
    }

    public String loadResourceToTempFile(String resourcePath) throws IOException {
        URL in = getClass().getResource("/" + resourcePath);
        if (in == null)
            throw new FileNotFoundException("Resource not found: " + resourcePath);

        String[] file = in.getFile().substring(1).split("%20");
        StringBuilder finalPath = new StringBuilder();
        if (file.length == 1) {
            return file[0];
        }
        int counter = 0;
        for (String p : file) {
            finalPath.append(p);
            if (counter < file.length-1) { finalPath.append(" "); }
            counter++;

        }

        return finalPath.toString();

    }


    public void loadKernels() throws IOException {

        GPUManager.allocVars();
        GPUManager.makeContextCurrent();
        try {
            GPUManager.loadModule(loadResourceToTempFile("justwalkforward/raytracing/cuda/kernels/ray_util.ptx"));
            GPUManager.loadModule(loadResourceToTempFile("justwalkforward/raytracing/cuda/kernels/examples/background.ptx"));
        } catch (Exception e) {
            try {
                GPUManager.loadModule("build/resources/main/justwalkforward/raytracing/cuda/kernels/ray_util.ptx");
                GPUManager.loadModule("build/resources/main/justwalkforward/raytracing/cuda/kernels/examples/background.ptx");
            } catch (Exception e1) {
                System.exit(1);
            }
        }

        GPUManager.loadFunction("travelRay", "ray_util.ptx");
        GPUManager.loadFunction("calculateRow", "background.ptx");
        print("Loaded Kernels!");
    }

    private void print(String text) {
        System.out.println("[GameUpdater] " + text);
    }

    public void addAllInitialTestRays() {
        GPUManager.makeContextCurrent();
        innerGameRenderer.addTestRays();
        GPUManager.raysCount=innerGameRenderer.rays.size();
        int j = 0;
        for (Ray ray : innerGameRenderer.rays) {
            GPUManager.actualXs[j] = ray.actualX;
            GPUManager.actualYs[j] = ray.actualY;
            GPUManager.xIntervals[j] = ray.intervalX;
            GPUManager.yIntervals[j] = ray.intervalY;
            GPUManager.originalXs[j] = ray.originalX;
            GPUManager.originalYs[j] = ray.originalY;
            GPUManager.rayColors[j] = (0 << 16) | (255 << 8) | 255;
            j++;
        }
        GPUManager.sendAllVars();

        print("Rays loaded into GPU memory!");
    }

    public void initUpdateSystem() throws IOException {
        this.initialPixels = new int[this.renderer.WIDTH*this.renderer.HEIGHT];
        GPUManager.initialPixels = initialPixels;
        this.raytracedPixels = new int[this.renderer.WIDTH*this.renderer.HEIGHT];
        startUpdateThread();
    }

    public void retryInit(GameRenderer renderer) throws IOException {
        this.renderer = renderer;
        this.innerGameRenderer = renderer.innerGameRenderer;
        if (this.renderer == null) {
            System.err.println("Failed GameUpdater Init! renderer is null");
        } else if (this.innerGameRenderer == null) {
            System.err.println("Failed GameUpdater Init! innerGameRenderer is null");
        }
        loadKernels();
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
//        BACKGROUND -----------------------------------------------------------------------------
        GPUManager.runBackgroundKernel(patternCounter); // make test background
        GPUManager.getVars(); // get test background into CPU
        // set test background to raytraced pixels array on GPU (so there are no blank spots)
        cuMemcpyHtoD(raytracedPixelsPointer, Pointer.to(GPUManager.initialPixels), (long) Sizeof.INT * innerGameRenderer.WIDTH*innerGameRenderer.HEIGHT);
        GPUManager.sendRepeatedVars(); // updates GPU data

//        update the initial pixels on CPU from GPU (no need for this, but we do it anyway just to remind our self that to manipulate initialPixels this is required)
        System.arraycopy(GPUManager.initialPixels, 0, this.initialPixels, 0, innerGameRenderer.pixels.length);

//        RAYS -----------------------------------------------------------------------------------

        GPUManager.runTravelRayKernel(); // run the rays
        GPUManager.getVars(); // get the raytraced pixels array from the GPU and send to the CPU
        // set the inner game renderer's pixels to the raytraced pixels

        System.arraycopy(GPUManager.raytracedPixels, 0, innerGameRenderer.pixels, 0, innerGameRenderer.pixels.length);
//        increment the counter - change number to adjust example background pattern speed
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
        GPUManager.sendAllVars();

        final int targetUPS = 120;
        final long targetDelta = 1_000_000_000 / targetUPS; // milliseconds per update (≈8.33ms)

        long lastTime = System.nanoTime();

//        -----------------------------

        long upsUpdateInterval = 1000;
        long lastUPSUpdateTime = System.nanoTime();

//        -----------------------------

        int updateCount = 0;

        while (running) {
            long now = System.nanoTime();

            while (now-lastTime >= targetDelta) {
                update();
                updateCount++;
                lastTime+=targetDelta;

                now = System.nanoTime();

                if (now - lastUPSUpdateTime >= 1_000_000_000L) {
                    innerGameRenderer.UPS = updateCount;
                    updateCount = 0;
                    lastUPSUpdateTime = now;
                }
            }

//            // Update UPS once per second
            if (System.nanoTime() - lastUPSUpdateTime >= 1_000_000_000L) {
                innerGameRenderer.UPS = updateCount;
                updateCount = 0;
                lastUPSUpdateTime = System.nanoTime();
            }
        }
    }
}
