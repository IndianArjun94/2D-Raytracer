package net.arjun.justwalkforward.game.raytracing;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

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

    public static float[] actualXs;
    public static float[] actualYs;
    public static float[] xIntervals;
    public static float[] yIntervals;

    public static ArrayList<CUmodule> modules = new ArrayList<>();
    public static ArrayList<String> moduleNames = new ArrayList<>();

    public static ArrayList<CUfunction> functions = new ArrayList<>();
    public static ArrayList<String> functionNames = new ArrayList<>();

    public static int raysCount = 0;

    public static void init() {
        if (!initialized) {
            JCudaDriver.setExceptionsEnabled(true);
            cuInit(0);

            device = new CUdevice();
            cuDeviceGet(device, 0);

            context = new CUcontext();
            cuCtxCreate(context, 0, device);

            initialized = true;
        } else {
            System.err.println("GPUManager has already been initialized!");
        }
    }

    public static void makeContextCurrent() {
        cuCtxSetCurrent(context);
    }

    public static void allocVars() {
        makeContextCurrent();

        actualXs = new float[20000000];
        actualYs = new float[20000000];

        xIntervals = new float[20000000];
        yIntervals = new float[20000000];

        actualXsPointer = new CUdeviceptr();
        actualYsPointer = new CUdeviceptr();
        xIntervalsPointer = new CUdeviceptr();
        yIntervalsPointer = new CUdeviceptr();

        cuMemAlloc(actualXsPointer, (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(actualYsPointer, (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(xIntervalsPointer,  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemAlloc(yIntervalsPointer,  (long) Sizeof.FLOAT * defaultArraySize);

        cuMemcpyHtoD(actualXsPointer, Pointer.to(actualXs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(actualYsPointer, Pointer.to(actualYs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(xIntervalsPointer, Pointer.to(xIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(yIntervalsPointer, Pointer.to(yIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
    }

    public static void updateVars() { // called by externals, so makeContextCurrent() shouldn't be called (or else the external-called stat would be reset to the current Thread)
        cuMemcpyHtoD(actualXsPointer, Pointer.to(actualXs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(actualYsPointer, Pointer.to(actualYs),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(xIntervalsPointer, Pointer.to(xIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
        cuMemcpyHtoD(yIntervalsPointer, Pointer.to(yIntervals),  (long) Sizeof.FLOAT * defaultArraySize);
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

    public static void runTickAllKernel(int amountToTick) {
        if (raysCount < 1) {
            return;
        }

        Pointer kernelParams = Pointer.to(
                Pointer.to(new int[]{amountToTick}),
                Pointer.to(new int[]{raysCount}),
                Pointer.to(actualXsPointer),   // <-- PASS DEVICE POINTER
                Pointer.to(xIntervalsPointer), // <-- PASS DEVICE POINTER
                Pointer.to(actualYsPointer),   // <-- PASS DEVICE POINTER
                Pointer.to(yIntervalsPointer)  // <-- PASS DEVICE POINTER
        );

        int threadsPerBlock = 360; // good default
        int blocksPerGrid = (raysCount + threadsPerBlock - 1) / threadsPerBlock;

        CUfunction function = new CUfunction();
        boolean functionLoaded = false;
        for (int i = 0; i < functions.size(); i++) {
            if (Objects.equals(functionNames.get(i), "tickAll")) {
                function = functions.get(i);
                functionLoaded = true;
            }
        }
        if (!functionLoaded) { System.err.println("GPUManager couldn't find tickAll function because it was not loaded"); }

        cuLaunchKernel(function,
                blocksPerGrid, 1, 1,        // Grid dimension
                threadsPerBlock, 1, 1,      // Block dimension
                0, null,                     // Shared memory size and stream
                kernelParams, null
        );

        cuCtxSynchronize();  // Wait for completion
    }
}
