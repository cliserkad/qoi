package main.java.com.xarql.qoi;

public enum Tag {
    RGB(0B1111110),
    RGBA(0B11111111),
    INDEX(0B00000000),
    DIFF(0B01000000),
    LUMA(0B10000000),
    RUN(0B11000000);

    /** Selects only the first 2 bits of a byte when using bitwise AND */
    public static final int TOP_2_BITMASK = 0B11000000;
    public static final int BOTTOM_8_BITMASK = 0B00000000_00000000_00000000_11111111;

    public final int value;

    Tag(final int value) {
        this.value = value;
    }

    public static Tag matchTag(int encodedByte) {
        encodedByte &= BOTTOM_8_BITMASK;
        if(encodedByte == RGB.value)
            return RGB;
        else if(encodedByte == RGBA.value)
            return RGBA;
        else if((encodedByte & TOP_2_BITMASK) == INDEX.value)
            return INDEX;
        else if((encodedByte & TOP_2_BITMASK) == DIFF.value)
            return DIFF;
        else if((encodedByte & TOP_2_BITMASK) == LUMA.value)
            return LUMA;
        else if((encodedByte & TOP_2_BITMASK) == RUN.value)
            return RUN;
        else
            throw new IllegalArgumentException("Encoded byte " + encodedByte + " did not contain a valid tag");
    }
}
