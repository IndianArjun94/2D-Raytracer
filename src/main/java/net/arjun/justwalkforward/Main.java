package net.arjun.justwalkforward;

import net.arjun.justwalkforward.game.GameRenderer;
import net.arjun.justwalkforward.game.GameUpdater;

import java.awt.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        GameRenderer gameRenderer = new GameRenderer(new Dimension(1920, 1080));
        gameRenderer.initRenderSystem();

        GameUpdater gameUpdater = new GameUpdater(gameRenderer);
        gameUpdater.initUpdateSystem();


        // put the number 42 in GPU ram
//        GPUManager.init();
//
//        int coolNumber = 42;
//        CUdeviceptr coolNumberPointerOnDevice = new CUdeviceptr();
//
//        cuMemAlloc(coolNumberPointerOnDevice, Sizeof.INT);
//        cuMemcpyHtoD(coolNumberPointerOnDevice, Pointer.to(new int[]{coolNumber}), Sizeof.INT);
//
//        // run one CUDA kernal to change the number (*2)
//        String ptxPath = "build/resources/main/justwalkforward/raytracing/cuda/kernels/examples/multiply.ptx";
//        File file = new File(ptxPath);
//        System.out.println("File exists: " + file.exists() + " -> " + file.getAbsolutePath());
//
//        CUmodule module = new CUmodule();
//        cuModuleLoad(module, ptxPath);
//
//        CUfunction function = new CUfunction();
//        cuModuleGetFunction(function, module, "multiplyByTwo");
//
//        Pointer kernalParameters = Pointer.to(
//                Pointer.to(coolNumberPointerOnDevice)
//        );
//
//        cuLaunchKernel(function, 1, 1, 1, 1, 1, 1, 0, null, kernalParameters, null);
//        cuCtxSynchronize();
//
//        // output the resulting number
//        int[] output = new int[1];
//        cuMemcpyDtoH(Pointer.to(output), coolNumberPointerOnDevice, Sizeof.INT);
//
//        System.out.println(output[0]);
    }
}