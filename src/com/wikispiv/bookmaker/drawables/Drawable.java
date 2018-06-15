package com.wikispiv.bookmaker.drawables;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;
import com.wikispiv.bookmaker.Utils;
import com.wikispiv.bookmaker.enums.Alignment;
import com.wikispiv.bookmaker.enums.CardinalDirection;
import com.wikispiv.bookmaker.enums.TransformAction;
import com.wikispiv.bookmaker.rendering.WSFont;
import com.wikispiv.bookmaker.ui.WSMouseEvent;

/**
 * Represents anything which will be drawn either as a preview or in the pdf
 * 
 * @author Daniel Centore
 */
public abstract class Drawable implements Serializable
{
    private static final long serialVersionUID = 1L;

    private static final int MINIMUM_DIMENSION = 10;
    private static final int RESIZE_BAR_SIZE = 10;
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 300;
    private double width;
    private double height;
    private double x;
    private double y;

    // Higher value uses more memory but renders faster
    private static final int CACHED_PDF_MAX_USAGES = 100000;
    private static int cachedPdfUsages = 0;
    private static Graphics2D cachedFakeGraphics;
    private static PDPageContentStream cachedFakeContentStream;
    private static PDDocument cachedFakeDocument;

    public Drawable()
    {
        setX(0);
        setY(0);
        setWidth(DEFAULT_WIDTH);
        setHeight(DEFAULT_HEIGHT);
    }

