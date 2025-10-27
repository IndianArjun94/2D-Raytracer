extern "C" __global__
void tickAll(int amountToTick, int raysCount, float[] actualXs, float[] xIntervals, float[] actualYs, float[] yIntervals) {
        int i = blockIdx.x * blockDim.x + threadIdx.x;

        if (i < amountToTick * raysCount) {
            int index = (int)ceilf((float)i / amountToTick); // ceil divides i/amountToTick to get the current ray we are calculating
            actualXs[index] += xIntervals[index];
            actualYs[index] += yIntervals[index];

        }
}