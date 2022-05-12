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
 * TODO: Offer some sort of warning if an index chunk is empty, like on the song
 * one
 * 
 * @author Daniel Centore
 *
 */
public class IndexDrawable extends ContinuableDrawable implements Serializable
{
    private static final long serialVersionUID = 1L;
    public static final String ALPHABET_ORDER = "АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЮЯ";

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
        WSFont indexAlphFont = prefs.getIndexAlphabetFont();
        WSFont indexAlphBoldFont = prefs.getIndexAlphabetHighlightFont();
        if (renderTitle) {
            positionY = getY() + indexTitleFont.getFontSize();
            Rectangle2D titleRect = drawString(indexTitleFont, g2, pg, document, getX(), positionY, getIndexTitle(),
                    actuallyDraw, actuallyDrawPdf, Alignment.CENTERED, getWidth(), editPanelSize);
            positionY = titleRect.getMaxY();
        }

        // Draw entries
        int lastRenderedEntry = firstRenderedEntry;
        for (int entryId = firstRenderedEntry; entryId < entries.size(); ++entryId) {
            Entry entry = entries.get(entryId);

            // boolean appendMainTitle = !entry.isMainTitle() &&
            // !entry.getTitle().contains("(")
            // && !entry.getTitle().contains(")");
            // String title = entry.getTitle() + (appendMainTitle ? " (" +
            // entry.getMainTitle() + ")" : "");
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
            lastRenderedEntry = entryId;
        }
        
        if (firstRenderedEntry < entries.size()) {
            // == Render the Alphabet == //
            positionY += indexFont.getLineHeight();

            char firstLetter = entries.get(firstRenderedEntry).getTitle().charAt(0);
            char lastLetter = entries.get(lastRenderedEntry).getTitle().charAt(0);

            int firstLetterIdx = ALPHABET_ORDER.indexOf(firstLetter);
            int lastLetterIdx = ALPHABET_ORDER.indexOf(lastLetter);
            lastLetterIdx = lastLetterIdx >= 0 ? lastLetterIdx : ALPHABET_ORDER.length() - 1;

            String firstChars = firstLetterIdx < 0 ? ALPHABET_ORDER : ALPHABET_ORDER.substring(0, firstLetterIdx);
            String middleChars = firstLetterIdx < 0 ? "" : ALPHABET_ORDER.substring(firstLetterIdx, lastLetterIdx + 1);
            String endChars = firstLetterIdx < 0 ? ""
                    : ALPHABET_ORDER.substring(Math.min(lastLetterIdx + 1, ALPHABET_ORDER.length()));

            // Kerning hack to keep the sections from being too close. I'm being lazy
            double spaceWidthKludge = indexAlphFont.getWidth(document, " ") / 3;
            
            double firstWidth = indexAlphFont.getWidth(document, firstChars) + spaceWidthKludge;
            double middleWidth = indexAlphBoldFont.getWidth(document, middleChars) + spaceWidthKludge;
            double endWidth = indexAlphFont.getWidth(document, endChars);
            double totalAlphWidth = firstWidth + middleWidth + endWidth;

            double startX = positionX + getWidth() / 2 - totalAlphWidth / 2;

            drawString(
                    indexAlphFont,
                    g2, pg, document,
                    startX,
                    positionY,
                    firstChars,
                    actuallyDraw,
                    actuallyDrawPdf,
                    Alignment.LEFT_ALIGNED,
                    getWidth(),
                    editPanelSize);
            drawString(indexAlphBoldFont, g2, pg, document, startX + firstWidth, positionY,
                    middleChars,
                    actuallyDraw,
                    actuallyDrawPdf,
                    Alignment.LEFT_ALIGNED, getWidth(), editPanelSize);
            drawString(indexAlphFont, g2, pg, document, startX + firstWidth + middleWidth, positionY,
                    endChars,
                    actuallyDraw, actuallyDrawPdf,
                    Alignment.LEFT_ALIGNED, getWidth(), editPanelSize);
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
