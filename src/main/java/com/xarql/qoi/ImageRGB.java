package main.java.com.xarql.qoi;

/**
 * Contains a 2D array of sRGB pixels
 */
public class ImageRGB {
    private final PixelRGB[][] pixels;

    public ImageRGB(int height, int width) {
        pixels = new PixelRGB[height][width];
    }

    public int getHeight() {
        return pixels.length;
    }

    public int getWidth() {
        return pixels[0].length;
    }

}
