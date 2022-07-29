package com.wikispiv.bookmaker.drawables;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;
import com.wikispiv.bookmaker.data.Chunk;
import com.wikispiv.bookmaker.data.Line;
import com.wikispiv.bookmaker.data.Song;
import com.wikispiv.bookmaker.data.Stanza;
import com.wikispiv.bookmaker.enums.Alignment;
import com.wikispiv.bookmaker.rendering.WSFont;

/**
 * Handles drawing some chunks (i.e. title, stanzas) of a Song
 * 
 * @author Daniel Centore
 *
 */
public class SongChunkDrawable extends ContinuableDrawable implements Serializable, AlignableDrawable
{
    private static final long serialVersionUID = 1L;

    private Song song;
    private int align = Alignment.LEFT_ALIGNED;

    public SongChunkDrawable(Song song)
    {
        this.song = song;
    }

    @Override
    public void drawMeContiuably(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize,
            boolean actuallyDraw, boolean actuallyDrawPdf)
            throws IOException, IllegalArgumentException
    {
        SpivanykPrefs prefs = Main.getPrefs();

        int firstRenderedStanza = getFirstRenderedChunk();
        boolean renderTitle = shouldRenderTitle();

        // Draw the title & credits
        double bottomOfTitleY = getY();
        double positionY = getY();
        WSFont titleFont = prefs.getTitleFont();
        WSFont creditsFont = prefs.getCreditsFont();
        if (renderTitle) {
            positionY = getY() + titleFont.getFontSize();
            Rectangle2D titleRect = drawString(titleFont, g2, pg, document, getX(), positionY, song.getMainTitle(),
                    actuallyDraw, actuallyDrawPdf, align, getWidth(), editPanelSize);
            positionY = titleRect.getMaxY();
            if (song.getCredits() != null && !song.getCredits().trim().isEmpty()) {
                Rectangle2D creditRect = drawString(creditsFont, g2, pg, document, getX(), positionY,
                        song.getCredits(), actuallyDraw, actuallyDrawPdf, align, getWidth(), editPanelSize);
                positionY = creditRect.getMaxY();
            }
            positionY += prefs.getBeforeLyrics();
            bottomOfTitleY = positionY;
        }

        // Draw stanzas
        double columnWidth = maxStanzaWidth(g2, pg, document, editPanelSize);
        int availableColumns = (int) (this.getWidth() / columnWidth);
        // if (availableColumns < 1) {
        // availableColumns = 1;
        // setWidth(columnWidth);
        // }
        double extraSpace = this.getWidth() - columnWidth * availableColumns;
        double betweenColumns = 0;
        if (availableColumns > 1) {
            betweenColumns = extraSpace / (availableColumns - 1);
        }

        double positionX = getX();
        List<Stanza> stanzas = song.getStanzas();
        boolean renderedStanza = false;
        for (int i = firstRenderedStanza; i < stanzas.size(); ++i) {
            Stanza stan = stanzas.get(i);
            Rectangle2D preview = renderStanza(g2, pg, document, stan, positionX, positionY, false, false,
                    editPanelSize);
            if (preview.getMaxY() > this.getBottomY()) {
                // Time for a new column!
                positionX += columnWidth + betweenColumns;
                positionY = bottomOfTitleY;
            }

            double EPSILON = 0.01;
            if (positionX + columnWidth - EPSILON > this.getRightX()
                    || positionY + preview.getHeight() > this.getBottomY()) {
                // Can't render any more :(
                break;
            }

            Rectangle2D rendered = renderStanza(g2, pg, document, stan, positionX, positionY, actuallyDraw,
                    actuallyDrawPdf, editPanelSize);
            positionY += rendered.getHeight();
            // lastRenderedStanza = i;
            setLastRenderedChunk(i);
            renderedStanza = true;
        }

        if (!renderedStanza && !renderTitle) {
            drawString(titleFont, g2, pg, document, getX(), positionY, "EMPTY SONG CHUNK; DELETE ME!", actuallyDraw,
                    actuallyDrawPdf,
                    Alignment.LEFT_ALIGNED, 0, editPanelSize);
        }
    }

    private double maxStanzaWidth(Graphics2D g2, PDPageContentStream pg, PDDocument document, Dimension editPanelSize)
            throws IOException
    {
        double maxWidth = 0;
        for (Stanza stan : song.getStanzas()) {
            Rectangle2D rend = renderStanza(g2, pg, document, stan, 0, 0, false, false, editPanelSize);
            maxWidth = Math.max(maxWidth, rend.getWidth());
        }
        return maxWidth;
    }
    
    private static int chunkBeforeLastChunkWithContent(List<Chunk> chunks) {
        for (int i = chunks.size() - 1; i >= 0; --i) {
            if (!chunks.get(i).getText().isEmpty()) {
                return Math.max(i - 1, 0);
            }
        }
        return 0;
    }

