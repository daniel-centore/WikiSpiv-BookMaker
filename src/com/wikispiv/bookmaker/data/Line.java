package com.wikispiv.bookmaker.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Line implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String chordLine;
    private String lyricLine;
    private boolean isIndented;
    private boolean lyricIsInstruction;
    private transient List<Chunk> chunks;

    public Line(String chordLine, String lyricLine, boolean isIndented, boolean lyricIsInstruction)
    {
        if (chordLine == null && lyricLine == null) {
            throw new RuntimeException("Both chord and lyric lines cannot be null!");
        }
        this.chordLine = chordLine;
        this.lyricLine = lyricLine;
        this.isIndented = isIndented;
        this.lyricIsInstruction = lyricIsInstruction;
    }

    public List<Chunk> getChunks()
    {
        if (chunks == null) {
            if (lyricLine == null) {
                chunks = new ArrayList<>();
                chunks.add(new Chunk(chordLine, null));
            } else if (chordLine == null) {
                chunks = new ArrayList<>();
                chunks.add(new Chunk(null, lyricLine));
            } else {
                chunks = smush();
            }
        }
        return chunks;
    }

    // NOTE: This algorithm is a translation of Spiv.class.php/smush()
    private List<Chunk> smush()
    {
        ArrayList<Chunk> result = new ArrayList<>();
        
        // Dictionary from start index to the chord
        List<Integer> chordIndices = new ArrayList<>();
        HashMap<Integer, String> chords = new HashMap<>();

        // kludge to show the first chunk if no chord there
        chordIndices.add(0);
        chords.put(0, "");
        
        int i = 0;
        while (i < chordLine.length()) {
            if (!Character.isSpaceChar(chordLine.charAt(i))) {
                int startPos = i;
                String chord = "";
                while (i < chordLine.length() && !Character.isSpaceChar(chordLine.charAt(i))) {
                    chord += chordLine.charAt(i);
                    i++;
                }
                if (!chordIndices.contains(startPos)) {
                    chordIndices.add(startPos);
                }
                chords.put(startPos, chord);
            } else {
                i++;
            }
        }

        // Now go thru the text
        for (int startPos : chordIndices) {
            String chord = chords.get(startPos);
            
            String text = "";
            for (
                    i = startPos;
                    i < lyricLine.length() && (i == startPos || !chords.containsKey(i));
                    ++i
            ) {
                text += lyricLine.charAt(i);
            }
            result.add(new Chunk(chord, text));
        }
        return result;
    }

    public boolean hasChordLine()
    {
        return getChordLine() != null;
    }

    public boolean hasLyricLine()
    {
        return getLyricLine() != null;
    }

    public String getChordLine()
    {
        return chordLine;
    }

    public String getLyricLine()
    {
        return lyricLine;
    }

    public boolean isIndented()
    {
        return isIndented;
    }

    public boolean getLyricIsInstruction()
    {
        return lyricIsInstruction;
    }
}
