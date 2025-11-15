package net.arjun.justwalkforward.game;

import jcuda.Pointer;
import jcuda.Sizeof;
import net.arjun.justwalkforward.game.raytracing.GPUManager;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;

import static jcuda.driver.JCudaDriver.*;
import static net.arjun.justwalkforward.game.raytracing.GPUManager.*;
import static net.arjun.justwalkforward.game.raytracing.Ray.ray;

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

    public String[] getPtxPath(String path) throws IOException {
        InputStream inputStream = GameUpdater.class.getClassLoader().getResourceAsStream(
                "justwalkforward/raytracing/cuda/kernels/" + path);

        if (inputStream == null) {
            throw new FileNotFoundException("ray_util.cu not found in resources");
        }

        Path tempFile = Files.createTempFile("ray_util", ".ptx");
        tempFile.toFile().deleteOnExit();

        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        return new String[] {tempFile.toAbsolutePath().toString(), tempFile.toString()};
    }

    public void loadKernels() throws IOException {

        GPUManager.allocVars();
        GPUManager.makeContextCurrent();
        String ray_util_fileName = "ray_util.ptx";
        String background_fileName = "background.ptx";
        String reset_written_fileName = "calc_color.ptx";
        try {
            String[] temp = getPtxPath("ray_util.ptx");
            ray_util_fileName = temp[1];
            GPUManager.loadModule(temp[0]);

            temp = getPtxPath("background.ptx");
            background_fileName = temp[1];
            GPUManager.loadModule(temp[0]);

            temp = getPtxPath("calc_color.ptx");
            reset_written_fileName = temp[1];
            GPUManager.loadModule(temp[0]);
        } catch (Exception e) {
            try {
                GPUManager.loadModule("build/resources/main/justwalkforward/raytracing/cuda/kernels/ray_util.ptx");
                GPUManager.loadModule("build/resources/main/justwalkforward/raytracing/cuda/kernels/examples/background.ptx");
                GPUManager.loadModule("build/resources/main/justwalkforward/raytracing/cuda/kernels/calc_color.ptx");
            } catch (Exception e1) {
                System.exit(1);
                return;
            }
        }

        GPUManager.loadFunction("travelRay", ray_util_fileName);
        GPUManager.loadFunction("calculateRow", background_fileName);
        GPUManager.loadFunction("calcColors", reset_written_fileName);
        print("Loaded Kernels!");
    }

    private void print(String text) {
        System.out.println("[GameUpdater] " + text);
    }

    public void addAllInitialTestRays() {
        GPUManager.makeContextCurrent();

        int strength = 125;
        float decay = 0.05f;

        float r = 255;
        float g = 0;
        float b = 0;

        float degreeCounter = 0;

        for (int i = 0; i < 13333; i++) {
            GPUManager.addRay(ray(degreeCounter, strength, decay, new Color((int)r,(int)g,(int)b), WIDTH/2, HEIGHT/2));
            degreeCounter += 360f/39999;


            r -= (255f/13333);
            g += (255f/13333);
        }

        for (int i = 0; i < 13333; i++) {
            GPUManager.addRay(ray(degreeCounter, strength, decay, new Color((int)r,(int)g,(int)b), WIDTH/2, HEIGHT/2));
            degreeCounter += 360f/39999;

            g -= (255f/13333);
            b += (255f/13333);
        }
//
        for (int i = 0; i < 13333; i++) {
            GPUManager.addRay(ray(degreeCounter, strength, decay, new Color((int)r,(int)g,(int)b), WIDTH/2, HEIGHT/2));
            degreeCounter += 360f/39999;

            b -= (255f/13333);
            r += (255f/13333);
        }

        GPUManager.rayBandStarts[0] = 0;
        GPUManager.rayBandEnds[0] = 39999;
        GPUManager.rayBandsCount++;

        GPUManager.sendAllRayData();
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

    public synchronized void update(boolean debugMode) {
        if (debugMode) {
            double start = System.nanoTime();
            GPUManager.runBackgroundKernel(patternCounter);
            System.out.print("Background: " + ((System.nanoTime()-start)/1000000));

            start = System.nanoTime();
            GPUManager.getVars();
            System.out.print(" | Get Vars: " + ((System.nanoTime()-start)/1000000));

            start = System.nanoTime();
            GPUManager.runTravelRayKernel();
            System.out.print(" | Ray Travel: " + ((System.nanoTime()-start)/1000000));

            start = System.nanoTime();
            System.arraycopy(GPUManager.raytracedPixels, 0, innerGameRenderer.pixels, 0, innerGameRenderer.pixels.length);
            System.out.print(" | Copy: " + ((System.nanoTime()-start)/1000000) + "\n");
        } else {
            GPUManager.runBackgroundKernel(patternCounter);

            GPUManager.getVars();

            GPUManager.runTravelRayKernel();

            System.arraycopy(GPUManager.raytracedPixels, 0, innerGameRenderer.pixels, 0, innerGameRenderer.pixels.length);
        }


        patternCounter += 40;

        rayManager.requestMove(2, 1);

        GPUManager.updateRays();
        GPUManager.sendRepeatedVars();

    }

    private synchronized void startUpdateThread() {
        if (running) return;
        running = true;
        updateThread = new Thread(this, "updateGame");
        updateThread.setPriority(10);
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

        long lastUPSUpdateTime = System.nanoTime();

//        -----------------------------

        int updateCount = 0;
        int totalUpdateCount = 0;

        final int MAX_UPDATES_PER_FRAME = 5;


        while (running) {
            long now = System.nanoTime();

            int loops = 0;

            while (now-lastTime >= targetDelta && loops < MAX_UPDATES_PER_FRAME) {
                update(false);
                this.innerGameRenderer.renderFrame = true;
                updateCount++;
                totalUpdateCount++;
                lastTime+=targetDelta;
                loops++;


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
