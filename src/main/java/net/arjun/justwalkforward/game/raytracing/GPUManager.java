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

    public static float[] actualXs;
    public static float[] actualYs;
    public static float[] xIntervals;
    public static float[] yIntervals;
    public static int[] initialPixels;
    public static int[] raytracedPixels;
    public static int[] rayColors;
    public static float[] originalXs;
    public static float[] originalYs;

    public static int WIDTH;
    public static int HEIGHT;
    public static int raysCount = 0;

    public static GameRenderer.InnerGameRenderer innerGameRenderer;

    public static ArrayList<CUmodule> modules = new ArrayList<>();
    public static ArrayList<String> moduleNames = new ArrayList<>();

    public static ArrayList<CUfunction> functions = new ArrayList<>();
    public static ArrayList<String> functionNames = new ArrayList<>();

    public static CUfunction rayTravelFunction;
    public static CUfunction exampleBackgroundFunction;


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
        } else {
            System.err.println("GPUManager has already been initialized!");
        }
    }

    public static void makeContextCurrent() {
        cuCtxSetCurrent(context);
    }

    public static void allocVars() {
        makeContextCurrent();

        actualXs = new float[defaultArraySize];
        actualYs = new float[defaultArraySize];

        xIntervals = new float[defaultArraySize];
        yIntervals = new float[defaultArraySize];

        originalXs = new float[defaultArraySize];
        originalYs = new float[defaultArraySize];

        initialPixels = innerGameRenderer.pixels;
        raytracedPixels = new int[innerGameRenderer.WIDTH*innerGameRenderer.HEIGHT];

        rayColors = new int[defaultArraySize]; // the min of this should be 1 element so it doesn't ever error.

        WIDTH = innerGameRenderer.WIDTH;
        HEIGHT = innerGameRenderer.HEIGHT;

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

        raysCountPointer = new CUdeviceptr();
        WIDTHPointer = new CUdeviceptr();
        HEIGHTPointer = new CUdeviceptr();

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

        cuMemcpyHtoD(actualXsPointer, Pointer.to(actualXs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(actualYsPointer, Pointer.to(actualYs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(xIntervalsPointer, Pointer.to(xIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(yIntervalsPointer, Pointer.to(yIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(initialPixelsPointer, Pointer.to(initialPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(raytracedPixelsPointer, Pointer.to(raytracedPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(rayColorsPointer, Pointer.to(rayColors), (long) Sizeof.INT * rayColors.length);
        cuMemcpyHtoD(raysCountPointer, Pointer.to(new int[]{raysCount}), Sizeof.INT);
        cuMemcpyHtoD(WIDTHPointer, Pointer.to(new int[]{WIDTH}), Sizeof.INT);
        cuMemcpyHtoD(HEIGHTPointer, Pointer.to(new int[]{HEIGHT}), Sizeof.INT);
        cuMemcpyHtoD(originalXsPointer, Pointer.to(originalXs), (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(originalYsPointer, Pointer.to(originalYs), (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(patternCounterPointer, Pointer.to(new int[]{0}), Sizeof.INT);
    }

    public static void sendAllVars() { // called by externals, so makeContextCurrent() shouldn't be called (or else the external-called stat would be reset to the current Thread)
        long bytesToCopy = (long) raysCount * Sizeof.FLOAT;

        cuMemcpyHtoD(actualXsPointer, Pointer.to(actualXs),  bytesToCopy);
        cuMemcpyHtoD(actualYsPointer, Pointer.to(actualYs),  bytesToCopy);
        cuMemcpyHtoD(xIntervalsPointer, Pointer.to(xIntervals),  bytesToCopy);
        cuMemcpyHtoD(yIntervalsPointer, Pointer.to(yIntervals),  bytesToCopy);
        cuMemcpyHtoD(initialPixelsPointer, Pointer.to(initialPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(raytracedPixelsPointer, Pointer.to(raytracedPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
        cuMemcpyHtoD(rayColorsPointer, Pointer.to(rayColors), (long) Sizeof.INT * rayColors.length);
        cuMemcpyHtoD(raysCountPointer, Pointer.to(new int[]{raysCount}), Sizeof.INT);
        cuMemcpyHtoD(WIDTHPointer, Pointer.to(new int[]{WIDTH}), Sizeof.INT);
        cuMemcpyHtoD(HEIGHTPointer, Pointer.to(new int[]{HEIGHT}), Sizeof.INT);
        cuMemcpyHtoD(originalXsPointer, Pointer.to(originalXs), (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(originalYsPointer, Pointer.to(originalYs), (long) Sizeof.FLOAT * defaultArraySize);
    }

    public static void sendRepeatedVars() {
        cuMemcpyHtoD(initialPixelsPointer, Pointer.to(initialPixels), (long) Sizeof.INT * WIDTH*HEIGHT);
    }


    public static void getVars() {
//        long bytesToCopy = (long) raysCount * Sizeof.FLOAT;

//        cuMemcpyDtoH(Pointer.to(actualXs), actualXsPointer, bytesToCopy);
//        cuMemcpyDtoH(Pointer.to(actualYs), actualYsPointer, bytesToCopy);
        cuMemcpyDtoH(Pointer.to(initialPixels), initialPixelsPointer, (long) Sizeof.INT * WIDTH*HEIGHT);
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
        if (raysCount < 1) {
            return;
        }

        Pointer kernelParams = Pointer.to(
                Pointer.to(new int[]{raysCount}),
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
                Pointer.to(new int[]{HEIGHT})
        );

        int threadsPerBlock = 256; // good default
        int blocksPerGrid = (raysCount + threadsPerBlock - 1) / threadsPerBlock;

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

        cuLaunchKernel(rayTravelFunction,
                blocksPerGrid, 1, 1,        // Grid dimension
                threadsPerBlock, 1, 1,      // Block dimension
                0, null,                     // Shared memory size and stream
                kernelParams, null
        );

        cuCtxSynchronize();  // Wait for completion
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
