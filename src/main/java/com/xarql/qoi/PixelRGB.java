package main.java.com.xarql.qoi;

// this could be a record, but git would struggle to track it

/**
 * Holds 3 discrete bytes, one for each color channel
 */
public class PixelRGB {
    public final byte r;
    public final byte g;
    public final byte b;

    public PixelRGB(final byte r, final byte g, final byte b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
