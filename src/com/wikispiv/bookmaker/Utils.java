package com.wikispiv.bookmaker;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javax.swing.JSpinner;

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import com.wikispiv.bookmaker.enums.CardinalDirection;

public class Utils
{
    public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary)
    {

        int original_width = imgSize.width;
        int original_height = imgSize.height;
        int bound_width = boundary.width;
        int bound_height = boundary.height;
        int new_width = original_width;
        int new_height = original_height;

        // scale width to fit
        new_width = bound_width;
        // scale height to maintain aspect ratio
        new_height = (new_width * original_height) / original_width;

        // then check if we need to scale even with the new height
        if (new_height > bound_height) {
            // scale height to fit instead
            new_height = bound_height;
            // scale width to maintain aspect ratio
            new_width = (new_height * original_width) / original_height;
        }

        return new Dimension(new_width, new_height);
    }

    /**
     * Note: Assumes (0,0) origin
     * @param pdr
     * @return
     */
    public static Rectangle2D rectFromPd(PDRectangle pdr)
    {
        return new Rectangle2D.Float(0, 0, pdr.getWidth(), pdr.getHeight());
    }
    
    /**
     * Note: Assume (0,0) origin
     * @param r2d
     * @return
     */
    public static PDRectangle pdFromRect(Rectangle2D r2d)
    {
        return new PDRectangle(Math.round(r2d.getWidth()), Math.round(r2d.getHeight()));
    }
    
    public static Rectangle2D growRect(Rectangle2D in, int by)
    {
        return new Rectangle2D.Double(in.getX() - by, in.getY() - by, in.getWidth() + by, in.getHeight() + by);
    }
    
    public static Rectangle2D getResizeRect(Rectangle2D in, CardinalDirection cd, double thick)
    {
        switch (cd) {
            case E:
                return new Rectangle2D.Double(in.getX() + in.getWidth() - thick / 2.0, in.getY(), thick, in.getHeight());
            case W:
                return new Rectangle2D.Double(in.getX() - thick / 2.0, in.getY(), thick, in.getHeight());
            case N:
                return new Rectangle2D.Double(in.getX(), in.getY() - thick / 2.0, in.getWidth(), thick);
            case S:
                return new Rectangle2D.Double(in.getX(), in.getY() + in.getHeight() - thick / 2.0, in.getWidth(), thick);
            default:
                throw new RuntimeException("Invalid CD: " + cd.toString());
        }
    }
    
    public static double flipY(double y)
    {
        return Main.getPrefs().getPageHeight() - y;
    }
    
    public static int iflipY(double y)
    {
        return (int) Math.round(Main.getPrefs().getPageHeight() - y);
    }
    
    public static int i(double d)
    {
        return (int) Math.round(d);
    }
    
    public static double getDoubleValue(JSpinner spin)
    {
        Object obj = spin.getValue();
        if (obj instanceof Integer) {
            return (int) obj;
        } else {
            return (double) obj;
        }
    }

    public static float stringWidth(PDFont font, int fontSize, String text) throws IOException
    {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

    public static PDRectangle inchRectangle(double widthInch, double heightInch)
    {
        return new PDRectangle(Math.round(Utils.inchToPt(widthInch)), Math.round(Utils.inchToPt(heightInch)));
    }

    public static double inchToPt(double inch)
    {
        return inch * 72;
    }

    /**
     * Preserves only characters in a bag of valid characters
     * @param text
     * @param bag
     * @param with What to replace the removed chars with
     * @return Text with any characters not in bag removed
     */
    public static String replaceNotIn(String text, String bag, String with)
    {
        String result = "";
        for (char c : text.toCharArray()) {
            if (bag.indexOf(c) >= 0) {
                result += c;
            } else {
                result += with;
            }
        }
        return result;
    }
    
    public static void moveToCursor(Component c)
    {
        Point cursorLocation = MouseInfo.getPointerInfo().getLocation();
        c.setLocation(cursorLocation);
    }
}
