package main.java.com.xarql.qoi;

/**
 * Contains a 2D array of sRGB pixels
 */
public class ImageRGBA {
    private final PixelRGBA[][] pixels;

    public ImageRGBA(int height, int width) {
        pixels = new PixelRGBA[height][width];
    }

    public int getHeight() {
        return pixels.length;
    }

    public int getWidth() {
        return pixels[0].length;
    }

}
