#include <cuda_runtime.h>

extern "C" __global__
void travelRay(int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals, float* originalXs, float* originalYs, int* initialPixels, int* raytracedPixels, int* rayColors, int WIDTH, int HEIGHT, int* rayBandStartIndexes, int* rayBandEndIndexes, int rayBandsCount, int* written, int currentBand, int* rayColorsPerPixel) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < rayBandEndIndexes[currentBand]-rayBandStartIndexes[currentBand]) { // if this kernel is correctly representing a ray in our current ray band
        int rayIndex = i+rayBandStartIndexes[currentBand];
        bool firstOutOfScreen = false;
        bool firstScreenOn = false;
        while (actualXs[rayIndex] >= -WIDTH && actualXs[rayIndex] < WIDTH*2 &&
               actualYs[rayIndex] >= -HEIGHT && actualYs[rayIndex] < HEIGHT*2) { // while this ray hasn't touched the edge of the screen
            actualXs[rayIndex] += xIntervals[rayIndex]; // tick the rays
            actualYs[rayIndex] += yIntervals[rayIndex];

            if (!(actualXs[rayIndex] >= 0 && actualXs[rayIndex] < WIDTH &&
                actualYs[rayIndex] >= 0 && actualYs[rayIndex] < HEIGHT)) {

                if (!firstOutOfScreen) {
                    firstOutOfScreen = true;
                } else if (firstScreenOn) {
                    break; // were out of the visible range
                }
            } else {
                if (!firstScreenOn) {
                    firstScreenOn = true;
                }
            }

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

//             if (atomicCAS(&written[pixelIndex], 0, 1) == 0 && (actualXs[rayIndex] >= 0 && actualXs[rayIndex] < WIDTH &&
//                              actualYs[rayIndex] >= 0 && actualYs[rayIndex] < HEIGHT)) { // if we have not written to this pixel yet, ...

            if (atomicCAS(&written[pixelIndex], 0, 1) == 0) {
                atomicExch(&rayColorsPerPixel[pixelIndex], rayColors[rayIndex]);
            }
//                 int pixelColor = 0;
//
//                 if (currentBand == 0) { // if its the first time, copy from initial
//                     pixelColor = initialPixels[pixelIndex]; // then set the pixel in raytracedPixels
//                 } else { // otherwise, copy from raytracedPixels
//                     pixelColor = raytracedPixels[pixelIndex]; // then ALSO set the pixel in raytracedPixels
//                 }
//
//                 int rayColor = rayColors[rayIndex];
//
//                 int rayR   = (rayColor >> 16) & 0xFF; // find the rgb values of the ray
//                 int rayG = (rayColor >> 8) & 0xFF;
//                 int rayB  = rayColor & 0xFF;
//
//                 int pixR = (pixelColor >> 16) & 0xFF;
//                 int pixG = (pixelColor >> 8) & 0xFF;
//                 int pixB = pixelColor & 0xFF;
//
//                 int newR = min(255, (pixR + rayR) >> 1);
//                 int newG = min(255, (pixG + rayG) >> 1);
//                 int newB = min(255, (pixB + rayB) >> 1);
//
//                 raytracedPixels[pixelIndex] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB; // set the new value
//             }
        }
        actualXs[rayIndex] = originalXs[rayIndex];
        actualYs[rayIndex] = originalYs[rayIndex];
    }
}
