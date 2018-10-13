package com.wikispiv.bookmaker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.wikispiv.bookmaker.rendering.ImageRepresentation;

import io.methvin.watcher.DirectoryWatcher;

/**
 * Monitor the filesystem for changes to the image folder
 * 
 * @author Daniel Centore
 *
 */
public class ImageMonitor
{
    private static final int MAX_DEPTH = 50;

    // Okay we should really start/stop monitoring
    private DirectoryWatcher watcher;
    private List<ImageRepresentation> latestImagesList = new ArrayList<>();

    public void monitor()
    {
        stopMonitoring();
        File currentFile = Main.getSh().getCurrentFile();
        if (currentFile == null || !currentFile.exists()) {
            throw new RuntimeException("Why is the current file null??");
        }
        Path directoryToWatch = Paths.get(currentFile.getParent(), Main.IMG_DIRECTORY);
        try {
            // Watch the directory for changes recursively
            this.watcher = DirectoryWatcher.create(directoryToWatch, event -> {
                // In principle we could speed this up by using the event to check what actually
                // changed, but ¯\_(ツ)_/¯
                directoryChanged();
            });
            this.watcher.watchAsync();
            directoryChanged(); // Initial setup
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void directoryChanged() throws IOException
    {
        File currentFile = Main.getSh().getCurrentFile();
        if (currentFile == null || !currentFile.exists()) {
            throw new RuntimeException("Why is the current file null??");
        }
        List<ImageRepresentation> files = Files.find(Paths.get(currentFile.getParent(), Main.IMG_DIRECTORY),
                MAX_DEPTH, (p, bfa) -> bfa.isRegularFile())
                .filter(p -> isImage(p.toFile()))
                .map(p -> new ImageRepresentation(p.toFile()))
                .sorted()
                .collect(Collectors.toList());
        if (!files.equals(latestImagesList)) {
            latestImagesList = files;
            Main.somethingChanged();
        }
    }

    public static boolean isImage(File file)
    {
        try {
            return ImageIO.read(file) != null;
        } catch (Exception e) {
            Main.println(String.format("[%s]: %s", file.getName(), e.getMessage()));
            return false;
        }
    }

    public void stopMonitoring()
    {
        try {
            if (this.watcher != null) {
                this.watcher.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ImageRepresentation> getLatestImagesList()
    {
        return latestImagesList;
    }
}
