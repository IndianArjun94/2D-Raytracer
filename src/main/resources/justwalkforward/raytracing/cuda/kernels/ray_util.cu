#include <cuda_runtime.h>

extern "C"



__global__ void travelRay(int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals, float* originalXs, float* originalYs, int* initialPixels, int* raytracedPixels, int* rayColors, int WIDTH, int HEIGHT, int* rayBandStartIndexes, int* rayBandEndIndexes, int rayBandsCount, int* written, int currentBand, int* rayColorsPerPixel, int* originalStrengths, float* strengthDecays, int* strengthPerPixel, int* hitboxXs, int* hitboxYs, int* hitboxWidths, int* hitboxHeights, int* rayIntervalXInverses, int* rayIntervalYInverses, int hitboxCount) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < rayBandEndIndexes[currentBand]-rayBandStartIndexes[currentBand]) { // if this kernel is correctly representing a ray in our current ray band


        int rayIndex = i+rayBandStartIndexes[currentBand];


        if (!(originalXs[rayIndex] >= -WIDTH && originalXs[rayIndex] < WIDTH*2 &&
            originalYs[rayIndex] >= -HEIGHT && originalYs[rayIndex] < HEIGHT*2)) {
            return;
        }


        bool firstOutOfScreen = false;
        bool firstScreenOn = false;

        double strength = originalStrengths[rayIndex];

        float minX = -WIDTH;
        float maxX = WIDTH*2;
        float minY = -HEIGHT;
        float maxY = HEIGHT*2;

        float x = actualXs[rayIndex];
        float y = actualYs[rayIndex];



        while (x >= minX && x < maxX && y >= minY && y < maxY && strength >= 0) { // while this ray hasn't touched the edge of the screen

// ==================== COORDINATE CALCULATIONS ====================

            actualXs[rayIndex] += xIntervals[rayIndex]*rayIntervalXInverses[rayIndex]; // tick the rays
            actualYs[rayIndex] += yIntervals[rayIndex]*rayIntervalYInverses[rayIndex]; // reverse the values if bounced

            bool offBounds = false;

            if (!(actualXs[rayIndex] >= 0 && actualXs[rayIndex] < WIDTH &&
                actualYs[rayIndex] >= 0 && actualYs[rayIndex] < HEIGHT)) {

                offBounds = true;

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

// ==================== RAY BOUNCE CALCULATIONS ====================

            for (int j = 0; j < hitboxCount; j++) {
                bool inXRange = false;
                bool inYRange = false;

                if (x >= hitboxXs[j] && x <= hitboxXs[j]+hitboxWidths[j]) {
                    inXRange = true;
                } if (y >= hitboxYs[j] && y <= hitboxYs[j]+hitboxHeights[j]) {
                    inYRange = true;
                }

                if (!inXRange || !inYRange) {
                    continue;
                }

//                 printf("collided!\n");

                float prevX = actualXs[rayIndex] - xIntervals[rayIndex];
                float prevY = actualYs[rayIndex] - yIntervals[rayIndex];

                bool crossedTop    = prevY < hitboxYs[j] &&
                                     y     >= hitboxYs[j];
                bool crossedBottom = prevY > hitboxYs[j] + hitboxHeights[j] &&
                                     y     <= hitboxYs[j] + hitboxHeights[j];
                bool crossedLeft   = prevX < hitboxXs[j] &&
                                     x     >= hitboxXs[j];
                bool crossedRight  = prevX > hitboxXs[j] + hitboxWidths[j] &&
                                     x     <= hitboxXs[j] + hitboxWidths[j];

                if (crossedTop || crossedBottom) {
                    rayIntervalYInverses[rayIndex] = -1;
                } else if (crossedLeft || crossedRight) {
                    rayIntervalXInverses[rayIndex] = -1;
                }

//                 actualXs[rayIndex] += xIntervals[rayIndex]*rayIntervalXInverses[rayIndex]; // tick the rays
//                 actualYs[rayIndex] += yIntervals[rayIndex]*rayIntervalYInverses[rayIndex]; // reverse the values if bounced
            }

//             for (int j = 0; j < hitboxCount; j++) {
//                     int hx = hitboxXs[j];
//                     int hy = hitboxYs[j];
//                     int hw = hitboxWidths[j];
//                     int hh = hitboxHeights[j];
//
//                     bool insideNow = (x >= hx && x < hx + hw && y >= hy && y < hy + hh);
// //                     bool insidePrev = ( (int)floorf(prevX) >= hx && (int)floorf(prevX) < hx + hw &&
// //                                         (int)floorf(prevY) >= hy && (int)floorf(prevY) < hy + hh );
//
//                     if (insideNow) {
//                         // we entered the hitbox this tick — determine the crossing side
// //                         float dx = actualXs[rayIndex] - prevX;
// //                         float dy = actualYs[rayIndex] - prevY;
//
//                         // Compare which axis movement was larger in magnitude to pick side
//                         if (fabsf(x) > fabsf(y)) {
//                             // horizontal crossing -> flip X interval
//                             rayIntervalXInverses[rayIndex] *= -1;
//                             // nudge the ray slightly outside to avoid immediate re-entry
//                             actualXs[rayIndex] += (x > 0) ? 0.001f : -0.001f;
//                         } else {
//                             // vertical crossing -> flip Y interval
//                             rayIntervalYInverses[rayIndex] *= -1;
//                             actualYs[rayIndex] += (y > 0) ? 0.001f : -0.001f;
//                         }
//                         // mark pixel strength if desired
//                         strengthPerPixel[y * WIDTH + x] = 1000;
//                     }
//                 }

// ==================== COLOR PREPARATION ====================

            int pixelIndex = y * WIDTH + x; // find the pixel index in the array
            strength -= (double)strengthDecays[rayIndex];
            if (strength < 0) {
                strength = 0;
            }

            if (!offBounds) {
                if (atomicCAS(&written[pixelIndex], 0, 1) == 0) {
                    atomicExch(&rayColorsPerPixel[pixelIndex], rayColors[rayIndex]);
                    atomicExch(&strengthPerPixel[pixelIndex], (int)strength);
                }
            }
        }
        actualXs[rayIndex] = originalXs[rayIndex];
        actualYs[rayIndex] = originalYs[rayIndex];
    }
}

__device__ int abs(int x) {
    return (x < 0) ? -x : x;
}

__device__ int min(int x, int y) {
    return (x <= y) ? x : y;
}