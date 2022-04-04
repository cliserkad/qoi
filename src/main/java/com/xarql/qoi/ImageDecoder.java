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

            ImageIcon icon = new ImageIcon(img);
            JFrame frame = new JFrame();
            frame.setLayout(new FlowLayout());
            frame.setSize(500,500);
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

        return img;
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
