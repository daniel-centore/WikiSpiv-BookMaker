package com.wikispiv.bookmaker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;
import javax.swing.JOptionPane;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.wikipedia.Wiki;

import com.wikispiv.bookmaker.data.Song;
import com.wikispiv.bookmaker.drawables.AlignableDrawable;
import com.wikispiv.bookmaker.drawables.Drawable;
import com.wikispiv.bookmaker.drawables.ImageDrawable;
import com.wikispiv.bookmaker.drawables.IndexDrawable;
import com.wikispiv.bookmaker.drawables.PreviewDrawable;
import com.wikispiv.bookmaker.drawables.SongChunkDrawable;
import com.wikispiv.bookmaker.drawables.TextLineDrawable;
import com.wikispiv.bookmaker.drawables.WSPage;
import com.wikispiv.bookmaker.enums.PageInsertPosition;
import com.wikispiv.bookmaker.enums.PageSide;
import com.wikispiv.bookmaker.rendering.ImageRepresentation;
import com.wikispiv.bookmaker.rendering.WSFont;
import com.wikispiv.bookmaker.tasks.ReloadSongs;
import com.wikispiv.bookmaker.ui.BookMakerFrame;
import com.wikispiv.bookmaker.ui.FontPicker;

/**
 * TODO: Split this up. UI event handling needs its own class
 * 
 * @author Daniel Centore
 *
 */
public class Main
{
    // Hackily draw the page outline
    public static final boolean DRAW_PAGE_OUTLINE = true;

    // Maximal rectangle https://gist.github.com/mmadson/9637974
    public static final String UKRAINIAN_PANGRAM = ""
            + "Жебракують філософи при ґанку церкви в Гадячі, ще й шатро їхнє п'яне знаємо.\n\n"
            + "0123456789\n\nThe quick brown fox jumps over the lazy dog!";

    public static WSFont DEFAULT_FONT = new WSFont();

    // Units --> pt = inches * 72
    public static final PDRectangle SIZE_LETTER = Utils.inchRectangle(8.5, 11);
    public static final PDRectangle SIZE_A4 = new PDRectangle(595, 842);
    public static final PDRectangle SIZE_TRADE = Utils.inchRectangle(6, 9);
    public static final PDRectangle SIZE_A5 = new PDRectangle(420, 595);

    public static final String ALL_CATEGORIES_STRING = "== All ==";

    private static BookMakerFrame bmf;

    private volatile int reloadProgress = 0;

    public static final String IMG_DIRECTORY = "Images";
    private static SaveHandler saveHandler = new SaveHandler();

    private static Main main;

    private static ImageMonitor imageMonitor;

    // https://www.tutorialspoint.com/pdfbox/pdfbox_loading_a_document.htm

    public static void main(String args[]) throws IOException, FailedLoginException
    {
        setupSomeInputThings();

        main = new Main();

        bmf = new BookMakerFrame(main);
        Utils.moveToCursor(bmf);
        bmf.setVisible(true);

        imageMonitor = new ImageMonitor();

        somethingChanged(false);
        saveHandler.setDirty(false);
    }

    public void kapusta()
    {
        // getPrefs().getPages().get(getPrefs().getCurrentLeftPageIndex()).getDrawables()
        // .add(new ImageDrawable("Deer.jpg"));
        // somethingChanged();

        // List<IndexCalculator.Entry> generateEntries =
        // IndexCalculator.getSingleton().generateEntries();
        // for (Entry entry : generateEntries) {
        // System.out.println(entry);
        // }
    }

    public void indexBtnPushed()
    {
        getPrefs().getPages().get(getPrefs().getCurrentLeftPageIndex()).getDrawables()
                .add(new IndexDrawable());
        somethingChanged();
    }

    public void insertText(String text, String fontType)
    {
        getPrefs().getPages().get(getPrefs().getCurrentLeftPageIndex()).getDrawables()
                .add(new TextLineDrawable(text, fontType));
        somethingChanged();
    }

