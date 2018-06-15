package com.wikispiv.bookmaker.data;

import java.io.Serializable;

public class Chunk implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private String chord;
    private String text;
    
    public Chunk(String chord, String text)
    {
        this.chord = chord;
        this.text = text;
    }
    
    public String getChord()
    {
        return chord;
    }
    
    public String getText()
    {
        return text;
    }
}
