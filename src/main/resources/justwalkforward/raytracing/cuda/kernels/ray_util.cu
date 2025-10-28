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

            float r_contrib = rayRed > 0 ? rayRed / fmaxf(1.0f, 180.0f / rayRed) : 0.0f;
            float g_contrib = rayGreen > 0 ? rayGreen / fmaxf(1.0f, 180.0f / rayGreen) : 0.0f;
            float b_contrib = rayBlue > 0 ? rayBlue / fmaxf(1.0f, 180.0f / rayBlue) : 0.0f;

            int newRed   = min(255, ((pixelAtPos >> 16) & 0xFF) + static_cast<int>(r_contrib));
            int newGreen = min(255, ((pixelAtPos >> 8) & 0xFF) + static_cast<int>(g_contrib));
            int newBlue  = min(255, (pixelAtPos & 0xFF) + static_cast<int>(b_contrib));

            raytracedPixels[pixelIndex] = (0xFF << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
//             raytracedPixels[pixelIndex] = 0xFFFF00FF;
        }

//         if (originalXs[i] == WIDTH/2) {
            actualXs[i] = originalXs[i];
//         }

//         if (originalYs[i] = HEIGHT/2) {
            actualYs[i] = originalYs[i];
//         }
    }
}
