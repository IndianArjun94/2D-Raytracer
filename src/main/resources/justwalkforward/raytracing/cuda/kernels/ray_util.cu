#include <cuda_runtime.h>

extern "C" __global__
void travelRay(int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals, int* initialPixels, int* raytracedPixels, int width, int height) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < raysCount) {   // <<< braces added
//         int index = i / amountToTick;
        if (i < raysCount) {          // nested check
//             atomicAdd(&actualXs[index], xIntervals[index]);
//             atomicAdd(&actualYs[index], yIntervals[index]);
            actualXs[i] += xIntervals[i]=amountToTick;
            actualYs[i] += yIntervals[i]=amountToTick;
        }
    }
}