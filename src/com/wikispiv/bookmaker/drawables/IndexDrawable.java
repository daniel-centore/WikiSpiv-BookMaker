package com.wikispiv.bookmaker.drawables;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;
import com.wikispiv.bookmaker.data.Transliterator;
import com.wikispiv.bookmaker.enums.Alignment;
import com.wikispiv.bookmaker.rendering.IndexCalculator;
import com.wikispiv.bookmaker.rendering.IndexCalculator.Entry;
import com.wikispiv.bookmaker.rendering.WSFont;

/**
 * Represents a chunk of the index
 * 
 * TODO: Offer some sort of warning if the last element of the index is not
 * rendered
 * 
 * TODO: Offer some sort of warning if an index chunk is empty, like on the song one
 * 
 * @author Daniel Centore
 *
 */
public class IndexDrawable extends ContinuableDrawable implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Override
    public int getNumberChunks()
    {
        return IndexCalculator.getSingleton().generateEntries().size();
    }

    @Override
    public void drawMeContiuably(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf) throws IOException, IllegalArgumentException
    {
        SpivanykPrefs prefs = Main.getPrefs();

        int firstRenderedEntry = getFirstRenderedChunk();
        boolean renderTitle = shouldRenderTitle();

        Font oldFont = g2.getFont();

        g2.setColor(Color.BLACK);

        List<Entry> entries = IndexCalculator.getSingleton().generateEntries();

        // Draw the title & credits
        double positionY = getY();
        double positionX = getX();
        WSFont indexTitleFont = prefs.getIndexTitleFont();
        WSFont indexFont = prefs.getIndexFont();
        WSFont indexAltFont = prefs.getIndexAltFont();
        if (renderTitle) {
            positionY = getY() + indexTitleFont.getFontSize();
            Rectangle2D titleRect = drawString(indexTitleFont, g2, pg, document, getX(), positionY, getIndexTitle(),
                    actuallyDraw, actuallyDrawPdf, Alignment.CENTERED, getWidth(), editPanelSize);
            positionY = titleRect.getMaxY();
        }

        // Draw entries
        for (int entryId = firstRenderedEntry; entryId < entries.size(); ++entryId) {
            Entry entry = entries.get(entryId);

            String title = entry.getTitle();
            int pageNum = entry.getPage() + prefs.getFirstPageNum();
            WSFont currentLineFont = entry.isMainTitle() ? indexFont : indexAltFont;
            Rectangle2D boundsPreview = drawString(currentLineFont, g2, pg, document, positionX, positionY, title,
                    false, false, Alignment.LEFT_ALIGNED, getWidth(), editPanelSize);
            if (positionY + boundsPreview.getHeight() > this.getBottomY()) {
                // Can't render any more :(
                break;
            }

            Rectangle2D pagenoBounds = drawString(currentLineFont, g2, pg, document, positionX, positionY, pageNum + "",
                    actuallyDraw, actuallyDrawPdf, Alignment.RIGHT_ALIGNED, getWidth(), editPanelSize);

            int numPeriods = 0;
            if (actuallyDraw || actuallyDrawPdf) {
                for (numPeriods = 0;; ++numPeriods) {
                    String periods = String.join("", Collections.nCopies(numPeriods + 1, "."));
                    Rectangle2D titleBounds = drawString(currentLineFont, g2, pg, document, positionX, positionY,
                            title + periods, false, false, Alignment.LEFT_ALIGNED, getWidth(), editPanelSize);
                    if (titleBounds.getMaxX() > pagenoBounds.getMinX()) {
                        // Can't fit one more period!
                        break;
                    }
                }
                String periods = String.join("", Collections.nCopies(numPeriods, "."));
                drawString(currentLineFont, g2, pg, document, positionX, positionY, title + periods, actuallyDraw,
                        actuallyDrawPdf, Alignment.LEFT_ALIGNED, getWidth(), editPanelSize);
            }

            positionY += pagenoBounds.getHeight();
            setLastRenderedChunk(entryId);
        }

        g2.setFont(oldFont);
    }

    public String getIndexTitle()
    {
        // TODO: Make this customizable
        return Transliterator.transliterate("Зміст", true);
    }

    @Override
    public boolean isSameContinuum(Drawable d)
    {
        // There's only 1 index so this is true -- see SongChunkDrawable for a more
        // comprehensive implementation
        return (d instanceof IndexDrawable);
    }

}
