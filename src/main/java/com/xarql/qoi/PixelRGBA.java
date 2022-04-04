package main.java.com.xarql.qoi;

// this could be a record, but git would struggle to track it

/**
 * Holds 4 discrete bytes, one for each color channel plus an alpha channel
 */
public class PixelRGBA {
    public final int r;
    public final int g;
    public final int b;
    public final int a;

    public PixelRGBA(final int r, final int g, final int b, final int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PixelRGBA pixelRGBA = (PixelRGBA) o;
        return r == pixelRGBA.r && g == pixelRGBA.g && b == pixelRGBA.b && a == pixelRGBA.a;
    }

    @Override
    public int hashCode() {
        return indexPosition();
    }

    /**
     * Determines the index in the hash table at which this pixel should occur during encoding/decoding.
     * The hash table is limited to 64 values, hence the mod by 64.
     */
    public int indexPosition() {
        return (r * 3 + g * 5 + b * 7 + a * 11) % 64;
    }

}
