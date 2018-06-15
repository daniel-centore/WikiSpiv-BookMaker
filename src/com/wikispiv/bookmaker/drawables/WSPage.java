package com.wikispiv.bookmaker.drawables;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;
import com.wikispiv.bookmaker.enums.Alignment;

/**
 * Represents a single page in the spivanyk
 * 
 * @author Daniel Centore
 *
 */
public class WSPage implements Serializable
{
    private static final long serialVersionUID = 1L;
    public static final int DRAWABLE_BORDER_WIDTH = 1;
    public static final int PAGE_BORDER_WIDTH = 3;
    public static final Color MARGIN_COLOR = new Color(239, 242, 247);
    public static final Color REGULAR_COLOR = new Color(0, 0, 0, 0);
    public static final Color HOVER_COLOR = Color.LIGHT_GRAY;
    public static final Color SELECTED_COLOR = new Color(255, 0, 0, 100);
    private List<Drawable> drawables;

    public WSPage()
    {
    }

    /**
     * Calculates the page number this page is on. This is O(n)!
     * 
     * @return The page number starting at 0
     */
    public int calcPageIndex()
    {
        return Main.getPrefs().getPages().indexOf(this);
    }

    /**
     * Calculates whether the spine is on the right or the left of this page. This
     * is O(n)!
     * 
     * @return True if spine is on the right side of the page; False if it is on the
     *         left
     */
    public boolean calcSpineIsRight()
    {
        // return calcPageIndex() % 2 == 0;
        return isSpineRight(calcPageIndex());
    }

    public static boolean isSpineRight(int pageIndex)
    {
        return pageIndex % 2 == 0;
    }

    /**
     * Draws the page onto a graphics
     * 
     * NOTE: Make sure to call Main.drawAllPages() before this if they're not going
     * to all be rendered first! Otherwise we'll use stale historical data about
     * things like how much of a song from a previous page has been rendered thus
     * far. This could lead to some rare and obscure bugs cropping up.
     * 
     * @param g2
     *            The graphics to draw onto
     * @param x
     *            Upper left (x,y)
     * @param y
     * @param panelPageWidth
     *            Height and width of the page
     * @param panelPageHeight
     * @param selectedDrawable
     */
    public void drawMe(Graphics2D g2, double x, double y, double panelPageWidth, double panelPageHeight,
            Drawable selectedDrawable, Drawable hoverDrawable, PreviewDrawable previewDrawable, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf)
    {
        SpivanykPrefs prefs = Main.getPrefs();

        AffineTransform oldTransform = g2.getTransform();
        g2.setClip((int) x, (int) y, (int) panelPageWidth, (int) panelPageHeight);
        // Set scale and translate to draw things relative to (0, 0) and the page pt
        // width,height
        g2.transform(AffineTransform.getTranslateInstance(x, y));
        g2.transform(AffineTransform.getScaleInstance(panelPageWidth / prefs.getPageWidth(),
                panelPageHeight / prefs.getPageHeight()));

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, (int) prefs.getPageWidth(), (int) prefs.getPageHeight());

        g2.setColor(MARGIN_COLOR);
        boolean spineIsRight = calcSpineIsRight();
        double marginLeft, marginRight;
        if (spineIsRight) {
            marginLeft = prefs.getPageMarginSideEdge();
            marginRight = prefs.getPageMarginSpine();
        } else {
            marginLeft = prefs.getPageMarginSpine();
            marginRight = prefs.getPageMarginSideEdge();
        }
        g2.fillRect(0, 0, (int) marginLeft, (int) prefs.getPageHeight());
        g2.fillRect(0, 0, (int) prefs.getPageWidth(), (int) prefs.getPageMarginTop());
        g2.fillRect(0, (int) (prefs.getPageHeight() - prefs.getPageMarginBottom()), (int) prefs.getPageWidth(),
                (int) prefs.getPageMarginBottom());
        g2.fillRect((int) (prefs.getPageWidth() - marginRight), 0, (int) marginRight, (int) prefs.getPageHeight());

        g2.setColor(Color.BLACK);
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(PAGE_BORDER_WIDTH));
        g2.drawRect(0, 0, (int) prefs.getPageWidth(), (int) prefs.getPageHeight());
        g2.setStroke(oldStroke);

        for (Drawable d : getDrawables()) {
            try {
                d.drawMe(g2, editPanelSize, actuallyDraw, actuallyDrawPdf);
            } catch (IOException e) {
                Main.println("ERROR rendering drawable: " + e.getMessage());
                e.printStackTrace();
            }
            if (d == selectedDrawable) {
                g2.setColor(SELECTED_COLOR);
            } else if (d == hoverDrawable) {
                g2.setColor(HOVER_COLOR);
            } else {
                g2.setColor(REGULAR_COLOR);
            }
            g2.setStroke(new BasicStroke(DRAWABLE_BORDER_WIDTH));
            g2.drawRect((int) d.getX(), (int) d.getY(), (int) d.getWidth(), (int) d.getHeight());
            g2.setColor(Color.BLACK);
            g2.setStroke(oldStroke);
        }

        // Reset to original transformation
        g2.setClip(null);
        g2.setTransform(oldTransform);
    }

    /**
     * NOTE: Make sure to call Main.drawAllPages() before this if they're not going
     * to all be rendered!
     * 
     * @param contentStream
     * @param document
     * @throws IOException
     */
    public void drawMe(PDPageContentStream contentStream, PDDocument document, boolean actuallyDraw,
            boolean actuallyDrawPdf) throws IOException
    {
        for (Drawable d : getDrawables()) {
            d.drawMe(contentStream, document, actuallyDraw, actuallyDrawPdf);
        }
    }

    public List<Drawable> getDrawables()
    {
        if (drawables == null) {
            drawables = new ArrayList<>();
        }
        int pageIndex = calcPageIndex();
        SpivanykPrefs prefs = Main.getPrefs();
        PageNumDrawable correctPageNoDrawable = isSpineRight(pageIndex) ? prefs.getLeftPageNum() : prefs.getRightPageNum();
        if (!drawables.contains(correctPageNoDrawable)) {
            drawables.removeIf(d -> d instanceof PageNumDrawable);
            drawables.add(correctPageNoDrawable);
        }
        correctPageNoDrawable.setPageNumber(pageIndex + prefs.getFirstPageNum());
        return drawables;
    }
}
