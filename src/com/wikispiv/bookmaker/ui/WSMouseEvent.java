package com.wikispiv.bookmaker.ui;

import com.wikispiv.bookmaker.drawables.WSPage;

public class WSMouseEvent
{
    private final WSPage page;
    private final double x;
    private final double y;

    public WSMouseEvent(WSPage page, double pageX, double pageY)
    {
        this.page = page;
        this.x = pageX;
        this.y = pageY;
    }

    public WSPage getPage()
    {
        return page;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    @Override
    public String toString()
    {
        return "WSMouseEvent [page=" + page + ", x=" + x + ", y=" + y + "]";
    }
}
