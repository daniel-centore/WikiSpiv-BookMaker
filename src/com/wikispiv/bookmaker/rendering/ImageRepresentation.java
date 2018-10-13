package com.wikispiv.bookmaker.rendering;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import com.wikispiv.bookmaker.Main;

public class ImageRepresentation implements Comparable<ImageRepresentation>, Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * @deprecated This locks us into a specific absolute path
     */
    private File file;
    
    private String folder;
    private String filename;

    public ImageRepresentation(File file)
    {
        this.defileize(file);
    }

    public File getFile()
    {
        File currentFile = Main.getSh().getCurrentFile();
        if (currentFile == null || !currentFile.exists()) {
            throw new RuntimeException("Why is the current file null??");
        }
        String s = FilenameUtils.concat(currentFile.getParent(), Main.IMG_DIRECTORY);
        s = FilenameUtils.concat(s, folder);
        s = FilenameUtils.concat(s, filename);
        return new File(s);
    }
    
    private void defileize(File file)
    {
        File currentFile = Main.getSh().getCurrentFile();
        if (currentFile == null || !currentFile.exists()) {
            throw new RuntimeException("Why is the current file null??");
        }
        String imagesPath = Paths.get(currentFile.getParent(), Main.IMG_DIRECTORY).toString();
        String trimPath = file.getAbsolutePath().substring(imagesPath.length() + 1);
        this.filename = FilenameUtils.getName(trimPath);
        this.folder = FilenameUtils.getFullPathNoEndSeparator(trimPath);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        
        // Upgrade from the deprecated format
        if (this.file != null) {
            defileize(this.file);
            this.file = null;
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", FilenameUtils.getBaseName(filename), folder);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filename == null) ? 0 : filename.hashCode());
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
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
        ImageRepresentation other = (ImageRepresentation) obj;
        if (filename == null) {
            if (other.filename != null)
                return false;
        } else if (!filename.equals(other.filename))
            return false;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }

    @Override
    public int compareTo(ImageRepresentation o)
    {
        return toString().compareTo(o.toString());
    }
}
