package net.arjun.justwalkforward.game.raytracing;

import net.arjun.justwalkforward.game.GameRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.arjun.justwalkforward.game.GameRenderer.ARGB.argb;

@Deprecated
public class ARGBMRSprite {
    public BufferedImage image;
    public int[] pixels;

    public ARGBMRSprite() {}

    public void load(String name) throws IOException {
        try (InputStream inputStream = ARGBMRSprite.class.getResourceAsStream("/justwalkforward/" + name)) {
            if (inputStream == null) {
                System.err.println("Resource not found: /justwalkforward/" + name);
                return;
            }
            BufferedImage preImage = ImageIO.read(inputStream);
            image = new BufferedImage(preImage.getWidth(), preImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            image.setData(preImage.getRaster());

            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            // ... your existing code
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized GameRenderer.ARGB getPixel(int x, int y) {
        if (pixels != null) {
            int pixelValue = pixels[(image.getWidth()*y)+x];
            int a = (pixelValue >> 24) & 0xFF;
            int r = (pixelValue >> 16) & 0xFF;
            int g = (pixelValue >> 8) & 0xFF;
            int b = pixelValue & 0xFF;
        }

        return argb(0,0,0,0);
    }

    public synchronized void setPixel(int x, int y, GameRenderer.RGB rgb) {
        pixels[(image.getWidth()*y)+x] = (255 << 24) | (rgb.r << 16) | (rgb.g << 8) | rgb.b;
    }

    public synchronized void setPixel(int x, int y, GameRenderer.ARGB argb) {
        pixels[(image.getWidth()*y)+x] = (argb.a << 24) | (argb.r << 16) | (argb.g << 8) | argb.b;
    }
}
