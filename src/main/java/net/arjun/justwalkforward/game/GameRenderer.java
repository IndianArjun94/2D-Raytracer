package net.arjun.justwalkforward.game;

import net.arjun.justwalkforward.game.raytracing.ARGBMR;
import net.arjun.justwalkforward.game.raytracing.Ray;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static net.arjun.justwalkforward.game.raytracing.Ray.ray;
import static net.arjun.justwalkforward.game.raytracing.ARGBMR.argbmr;
import static net.arjun.justwalkforward.game.GameRenderer.ARGB.argb;
import static net.arjun.justwalkforward.game.GameRenderer.RGB.rgb;

public class GameRenderer {
    public JFrame frame;
    public final int WIDTH;
    public final int HEIGHT;

    public InnerGameRenderer innerGameRenderer;

    public static GameRenderer instance;

    public GameRenderer(Dimension2D dimension2D) {
        WIDTH = (int) dimension2D.getWidth();
        HEIGHT = (int) dimension2D.getHeight();
        instance = this;
        initJFrame();
    }

    private void initJFrame() {
        frame = new JFrame();
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void initRenderSystem() {
        this.innerGameRenderer = new InnerGameRenderer(WIDTH, HEIGHT);
        frame.add(innerGameRenderer);
        frame.pack();
        innerGameRenderer.startRenderThread();
    }

    public static class RGB {
        public int r;
        public int g;
        public int b;
        private RGB(int r,int g,int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
        public static RGB rgb(int r,int g,int b) {
            return new RGB(r,g,b);
        }
    }

    public static class ARGB {
        public int r;
        public int g;
        public int b;
        public int a;
        private ARGB(int a,int r,int g,int b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
        public static ARGB argb(int a, int r, int g, int b) {
            return new ARGB(a,r,g,b);
        }
    }

    public static class Material {
        public byte roughness;
        public byte metallicness;

        private Material(byte roughness, byte metallicness) {
            this.roughness = roughness;
            this.metallicness = metallicness;
        }

        public static Material material(byte roughness, byte metallicness) {
            return new Material(roughness, metallicness);
        }

        public static ARGB calculateARGB(ARGBMR argbmr, int x, int y, Ray ray) {
            if (ray.x == x && ray.y == y) {
                double lightStrength = (ray.strength-50)/(25);
                return argb(argbmr.argb.a,
                        (int) (argbmr.argb.r*lightStrength),
                        (int) (argbmr.argb.g*lightStrength),
                        (int) (argbmr.argb.b*lightStrength));
            } else {
                return null;
            }
        }
    }


    public class InnerGameRenderer extends Canvas implements Runnable {
        private final int WIDTH;
        private final int HEIGHT;

        public BufferStrategy bufferStrategy;
        public BufferedImage image;

        public int[] pixels;
        public byte[] roughnessBuffer;
        public byte[] metallicnessBuffer;

        public Thread renderThread;
        public boolean running = false;

        public boolean renderFrame = false;

        public int targetFPS = 120;

        double counter = 0;

        public ArrayList<Ray> rays = new ArrayList<>();

        public InnerGameRenderer(int width, int height) {
            WIDTH = width;
            HEIGHT = height;

            this.setPreferredSize(new Dimension(width, height));

//            Creates the off-screen pixels
            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();


            roughnessBuffer = new byte[WIDTH*HEIGHT];
            metallicnessBuffer = new byte[WIDTH*HEIGHT];
        }

        public synchronized void addTestRays() {
            for (int i = 0; i < 3600; i++) {
                this.rays.add(ray(counter, 100, Color.BLUE, WIDTH/2, HEIGHT/2));
                counter *= 10;
                counter++;
                counter /= 10;
                if (counter == 360) {
                    counter = 0;
                }
            }

        }

        public synchronized void removeRay(Ray ray) {
            rays.remove(ray);
        }

        public synchronized void addRay(Ray ray) {
            rays.add(ray);
        }

        public synchronized int[] getPixel(int x, int y) {
            int pixelValue = pixels[(WIDTH*y)+x];
            int a = (pixelValue >> 24) & 0xFF;
            int r = (pixelValue >> 16) & 0xFF;
            int g = (pixelValue >> 8) & 0xFF;
            int b = pixelValue & 0xFF;

            return new int[]{a,r,g,b};
        }

        public synchronized void setPixel(int x, int y, RGB rgb) {
            pixels[(WIDTH*y)+x] = (255 << 24) | (rgb.r << 16) | (rgb.g << 8) | rgb.b;
        }

        public synchronized void setPixel (int x, int y, int pixel) {
            pixels[(WIDTH*y)+x] = pixel;
        }

        public synchronized void setPixel(int x, int y, ARGB argb) {
            pixels[(WIDTH*y)+x] = (argb.a << 24) | (argb.r << 16) | (argb.g << 8) | argb.b;
        }

        public synchronized void setPixel(int x, int y, ARGB argb, Material material) {
            pixels[(WIDTH*y)+x] = (argb.a << 24) | (argb.r << 16) | (argb.g << 8) | argb.b;
            metallicnessBuffer[(WIDTH*y)+x] = material.metallicness;
            roughnessBuffer[(WIDTH*y)+x] = material.roughness;
        }

        public synchronized void render() {
            Graphics graphics = bufferStrategy.getDrawGraphics();

            graphics.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            graphics.dispose();

            bufferStrategy.show();

//            reset
        }

        private synchronized void startRenderThread() {
            if (running) return;
            running = true;
            renderThread = new Thread(this, "renderGame");
            renderThread.setPriority(1);
            renderThread.start();
        }

        private synchronized void stopRenderThread() {
            if (!running) return;
            running = false;
            try {
                renderThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            createBufferStrategy(3);
            bufferStrategy = getBufferStrategy();

            long frameStartTime;
            long frameEndTime;

            while (running) {
                frameStartTime = System.currentTimeMillis();
                if (renderFrame) {
                    try {
                        SwingUtilities.invokeAndWait(this::render);
                    } catch (InterruptedException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }


                    renderFrame = false;
                }
                frameEndTime = System.currentTimeMillis();
                if ((1000/targetFPS)-(frameEndTime-frameStartTime) > 0) {
                    try {
                        Thread.sleep((1000/targetFPS) - (frameEndTime-frameStartTime));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
