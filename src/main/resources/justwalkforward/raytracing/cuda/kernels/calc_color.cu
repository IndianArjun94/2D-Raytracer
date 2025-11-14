#include <cuda_runtime.h>

extern "C" __global__
void calcColors(int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals, float* originalXs, float* originalYs, int* initialPixels, int* raytracedPixels, int* rayColors, int WIDTH, int HEIGHT, int* rayBandStartIndexes, int* rayBandEndIndexes, int rayBandsCount, int* written, int currentBand, int* rayColorsPerPixel) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < WIDTH*HEIGHT) { // if this kernel is correctly representing a ray in our current ray band
        int pixelColor = 0;

        if (currentBand == 0) { // if its the first time, copy from initial
            pixelColor = initialPixels[i]; // then set the pixel in raytracedPixels
        } else { // otherwise, copy from raytracedPixels
            pixelColor = raytracedPixels[i]; // then ALSO set the pixel in raytracedPixels
        }

        int rayColor = rayColorsPerPixel[i];

        int rayR   = (rayColor >> 16) & 0xFF; // find the rgb values of the ray
        int rayG = (rayColor >> 8) & 0xFF;
        int rayB  = rayColor & 0xFF;

        if (written[i] == 0) {
            raytracedPixels[i] = pixelColor;
            return;
        }

        int pixR = (pixelColor >> 16) & 0xFF;
        int pixG = (pixelColor >> 8) & 0xFF;
        int pixB = pixelColor & 0xFF;

        int newR = min(255, (pixR + rayR) >> 1);
        int newG = min(255, (pixG + rayG) >> 1);
        int newB = min(255, (pixB + rayB) >> 1);

        raytracedPixels[i] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB; // set the new value
    }
}
