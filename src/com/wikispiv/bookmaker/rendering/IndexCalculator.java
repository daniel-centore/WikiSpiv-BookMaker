package com.wikispiv.bookmaker.rendering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.SpivanykPrefs;
import com.wikispiv.bookmaker.Utils;
import com.wikispiv.bookmaker.data.Song;
import com.wikispiv.bookmaker.drawables.Drawable;
import com.wikispiv.bookmaker.drawables.SongChunkDrawable;
import com.wikispiv.bookmaker.drawables.WSPage;

public class IndexCalculator
{
    private static final IndexCalculator SINGLETON = new IndexCalculator();
    
    private IndexCalculator()
    {
        // Singleton
    }
    
    public static IndexCalculator getSingleton()
    {
        return SINGLETON;
    }
    
    public List<Entry> generateEntries()
    {
        List<Entry> result = new ArrayList<>();
        SpivanykPrefs prefs = Main.getPrefs();
        for (int i = 0; i < prefs.getPages().size(); ++i) {
            WSPage page = prefs.getPages().get(i);
            for (Drawable d : page.getDrawables()) {
                if (d instanceof SongChunkDrawable) {
                    SongChunkDrawable scd = (SongChunkDrawable) d;
                    if (scd.shouldRenderTitle()) {
                        Song s = scd.getSong();
                        result.add(new Entry(i, s.getMainTitle(), s.getMainTitle()));
                        for (String at : s.getAlternateTitles()) {
                            result.add(new Entry(i, at, s.getMainTitle()));
                        }
                    }
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    public class Entry implements Comparable<Entry>
    {
        public static final String ORDER = " -АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЮЯЬABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?";

        private int pageIndex;
        private String title;
        private String mainTitle;
        private String comparableTitle;

        public Entry(int page, String title, String mainTitle)
        {
            this.pageIndex = page;
            this.title = title;
            this.mainTitle = mainTitle;
            this.comparableTitle = Utils.replaceNotIn(title.toUpperCase(), ORDER, "");
        }

        public boolean isMainTitle()
        {
            return title.equals(mainTitle);
        }

        public boolean isAlternateTitle()
        {
            return !isMainTitle();
        }

        public int getPage()
        {
            return pageIndex;
        }

        public String getTitle()
        {
            return title;
        }

        public String getMainTitle()
        {
            return mainTitle;
        }

        @Override
        public int compareTo(Entry o)
        {
            String o1 = this.comparableTitle;
            String o2 = o.comparableTitle;
            
            int pos1 = 0;
            int pos2 = 0;
            for (int i = 0; i < Math.min(o1.length(), o2.length()) && pos1 == pos2; i++) {
                pos1 = ORDER.indexOf(o1.charAt(i));
                pos2 = ORDER.indexOf(o2.charAt(i));
            }

            if (pos1 == pos2 && o1.length() != o2.length()) {
                return o1.length() - o2.length();
            }

            return pos1 - pos2;
        }

        @Override
        public String toString()
        {
            return "Entry [pageIndex=" + pageIndex + ", title=" + title + ", mainTitle=" + mainTitle + "]";
        }
    }
}
