package net.arjun.justwalkforward.game.raytracing;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import net.arjun.justwalkforward.game.GameRenderer;
import java.util.ArrayList;
import java.util.Objects;

import static jcuda.driver.JCudaDriver.*;

public class GPUManager {
    public static boolean initialized = false;
    public static int defaultArraySize = 20000000;

    public static CUdevice device;
    public static CUcontext context;

    public static CUdeviceptr actualXsPointer;
    public static CUdeviceptr actualYsPointer;
    public static CUdeviceptr xIntervalsPointer;
    public static CUdeviceptr yIntervalsPointer;
    public static CUdeviceptr initialPixelsPointer;
    public static CUdeviceptr raytracedPixelsPointer;
    public static CUdeviceptr rayColorsPointer;
    public static CUdeviceptr originalXsPointer;
    public static CUdeviceptr originalYsPointer;

    public static CUdeviceptr WIDTHPointer;
    public static CUdeviceptr HEIGHTPointer;
    public static CUdeviceptr raysCountPointer;

    public static CUdeviceptr patternCounterPointer;
    public static CUdeviceptr rayColorsPerPixelPointer;

    public static CUdeviceptr originalStrengthPointer;
    public static CUdeviceptr strengthDecayPointer;
    public static CUdeviceptr strengthPerPixelPointer;

    public static float[] actualXs;
    public static float[] actualYs;
    public static float[] xIntervals;
    public static float[] yIntervals;
    public static int[] initialPixels;
    public static int[] raytracedPixels;
    public static int[] rayColors;
    public static float[] originalXs;
    public static float[] originalYs;
    public static int[] rayColorsPerPixel;
    public static int[] originalStrengths;
    public static float[] strengthDecays;
    public static int[] strengthPerPixel;

    public static RayManager rayManager;

    public static int WIDTH;
    public static int HEIGHT;
    public static int usableRaysCount = 0;
    public static int totalRaysCount = 0;

    public static GameRenderer.InnerGameRenderer innerGameRenderer;

    public static ArrayList<CUmodule> modules = new ArrayList<>();
    public static ArrayList<String> moduleNames = new ArrayList<>();

    public static ArrayList<CUfunction> functions = new ArrayList<>();
    public static ArrayList<String> functionNames = new ArrayList<>();

    public static CUfunction rayTravelFunction;
    public static CUfunction exampleBackgroundFunction;
    public static CUfunction calcColorFunction;

    public static int[] rayBandStarts;
    public static int[] rayBandEnds;

    public static CUdeviceptr rayBandStartsPointer;
    public static CUdeviceptr rayBandEndsPointer;

    public static int rayBandsCount = 0;

    public static int[] written;
    public static CUdeviceptr writtenPointer;

    public static void init(GameRenderer.InnerGameRenderer innerGameRenderer) {
        if (!initialized) {
            JCudaDriver.setExceptionsEnabled(true);
            cuInit(0);

            device = new CUdevice();
            cuDeviceGet(device, 0);

            context = new CUcontext();
            cuCtxCreate(context, 0, device);

            initialized = true;

            GPUManager.innerGameRenderer = innerGameRenderer;

            rayManager = new RayManager(); // allocate space for up to 500k rays
            rayManager.start();
        } else {
            System.err.println("GPUManager has already been initialized!");
        }
    }

    public static void makeContextCurrent() {
        cuCtxSetCurrent(context);
    }

