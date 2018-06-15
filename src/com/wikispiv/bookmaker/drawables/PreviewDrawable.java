package com.wikispiv.bookmaker.drawables;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.Serializable;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

public class PreviewDrawable extends Drawable implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private Drawable preview;
    private static final Color PREVIEW_COLOR_EMPTY = new Color(0, 0, 255, 20);
    private static final Color PREVIEW_COLOR_FULL = new Color(0, 255, 0, 20);
    
    @Override
    public void drawMe(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize, boolean actuallyDraw, boolean actuallyDrawPdf) throws IOException
    {
        // Only draw a PreviewDrawable on a java graphics -- ignore it for painting onto a PDDocument
        Color oldColor = g2.getColor();
        if (preview == null) {
            g2.setColor(PREVIEW_COLOR_EMPTY);
        } else {
            g2.setColor(PREVIEW_COLOR_FULL);
        }
        
        g2.fillRect((int) getX(), (int) getY(), (int) getWidth(), (int) getHeight());
        g2.setColor(oldColor);
        if (preview != null) {
            preview.setX(getX());
            preview.setY(getY());
            preview.setWidth(getWidth());
            preview.setHeight(getHeight());
            
            preview.drawMe(g2, editPanelSize, actuallyDraw, actuallyDrawPdf);
            
            this.setX(preview.getX());
            this.setY(preview.getY());
            this.setWidth(preview.getWidth());
            this.setHeight(preview.getHeight());
        }
    }

    public Drawable getPreview()
    {
        return preview;
    }

    public void setPreview(Drawable preview)
    {
        this.preview = preview;
    }

}
