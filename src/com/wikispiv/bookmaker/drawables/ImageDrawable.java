package com.wikispiv.bookmaker.drawables;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.Utils;

public class ImageDrawable extends Drawable implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String imageName;
    private transient BufferedImage cachedJavaImage;
    private transient PDDocument lastDocument;
    private transient PDImageXObject cachedPdfImage;

    public ImageDrawable(String imageName)
    {
        this.imageName = imageName;
    }

    @Override
    public void drawMe(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf) throws IOException
    {
        BufferedImage image = getJavaImage();
        Dimension scaledDimension = Utils.getScaledDimension(new Dimension(image.getWidth(), image.getHeight()),
                getDimension());
        if (image != null) {
            if (actuallyDraw) {
                g2.drawImage(image, (int) getX(), (int) getY(), (int) scaledDimension.getWidth(),
                        (int) scaledDimension.getHeight(), null);
            }
            if (actuallyDrawPdf) {
                pg.drawImage(getPdfImage(document), (float) getX(),
                        (float) (Utils.flipY(getY()) - scaledDimension.getHeight()), (float) scaledDimension.getWidth(),
                        (float) scaledDimension.getHeight());
            }
        }
    }

    public PDImageXObject getPdfImage(PDDocument document) throws IOException
    {
        if (lastDocument == document && cachedPdfImage != null) {
            return cachedPdfImage;
        }
        cachedPdfImage = PDImageXObject
                .createFromFile(new File(getImageDirectory(), imageName).getAbsolutePath(), document);
        lastDocument = document;
        return cachedPdfImage;
    }

    public BufferedImage getJavaImage()
    {
        if (cachedJavaImage == null) {
            try {
                this.cachedJavaImage = ImageIO.read(new File(getImageDirectory(), imageName));
            } catch (IOException e) {
                e.printStackTrace();
                Main.println(e.getMessage());
            }
        }
        return this.cachedJavaImage;
    }

    public static File getImageDirectory()
    {
        File currentFile = Main.getSh().getCurrentFile();
        if (currentFile == null || !currentFile.exists()) {
            return null;
        }
        return new File(currentFile.getParentFile(), Main.IMG_DIRECTORY);
    }

}
