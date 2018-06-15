package com.wikispiv.bookmaker.drawables;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.Serializable;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.wikispiv.bookmaker.Utils;

public class DummyDrawable extends Drawable implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    public static final String DUMMY_TEXT = "Kapusta is yummy.";
    public static final int FONT_SIZE = 12;

    @Override
    public void drawMe(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize, boolean actuallyDraw, boolean actuallyDrawPdf) throws IOException
    {
        Font oldFont = g2.getFont();
        pg.beginText();

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Times New Roman", Font.PLAIN, FONT_SIZE));
        pg.setFont(PDType1Font.TIMES_ROMAN, FONT_SIZE);
        
        int x = (int) getX();
        int y = (int) (getY() + g2.getFontMetrics().getHeight());
        g2.drawString(DUMMY_TEXT, x, y);
        pg.newLineAtOffset(x, Utils.iflipY(y));

        String text = "Kapusta is yummy.";
        pg.showText(text);
        pg.endText();
        
        g2.setFont(oldFont);
    }

}
