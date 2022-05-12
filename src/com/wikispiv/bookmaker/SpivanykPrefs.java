package com.wikispiv.bookmaker;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.wikispiv.bookmaker.data.Song;
import com.wikispiv.bookmaker.drawables.Drawable;
import com.wikispiv.bookmaker.drawables.ImageDrawable;
import com.wikispiv.bookmaker.drawables.PageNumDrawable;
import com.wikispiv.bookmaker.drawables.PreviewDrawable;
import com.wikispiv.bookmaker.drawables.SongChunkDrawable;
import com.wikispiv.bookmaker.drawables.WSPage;
import com.wikispiv.bookmaker.enums.Alignment;
import com.wikispiv.bookmaker.rendering.ImageRepresentation;
import com.wikispiv.bookmaker.rendering.WSFont;

public class SpivanykPrefs implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Rectangle2D pageSize;
    private List<Song> allSongs;
    private List<WSPage> pages;
    private int currentLeftPageIndex;
    // TODO: Deprecate these in favour of a less shitty font storage scheme?
    // Don't forget about backward compatibility w/ serialization...
    private WSFont lyricFont;
    private WSFont instructionFont;
    private WSFont chordFont;
    private WSFont titleFont;
    private WSFont creditsFont;
    private WSFont catTitleFont;
    private WSFont catSubtitleFont;
    private WSFont indexTitleFont;
    private WSFont indexFont;
    private WSFont indexAltFont;
    private WSFont pageNumFont; // Page Num
    private WSFont indexAlphabetFont;
    private WSFont indexAlphabetHighlightFont;
    private double indentSize = 6;
    private double beforeLyrics = 4;
    private PreviewDrawable previewDrawable;
    private List<String> allCategories;
    private String selectedCategory;
    private boolean showOnlyFitting;
    private int firstPageNum;
    private PageNumDrawable leftPageNum;
    private PageNumDrawable rightPageNum;
    private boolean shouldTransliterate;

    public SpivanykPrefs()
    {
        initialize();
    }

    public void initialize()
    {
        if (this.allSongs == null) {
            this.allSongs = new ArrayList<>();
        }
        if (this.pages == null) {
            this.pages = new ArrayList<>();
        }
        if (this.pageSize == null) {
            this.pageSize = Utils.rectFromPd(Main.SIZE_LETTER);
        }
    }

    public double getPageWidth()
    {
        Rectangle2D ps = getPageSize();
        return ps.getWidth();
    }

    public double getPageHeight()
    {
        Rectangle2D ps = getPageSize();
        return ps.getHeight();
    }

    public List<Song> getAllSongs()
    {
        if (allSongs == null) {
            allSongs = new ArrayList<>();
        }
        return Collections.unmodifiableList(allSongs);
    }

    public void setAllSongs(List<Song> refreshedSongs)
    {
        List<Song> oldSongs = this.allSongs;
        List<Song> resultList = new ArrayList<>();
        for (Song newVersion : refreshedSongs) {
            String title = newVersion.getMainTitle();
            // Does this song already exist in the old list?
            if (oldSongs.contains(newVersion)) {
                // Song already existed; replace it so we don't destroy our pointers elsewhere
                Main.println(title + " already existed; updating");
                Song oldVersion = oldSongs.get(oldSongs.indexOf(newVersion));
                oldVersion.replaceWith(newVersion);
                resultList.add(oldVersion);
                oldSongs.remove(oldVersion);
            } else {
                // Song did not exist; just add it
                Main.println(title + " did not exist; adding");
                resultList.add(newVersion);
            }
        }
        removeSongsEverywhere(oldSongs);
        this.allSongs = resultList;
        Main.getSh().songListNeedsUpdating();
    }

    private void removeSongsEverywhere(List<Song> oldSongs)
    {
        Main.println("== Songs Removed ==");
        for (Song s : oldSongs) {
            Main.println(" * " + s.getMainTitle());
            for (WSPage p : this.getPages()) {
                // Remove all drawables which include this song
                p.getDrawables().removeIf(d -> {
                    if (d instanceof SongChunkDrawable) {
                        SongChunkDrawable scd = (SongChunkDrawable) d;
                        if (scd.getSong() == s) {
                            Main.println("Removing song " + s.getMainTitle() + " from page " + p.calcPageIndex());
                            return true;
                        }
                    }
                    return false;
                });
            }
        }
        Main.println("===================");
    }

    public Rectangle2D getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(Rectangle2D pageSize)
    {
        this.pageSize = pageSize;
    }

    public List<WSPage> getPages()
    {
        return pages;
    }

    public void setPages(List<WSPage> pages)
    {
        this.pages = pages;
    }

    public int getCurrentLeftPageIndex()
    {
        return currentLeftPageIndex;
    }

    public WSPage getLeftPage()
    {
        int pg = getCurrentLeftPageIndex();
        if (pg >= pages.size()) {
            return null;
        } else {
            return pages.get(pg);
        }
    }

    public WSPage getRightPage()
    {
        int pg = getCurrentLeftPageIndex() + 1;
        if (pg >= pages.size()) {
            return null;
        } else {
            return pages.get(pg);
        }
    }

    public void setCurrentLeftPageIndex(int currentPage)
    {
        // Make sure current page is always a valid, left page
        if (currentPage >= pages.size()) {
            currentPage = pages.size() - 1;
        }
        if (currentPage % 2 != 0) {
            currentPage -= 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }

        this.currentLeftPageIndex = currentPage;
    }

    public double getPageMarginSpine()
    {
        return Utils.inchToPt(1.00);
    }

    public double getPageMarginSideEdge()
    {
        return Utils.inchToPt(0.75);
    }

    public double getPageMarginTop()
    {
        return Utils.inchToPt(0.625);
    }

    public double getPageMarginBottom()
    {
        return Utils.inchToPt(0.75);
    }

    public WSFont getLyricFont()
    {
        if (lyricFont == null) {
            return Main.DEFAULT_FONT;
        }
        return lyricFont;
    }

    public void setLyricFont(WSFont lyricFont)
    {
        this.lyricFont = lyricFont;
    }

    public WSFont getChordFont()
    {
        if (chordFont == null) {
            return Main.DEFAULT_FONT;
        }
        return chordFont;
    }

    public void setChordFont(WSFont chordFont)
    {
        this.chordFont = chordFont;
    }

    public WSFont getTitleFont()
    {
        if (titleFont == null) {
            return Main.DEFAULT_FONT;
        }
        return titleFont;
    }

    public void setTitleFont(WSFont titleFont)
    {
        this.titleFont = titleFont;
    }

    public WSFont getCreditsFont()
    {
        if (creditsFont == null) {
            return Main.DEFAULT_FONT;
        }
        return creditsFont;
    }

    public void setCreditsFont(WSFont creditsFont)
    {
        this.creditsFont = creditsFont;
    }

    public WSFont getCatTitleFont()
    {
        if (catTitleFont == null) {
            return Main.DEFAULT_FONT;
        }
        return catTitleFont;
    }

    public void setCatTitleFont(WSFont catTitleFont)
    {
        this.catTitleFont = catTitleFont;
    }

    public WSFont getCatSubtitleFont()
    {
        if (catSubtitleFont == null) {
            return Main.DEFAULT_FONT;
        }
        return catSubtitleFont;
    }

    public void setCatSubtitleFont(WSFont catSubtitleFont)
    {
        this.catSubtitleFont = catSubtitleFont;
    }

    public WSFont getInstructionFont()
    {
        if (instructionFont == null) {
            return Main.DEFAULT_FONT;
        }
        return instructionFont;
    }

    public void setInstructionFont(WSFont instructionFont)
    {
        this.instructionFont = instructionFont;
    }
    
    public WSFont getIndexTitleFont()
    {
        if (indexTitleFont == null) {
            return Main.DEFAULT_FONT;
        }
        return indexTitleFont;
    }

    public void setIndexTitleFont(WSFont indexTitleFont)
    {
        this.indexTitleFont = indexTitleFont;
    }
    
    public WSFont getIndexFont()
    {
        if (indexFont == null) {
            return Main.DEFAULT_FONT;
        }
        return indexFont;
    }

    public void setIndexFont(WSFont indexFont)
    {
        this.indexFont = indexFont;
    }
    
    public WSFont getIndexAltFont()
    {
        if (indexAltFont == null) {
            return Main.DEFAULT_FONT;
        }
        return indexAltFont;
    }

    public void setPageNumFont(WSFont pageNumFont)
    {
        this.pageNumFont = pageNumFont;
    }
    
    public WSFont getPageNumFont()
    {
        if (pageNumFont == null) {
            return Main.DEFAULT_FONT;
        }
        return pageNumFont;
    }

    public void setIndexAltFont(WSFont indexAltFont)
    {
        this.indexAltFont = indexAltFont;
    }

    public double getIndentSize()
    {
        return indentSize;
    }

    public void setIndentSize(double indentSize)
    {
        this.indentSize = indentSize;
    }

    public double getBeforeLyrics()
    {
        return beforeLyrics;
    }

    public void setBeforeLyrics(double beforeLyrics)
    {
        this.beforeLyrics = beforeLyrics;
    }

    public Song getSong(String title)
    {
        if (title == null) {
            return null;
        }
        for (Song s : allSongs) {
            if (s.getMainTitle().equals(title) || s.getAlternateTitles().contains(title)) {
                return s;
            }
        }
        return null;
    }

    public PreviewDrawable getPreviewDrawable()
    {
        if (previewDrawable == null) {
            previewDrawable = new PreviewDrawable();
        }
        return previewDrawable;
    }

    public Collection<Song> findUsedSongs()
    {
        HashSet<Song> result = new HashSet<>();
        for (WSPage page : getPages()) {
            for (Drawable d : page.getDrawables()) {
                if (d instanceof SongChunkDrawable) {
                    SongChunkDrawable scd = (SongChunkDrawable) d;
                    result.add(scd.getSong());
                }
            }
        }
        return result;
    }
    
    public Collection<ImageRepresentation> findUsedImages()
    {
        HashSet<ImageRepresentation> result = new HashSet<>();
        for (WSPage page : getPages()) {
            for (Drawable d : page.getDrawables()) {
                if (d instanceof ImageDrawable) {
                    ImageDrawable id = (ImageDrawable) d;
                    result.add(id.getImageRep());
                }
            }
        }
        return result;
    }

    public List<Song> findIncompleteSongs()
    {
        HashMap<Song, Boolean> complete = new HashMap<>();

        for (WSPage page : getPages()) {
            for (Drawable d : page.getDrawables()) {
                Drawable actualD = d;
                if (d instanceof PreviewDrawable) {
                    actualD = ((PreviewDrawable) d).getPreview();
                }
                if (actualD instanceof SongChunkDrawable) {
                    SongChunkDrawable scd = (SongChunkDrawable) actualD;
                    Song s = scd.getSong();
                    if (!complete.containsKey(s)) {
                        complete.put(s, Boolean.FALSE);
                    }
                    if (scd.includesLastStanza()) {
                        complete.put(s, Boolean.TRUE);
                    }
                }
            }
        }

        return complete.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    private transient double lastFittingPreviewHeight;
    private transient double lastFittingPreviewWidth;
    private transient boolean fittingSongsChanged;
    private transient List<Song> cachedFittingSongs;

    public void fittingSongsChanged()
    {
        fittingSongsChanged = true;
    }

    public List<Song> findFittingSongs()
    {
        PreviewDrawable pd = getPreviewDrawable();
        if (!fittingSongsChanged && cachedFittingSongs != null && pd.getWidth() == lastFittingPreviewWidth
                && pd.getHeight() == lastFittingPreviewHeight) {
            return cachedFittingSongs;
        }
        List<Song> result = new ArrayList<>();
        for (Song s : getAllSongs()) {
            SongChunkDrawable scd = new SongChunkDrawable(s);
            scd.setHeight(pd.getHeight());
            scd.setWidth(pd.getWidth());
            scd.drawMe();
            if (scd.includesLastStanza()) {
                result.add(s);
            }
        }
        lastFittingPreviewHeight = pd.getHeight();
        lastFittingPreviewWidth = pd.getWidth();
        cachedFittingSongs = result;
        fittingSongsChanged = false;
        return result;
    }

    public List<String> getAllCategories()
    {
        if (allCategories == null) {
            allCategories = new ArrayList<>();
        }
        List<String> result = new ArrayList<>(allCategories);
        result.add(0, Main.ALL_CATEGORIES_STRING);
        return result;
    }

    public void setAllCategories(List<String> allCategories)
    {
        this.allCategories = allCategories;
        Collections.sort(this.allCategories);
    }

    public String getSelectedCategory()
    {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory)
    {
        this.selectedCategory = selectedCategory;
    }

    public boolean isShowOnlyFitting()
    {
        return showOnlyFitting;
    }

    public void setShowOnlyFitting(boolean showOnlyFitting)
    {
        this.showOnlyFitting = showOnlyFitting;
    }

    public int getFirstPageNum()
    {
        return firstPageNum;
    }

    public void setFirstPageNum(int firstPageNum)
    {
        this.firstPageNum = firstPageNum;
    }

    public PageNumDrawable getLeftPageNum()
    {
        if (leftPageNum == null) {
            leftPageNum = new PageNumDrawable(Alignment.LEFT_ALIGNED);
        }
        return leftPageNum;
    }

    public PageNumDrawable getRightPageNum()
    {
        if (rightPageNum == null) {
            rightPageNum = new PageNumDrawable(Alignment.RIGHT_ALIGNED);
        }
        return rightPageNum;
    }

    public boolean getShouldTransliterate()
    {
        return shouldTransliterate;
    }

    public void setShouldTransliterate(boolean shouldTransliterate)
    {
        this.shouldTransliterate = shouldTransliterate;
    }

    public WSFont getIndexAlphabetFont()
    {
        if (indexAlphabetFont == null) {
            return Main.DEFAULT_FONT;
        }
        return indexAlphabetFont;
    }

    public void setIndexAlphabetFont(WSFont indexAlphabetFont)
    {
        this.indexAlphabetFont = indexAlphabetFont;
    }

    public WSFont getIndexAlphabetHighlightFont()
    {
        if (indexAlphabetHighlightFont == null) {
            return Main.DEFAULT_FONT;
        }
        return indexAlphabetHighlightFont;
    }

    public void setIndexAlphabetHighlightFont(WSFont indexAlphabetHighlightFont)
    {
        this.indexAlphabetHighlightFont = indexAlphabetHighlightFont;
    }

}
