package com.wikispiv.bookmaker.drawables;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;

/**
 * Represents a drawable which continues over multiple pages
 * 
 * You are responsible for
 * calling setLastRenderedChunk(i) with the last chunk you render (no harm in
 * multiple calls, as long as the last one is correct)
 * 
 * @author Daniel Centore
 *
 */
public abstract class ContinuableDrawable extends Drawable
{
    private static final long serialVersionUID = 1L;

    private int firstRenderedChunk = 0;
    private int lastRenderedChunk = 0;
    private boolean renderTitle = false;

    public abstract int getNumberChunks();

    public abstract void drawMeContiuably(Graphics2D g2, PDPageContentStream pg, PDDocument document,
            Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf)
            throws IOException, IllegalArgumentException;

    @Override
    public final void drawMe(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf)
            throws IOException, IllegalArgumentException
    {
        firstRenderedChunk = calculateStartChunk(g2, pg, document);
        this.renderTitle = firstRenderedChunk < 0;
        if (renderTitle) {
            firstRenderedChunk = 0;
        }
        lastRenderedChunk = firstRenderedChunk - 1;
        drawMeContiuably(g2, pg, document, editPanelSize, actuallyDraw, actuallyDrawPdf);
    }

    /**
     * NOTE: You need to render to refresh this value!
     * 
     * @return
     */
    public boolean includesLastChunk()
    {
        return lastRenderedChunk == getNumberChunks() - 1;
    }

    /**
     * NOTE: You need to render to refresh this value!
     * 
     * @return
     */
    public boolean shouldRenderTitle()
    {
        return renderTitle;
    }

    /**
     * Tells us if this drawable is a continuation, an ancestor, or the same
     * drawable as d
     * 
     * @param d
     * @return
     */
    public abstract boolean isSameContinuum(Drawable d);

    /**
     * 
     * @param g2
     * @param pg
     * @param document
     * @return -1 = This is the title slide, 0... The first chunk index we should
     *         render
     */
    private int calculateStartChunk(Graphics2D g2, PDPageContentStream pg, PDDocument document)
    {
        SpivanykPrefs prefs = Main.getPrefs();
        int startChunk = -1;
        for (WSPage page : prefs.getPages()) {
            for (Drawable d : page.getDrawables()) {
                Drawable actualD = d;
                if (actualD instanceof PreviewDrawable) {
                    actualD = ((PreviewDrawable) actualD).getPreview();
                }
                if (actualD == this) {
                    return startChunk;
                }
                if (actualD instanceof ContinuableDrawable) {
                    ContinuableDrawable cd = (ContinuableDrawable) actualD;
                    if (this.isSameContinuum(cd)) {
                        startChunk = cd.lastRenderedChunk + 1;
                    }
                }
                // if (actualD instanceof SongChunkDrawable) {
                // SongChunkDrawable scd = (SongChunkDrawable) actualD;
                // if (scd.getSong() == this.getSong()) {
                // startChunk = scd.lastRenderedStanza + 1;
                // }
                // }
            }
        }
        return -1;
    }

    public int getLastRenderedChunk()
    {
        return lastRenderedChunk;
    }

    public void setLastRenderedChunk(int lastRenderedChunk)
    {
        this.lastRenderedChunk = lastRenderedChunk;
    }

    public int getFirstRenderedChunk()
    {
        return firstRenderedChunk;
    }

    public boolean isRenderTitle()
    {
        return renderTitle;
    }

}
