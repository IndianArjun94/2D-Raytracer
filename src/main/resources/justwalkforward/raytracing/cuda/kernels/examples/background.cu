extern "C" __global__
void calculateRow(int* patternCounter, int WIDTH, int HEIGHT, int* initialPixels) {
    int i = blockIdx.x * blockDim.x + threadIdx.x; // this is y

    if (i < HEIGHT) {
        for (int x = 0; x < WIDTH; x++) {
            int r = (int) ((sinf(*patternCounter / 1000.0) + 1) / 2 * 255);
            int g = (int) ((sinf(x / 50.0 + *patternCounter / 2000.0) + 1) / 2 * 255);
            int b = (int) ((cosf(i / 50.0 + *patternCounter / 1500.0) + 1) / 2 * 255);

            initialPixels[(i*WIDTH)+x] = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }
    }
}
