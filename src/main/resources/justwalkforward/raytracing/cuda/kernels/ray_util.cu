#include <cuda_runtime.h>

extern "C" __global__
void tickAll(int amountToTick, int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < amountToTick * raysCount) {   // <<< braces added
        int index = i / amountToTick;
        if (index < raysCount) {          // nested check
            atomicAdd(&actualXs[index], xIntervals[index]);
            atomicAdd(&actualYs[index], yIntervals[index]);
//             actualXs[index] += xIntervals[index];
//             actualYs[index] += yIntervals[index];
        }
    }
}
