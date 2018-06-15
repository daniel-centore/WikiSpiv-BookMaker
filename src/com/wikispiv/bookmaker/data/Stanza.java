package com.wikispiv.bookmaker.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Stanza implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String rawContent;
    private List<Line> lines;

    public Stanza(String rawContent)
    {
        this.rawContent = rawContent;
        this.lines = makeLines(rawContent);
    }

    private List<Line> makeLines(String rawContent)
    {
        List<Line> result = new ArrayList<>();

        String[] split = rawContent.split("\n");
        String lastChord = null;
        boolean lastWasChord = false;
        boolean lastWasIndented = false;
        for (String s : split) {
            if (s.trim().isEmpty()) {
                continue;
            }
            boolean isChord = s.startsWith(";");
            boolean lyricIsInstruction = s.startsWith("!");
            boolean isIndented = s.length() >= 2 && (s.charAt(1) == ';' || s.charAt(1) == ':');
            String line = s;
            for (String p : new String[] { "::", ":;", ";:", ";;", ":", ";", "!" }) {
                if (s.startsWith(p)) {
                    line = s.substring(p.length());
                    break;
                }
            }

            if (isChord) {
                if (lastWasChord) {
                    Line l = new Line(lastChord, null, isIndented, false);
                    result.add(l);
                }
                lastChord = line;
            } else {
                if (lastWasChord) {
                    Line l = new Line(lastChord, line, isIndented, lyricIsInstruction);
                    result.add(l);
                    lastChord = null;
                } else {
                    Line l = new Line(null, line, isIndented, lyricIsInstruction);
                    result.add(l);
                }
            }
            lastWasChord = isChord;
            lastWasIndented = isIndented;
        }
        // Cover situation where last line is a chord
        if (lastWasChord) {
            Line l = new Line(lastChord, null, lastWasIndented, false);
            result.add(l);
        }

        return result;
    }

    public String getRawContent()
    {
        return rawContent;
    }

    public List<Line> getLines()
    {
        return lines;
    }

}