    public static void exportPdf()
    {
        if (saveHandler.getCurrentFile() == null) {
            Main.println("You need to save before you can export");
            return;
        }
        Main.println("Exporting...");
        SpivanykPrefs prefs = getPrefs();
        String filename = saveHandler.getCurrentFile().getAbsolutePath();
        if (filename.endsWith(".spv")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        filename = filename + ".pdf";
        try {
            PDDocument document = new PDDocument();
            for (WSPage wspage : prefs.getPages()) {
                PDRectangle pagesize = Utils.pdFromRect(prefs.getPageSize());
                PDPage pdpage = new PDPage(pagesize);
                PDPageContentStream contentStream = new PDPageContentStream(document, pdpage);
                wspage.drawMe(contentStream, document, true, true);
                contentStream.close();
                document.addPage(pdpage);
            }
            document.save(new File(filename));
            document.close();
        } catch (IOException e) {
            Main.println("Something went wrong exporting");
            e.printStackTrace();
        }
        Main.println("Successfully exported to: " + filename);
    }

    /**
     * We do this to make sure all the pages are rendered in order once Otherwise
     * the preview can get fucked up because the start stanza can get out of sync
     * if, e.g., the font size is changed
     */
    public static void drawAllPages()
    {
        // TODO: Maybe only do this constantly in a new thread?
        // And repaint constantly in a separate new thread too?
        for (WSPage p : Main.getPrefs().getPages()) {
            for (Drawable d : p.getDrawables()) {
                d.drawMe();
            }
        }
    }

    public static SpivanykPrefs getPrefs()
    {
        return saveHandler.getPrefs();
    }

    public static void somethingChanged()
    {
        somethingChanged(true);
    }

    // shouldUndoStack: If we should add this version of SpivanykPrefs to the undo
    // stack (serialized?)
    public static void somethingChanged(boolean shouldUndoStack)
    {
        // TODO: Undo/redo
        saveHandler.somethingChanged(shouldUndoStack);
    }

    public void firstPageNumChanged(int firstPageNum)
    {
        getPrefs().setFirstPageNum(firstPageNum);
        somethingChanged();
    }

    public void titleAlignmentChanged(int align)
    {
        Drawable selected = getBmf().getEditPanel().getSelectedDrawable();
        if (selected instanceof AlignableDrawable) {
            ((AlignableDrawable) selected).setAlign(align);
        }
        somethingChanged();
    }

    public void previewChangedSong(String songTitle)
    {
        Song s = getPrefs().getSong(songTitle);
        if (s != null) {
            SongChunkDrawable songChunkDrawable = new SongChunkDrawable(s);
            getPrefs().getPreviewDrawable().setPreview(songChunkDrawable);
            Main.somethingChanged(false);
        }
    }

    public void imageListValueChanged(String imageTitle)
    {
        Optional<ImageRepresentation> imageRep = imageMonitor.getLatestImagesList().stream()
                .filter(s -> s.toString().equals(imageTitle))
                .findFirst();
        if (imageRep.isPresent()) {
            ImageDrawable id = new ImageDrawable(imageRep.get());
            getPrefs().getPreviewDrawable().setPreview(id);
            Main.somethingChanged(false);
        }
    }

    public void sendToBack()
    {
        Drawable selectedDrawable = getBmf().getEditPanel().getSelectedDrawable();
        if (selectedDrawable != null) {
            WSPage page = getBmf().getEditPanel().getSelectedEvent().getPage();
            page.getDrawables().remove(selectedDrawable);
            page.getDrawables().add(0, selectedDrawable);
        }
        somethingChanged();
    }

    public void onlyFittingChanged(boolean onlyFitting)
    {
        getPrefs().setShowOnlyFitting(onlyFitting);
        Main.somethingChanged();
    }

    public void categoryFilterChanged(String filterCat)
    {
        SpivanykPrefs prefs = getPrefs();
        if (!(filterCat == prefs.getSelectedCategory() || filterCat.equals(prefs.getSelectedCategory()))) {
            if (filterCat.equals(ALL_CATEGORIES_STRING)) {
                prefs.setSelectedCategory(null);
            } else {
                prefs.setSelectedCategory(filterCat);
            }
            getSh().songListNeedsUpdating();
            somethingChanged(false);
        }
    }

    public static void isClosing()
    {
        if (getSh().isDirty()) {
            int dialogResult = JOptionPane.showConfirmDialog(null, "Save your changes?", "Save",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
                if (!saveHandler.save(false)) {
                    return;
                }
            } else if (dialogResult == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        System.exit(0);
    }

    public static void println(String s)
    {
        bmf.print(s + "\n");
    }

    public static void print(String s)
    {
        bmf.print(s);
    }

    public static BookMakerFrame getBmf()
    {
        return bmf;
    }

    public static SaveHandler getSh()
    {
        return saveHandler;
    }

    public WSFont getFontByName(String fontType)
    {
        WSFont font = null;
        switch (fontType)
        {
            case "Instructions":
                font = getPrefs().getInstructionFont();
                break;
            case "Lyrics":
                font = getPrefs().getLyricFont();
                break;
            case "Chords":
                font = getPrefs().getChordFont();
                break;
            case "Title":
                font = getPrefs().getTitleFont();
                break;
            case "Credits":
                font = getPrefs().getCreditsFont();
                break;
            case "Cat Title":
                font = getPrefs().getCatTitleFont();
                break;
            case "Cat Subtitle":
                font = getPrefs().getCatSubtitleFont();
                break;
            case "Index Title":
                font = getPrefs().getIndexTitleFont();
                break;
            case "Index":
                font = getPrefs().getIndexFont();
                break;
            case "Index Alt":
                font = getPrefs().getIndexAltFont();
                break;
            case "Page Num":
                font = getPrefs().getPageNumFont();
                break;
            default:
                throw new RuntimeException("Invalid font type: " + fontType);
        }
        return font;
    }

    public void setFontByName(String fontType, WSFont font)
    {
        switch (fontType)
        {
            case "Instructions":
                getPrefs().setInstructionFont(font);
                break;
            case "Lyrics":
                getPrefs().setLyricFont(font);
                break;
            case "Chords":
                getPrefs().setChordFont(font);
                break;
            case "Title":
                getPrefs().setTitleFont(font);
                break;
            case "Credits":
                getPrefs().setCreditsFont(font);
                break;
            case "Cat Title":
                getPrefs().setCatTitleFont(font);
                break;
            case "Cat Subtitle":
                getPrefs().setCatSubtitleFont(font);
                break;
            case "Index Title":
                getPrefs().setIndexTitleFont(font);
                break;
            case "Index":
                getPrefs().setIndexFont(font);
                break;
            case "Index Alt":
                getPrefs().setIndexAltFont(font);
                break;
            case "Page Num":
                getPrefs().setPageNumFont(font);
                break;
        }
    }

    public void setFont(String fontType)
    {
        WSFont font = getFontByName(fontType);

        WSFont newFont = FontPicker.showFontPicker(getBmf(), font);
        if (newFont != null) {
            font = newFont;
        }

        setFontByName(fontType, font);
        somethingChanged();
    }

    public void pageAdded(PageInsertPosition pip)
    {
        List<WSPage> pages = getPrefs().getPages();
        int currentPage = getPrefs().getCurrentLeftPageIndex();
        WSPage page = new WSPage();
        int idx = -1;
        switch (pip)
        {
            case CENTER:
                idx = currentPage + 1;
                break;
            case LEFT:
                idx = currentPage;
                break;
            case RIGHT:
                idx = currentPage + 2;
                break;
        }
        int insertedAt = idx;
        if (idx >= pages.size()) {
            pages.add(page);
            insertedAt = pages.size() - 1;
        } else {
            pages.add(idx, page);
        }
        getPrefs().setCurrentLeftPageIndex(insertedAt);
        somethingChanged();
    }

    public void pageRemoved(PageSide ps)
    {
        List<WSPage> pages = getPrefs().getPages();
        int page = getPrefs().getCurrentLeftPageIndex() + (ps == PageSide.LEFT ? 0 : 1);
        if (page < pages.size()) {
            int dialogResult = JOptionPane.showConfirmDialog(null, "Delete the " + ps.toString() + " side?",
                    "Are you sure??", JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
                pages.remove(page);
            }
        }
        somethingChanged();
    }

    public void pageSpinnerSet(int page)
    {
        SpivanykPrefs prefs = getPrefs();
        prefs.setCurrentLeftPageIndex(page);
        somethingChanged();
    }

    public void savePreviewPressed()
    {
        SpivanykPrefs prefs = getPrefs();
        PreviewDrawable previewDrawable = prefs.getPreviewDrawable();
        Drawable preview = previewDrawable.getPreview();
        if (preview == null) {
            return;
        }
        previewDrawable.setPreview(null);
        WSPage leftPage = prefs.getLeftPage();
        WSPage rightPage = prefs.getRightPage();
        if (leftPage != null && leftPage.getDrawables().contains(previewDrawable)) {
            leftPage.getDrawables().add(preview);
        } else if (rightPage != null && rightPage.getDrawables().contains(previewDrawable)) {
            rightPage.getDrawables().add(preview);
        }
        Main.somethingChanged();
    }

    public void reloadSongs(ReloadSongs reloadSongs) throws IOException
    {
        Main.println("Reloading songs...");
        reloadProgress = 0;
        reloadSongs.setMyProgress(0);
        Wiki wiki = new Wiki("www.wikispiv.com", "");
        Main.println("Fetching list of songs...");
        List<String> allSongNames = Arrays.asList(wiki.getCategoryMembers("Category:Пісні"));
        // List<String> allSongNames = new ArrayList<>();
        // allSongNames.add("А-а-а, котки два");
        // Category:Зимові_пісні
        // Category:Пісні
        List<Song> allSongs;
        HashSet<String> allCategories = new HashSet<>();
        allSongs = allSongNames.parallelStream().map(songName -> {
            Main.println("Fetching " + songName + "...");
            try {
                List<String> categories = Arrays.asList(wiki.getCategories(songName));
                categories = categories.stream().map(s -> s.replaceFirst("Category:", "")).collect(Collectors.toList());
                allCategories.addAll(categories);
                String rawContent = wiki.getSectionText(songName, 0);
                String credits = between(rawContent, "credits");
                String spiv = trimBlankLinesStartAndTail(between(rawContent, "spiv"));
                String[] redirects = wiki.whatLinksHere(songName, true);
                String creditsPlainText = null;
                if (credits != null) {
                    String creditsHtml = wiki.parse("<credits>" + credits + "</credits>");
                    Document creditsParsed = Jsoup.parse(creditsHtml);
                    creditsPlainText = trimBlankLinesStartAndTail(creditsParsed.wholeText());
                }
                Song song = new Song(songName, Arrays.asList(redirects), spiv, creditsPlainText, categories);
                reloadProgress++;
                int percent = reloadProgress * 100 / allSongNames.size();
                reloadSongs.setMyProgress(percent);
                return song;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
        reloadSongs.setMyProgress(100);
        Main.println("Done fetching songs! :)");
        getPrefs().setAllSongs(allSongs);
        getPrefs().setAllCategories(new ArrayList<>(allCategories));
        getPrefs().fittingSongsChanged();
        somethingChanged();
    }

    public static String trimBlankLinesStartAndTail(String s)
    {
        if (s.trim().isEmpty()) {
            return s;
        }
        String[] split = s.split("\n");
        int start = split.length - 1;
        for (int i = 0; i < split.length; ++i) {
            if (!split[i].trim().isEmpty()) {
                start = i;
                break;
            }
        }
        int end = 0;
        for (int i = split.length - 1; i >= 0; --i) {
            if (!split[i].trim().isEmpty()) {
                end = i;
                break;
            }
        }
        String result = "";
        for (int i = start; i <= end; ++i) {
            result += split[i] + "\n";
        }
        return result.substring(0, result.length() - 1);
    }

    public static String between(String text, String tag)
    {
        String[] splitA = text.split("<\\s*" + tag + "\\s*>");
        if (splitA.length < 2) {
            return null;
        }
        String[] splitB = splitA[1].split("<\\s*/\\s*" + tag + "\\s*>");
        return splitB[0];
    }

    /**
     * Handles Cmd+Q
     */
    private static void setupSomeInputThings()
    {
        // // Handles CMD+Q in Java 9
        // if (Desktop.isDesktopSupported()) {
        // Desktop desktop = Desktop.getDesktop();
        // desktop.setQuitHandler(new QuitHandler()
        // {
        // @Override
        // public void handleQuitRequestWith(QuitEvent evt, QuitResponse res)
        // {
        // Main.isClosing();
        // res.cancelQuit();
        // }
        // });
        // }
    }

    public static Main getMain()
    {
        return main;
    }

    public static ImageMonitor getImageMonitor()
    {
        return imageMonitor;
    }

}