    /**
     * 
     * @param stan
     * @return The bounds of the drawn stanza
     * @throws IOException
     */
    private Rectangle2D renderStanza(Graphics2D g2, PDPageContentStream pg, PDDocument document, Stanza stan, double x,
            double y, boolean actuallyDraw, boolean actuallyDrawPdf, Dimension editPanelSize) throws IOException
    {
        SpivanykPrefs prefs = Main.getPrefs();
        WSFont lyricsFont = prefs.getLyricFont();
        WSFont instructionFont = prefs.getInstructionFont();
        WSFont chordFont = prefs.getChordFont();

        double upperLeftY = y;
        double upperLeftX = x;
        double maxWidth = 0;
        double height = 0;
        for (Line l : stan.getLines()) {
            double extraX = (l.isIndented() ? prefs.getIndentSize() : 0);
            WSFont textFont = l.getLyricIsInstruction() ? instructionFont : lyricsFont;
            if (l.hasLyricLine() && l.hasChordLine()) {
                // Align chords to the text
                List<Chunk> chunks = l.getChunks();
                double chunkX = x + extraX;
                double chordHeight = chordFont.getLineHeight();
                double maxHeight = 0;
                double totalLineWidth = 0;
                
                int chunkBeforeLastChunkWithContent = chunkBeforeLastChunkWithContent(chunks);

                boolean anyChordsWiderThanText = false;
                // We don't care about the last chunk because nothing after it will be affected
                for (int i = 0; i < chunkBeforeLastChunkWithContent; ++i) {
                    Chunk c = chunks.get(i);
                    if (chordFont.getWidth(document, c.getChord()) > textFont.getWidth(document, c.getText())) {
                        anyChordsWiderThanText = true;
                        break;
                    }
                }

                if (!anyChordsWiderThanText) {
                    // Just drop the whole line down and place only the chords
                    drawString(textFont, g2, pg, document, x + extraX, y + chordHeight, l.getLyricLine(), actuallyDraw,
                            actuallyDrawPdf, Alignment.LEFT_ALIGNED, 0, editPanelSize);
                }

                for (Chunk c : chunks) {
                    Rectangle2D chordRect = drawString(chordFont, g2, pg, document, chunkX, y, c.getChord() + " ",
                            actuallyDraw, actuallyDrawPdf, Alignment.LEFT_ALIGNED, 0, editPanelSize);
                    Rectangle2D chunkLineRect = drawString(textFont, g2, pg, document,
                            chunkX, y + chordHeight,
                            c.getText(),
                            anyChordsWiderThanText ? actuallyDraw : false,
                            anyChordsWiderThanText ? actuallyDrawPdf : false,
                            Alignment.LEFT_ALIGNED, 0, editPanelSize);

                    double chunkWidth = Math.max(chordRect.getWidth(), chunkLineRect.getWidth());
                    chunkX += chunkWidth;
                    totalLineWidth += chunkWidth;
                    maxHeight = Math.max(maxHeight, chunkLineRect.getMaxY() - chordRect.getMinY());
                }
                height += maxHeight;
                y += maxHeight;
                maxWidth = Math.max(maxWidth, totalLineWidth + extraX);
            } else if (l.hasChordLine()) {
                // Just a chord line
                Rectangle2D chordRect = drawString(chordFont, g2, pg, document, x + extraX, y, l.getChordLine(),
                        actuallyDraw, actuallyDrawPdf, Alignment.LEFT_ALIGNED, 0, editPanelSize);
                maxWidth = Math.max(maxWidth, chordRect.getWidth() + extraX);
                height += chordRect.getHeight();
                y += chordRect.getHeight();
            } else if (l.hasLyricLine()) {
                // Just a text line
                Rectangle2D lineRect = drawString(textFont, g2, pg, document, x + extraX, y, l.getLyricLine(),
                        actuallyDraw, actuallyDrawPdf, Alignment.LEFT_ALIGNED, 0, editPanelSize);
                maxWidth = Math.max(maxWidth, lineRect.getWidth() + extraX);
                height += lineRect.getHeight();
                y += lineRect.getHeight();
            } else {
                throw new RuntimeException(
                        "Line must have either lyric or chord line! Song: [" + song.getMainTitle() + "]");
            }
        }
        // Extra line between verses
        // TODO: Switch this to a configurable pt value
        height += lyricsFont.getLineHeight();

        return new Rectangle2D.Double(upperLeftX, upperLeftY, maxWidth, height);
    }

    public Song getSong()
    {
        return song;
    }

    public int getAlign()
    {
        return align;
    }

    public void setAlign(int align)
    {
        this.align = align;
    }

    public boolean includesLastStanza()
    {
        return includesLastChunk();
    }

    @Override
    public int getNumberChunks()
    {
        return song.getStanzas().size();
    }

    @Override
    public boolean isSameContinuum(Drawable d)
    {
        if (d instanceof SongChunkDrawable) {
            SongChunkDrawable scd = (SongChunkDrawable) d;
            if (scd.getSong() == this.getSong()) {
                return true;
            }
        }
        return false;
    }

}
