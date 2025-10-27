extern "C" __global__
void multiplyByTwo(float *number) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    if (i < 1) {
        *number *= 2.0f;
    }
}
