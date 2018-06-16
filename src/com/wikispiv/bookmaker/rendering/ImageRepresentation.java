package com.wikispiv.bookmaker.rendering;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import com.wikispiv.bookmaker.Main;

public class ImageRepresentation implements Comparable<ImageRepresentation>, Serializable
{
    private static final long serialVersionUID = 1L;
     
    public File file;

    public ImageRepresentation(File file)
    {
        this.file = file;
    }

    @Override
    public String toString()
    {
        File currentFile = Main.getSh().getCurrentFile();
        if (currentFile == null || !currentFile.exists()) {
            throw new RuntimeException("Why is the current file null??");
        }
        String imagesPath = Paths.get(currentFile.getParent(), Main.IMG_DIRECTORY).toString();
        String trimPath = file.getAbsolutePath().substring(imagesPath.length() + 1);
        String filename = FilenameUtils.getBaseName(trimPath);
        String path = FilenameUtils.getFullPathNoEndSeparator(trimPath);
        return String.format("%s (%s)", filename, path);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
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
        if (file == null) {
            if (other.file != null)
                return false;
        } else if (!file.equals(other.file))
            return false;
        return true;
    }

    @Override
    public int compareTo(ImageRepresentation o)
    {
        return toString().compareTo(o.toString());
    }
}
