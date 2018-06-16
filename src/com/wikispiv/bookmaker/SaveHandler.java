package com.wikispiv.bookmaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.wikispiv.bookmaker.data.Song;
import com.wikispiv.bookmaker.drawables.Drawable;
import com.wikispiv.bookmaker.rendering.ImageRepresentation;
import com.wikispiv.bookmaker.ui.BookMakerFrame;

public class SaveHandler
{
    private SpivanykPrefs prefs;
    private File currentFile;
    private boolean dirty = false;
    private boolean songListNeedsUpdating = false;

    public SaveHandler()
    {
        this.prefs = new SpivanykPrefs();
    }

    public SpivanykPrefs getPrefs()
    {
        return this.prefs;
    }

    public void songListNeedsUpdating()
    {
        this.songListNeedsUpdating = true;
    }

    public boolean save(boolean as)
    {
        if (!as && currentFile != null) {
            return saveTo(currentFile);
        }
        JFileChooser fileChooser = getMahFileChooserPlz();
        if (fileChooser.showSaveDialog(Main.getBmf()) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".spv")) {
                file = new File(file.toString() + ".spv");
            }
            return saveTo(file);
        }
        return false;
    }

    private boolean saveTo(File file)
    {
        Main.println("Saving...");
        try {
            // Save backup
            File backupFile = new File(file.getAbsolutePath() + "-backup.spv");
            File extantFile = new File(file.getAbsolutePath());
            if (extantFile.exists()) {
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                extantFile.renameTo(backupFile);
            }

            // Actually save
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this.prefs);
            out.close();
            fileOut.close();
            setCurrentFile(file);
            Main.println("Done saving!");
            dirty = false;
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(Main.getBmf(), e.getLocalizedMessage());
            e.printStackTrace();
        }
        return false;
    }

    public void open()
    {
        JFileChooser fileChooser = getMahFileChooserPlz();

        if (fileChooser.showOpenDialog(Main.getBmf()) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                this.prefs = (SpivanykPrefs) in.readObject();
                this.prefs.initialize();
                in.close();
                fileIn.close();
                setCurrentFile(file);
                songListNeedsUpdating();
                somethingChanged(false);
                Main.getImageMonitor().monitor();
            } catch (IOException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(Main.getBmf(), e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private JFileChooser getMahFileChooserPlz()
    {
        JFileChooser fileChooser = new JFileChooser();
        try {
            Action details = fileChooser.getActionMap().get("viewTypeDetails");
            details.actionPerformed(null);
        } catch (Exception e) {
            // This happens if Nimbus isn't used
        }
        if (currentFile != null) {
            fileChooser.setSelectedFile(currentFile);
        }
        fileChooser.setFileFilter(new FileFilter()
        {
            public String getDescription()
            {
                return "Spivanyk Files (*.spv)";
            }

            public boolean accept(File f)
            {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String filename = f.getName().toLowerCase();
                    return filename.endsWith(".spv");
                }
            }
        });
        return fileChooser;
    }

    /**
     * Call this if something changed and we need to refresh the gui
     */
    public void somethingChanged(boolean shouldUndoStack)
    {
        if (shouldUndoStack) {
            dirty = true;
        }
        BookMakerFrame bmf = Main.getBmf();

        Main.drawAllPages();

        String selectedCategory = prefs.getSelectedCategory();

        // Images
        JList<String> imageList = bmf.getImageList();
        DefaultListModel<String> imageModel = new DefaultListModel<String>();
        List<ImageRepresentation> images = new ArrayList<>(Main.getImageMonitor().getLatestImagesList());
        // images.removeAll(prefs.findUsedImages()); // TODO: Optional
        images.stream().forEach(s -> imageModel.addElement(s.toString()));
        imageList.setModel(imageModel);

        // Source songs
        JList<String> sourceSongList = bmf.getSourceSongList();
        DefaultListModel<String> sourceSongModel = new DefaultListModel<String>();
        List<Song> songs = new ArrayList<>(prefs.getAllSongs());
        songs.removeAll(prefs.findUsedSongs()); // TODO: Optional
        if (prefs.isShowOnlyFitting()) {
            songs.retainAll(prefs.findFittingSongs());
        }
        for (Song s : songs) {
            if (selectedCategory == null || s.getCategories().contains(selectedCategory)) {
                sourceSongModel.addElement(s.getMainTitle());
            }
        }
        sourceSongList.setModel(sourceSongModel);

        // Incomplete songs
        JList<String> incompleteSongList = bmf.getIncompleteSongList();
        DefaultListModel<String> islModel = new DefaultListModel<String>();
        for (Song s : prefs.findIncompleteSongs()) {
            islModel.addElement(s.getMainTitle());
        }
        incompleteSongList.setModel(islModel);

        // Filtered categories
        JComboBox<String> filterSource = bmf.getFilterSource();
        DefaultComboBoxModel<String> fsModel = new DefaultComboBoxModel<String>();
        for (String s : prefs.getAllCategories()) {
            fsModel.addElement(s);
        }
        filterSource.setModel(fsModel);
        if (prefs.getSelectedCategory() == null) {
            filterSource.setSelectedIndex(0);
        } else {
            filterSource.setSelectedItem(prefs.getSelectedCategory());
        }

        // Handle current page stuff
        if (prefs.getCurrentLeftPageIndex() >= prefs.getPages().size()) {
            prefs.setCurrentLeftPageIndex(prefs.getPages().size() - 1);
        }
        bmf.setPageSpinner(prefs.getCurrentLeftPageIndex());

        // Repaint the preview panel
        bmf.repaint();

        // Handle is selected
        Drawable selectedDrawable = bmf.getEditPanel().getSelectedDrawable();
        bmf.somethingSelected(selectedDrawable);

        // Spinners
        bmf.setIndentSizeSpinner(prefs.getIndentSize());
        bmf.setBeforeLyricsSpinner(prefs.getBeforeLyrics());
        bmf.setFirstPageNum(prefs.getFirstPageNum());

        // Other checkboxes
        bmf.setOnlyFittingCheckbox(prefs.isShowOnlyFitting());
    }

    private void setCurrentFile(File file)
    {
        this.currentFile = file;
        Main.getBmf().updateCurrentFile(file.getName());
    }

    public File getCurrentFile()
    {
        return currentFile;
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
    }
}
