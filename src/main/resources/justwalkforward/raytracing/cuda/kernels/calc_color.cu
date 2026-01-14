#include <cuda_runtime.h>

extern "C" __global__
void calcColors(int* initialPixels, int* raytracedPixels, int WIDTH, int HEIGHT, int* written, int currentBand, int* rayColorsPerPixel, int* strengthPerPixel) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;

    if (i < WIDTH * HEIGHT) {
        int pixelColor = 0;

        if (currentBand == 0) {
            pixelColor = initialPixels[i];
        } else {
            pixelColor = raytracedPixels[i];
        }

        int rayColor = rayColorsPerPixel[i];

        double rayR = (rayColor >> 16) & 0xFF;
        double rayG = (rayColor >> 8) & 0xFF;
        double rayB =  rayColor & 0xFF;

        if (written[i] == 0) {
            raytracedPixels[i] = pixelColor;
            return;
        }

        double originalPixelMultiplier = (double)(255 - strengthPerPixel[i]) / 255.0;
        double rayColorMultiplier      = (double)(strengthPerPixel[i]) / 255.0;

        int pixR = (pixelColor >> 16) & 0xFF;
        int pixG = (pixelColor >> 8) & 0xFF;
        int pixB =  pixelColor & 0xFF;

        int newR = max(min(255, (int)(pixR * originalPixelMultiplier + rayR * rayColorMultiplier)), 0);
        int newG = max(min(255, (int)(pixG * originalPixelMultiplier + rayG * rayColorMultiplier)), 0);
        int newB = max(min(255, (int)(pixB * originalPixelMultiplier + rayB * rayColorMultiplier)), 0);

        raytracedPixels[i] = (0xFF << 24) | (newR << 16) | (newG << 8) | newB;
    }
}