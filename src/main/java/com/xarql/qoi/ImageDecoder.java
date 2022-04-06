package main.java.com.xarql.qoi;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class ImageDecoder {
    /**
     * Amount of pixels to be indexed. Set as 64 because 2 bits in the encoded byte are used as a tag, leaving 6 bits that can represent 64 values.
     */
    public static final int PIXEL_INDEX_SIZE = 64;
    /** Magic number. Purpose unknown */
    public static final byte[] MAGIC = { 'q', 'o', 'i', 'f' };
    public static final PixelRGBA INIT_PIXEL = new PixelRGBA(0, 0, 0, 255);

    public static final int DIFF_MASK_RED   = 0B00110000;
    public static final int DIFF_MASK_GREEN = 0B00001100;
    public static final int DIFF_MASK_BLUE  = 0B00000011;
    public static final int DIFF_OFFSET = -2; // minus 2 from raw unsigned value
    public static final int RUN_OFFSET = 1;
    public static final int RUN_MAX = 62; // to prevent collisions with RGB & RGBA tags

    public static final int BOTTOM_SIX_MASK = 0B00000000_00000000_00000000_00111111;

    public static final int LUMA_MASK_GREEN = 0B00000000_00000000_00111111_00000000;
    public static final int LUMA_MASK_RED   = 0B00000000_00000000_00000000_11110000;
    public static final int LUMA_MASK_BLUE  = 0B00000000_00000000_00000000_00001111;
    public static final int LUMA_GREEN_OFFSET = -32;
    public static final int LUMA_RED_BLUE_OFFSET = -8;

    // specification calls for an unsigned int, but java doesn't support unsigned numbers
    // longs will encompass the entirety of an unsigned int's range without issue




    public static void main(String[] args) throws IOException {
        String homeDir = System.getProperty("user.home");
        if(args.length > 0) {
            BufferedImage img = ImageDecoder.decode(Files.readAllBytes(new File(homeDir + args[0]).toPath()));
            BufferedImage img2 = ImageIO.read(new File(homeDir + args[0].split("\\.")[0] + ".png"));

            ImageIcon icon = new ImageIcon(img);
            JFrame frame = new JFrame();
            frame.setLayout(new FlowLayout());
            frame.setSize(icon.getIconWidth(), icon.getIconHeight());
            JLabel lbl = new JLabel();
            lbl.setIcon(icon);
            frame.add(lbl);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    public static BufferedImage decode(byte[] data) {
        // check for magic numbers :)
        for(int i = 0; i < 4; i++)
            if(data[i] != MAGIC[i]) throw new IllegalArgumentException("File does not start with magic numbers: " + Arrays.toString(MAGIC));
        if(data[data.length - 1] != 0x01) throw new IllegalArgumentException("File's last byte is not " + 0x01);
        for(int i = data.length - 2; i > data.length - 9; i--)
            if(data[i] != 0x00) throw new IllegalArgumentException("Not all of bytes " + (data.length - 9) + " through " + (data.length - 2) + " are " + 0x00);

        int width = decodeInt(4, data);
        int height = decodeInt(8, data);
        int channels = data[13];
        int colorspace = data[14];

        System.out.println("width: " + width);
        System.out.println("height: " + height);

        // Practically forms a hash table
        PixelRGBA[] index = new PixelRGBA[PIXEL_INDEX_SIZE];
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelRGBA pixel = INIT_PIXEL;
        int pos = 15;
        int run = 0;
        int x = 0;
        int y = 0;
        while(y < height && pos < data.length) {
            if(run == 0) {
                Tag tag = Tag.matchTag(data[pos]);
                switch(tag) {
                    case DIFF -> {
                        int redDiff = (data[pos] & DIFF_MASK_RED) + DIFF_OFFSET;
                        int greenDiff = (data[pos] & DIFF_MASK_GREEN) + DIFF_OFFSET;
                        int blueDiff = (data[pos] & DIFF_MASK_BLUE) + DIFF_OFFSET;

                        int red = (pixel.r + redDiff) & Tag.BOTTOM_8_BITMASK;
                        int green = (pixel.g + greenDiff) & Tag.BOTTOM_8_BITMASK;
                        int blue = (pixel.b + blueDiff) & Tag.BOTTOM_8_BITMASK;

                        pixel = new PixelRGBA(red, green, blue, pixel.a);
                    }
                    case INDEX -> {
                        int entry = 0;
                        try {
                            entry = data[pos] & BOTTOM_SIX_MASK;
                            if(index[entry] == null)
                                throw new NullPointerException("Index tag pointed to a null index");
                            else pixel = index[entry];
                        } catch(NullPointerException npe) {
                            System.err.print("entry: " + entry + " / ");
                            System.err.println(Integer.toBinaryString(entry));

                        }
                    }
                    case RUN -> {
                        run = data[pos] & BOTTOM_SIX_MASK;
                        if(run > RUN_MAX) System.err.println("Run tag had an invalid value of " + run);
                        run += RUN_OFFSET;
                    }
                    case LUMA -> {
                        int byte1 = data[pos];
                        int byte2 = data[++pos];
                        int diffGreen = (byte1 & 0x3f) + LUMA_GREEN_OFFSET;
                        int redDiff = diffGreen - 8 + ((byte2 >> 4) & 0x0f);
                        int blueDiff = diffGreen - 8 + (byte2 & 0x0f);

                        /*
                        int luma = (data[position] << 8) + data[position + 1];
                        int greenDiff = luma & LUMA_MASK_GREEN;
                        int redDiff = luma & LUMA_MASK_RED;
                        int blueDiff = luma & LUMA_MASK_BLUE;
                        */

                        int red = (pixel.r + redDiff) & Tag.BOTTOM_8_BITMASK;
                        int green = (pixel.g + diffGreen) & Tag.BOTTOM_8_BITMASK;
                        int blue = (pixel.b + blueDiff) & Tag.BOTTOM_8_BITMASK;

                        pixel = new PixelRGBA(red, green, blue, pixel.a);
                    }
                    case RGB -> {
                        int red = Byte.toUnsignedInt(data[++pos]);
                        int green = Byte.toUnsignedInt(data[++pos]);
                        int blue = Byte.toUnsignedInt(data[++pos]);
                        pixel = new PixelRGBA(red, green, blue, pixel.a);
                    }
                    case RGBA -> {
                        int red = Byte.toUnsignedInt(data[++pos]);
                        int green = Byte.toUnsignedInt(data[++pos]);
                        int blue = Byte.toUnsignedInt(data[++pos]);
                        int alpha = Byte.toUnsignedInt(data[++pos]);
                        pixel = new PixelRGBA(red, green, blue, alpha);
                    }
                    default -> System.err.println(tag.name());
                }
            } else
                run--;
            img.setRGB(x, y, pixel.int_ARGB());
            index[pixel.indexPosition()] = pixel;
            pos++;
            x += 1;
            if(x == width) {
                x = 0;
                y += 1;
            }
        }
        return img;
    }

    public static int decodeInt(int start, byte[] data) {
        int a = Byte.toUnsignedInt(data[start]) << 24;
        int b = Byte.toUnsignedInt(data[start + 1]) << 16;
        int c = Byte.toUnsignedInt(data[start + 2]) << 8;
        int d = Byte.toUnsignedInt(data[start + 3]);
        return a | b | c | d;
    }

}
