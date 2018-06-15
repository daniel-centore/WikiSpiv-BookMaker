package com.wikispiv.bookmaker.drawables;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.Serializable;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;

public class PageNumDrawable extends Drawable implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int align;
    private int pageNumber;

    public PageNumDrawable(int align)
    {
        this.align = align;
    }

    @Override
    public void drawMe(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf) throws IOException
    {
        SpivanykPrefs prefs = Main.getPrefs();
        drawString(prefs.getPageNumFont(), g2, pg, document, getX(), getY(), pageNumber + "", actuallyDraw,
                actuallyDrawPdf, align, getWidth(), editPanelSize);
    }

    public void setPageNumber(int pageNumber)
    {
        this.pageNumber = pageNumber;
    }

}
