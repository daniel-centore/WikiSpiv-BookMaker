package com.wikispiv.bookmaker.rendering;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.wikispiv.bookmaker.Main;

public class WSFont implements Serializable
{
    private static final long serialVersionUID = 1L;

    private static final String FONT_DIRECTORY = "Fonts";

    private String filename;
    private double fontSize;
    private double lineSpacing;
    private Color fontColor;

    private transient Font javafont;
    private transient PDDocument lastDocument;
    private transient PDFont lastPdFont;

    private transient Dimension lastEditPanelSize;
    // private transient AffineTransform lastAffineTransform;
    private transient ConcurrentHashMap<String, Font> cachedFixedFonts;

    // Minimum and maximum values of the tracking adjustments
    private static final double TRACKING_MAX = 0.3;
    private static final double TRACKING_MIN = -0.3;

    // More iterations = slower, but more accurate preview
    // 7 seems about the lowest we can go without big issues
    private static final int TRACKING_ITERATIONS = 10;

    private transient ConcurrentHashMap<String, Double> stringWidthCache;
    private transient ConcurrentHashMap<String, String> sanitizeCache;

    public WSFont(String filename, double fontSize, double lineSpacing, Color fontColor)
    {
        this.filename = filename;
        this.fontSize = fontSize;
        this.lineSpacing = lineSpacing;
        this.fontColor = fontColor;
    }

    public WSFont()
    {
        this.filename = null;
        this.fontSize = 12;
        this.lineSpacing = 1.0;
        this.fontColor = Color.BLACK;
    }