    public static void allocVars() {
        makeContextCurrent();

        WIDTH = innerGameRenderer.WIDTH;
        HEIGHT = innerGameRenderer.HEIGHT;

        actualXs = new float[defaultArraySize];
        actualYs = new float[defaultArraySize];

        xIntervals = new float[defaultArraySize];
        yIntervals = new float[defaultArraySize];

        originalXs = new float[defaultArraySize];
        originalYs = new float[defaultArraySize];

        originalStrengths = new int[defaultArraySize];
        strengthDecays = new float[defaultArraySize];
        strengthPerPixel = new int[WIDTH*HEIGHT];

        originalStrengthPointer = new CUdeviceptr();
        strengthDecayPointer = new CUdeviceptr();
        strengthPerPixelPointer = new CUdeviceptr();

        cuMemAlloc(originalStrengthPointer, (long) Sizeof.INT * defaultArraySize);
        cuMemAlloc(strengthDecayPointer, (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(strengthPerPixelPointer, (long) Sizeof.INT * WIDTH*HEIGHT);

        rayColorsPerPixel = new int[WIDTH*HEIGHT];

        written = new int[innerGameRenderer.WIDTH*innerGameRenderer.HEIGHT];

        initialPixels = innerGameRenderer.pixels;
        raytracedPixels = new int[innerGameRenderer.WIDTH*innerGameRenderer.HEIGHT];

        rayColors = new int[defaultArraySize]; // the min of this should be 1 element so it doesn't ever error.

        rayBandStarts = new int[500];
        rayBandEnds = new int[500];

        rayBandStartsPointer = new CUdeviceptr();
        rayBandEndsPointer = new CUdeviceptr();

        cuMemAlloc(rayBandStartsPointer, (long) Sizeof.INT*500);
        cuMemAlloc(rayBandEndsPointer, (long) Sizeof.INT*500);

        cuMemcpyHtoD(rayBandStartsPointer, Pointer.to(rayBandStarts), (long) Sizeof.INT*500);
        cuMemcpyHtoD(rayBandEndsPointer, Pointer.to(rayBandEnds), (long) Sizeof.INT*500);

        patternCounterPointer = new CUdeviceptr();

        actualXsPointer = new CUdeviceptr();
        actualYsPointer = new CUdeviceptr();
        xIntervalsPointer = new CUdeviceptr();
        yIntervalsPointer = new CUdeviceptr();
        initialPixelsPointer = new CUdeviceptr();
        raytracedPixelsPointer = new CUdeviceptr();
        rayColorsPointer = new CUdeviceptr();
        originalXsPointer = new CUdeviceptr();
        originalYsPointer = new CUdeviceptr();
        writtenPointer = new CUdeviceptr();

        raysCountPointer = new CUdeviceptr();
        WIDTHPointer = new CUdeviceptr();
        HEIGHTPointer = new CUdeviceptr();
        rayColorsPerPixelPointer = new CUdeviceptr();

        cuMemAlloc(actualXsPointer, (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(actualYsPointer, (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(xIntervalsPointer,  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(yIntervalsPointer,  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(initialPixelsPointer, (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemAlloc(raytracedPixelsPointer, (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemAlloc(rayColorsPointer, (long) Sizeof.INT * rayColors.length);
        cuMemAlloc(raysCountPointer, Sizeof.INT);
        cuMemAlloc(WIDTHPointer, Sizeof.INT);
        cuMemAlloc(HEIGHTPointer, Sizeof.INT);
        cuMemAlloc(originalXsPointer, (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(originalYsPointer, (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(patternCounterPointer, Sizeof.INT);
        cuMemAlloc(writtenPointer, (long) WIDTH*HEIGHT * Sizeof.INT);
        cuMemAlloc(rayColorsPerPixelPointer, (long) WIDTH*HEIGHT * Sizeof.INT);

        cuMemcpyHtoD(actualXsPointer, Pointer.to(actualXs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(actualYsPointer, Pointer.to(actualYs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(xIntervalsPointer, Pointer.to(xIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(yIntervalsPointer, Pointer.to(yIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(initialPixelsPointer, Pointer.to(initialPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(raytracedPixelsPointer, Pointer.to(raytracedPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(rayColorsPointer, Pointer.to(rayColors), (long) Sizeof.INT * rayColors.length);
        cuMemcpyHtoD(raysCountPointer, Pointer.to(new int[]{usableRaysCount}), Sizeof.INT);
        cuMemcpyHtoD(WIDTHPointer, Pointer.to(new int[]{WIDTH}), Sizeof.INT);
        cuMemcpyHtoD(HEIGHTPointer, Pointer.to(new int[]{HEIGHT}), Sizeof.INT);
        cuMemcpyHtoD(originalXsPointer, Pointer.to(originalXs), (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(originalYsPointer, Pointer.to(originalYs), (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(patternCounterPointer, Pointer.to(new int[]{0}), Sizeof.INT);
        cuMemcpyHtoD(writtenPointer, Pointer.to(written), (long) WIDTH*HEIGHT * Sizeof.INT);
        cuMemcpyHtoD(rayColorsPerPixelPointer, Pointer.to(rayColorsPerPixel), (long) WIDTH*HEIGHT * Sizeof.INT);

        cuMemcpyHtoD(rayBandStartsPointer, Pointer.to(rayBandStarts), (long) Sizeof.INT*500);
        cuMemcpyHtoD(rayBandEndsPointer, Pointer.to(rayBandEnds), (long) Sizeof.INT*500);

        cuMemcpyHtoD(originalStrengthPointer, Pointer.to(originalStrengths), (long) Sizeof.INT * defaultArraySize);
        cuMemcpyHtoD(strengthDecayPointer, Pointer.to(strengthDecays), (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(strengthPerPixelPointer, Pointer.to(strengthPerPixel), (long) Sizeof.INT * WIDTH*HEIGHT);
    }

    public static void sendAllVars() { // called by externals, so makeContextCurrent() shouldn't be called (or else the external-called stat would be reset to the current Thread)
        long bytesToCopy = (long) usableRaysCount * Sizeof.FLOAT;

        cuMemcpyHtoD(actualXsPointer, Pointer.to(actualXs),  (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(actualYsPointer, Pointer.to(actualYs),  (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(xIntervalsPointer, Pointer.to(xIntervals),  (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(yIntervalsPointer, Pointer.to(yIntervals),  (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(initialPixelsPointer, Pointer.to(initialPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(raytracedPixelsPointer, Pointer.to(raytracedPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(rayColorsPointer, Pointer.to(rayColors), (long) Sizeof.INT * rayColors.length);
        cuMemcpyHtoD(raysCountPointer, Pointer.to(new int[]{usableRaysCount}), Sizeof.INT);
        cuMemcpyHtoD(WIDTHPointer, Pointer.to(new int[]{WIDTH}), Sizeof.INT);
        cuMemcpyHtoD(HEIGHTPointer, Pointer.to(new int[]{HEIGHT}), Sizeof.INT);
        cuMemcpyHtoD(originalXsPointer, Pointer.to(originalXs), (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(originalYsPointer, Pointer.to(originalYs), (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(patternCounterPointer, Pointer.to(new int[]{0}), Sizeof.INT);
        cuMemcpyHtoD(writtenPointer, Pointer.to(written), (long) WIDTH*HEIGHT * Sizeof.INT);
        cuMemcpyHtoD(rayColorsPerPixelPointer, Pointer.to(rayColorsPerPixel), (long) WIDTH*HEIGHT * Sizeof.INT);

        cuMemcpyHtoD(rayBandStartsPointer, Pointer.to(rayBandStarts), (long) Sizeof.INT*500);
        cuMemcpyHtoD(rayBandEndsPointer, Pointer.to(rayBandEnds), (long) Sizeof.INT*500);

        cuMemcpyHtoD(originalStrengthPointer, Pointer.to(originalStrengths), (long) Sizeof.INT * usableRaysCount);
        cuMemcpyHtoD(strengthDecayPointer, Pointer.to(strengthDecays), (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(strengthPerPixelPointer, Pointer.to(strengthPerPixel), (long) Sizeof.INT * WIDTH*HEIGHT);
    }

    public static void sendRepeatedVars() {
        cuMemcpyHtoD(originalXsPointer, Pointer.to(originalXs), (long) Sizeof.FLOAT * usableRaysCount);
        cuMemcpyHtoD(originalYsPointer, Pointer.to(originalYs), (long) Sizeof.FLOAT * usableRaysCount);
    }

    public static void sendAllRayData() {
        int i = 0;
        for (Ray ray : rayManager.rays) {
//            if (ray.originalX >= 0 && ray.originalX < WIDTH &&
//                ray.originalY >= 0 && ray.originalY < HEIGHT) { // checks if the ray still starts in bounds

                actualXs[i] = ray.actualX;
                actualYs[i] = ray.actualY;
                xIntervals[i] = ray.intervalX;
                yIntervals[i] = ray.intervalY;
                originalXs[i] = ray.originalX;
                originalYs[i] = ray.originalY;
                originalStrengths[i] = ray.strength;
                strengthDecays[i] = ray.decay;

                rayColors[i] = (ray.color.getRed() << 16) | (ray.color.getGreen() << 8) | ray.color.getBlue();
            i++;
//            }
        }

        usableRaysCount = i;
    }

    public static void updateRays() { // doesn't send to gpu, gets from ray manager
        int i = 0;
        for (Ray ray : rayManager.rays) {
//            if (ray.originalX >= 0 && ray.originalX < WIDTH &&
//                ray.originalY >= 0 && ray.originalY < HEIGHT) { // checks if the ray still starts in bounds

//                actualXs[i] = ray.actualX;
//                actualYs[i] = ray.actualY;
//                xIntervals[i] = ray.intervalX;
//                yIntervals[i] = ray.intervalY;
                originalXs[i] = ray.originalX;
                originalYs[i] = ray.originalY;
//                rayColors[i] = (ray.color.getRed() << 16) | (ray.color.getGreen() << 8) | ray.color.getBlue();
                i++;
//            }
        }

        usableRaysCount = i;
    }

    public static void addRay(Ray ray) {
        rayManager.rays.add(ray);
        totalRaysCount++;
    }

    public static void getVars() {
        makeContextCurrent();
//        long bytesToCopy = (long) raysCount * Sizeof.FLOAT;

//        cuMemcpyDtoH(Pointer.to(actualXs), actualXsPointer, bytesToCopy);
//        cuMemcpyDtoH(Pointer.to(actualYs), actualYsPointer, bytesToCopy);
//        cuMemcpyDtoH(Pointer.to(initialPixels), initialPixelsPointer, (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyDtoH(Pointer.to(raytracedPixels), raytracedPixelsPointer, (long) Sizeof.INT * WIDTH*HEIGHT);
    }

    public static void loadModule(String path) {
        CUmodule module = new CUmodule();
        cuModuleLoad(module, path);
        modules.add(module);
        String[] pathSplit = path.split("/");
        moduleNames.add(pathSplit[pathSplit.length-1]);
    }

    public static void loadFunction(String functionName, String moduleName) {
        CUfunction function = new CUfunction();
        for (int i = 0; i < modules.size(); i++) {
            if (Objects.equals(moduleNames.get(i), moduleName)) {
                cuModuleGetFunction(function, modules.get(i), functionName);
                functions.add(function);
                functionNames.add(functionName);
                return;
            }
        }
        System.err.println("GPUManager couldn't find function: " + functionName + "in module: " + moduleName);
        System.err.println("GPUManager: Make sure the module parameter is ONLY the NAME of the module (with .ptx), not the whole path.");
    }

    public static void runTravelRayKernel() {
        if (usableRaysCount < 1) {
            return;
        }

        if (rayTravelFunction == null) {
            boolean functionLoaded = false;
            for (int i = 0; i < functions.size(); i++) {
                if (Objects.equals(functionNames.get(i), "travelRay")) {
                    rayTravelFunction = functions.get(i);
                    functionLoaded = true;
                }
            }
            if (!functionLoaded) { System.err.println("GPUManager couldn't find travelRay function because it was not loaded"); }
        }

        if (calcColorFunction == null) {
            boolean functionLoaded = false;
            for (int i = 0; i < functions.size(); i++) {
                if (Objects.equals(functionNames.get(i), "calcColors")) {
                    calcColorFunction = functions.get(i);
                    functionLoaded = true;
                }
            }
            if (!functionLoaded) { System.err.println("GPUManager couldn't find calcColors function because it was not loaded"); }
        }

//        All checks done, now launch kernel

        for (int i = 0; i < rayBandsCount; i++) {
            Pointer kernelParams = Pointer.to(Pointer.to(new int[]{usableRaysCount}),
                    Pointer.to(actualXsPointer),   // <-- PASS DEVICE POINTER
                    Pointer.to(xIntervalsPointer), // <-- PASS DEVICE POINTER
                    Pointer.to(actualYsPointer),   // <-- PASS DEVICE POINTER
                    Pointer.to(yIntervalsPointer),  // <-- PASS DEVICE POINTER
                    Pointer.to(originalXsPointer),
                    Pointer.to(originalYsPointer),
                    Pointer.to(initialPixelsPointer),
                    Pointer.to(raytracedPixelsPointer),
                    Pointer.to(rayColorsPointer),
                    Pointer.to(new int[]{WIDTH}),
                    Pointer.to(new int[]{HEIGHT}),
                    Pointer.to(rayBandStartsPointer),
                    Pointer.to(rayBandEndsPointer),
                    Pointer.to(new int[]{rayBandsCount}),
                    Pointer.to(writtenPointer),
                    Pointer.to(new int[]{i}),
                    Pointer.to(rayColorsPerPixelPointer),
                    Pointer.to(originalStrengthPointer),
                    Pointer.to(strengthDecayPointer),
                    Pointer.to(strengthPerPixelPointer));

            cuLaunchKernel(rayTravelFunction,
                    ((rayBandEnds[i]-rayBandStarts[i]+255)/256), 1, 1,
                    256, 1, 1,
                    0, null,
                    kernelParams, null);

            cuCtxSynchronize();


            cuLaunchKernel(calcColorFunction,
                    Math.ceilDiv(WIDTH*HEIGHT, 256), 1, 1,
                    256, 1, 1,
                    0, null,
                    kernelParams, null);

            cuMemsetD32(rayColorsPerPixelPointer, 0, (long)WIDTH * HEIGHT);
            cuMemsetD32(strengthPerPixelPointer, 0, (long)WIDTH * HEIGHT);
            cuMemsetD32(writtenPointer, 0, (long) WIDTH * HEIGHT);

            cuCtxSynchronize();
        }
    }

    public static void runBackgroundKernel(int patternCounter) {
        cuMemcpyHtoD(patternCounterPointer, Pointer.to(new int[]{patternCounter}), Sizeof.INT);

        Pointer kernelParams = Pointer.to(
                Pointer.to(patternCounterPointer),
                Pointer.to(new int[]{WIDTH}),
                Pointer.to(new int[]{HEIGHT}),
                Pointer.to(initialPixelsPointer)
        );

        int threadsPerBlock = 256; // good default
        int blocksPerGrid = Math.ceilDiv(WIDTH*HEIGHT, threadsPerBlock);

        if (exampleBackgroundFunction == null) {
            boolean functionLoaded = false;
            for (int i = 0; i < functions.size(); i++) {
                if (Objects.equals(functionNames.get(i), "calculateRow")) {
                    exampleBackgroundFunction = functions.get(i);
                    functionLoaded = true;
                }
            }
            if (!functionLoaded) { System.err.println("GPUManager couldn't find tickAll function because it was not loaded"); }
        }

        cuLaunchKernel(exampleBackgroundFunction,
                blocksPerGrid, 1, 1,        // Grid dimension
                threadsPerBlock, 1, 1,      // Block dimension
                0, null,                     // Shared memory size and stream
                kernelParams, null
        );

        cuCtxSynchronize();
    }
}
