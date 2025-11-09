#include <cuda_runtime.h>

extern "C" __global__
void travelRay(int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals, float* originalXs, float* originalYs, int* initialPixels, int* raytracedPixels, int* rayColors, int WIDTH, int HEIGHT, int* rayBandStartIndexes, int* rayBandEndIndexes, int rayBandsCount, int* written, int currentBand) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < rayBandEndIndexes[currentBand]-rayBandStartIndexes[currentBand]) { // if this kernel is correctly representing a ray in our current ray band
        int rayIndex = i+rayBandStartIndexes[currentBand];
        while (actualXs[rayIndex] >= 0.0f && actualXs[rayIndex] < WIDTH &&
               actualYs[rayIndex] >= 0.0f && actualYs[rayIndex] < HEIGHT) { // while this ray hasn't touched the edge of the screen
            actualXs[rayIndex] += xIntervals[rayIndex]; // tick the rays
            actualYs[rayIndex] += yIntervals[rayIndex];

            int x = static_cast<int>(floorf(actualXs[rayIndex]));
            int y = static_cast<int>(floorf(actualYs[rayIndex]));

            if (x < 0) { // check if the ray is out of bounds
                x = 0;
            } if (x >= WIDTH) {
                x = WIDTH-1;
            }
            if (y < 0) {
                y = 0;
            } if (y >= HEIGHT) {
                y = HEIGHT-1;
            }

            int pixelIndex = y * WIDTH + x; // find the pixel index in the array

            int old = atomicExch(&written[pixelIndex], 1); // make the current pixel's written status = true

            if (old == 0) { // if we have not written to this pixel yet, ...
                int pixelAtPos = 0;

                if (currentBand == 0) { // if its the first time, copy from initial
                    pixelAtPos = initialPixels[pixelIndex]; // then set the pixel in raytracedPixels
                } else { // otherwise, copy from raytracedPixels
                    pixelAtPos = raytracedPixels[pixelIndex]; // then ALSO set the pixel in raytracedPixels
                }

                int rayColor = rayColors[rayIndex];

                int rayRed   = (rayColor >> 16) & 0xFF; // find the rgb values of the ray
                int rayGreen = (rayColor >> 8) & 0xFF;
                int rayBlue  = rayColor & 0xFF;

//                 if (currentBand == 1) {
//                     printf("RGB: %d, %d, %d\n", rayRed, rayGreen, rayBlue);
//                 }

                int newRed   = min(255, static_cast<int>((((pixelAtPos >> 16) & 0xFF)+rayRed)/2)); // average the colors
                int newGreen = min(255, static_cast<int>((((pixelAtPos >> 8) & 0xFF)+rayGreen)/2));
                int newBlue  = min(255, static_cast<int>((((pixelAtPos) & 0xFF)+rayBlue)/2));

                raytracedPixels[pixelIndex] = (0xFF << 24) | (newRed << 16) | (newGreen << 8) | newBlue; // set the new value
            }
        }
        actualXs[rayIndex] = originalXs[rayIndex];
        actualYs[rayIndex] = originalYs[rayIndex];
    }
}
