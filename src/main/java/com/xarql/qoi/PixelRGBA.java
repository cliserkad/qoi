package main.java.com.xarql.qoi;

// this could be a record, but git would struggle to track it

/**
 * Holds 4 discrete bytes, one for each color channel plus an alpha channel
 */
public class PixelRGBA {
    public static final int DEFAULT_ALPHA = 255;
    public static final String OUT_OF_BOUNDS = "Value out of bounds [0, 255]: ";

    public final int r;
    public final int g;
    public final int b;
    public final int a;

    public PixelRGBA(final int r, final int g, final int b, final int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        checkBounds();
    }

    public PixelRGBA(final int r, final int g, final int b) {
        this(r, g, b, DEFAULT_ALPHA);
    }

    public PixelRGBA() {
        this(0, 0, 0, 0);
    }

    public void checkBounds() throws IllegalStateException {
        if(r < 0 || r > 255)
            throw new IllegalStateException(OUT_OF_BOUNDS + this);
        if(g < 0 || g > 255)
            throw new IllegalStateException(OUT_OF_BOUNDS + this);
        if(b < 0 || b > 255)
            throw new IllegalStateException(OUT_OF_BOUNDS + this);
        if(a < 0 || a > 255)
            throw new IllegalStateException(OUT_OF_BOUNDS + this);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        PixelRGBA pixelRGBA = (PixelRGBA) o;
        return r == pixelRGBA.r && g == pixelRGBA.g && b == pixelRGBA.b && a == pixelRGBA.a;
    }

    @Override
    public int hashCode() {
        return indexPosition();
    }

    @Override
    public String toString() {
        return "PixelRGBA{" +
                "r=" + r +
                ", g=" + g +
                ", b=" + b +
                ", a=" + a +
                '}';
    }

    /**
     * Determines the index in the hash table at which this pixel should occur during encoding/decoding.
     * The hash table is limited to 64 values, hence the mod by 64.
     */
    public int indexPosition() {
        checkBounds();
        return (r * 3 + g * 5 + b * 7 + a * 11) % 64;
    }

    public int int_ARGB() {
        checkBounds();
        int output = 0;
        output += a << 24;
        output += r << 16;
        output += g << 8;
        output += b;
        return output;
    }

}
