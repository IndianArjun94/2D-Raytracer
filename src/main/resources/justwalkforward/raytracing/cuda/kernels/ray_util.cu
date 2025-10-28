#include <cuda_runtime.h>

extern "C" __global__
void travelRay(int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals, float* originalXs, float* originalYs, int* initialPixels, int* raytracedPixels, int* rayColors, int WIDTH, int HEIGHT) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < raysCount) {
        while (actualXs[i] >= 0.0f && actualXs[i] < WIDTH &&
               actualYs[i] >= 0.0f && actualYs[i] < HEIGHT) {
            actualXs[i] += xIntervals[i];
            actualYs[i] += yIntervals[i];

            int x = static_cast<int>(floorf(actualXs[i]));
            int y = static_cast<int>(floorf(actualYs[i]));

            if (x < 0) {
                x = 0;
            } if (x >= WIDTH) {
                x = WIDTH-1;
            }
            if (y < 0) {
                y = 0;
            } if (y >= HEIGHT) {
                y = HEIGHT-1;
            }

            int pixelIndex = y * WIDTH + x;
            int pixelAtPos = initialPixels[pixelIndex];
            int rayColor = rayColors[i];

            int rayRed   = (rayColor >> 16) & 0xFF;
            int rayGreen = (rayColor >> 8) & 0xFF;
            int rayBlue  = rayColor & 0xFF;

//             float r_contrib = rayRed > 0 ? rayRed / fmaxf(1.0f, 180.0f / rayRed) : 0.0f;
//             float g_contrib = rayGreen > 0 ? rayGreen / fmaxf(1.0f, 180.0f / rayGreen) : 0.0f;
//             float b_contrib = rayBlue > 0 ? rayBlue / fmaxf(1.0f, 180.0f / rayBlue) : 0.0f;

            int newRed   = min(255, static_cast<int>((((pixelAtPos >> 16) & 0xFF)+rayRed)/2));
            int newGreen = min(255, static_cast<int>((((pixelAtPos >> 8) & 0xFF)+rayGreen)/2));
            int newBlue  = min(255, static_cast<int>((((pixelAtPos) & 0xFF)+rayBlue)/2));

            raytracedPixels[pixelIndex] = (0xFF << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
        }
            actualXs[i] = originalXs[i];
            actualYs[i] = originalYs[i];
    }
}
