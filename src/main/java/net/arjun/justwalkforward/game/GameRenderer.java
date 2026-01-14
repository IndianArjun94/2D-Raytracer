package net.arjun.justwalkforward.game;

import net.arjun.justwalkforward.game.raytracing.GPUManager;
import net.arjun.justwalkforward.game.raytracing.Ray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import static net.arjun.justwalkforward.game.raytracing.Ray.ray;

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
        frame.setUndecorated(true);
        frame.setIgnoreRepaint(true);
        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        device.setFullScreenWindow(frame);
        frame.setVisible(true);
    }

    public void initRenderSystem() throws IOException, FontFormatException {
        this.innerGameRenderer = new InnerGameRenderer(WIDTH, HEIGHT);
        frame.add(innerGameRenderer);
        frame.pack();
        innerGameRenderer.startRenderThread();
        GPUManager.init(innerGameRenderer);
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
    }


    public class InnerGameRenderer extends Canvas implements Runnable {
        public final int WIDTH;
        public final int HEIGHT;

        public BufferStrategy bufferStrategy;
        public BufferedImage image;

        public int[] pixels;
        public byte[] roughnessBuffer;
        public byte[] metallicnessBuffer;

        public Thread renderThread;
        public boolean running = false;

        public int FPS;
        public int UPS;

        public Font debugFont;

        public boolean renderFrame = false;

        public boolean[] dirKeysPressed = new boolean[4];

        public InnerGameRenderer(int width, int height) {
            WIDTH = width;
            HEIGHT = height;

            this.setPreferredSize(new Dimension(width, height));

//            Creates the off-screen pixels
            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();


            roughnessBuffer = new byte[WIDTH*HEIGHT];
            metallicnessBuffer = new byte[WIDTH*HEIGHT];

            this.setFocusable(true);
            this.requestFocusInWindow();


            this.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP ->    dirKeysPressed[0] = true;
                        case KeyEvent.VK_RIGHT -> dirKeysPressed[1] = true;
                        case KeyEvent.VK_DOWN ->  dirKeysPressed[2] = true;
                        case KeyEvent.VK_LEFT ->  dirKeysPressed[3] = true;
                    }
//                    System.out.println("pressed" + e.getKeyChar());
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP ->    dirKeysPressed[0] = false;
                        case KeyEvent.VK_RIGHT -> dirKeysPressed[1] = false;
                        case KeyEvent.VK_DOWN ->  dirKeysPressed[2] = false;
                        case KeyEvent.VK_LEFT ->  dirKeysPressed[3] = false;
                    }
//                    System.out.println("released" + e.getKeyChar());
                }
            });
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

//            ----------------------------

            graphics.setFont(debugFont);
            graphics.setColor(Color.BLACK);
            graphics.drawString("FPS: " + FPS, 25, 50);
            graphics.drawString("UPS: " + UPS, 25, 100);

//            graphics.drawRect(GPUManager.hitboxXs[0],GPUManager.hitboxYs[0], GPUManager.hitboxWidths[0], GPUManager.hitboxHeights[0]);

//            ----------------------------

            graphics.dispose();

            bufferStrategy.show();

        }

        private synchronized void startRenderThread() throws IOException, FontFormatException {
            if (running) return;
            running = true;
            renderThread = new Thread(this, "renderGame");
            renderThread.setPriority(1);

            String fontPath = "/justwalkforward/fonts/Rubik-Regular.ttf";

            try (InputStream stream = GameRenderer.class.getResourceAsStream(fontPath)) {
                if (stream == null) {
                    System.err.println("Font file not found: " + fontPath);
                }
                assert stream != null;
                debugFont = Font.createFont(Font.TRUETYPE_FONT, stream);
                debugFont = debugFont.deriveFont(38f);

                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(debugFont);

            } catch (IOException | FontFormatException e) {
                System.err.println("Error loading font: " + e.getMessage());
                e.printStackTrace();
            }

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

            final int targetFPS = 120;
            final double targetDelta = (double) 1_000_000_000 / targetFPS; // milliseconds per update (≈8.33ms)

            long lastTime = System.nanoTime();

//            --------------------------

            long fpsUpdateInterval = 1000;
            long lastFPSUpdateTime = System.nanoTime();

//            --------------------------

            int frameCount = 0;

            while (running) {
                long now = System.nanoTime();
                double delta = now - lastTime;

                if (delta >= targetDelta) { // frames are slower than target
                    if (renderFrame) {
                        SwingUtilities.invokeLater(this::render);
                    }

                    lastTime += targetDelta; // keeps timing consistent
                    frameCount++;
                } else { // frames are faster than target
                    long sleepTime = (long)((targetDelta - delta) / 1_000_000);
                    if (sleepTime > 0) {
                        try { Thread.sleep(sleepTime); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }

                // Update FPS every fpsUpdateInterval (ms)
                if (now - lastFPSUpdateTime >= fpsUpdateInterval*1_000_000) {
                    FPS = (int) (frameCount*(1000/fpsUpdateInterval));
                    frameCount = 0;
                    lastFPSUpdateTime = now;
                }
            }
        }
    }
}
