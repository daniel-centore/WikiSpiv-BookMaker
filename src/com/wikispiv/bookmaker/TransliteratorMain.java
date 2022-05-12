package com.wikispiv.bookmaker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.wikispiv.bookmaker.data.Transliterator;

public class TransliteratorMain
{
    public static void main(String args[]) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get("./transliterate.txt"));
        String input = new String(encoded, "UTF-8");
        
        Main.getPrefs().setShouldTransliterate(true);
        System.out.println(Transliterator.transliterate(input, false));
    }
}
