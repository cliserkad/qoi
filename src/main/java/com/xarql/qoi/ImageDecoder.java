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

    /** image width in pixels */
    private int width;
    /** image height in pixels */
    private int height;
    /** amount of color channels. 3 for RGB, 4 for RGBA */
    private int channels;
    /** colorspace id; 0 = sRGB with linear alpha, 1 = all channels linear */
    private int colorspace;

    /** Practically forms a hash table */
    private PixelRGBA[] pixelIndex = new PixelRGBA[PIXEL_INDEX_SIZE];
    /** stores the last seen pixel */
    private PixelRGBA prevPixel = INIT_PIXEL;

    private byte[] qoi;

    private int x = 0;
    private int y = 0;
    private int currByte = 15;
    private BufferedImage img;

    public static void main(String[] args) throws IOException {
        String homeDir = System.getProperty("user.home");
        if(args.length > 0) {
            BufferedImage img = new ImageDecoder().decode(Files.readAllBytes(new File(homeDir + args[0]).toPath()));
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

    public BufferedImage decode(byte[] qoi) {
        this.qoi = qoi;

        // check for magic numbers :)
        for(int i = 0; i < 4; i++)
            if(qoi[i] != MAGIC[i]) throw new IllegalArgumentException("File does not start with magic numbers: " + Arrays.toString(MAGIC));
        if(qoi[qoi.length - 1] != 0x01) throw new IllegalArgumentException("File's last byte is not " + 0x01);
        for(int i = qoi.length - 2; i > qoi.length - 9; i--)
            if(qoi[i] != 0x00) throw new IllegalArgumentException("Not all of bytes " + (qoi.length - 9) + " through " + (qoi.length - 2) + " are " + 0x00);

        width = decodeInt(4);
        height = decodeInt(8);

        System.out.println("width: " + width);
        System.out.println("height: " + height);

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try {
            while(y < height) {

                Tag tag = Tag.matchTag(qoi[currByte]);
                switch(tag) {
                    case DIFF -> setPixelAndAdvance(decodeDiff(currByte));
                    case INDEX -> setPixelAndAdvance(decodeIndex(currByte));
                    case RUN -> setPixelsInRun(currByte);
                    case LUMA -> setPixelAndAdvance(decodeLuma(currByte));
                    case RGBA, RGB ->  {
                        for(int i = 0; i < 2; i++)
                        setPixelAndAdvance(new PixelRGBA());
                    }
                    default -> {
                        System.out.println(tag.name());
                        setPixelAndAdvance(new PixelRGBA());
                    }
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.out.println("x: " + x + ", y: " + y);
            System.out.println("width: " + width + ", height: " + height);
            return img;
        }
        return img;
    }

    public void setPixelAndAdvance(PixelRGBA pixel) {
        img.setRGB(x, y, pixel.int_ARGB());
        if(pixelIndex[pixel.indexPosition()] == null)
            pixelIndex[pixel.indexPosition()] = pixel;
        prevPixel = pixel;
        currByte += 1;
        x += 1;
        if(x == width) {
            x = 0;
            y += 1;
        }
    }

    public void setPixelsInRun(int position) {
        int run = qoi[position] & BOTTOM_SIX_MASK;
        if(run < 0 || run > RUN_MAX)
            System.err.println("Run tag had an invalid value of " + run);
        run += RUN_OFFSET;
        // throw new IllegalStateException("Run tag had an invalid value of " + run);
        for(int i = 0; i < run; i++)
            setPixelAndAdvance(prevPixel);
    }

    public PixelRGBA decodeLuma(int position) {
        int byte1 = qoi[position];
        int byte2 = qoi[position + 1];
        int diffGreen = (byte1 & 0x3f) + LUMA_GREEN_OFFSET;
        int redDiff = diffGreen - 8 + ((byte2 >> 4) & 0x0f);
        int blueDiff = diffGreen - 8 + (byte2 & 0x0f);

        /*
        int luma = (qoi[position] << 8) + qoi[position + 1];
        int greenDiff = luma & LUMA_MASK_GREEN;
        int redDiff = luma & LUMA_MASK_RED;
        int blueDiff = luma & LUMA_MASK_BLUE;
         */

        int red = wrapInt(prevPixel.r + redDiff);
        int green = wrapInt(prevPixel.g + diffGreen);
        int blue = wrapInt(prevPixel.b + blueDiff);

        return new PixelRGBA(red, green, blue, prevPixel.a);
    }

    public PixelRGBA decodeIndex(int position) {
        int index = qoi[position] & BOTTOM_SIX_MASK;
        if(pixelIndex[index] == null) return new PixelRGBA();
        return pixelIndex[index];
    }

    public PixelRGBA decodeDiff(int position) {
        int redDiff = ((qoi[position] & DIFF_MASK_RED) >>> 4) + DIFF_OFFSET;
        int greenDiff = ((qoi[position] & DIFF_MASK_GREEN) >>> 2) + DIFF_OFFSET;
        int blueDiff = (qoi[position] & DIFF_MASK_BLUE) + DIFF_OFFSET;

        int red = wrapInt(prevPixel.r + redDiff);
        int green = wrapInt(prevPixel.g + greenDiff);
        int blue = wrapInt(prevPixel.b + blueDiff);

        return new PixelRGBA(red, green, blue, 0);
    }

    public int wrapInt(int i) {
        if(i < 0)
            return 256 + i;
        else if(i > 255)
            return i - 256;
        else
            return i;
    }

    public int decodeInt(int start) {
        int num = 0;
        num = qoi[start] << 24;
        num += qoi[start + 1] << 16;
        num += qoi[start + 2] << 8;
        num += qoi[start + 3];
        return num;
    }

}