    /**
     * This is the java.awt font without any changes. Using it will have notable
     * deviations from the PDF one, especially with regard to its width. Use of
     * getFixedJavaFont(..) when possible is recommended.
     * 
     * @return
     */
    public Font getGenericJavaFont()
    {
        if (javafont != null) {
            return javafont;
        }
        File file = getFontFile();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, file);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            Main.println("ERROR: Could not find font " + filename);
            Main.println("Falling back on courier");
            javafont = getDefaultJavaFont();
            return javafont;
        }
        ge.registerFont(font);
        font = font.deriveFont((float) fontSize);

        this.javafont = font;
        return this.javafont;
    }

    private File getFontFile()
    {
        if (filename == null) {
            return new File("./Ubuntu.ttf");
        }
        File file = new File(getFontDirectory(), filename);
        if (!file.exists()) {
            return new File("./Ubuntu.ttf");
        }
        return file;
    }

    private Font getDefaultJavaFont()
    {
        return new Font("Courier", Font.PLAIN, 80).deriveFont(Font.PLAIN, (float) fontSize);
    }

    /**
     * The Java font doesn't render exactly the same as in the PDF, and in fact
     * varies in width depending on the container size. So, this takes the input
     * text and tweaks the tracking of the font, honing in on the closest it can get
     * to the canonical value
     * 
     * @param g2
     * @param document
     * @param text
     * @param editPanelSize
     * @return
     */
    public Font getFixedJavaFont(Graphics2D g2, PDDocument document, String text, Dimension editPanelSize)
    {
        // We cache the fixed font for a given graphics transformation
        // if (g2.getTransform().equals(lastAffineTransform)) {
        if (editPanelSize == null) {
            return null;
        }
        if (editPanelSize.equals(lastEditPanelSize)) {
            if (cachedFixedFonts.containsKey(text)) {
                return cachedFixedFonts.get(text);
            }
        } else {
            if (cachedFixedFonts != null) {
                cachedFixedFonts.clear();
            } else {
                cachedFixedFonts = new ConcurrentHashMap<>();
            }
        }

        // lastAffineTransform = g2.getTransform();
        lastEditPanelSize = editPanelSize;
        Font original = getGenericJavaFont();
        int correctWidth = (int) Math.round(getWidth(document, text));
        double trackingHigh = TRACKING_MAX;
        double trackingLow = TRACKING_MIN;
        Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
        Font result = null;
        for (int i = 0; i < TRACKING_ITERATIONS; ++i) {
            attributes.put(TextAttribute.TRACKING, (trackingHigh + trackingLow) / 2.0);
            result = original.deriveFont(attributes);
            int newWidth = g2.getFontMetrics(result).stringWidth(text);
            int widthDifference = correctWidth - newWidth;
            if (widthDifference > 0) {
                trackingLow = (trackingHigh + trackingLow) / 2.0;
            } else if (widthDifference < 0) {
                trackingHigh = (trackingHigh + trackingLow) / 2.0;
            } else {
                // Found perfect match
                break;
            }
        }
        cachedFixedFonts.put(text, result);
        return result;
    }

    /**
     * Gets the font for use in the PDF
     * 
     * @param document
     * @return
     */
    public PDFont getPdfFont(PDDocument document)
    {
        if (document == lastDocument) {
            return lastPdFont;
        }
        File file = getFontFile();
        PDType0Font font;
        try {
            font = PDType0Font.load(document, file);
        } catch (IOException e) {
            e.printStackTrace();
            Main.println("ERROR: Could not find font " + filename);
            Main.println("Falling back on courier");
            lastPdFont = PDType1Font.COURIER;
            return lastPdFont;
        }
        this.lastPdFont = font;
        this.lastDocument = document;
        return font;
    }

    public int getJavaWidth(Graphics2D g2, String text)
    {
        return g2.getFontMetrics(getGenericJavaFont()).stringWidth(text);
    }

    public double getWidth(PDDocument document, String text)
    {
        if (stringWidthCache == null) {
            stringWidthCache = new ConcurrentHashMap<>();
        } else if (stringWidthCache.containsKey(text)) {
            return stringWidthCache.get(text);
        }
        try {
            text = sanitize(document, text);
            double result = getPdfFont(document).getStringWidth(text) / 1000 * getFontSize();
            stringWidthCache.put(text, result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            Main.println("Problem with text: " + text);
            throw new RuntimeException(e);
        }
    }

    public String getFilename()
    {
        return filename;
    }

    public double getFontSize()
    {
        return fontSize;
    }

    public static File getFontDirectory()
    {
        File currentFile = Main.getSh().getCurrentFile();
        if (currentFile == null || !currentFile.exists()) {
            return null;
        }
        return new File(currentFile.getParentFile(), FONT_DIRECTORY);
    }

    /**
     * What we multiply the font size by to get the total line height
     * 
     * @return
     */
    public double getLineSpacing()
    {
        return lineSpacing;
    }

    public double getLineHeight()
    {
        return lineSpacing * fontSize;
    }

    public Color getFontColor()
    {
        if (fontColor == null) {
            fontColor = Color.BLACK;
        }
        return fontColor;
    }

    public String sanitize(PDDocument document, String s) throws IOException
    {
        if (sanitizeCache == null) {
            sanitizeCache = new ConcurrentHashMap<>();
        } else if (sanitizeCache.containsKey(s)) {
            return sanitizeCache.get(s);
        }
        String result = "";
        for (char c : s.toCharArray()) {
            switch (c)
            {
                case '’':
                case '‘':
                    c = '\'';
                    break;
                case '“':
                case '”':
                    c = '"';
                    break;
                case '♯':
                    c = '#';
                    break;
                case '♭':
                    c = 'b';
                    break;
                case '–':
                case '—':
                    c = '-';
                    break;
            }

            if (hasGlyph(document, c)) {
                result += c;
            } else if (Character.isWhitespace(c)) {
                result += " ";
            } else {
                result += "???????????????????????????????";
            }
        }
        sanitizeCache.put(s, result);
        return result;
    }

    public boolean hasGlyph(PDDocument document, char c) throws IOException
    {
        PDFont pdfFont = getPdfFont(document);
        if (pdfFont instanceof PDType0Font) {
            PDType0Font font0 = (PDType0Font) pdfFont;
            return font0.hasGlyph(c);
        } else if (pdfFont instanceof PDType1Font) {
            // PDType1Font font1 = (PDType1Font) pdfFont;
            // TODO Unfuck this?
            return true;
        }
        throw new RuntimeException("Unknown font type! " + pdfFont);
    }
}
