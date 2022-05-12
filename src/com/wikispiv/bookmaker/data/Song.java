package com.wikispiv.bookmaker.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;
import com.wikispiv.bookmaker.drawables.Drawable;
import com.wikispiv.bookmaker.drawables.SongChunkDrawable;
import com.wikispiv.bookmaker.drawables.WSPage;
import com.wikispiv.bookmaker.rendering.IndexCalculator.Entry;

public final class Song implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String mainTitle;
    private List<String> alternateTitles;
    private List<Stanza> stanzas;
    private String credits;
    private List<String> categories;

    public Song(String mainTitle, List<String> alternateTitles, String content, String credits, List<String> categories)
    {
        this.mainTitle = mainTitle;
        this.alternateTitles = Collections.unmodifiableList(alternateTitles);
        this.stanzas = makeStanzas(content);
        this.credits = credits;
        this.categories = categories;
    }
    
    public void replaceWith(Song s)
    {
        this.mainTitle = s.mainTitle;
        this.alternateTitles = s.alternateTitles;
        this.stanzas = s.stanzas;
        this.credits = s.credits;
        this.categories = s.categories;
    }

    public String getMainTitle()
    {
        return Transliterator.transliterate(mainTitle, false);
    }

    public List<String> getAlternateTitles()
    {
        return alternateTitles.stream()
                .map(t -> Transliterator.transliterate(t, false))
                .collect(Collectors.toList());
    }

    public List<Stanza> getStanzas()
    {
        return stanzas;
    }

    // Please keep this function in sync with the App's JS version!!!
    private List<Stanza> makeStanzas(String content)
    {
        if (!content.endsWith("\n")) {
            content += "\n";
        }
        List<Stanza> result = new ArrayList<>();
        // The -1 makes it so we get the trailing newline
        String[] lines = content.split("\n", -1);
        String currentStanza = "";
        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().equals(":")) {
                if (!currentStanza.isEmpty()) {
                    Stanza stan = new Stanza(currentStanza);
                    result.add(stan);
                    currentStanza = "";
                }
            } else {
                currentStanza += line + "\n";
            }
        }
        return result;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mainTitle == null) ? 0 : mainTitle.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Song other = (Song) obj;
        if (mainTitle == null) {
            if (other.mainTitle != null)
                return false;
        } else if (!mainTitle.equals(other.mainTitle))
            return false;
        return true;
    }

    public String getCredits()
    {
        return Transliterator.transliterate(credits, true);
    }

    public List<String> getCategories()
    {
        return categories;
    }
    
    public int getPageIndex()
    {
        SpivanykPrefs prefs = Main.getPrefs();
        for (int i = 0; i < prefs.getPages().size(); ++i) {
            WSPage page = prefs.getPages().get(i);
            for (Drawable d : page.getDrawables()) {
                if (d instanceof SongChunkDrawable) {
                    SongChunkDrawable scd = (SongChunkDrawable) d;
                    if (scd.shouldRenderTitle()) {
                        Song s = scd.getSong();
                        if (s.equals(this)) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }
}