    public Drawable(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public abstract void drawMe(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf)
            throws IOException;

    /**
     * Draws the Drawable using a completely fake environment. You would do this to
     * invoke side effects, like updating what the last rendered verse is in a
     * {@link SongChunkDrawable}
     */
    public void drawMe()
    {
        SpivanykPrefs prefs = Main.getPrefs();
        if (cachedFakeGraphics == null) {
            Image img = Main.getBmf().createImage((int) prefs.getPageWidth(), (int) prefs.getPageHeight());
            cachedFakeGraphics = (Graphics2D) img.getGraphics();
        }

        try {
            drawMe(cachedFakeGraphics, null, false, false);
        } catch (Exception e) {
            Main.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void drawMe(Graphics2D g2, Dimension editPanelSize, boolean actuallyDraw, boolean actuallyDrawPdf)
            throws IOException
    {
        SpivanykPrefs prefs = Main.getPrefs();
        if (cachedFakeContentStream == null) {
            cachedFakeDocument = new PDDocument();
            PDRectangle pagesize = Utils.pdFromRect(prefs.getPageSize());
            PDPage pdpage = new PDPage(pagesize);
            cachedFakeContentStream = new PDPageContentStream(cachedFakeDocument, pdpage);
        }

        try {
            drawMe(g2, cachedFakeContentStream, cachedFakeDocument, editPanelSize, actuallyDraw, actuallyDrawPdf);
        } catch (Exception e) {
            Main.println(e.getLocalizedMessage());
            e.printStackTrace();
        }

        if (cachedPdfUsages++ > CACHED_PDF_MAX_USAGES) {
            cachedFakeContentStream.close();
            cachedFakeDocument.close();
            cachedFakeContentStream = null;
            cachedFakeDocument = null;
            cachedPdfUsages = 0;
        }
    }

    public void drawMe(PDPageContentStream pg, PDDocument document, boolean actuallyDraw, boolean actuallyDrawPdf)
            throws IOException
    {
        SpivanykPrefs prefs = Main.getPrefs();
        if (cachedFakeGraphics == null) {
            Image img = Main.getBmf().createImage((int) prefs.getPageWidth(), (int) prefs.getPageHeight());
            cachedFakeGraphics = (Graphics2D) img.getGraphics();
        }

        try {
            drawMe(cachedFakeGraphics, pg, document, null, actuallyDraw, actuallyDrawPdf);
        } catch (Exception e) {
            Main.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public Rectangle2D getRect()
    {
        return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
    }

    public double getWidth()
    {
        return width;
    }

    public void setWidth(double width)
    {
        width = Math.max(MINIMUM_DIMENSION, width);
        this.width = width;
    }

    public double getHeight()
    {
        return height;
    }

    public void setHeight(double height)
    {
        height = Math.max(MINIMUM_DIMENSION, height);
        this.height = height;
    }

    public double getX()
    {
        return x;
    }

    public void setX(double x)
    {
        this.x = x;
    }

    public double getY()
    {
        return y;
    }

    public void setY(double y)
    {
        this.y = y;
    }

    public double getBottomY()
    {
        return getY() + getHeight();
    }

    public double getRightX()
    {
        return getX() + getWidth();
    }

    public Point2D getPoint()
    {
        return new Point2D.Double(getX(), getY());
    }

    public TransformAction shouldDoAction(double x, double y)
    {
        // Should resize
        boolean n = false;
        boolean s = false;
        boolean e = false;
        boolean w = false;
        if (Utils.getResizeRect(getRect(), CardinalDirection.N, RESIZE_BAR_SIZE).contains(x, y)) {
            n = true;
        }
        if (Utils.getResizeRect(getRect(), CardinalDirection.S, RESIZE_BAR_SIZE).contains(x, y)) {
            s = true;
        }
        if (Utils.getResizeRect(getRect(), CardinalDirection.E, RESIZE_BAR_SIZE).contains(x, y)) {
            e = true;
        }
        if (Utils.getResizeRect(getRect(), CardinalDirection.W, RESIZE_BAR_SIZE).contains(x, y)) {
            w = true;
        }
        if (n) {
            if (e) {
                return TransformAction.RESIZE_NE;
            } else if (w) {
                return TransformAction.RESIZE_NW;
            }
            return TransformAction.RESIZE_N;
        } else if (s) {
            if (e) {
                return TransformAction.RESIZE_SE;
            } else if (w) {
                return TransformAction.RESIZE_SW;
            }
            return TransformAction.RESIZE_S;
        } else if (e) {
            return TransformAction.RESIZE_E;
        } else if (w) {
            return TransformAction.RESIZE_W;
        }

        // Should move?
        if (getRect().contains(x, y)) {
            return TransformAction.MOVE;
        }

        // Shouldn't do anything
        return TransformAction.NOTHING;
    }

    public void move(WSMouseEvent initialClick, WSMouseEvent moveEvent, Point2D selectedInitialPoint)
    {
        setX((int) (moveEvent.getX() + (selectedInitialPoint.getX() - initialClick.getX())));
        setY((int) (moveEvent.getY() + (selectedInitialPoint.getY() - initialClick.getY())));
    }

    public void resize(TransformAction selectedAction, WSMouseEvent initialClick, WSMouseEvent moveEvent,
            Point2D selectedInitialPoint)
    {
        boolean resizeN = false;
        boolean resizeS = false;
        boolean resizeE = false;
        boolean resizeW = false;
        switch (selectedAction)
        {
            case RESIZE_E:
                resizeE = true;
                break;
            case RESIZE_N:
                resizeN = true;
                break;
            case RESIZE_NE:
                resizeN = true;
                resizeE = true;
                break;
            case RESIZE_NW:
                resizeN = true;
                resizeW = true;
                break;
            case RESIZE_S:
                resizeS = true;
                break;
            case RESIZE_SE:
                resizeS = true;
                resizeE = true;
                break;
            case RESIZE_SW:
                resizeS = true;
                resizeW = true;
                break;
            case RESIZE_W:
                resizeW = true;
                break;
            default:
                throw new RuntimeException("Why are we trying to resize with: " + selectedAction.toString() + "???");
        }
        double newX = getX();
        double newY = getY();
        double newW = getWidth();
        double newH = getHeight();
        if (resizeN) {
            newY = moveEvent.getY();
            newH = getHeight() + (this.getY() - moveEvent.getY());
        }
        if (resizeS) {
            newH = moveEvent.getY() - this.getY();
        }
        if (resizeE) {
            newW = moveEvent.getX() - this.getX();
        }
        if (resizeW) {
            newX = moveEvent.getX();
            newW = getWidth() + (this.getX() - moveEvent.getX());
        }
        if (newH >= MINIMUM_DIMENSION || newW >= MINIMUM_DIMENSION) {
            setY(newY);
            setX(newX);
            setHeight(newH);
            setWidth(newW);
        }
    }

    /**
     * 
     * @param font
     * @param g2
     * @param pg
     * @param document
     * @param x
     * @param y
     * @param text
     * @param actuallyDraw
     *            Actually draw the thing or are we just doing this for the return
     *            value?
     * @param actuallyDrawPdf
     *            If we are actually drawing, do we also want to draw the pdf?
     * @param align
     * @param alignWidth
     *            The width used for alignment (ignored for left align)
     * @return The bounds of the drawn string
     * @throws IOException
     */
    protected Rectangle2D drawString(WSFont font, Graphics2D g2, PDPageContentStream pg, PDDocument document, double x,
            double y, String text, boolean actuallyDraw, boolean actuallyDrawPdf, int align, double alignWidth,
            Dimension editPanelSize)
            throws IOException
    {
        double topLeftY = y;
        y += font.getFontSize();

        if (actuallyDrawPdf) {
            // g2.setFont(font.getGenericJavaFont());
            pg.setFont(font.getPdfFont(document), (float) font.getFontSize());
        }

        double maxWidth = 0;
        double leftmostPositionX = Double.MAX_VALUE;
        double height = 0;
        for (String line : text.split("\n")) {
            line = font.sanitize(document, line);
            double alignedPositionX = getPositionX(x, alignWidth, align, line, font, document);
            leftmostPositionX = Math.min(leftmostPositionX, alignedPositionX);
            if (actuallyDraw) {
                Font fixedJavaFont = font.getFixedJavaFont(g2, document, line, editPanelSize);
                g2.setFont(fixedJavaFont);
                g2.setColor(font.getFontColor());
                g2.drawString(line, Math.round(alignedPositionX), Math.round(y));
            }
            if (actuallyDrawPdf) {
                pg.beginText();
                pg.setNonStrokingColor(font.getFontColor());
                pg.setStrokingColor(font.getFontColor());
                pg.newLineAtOffset(Math.round(alignedPositionX), Utils.iflipY(y));
                pg.showText(line);
                pg.endText();

                if (Main.DRAW_PAGE_OUTLINE) {
                    // TODO Unjankify this?
                    SpivanykPrefs prefs = Main.getPrefs();
                    pg.addRect(0, 0, (float) prefs.getPageWidth(), (float) prefs.getPageHeight());
                    pg.stroke();
                }
            }
            maxWidth = Math.max(maxWidth, font.getWidth(document, line));
            y += font.getLineHeight();
            height += font.getLineHeight();
        }
        return new Rectangle2D.Double(leftmostPositionX, topLeftY, maxWidth, height);
    }

    protected double getPositionX(double originalX, double totalWidth, int align, String text, WSFont font,
            PDDocument document)
    {
        double stringWidth = font.getWidth(document, text);
        switch (align)
        {
            case Alignment.LEFT_ALIGNED:
                return originalX;
            case Alignment.CENTERED:
                return originalX + (totalWidth - stringWidth) / 2.0;
            case Alignment.RIGHT_ALIGNED:
                return originalX + totalWidth - stringWidth;
        }
        throw new RuntimeException("Invalid alignment: " + align);
    }

    public Dimension getDimension()
    {
        return new Dimension((int) Math.round(getWidth()), (int) Math.round(getHeight()));
    }
}
