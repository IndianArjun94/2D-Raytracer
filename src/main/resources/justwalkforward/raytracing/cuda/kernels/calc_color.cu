#include <cuda_runtime.h>

extern "C" __global__
void calcColors(int raysCount, float* actualXs, float* xIntervals, float* actualYs, float* yIntervals, float* originalXs, float* originalYs, int* initialPixels, int* raytracedPixels, int* rayColors, int WIDTH, int HEIGHT, int* rayBandStartIndexes, int* rayBandEndIndexes, int rayBandsCount, int* written, int currentBand, int* rayColorsPerPixel, int* originalStrengths, float* strengthDecays, int* strengthPerPixel) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < WIDTH*HEIGHT) { // if this kernel is correctly representing a ray in our current ray band
        int pixelColor = 0;

        if (currentBand == 0) { // if its the first time, copy from initial
            pixelColor = initialPixels[i]; // then set the pixel in raytracedPixels
        } else { // otherwise, copy from raytracedPixels
            pixelColor = raytracedPixels[i]; // then ALSO set the pixel in raytracedPixels
        }

        int rayColor = rayColorsPerPixel[i];

        double rayR   = (rayColor >> 16) & 0xFF; // find the rgb values of the ray
        double rayG = (rayColor >> 8) & 0xFF;
        double rayB  = rayColor & 0xFF;

        if (written[i] == 0) {
            raytracedPixels[i] = pixelColor;
            return;
        }

        double originalPixelMultiplier = (double)(255-strengthPerPixel[i])/(double)(255);
        double rayColorMultiplier = (double)(strengthPerPixel[i])/(double)(255);

        int pixR = (pixelColor >> 16) & 0xFF;
        int pixG = (pixelColor >> 8) & 0xFF;
        int pixB = pixelColor & 0xFF;

//         int newR = min(255, (pixR + (int)rayR) >> 1);
//         int newG = min(255, (pixG + (int)rayG) >> 1);
//         int newB = min(255, (pixB + (int)rayB) >> 1);

        int newR = max(min(255, (int)(pixR*originalPixelMultiplier + rayR*rayColorMultiplier)),0);
        int newG = max(min(255, (int)(pixG*originalPixelMultiplier + rayG*rayColorMultiplier)),0);
        int newB = max(min(255, (int)(pixB*originalPixelMultiplier + rayB*rayColorMultiplier)),0);

        raytracedPixels[i] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB; // set the new value
    }
}
