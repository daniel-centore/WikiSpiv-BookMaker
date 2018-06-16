package com.wikispiv.bookmaker.drawables;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.Serializable;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.enums.Alignment;
import com.wikispiv.bookmaker.rendering.WSFont;

public class TextLineDrawable extends Drawable implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String text;
    private String fontType;

    public TextLineDrawable(String text, String fontType)
    {
        this.text = text;
        this.fontType = fontType;
    }

    @Override
    public void drawMe(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf) throws IOException
    {
        WSFont font = Main.getMain().getFontByName(fontType);
        this.drawString(font, g2, pg, document, getX(), getY(), text, actuallyDraw, actuallyDrawPdf, Alignment.CENTERED,
                getWidth(), editPanelSize);
    }

}
