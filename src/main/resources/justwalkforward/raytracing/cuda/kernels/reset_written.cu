#include <cuda_runtime.h>

extern "C" __global__
void reset(int* written, int valueToSet, int WIDTH, int HEIGHT) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < WIDTH*HEIGHT) { // if this kernel is correctly representing a ray in our current ray band
        written[i] = valueToSet;
    }
}
