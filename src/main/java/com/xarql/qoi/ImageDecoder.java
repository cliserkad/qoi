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
    /** Amount of pixels to be indexed. Set as 64 because 2 bits in the encoded byte are used as a tag, leaving 6 bits
     * that can represent 64 values. */
    public static final int PIXEL_INDEX_SIZE = 64;
    /** Magic number. Purpose unknown */
    public static final byte[] MAGIC = { 'q', 'o', 'i', 'f' };
    public static final PixelRGBA INIT_PIXEL = new PixelRGBA(0, 0, 0, 255);

    public static final int DIFF_MASK_RED   = 0B00110000;
    public static final int DIFF_MASK_GREEN = 0B00001100;
    public static final int DIFF_MASK_BLUE  = 0B00000011;
    public static final int DIFF_OFFSET = -2; // minus 2 from raw unsigned value

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
            if(qoi[i] != MAGIC[i])
                throw new IllegalArgumentException("File does not start with magic numbers: " + Arrays.toString(MAGIC));
        if(qoi[qoi.length - 1] != 0x01)
            throw new IllegalArgumentException("File's last byte is not " + 0x01);
        for(int i = qoi.length - 2; i > qoi.length - 9; i--)
            if(qoi[i] != 0x00)
                throw new IllegalArgumentException("Not all of bytes " + (qoi.length - 9) + " through " + (qoi.length - 2) + " are " + 0x00);

        width = decodeInt(4);
        height = decodeInt(8);

        System.out.println("width: " + width);
        System.out.println("height: " + height);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        int y = 0;
        int currByte = 0;
        PixelRGBA newPixel = null;
        while(y < height) {
            try {
                Tag tag = Tag.matchTag(qoi[currByte]);
                if (currByte < 10) {
                    switch (tag) {
                        case DIFF -> newPixel = decodeDiff(currByte);
                        default -> {
                            System.out.println(tag.name());
                            newPixel = new PixelRGBA();
                        }
                    }
                }

                img.setRGB(x, y, newPixel.int_ARGB());
                prevPixel = newPixel;
                currByte += 1;
                x += 1;
                if (x == width) {
                    x = 0;
                    y += 1;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                System.out.println("x: " + x + ", y: " + y);
                System.out.println("width: " + width + ", height: " + height);
                return img;
            }
        }

        return img;
    }

    public PixelRGBA decodeDiff(int position) {
        int redDiff = ((qoi[position] & DIFF_MASK_RED) >>> 4) + DIFF_OFFSET;
        System.out.println("redDiff: " + redDiff);
        int greenDiff = ((qoi[position] & DIFF_MASK_GREEN) >>> 2) + DIFF_OFFSET;
        System.out.println("greenDiff: " + greenDiff);
        int blueDiff = (qoi[position] & DIFF_MASK_BLUE) + DIFF_OFFSET;
        System.out.println("blueDiff: " + blueDiff);

        int red = wrapInt(prevPixel.r + redDiff);
        int green = wrapInt(prevPixel.g + greenDiff);
        int blue = wrapInt(prevPixel.b + blueDiff);

        PixelRGBA output = new PixelRGBA(red, green, blue, 0);
        System.out.println(output);
        System.out.println("pixel: " + Integer.toBinaryString(output.int_ARGB()));
        return output;
    }

    public int wrapInt(int i) {
        if(i < 0)
            return 255;
        else if(i > 255)
            return 0;
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
