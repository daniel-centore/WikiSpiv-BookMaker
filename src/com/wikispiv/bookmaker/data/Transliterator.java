package com.wikispiv.bookmaker.data;

import java.util.HashMap;

public class Transliterator
{
    private static HashMap<TranslitArgs, String> cache = new HashMap<>();
    
    public static String transliterate(String s, boolean translate)
    {
//        if (s!= null) {
//            return s;
//        }
        if (s == null) {
            return null;
        }
        
        TranslitArgs ta = new TranslitArgs(s, translate);
        if (cache.containsKey(ta)) {
            return cache.get(ta);
        }
        
        // WikiSpiv translation rules
        if (translate) {
            s = replacePreserveCase(s, "Приспів", "Chorus");
            s = replacePreserveCase(s, "Вірш", "Verse");
            s = replacePreserveCase(s, "Стрічка", "Verse");
            s = replacePreserveCase(s, "Слова", "Lyrics");
            s = replacePreserveCase(s, "Мелодія", "Melody");
            s = replacePreserveCase(s, "Музика", "Music");
            s = replacePreserveCase(s, " І ", " And ");
            s = replacePreserveCase(s, "Народна", "Folk");
            s = replacePreserveCase(s, "Народні", "Folk");
            s = replacePreserveCase(s, "Невідомий", "Unknown");
            s = replacePreserveCase(s, "Обробка", "Arrangement");
        }
        
        // Special WikiSpiv exceptions
        s = replacePreserveCase(s, "йде", "ide");
        
        
        // This is mostly BGN/PCGN but combined with some of
        // the rules from Ukrainian National transliteration
        // and a few additions of my own

        // UNT Simplified
        s = replacePreserveCase(s, "ЖЖ", "ZH");
        s = replacePreserveCase(s, "XX", "КH");
        s = replacePreserveCase(s, "ЦЦ", "TS");
        s = replacePreserveCase(s, "ЧЧ", "CH");
        s = replacePreserveCase(s, "ШШ", "SH");

        // BGN/PCGN clarifications
        s = replacePreserveCase(s, "ЗГ", "Z-H");
        s = replacePreserveCase(s, "КГ", "K-H");
        s = replacePreserveCase(s, "СГ", "S-H");
        s = replacePreserveCase(s, "ЦГ", "TS-H");

        // WikiSpiv
        s = replacePreserveCase(s, "ЬО", "YO");
        s = replacePreserveCase(s, "ИЙ", "IY");

        // UNT Simplified
        s = replacePreserveCase(s, "'", "");

        // BGN/PCGN
        s = replacePreserveCase(s, "Ь", "'");

        // BGN/PCGN
        s = replacePreserveCase(s, "А", "A");
        s = replacePreserveCase(s, "Б", "B");
        s = replacePreserveCase(s, "В", "V");
        s = replacePreserveCase(s, "Г", "H");
        s = replacePreserveCase(s, "Ґ", "G");
        s = replacePreserveCase(s, "Д", "D");
        s = replacePreserveCase(s, "Е", "E");
        s = replacePreserveCase(s, "Є", "YE");
        s = replacePreserveCase(s, "Ж", "ZH");
        s = replacePreserveCase(s, "З", "Z");
        s = replacePreserveCase(s, "И", "Y");
        s = replacePreserveCase(s, "І", "I");
        s = replacePreserveCase(s, "Ї", "YI");
        s = replacePreserveCase(s, "Й", "Y");
        s = replacePreserveCase(s, "К", "K");
        s = replacePreserveCase(s, "Л", "L");
        s = replacePreserveCase(s, "М", "M");
        s = replacePreserveCase(s, "Н", "N");
        s = replacePreserveCase(s, "О", "O");
        s = replacePreserveCase(s, "П", "P");
        s = replacePreserveCase(s, "Р", "R");
        s = replacePreserveCase(s, "С", "S");
        s = replacePreserveCase(s, "Т", "T");
        s = replacePreserveCase(s, "У", "U");
        s = replacePreserveCase(s, "Ф", "F");
        s = replacePreserveCase(s, "Х", "KH");
        s = replacePreserveCase(s, "Ц", "TS");
        s = replacePreserveCase(s, "Ч", "CH");
        s = replacePreserveCase(s, "Ш", "SH");
        s = replacePreserveCase(s, "Щ", "SHCH");
        s = replacePreserveCase(s, "Ю", "YU");
        s = replacePreserveCase(s, "Я", "YA");
        
        cache.put(ta, s);

        return s;
    }

    public static String replacePreserveCase(String haystack, String needle, String with)
    {
        while (haystack.toLowerCase().contains(needle.toLowerCase())) {
            haystack = replaceOncePreserveCase(haystack, needle, with);
        }
        return haystack;
    }

    public static String replaceOncePreserveCase(String haystack, String needle, String with)
    {
        with = with.toLowerCase();
        
        int idx = haystack.toLowerCase().indexOf(needle.toLowerCase());
        String needleInHaystack = haystack.substring(idx, idx + needle.length());
        boolean firstCap = Character.isUpperCase(needleInHaystack.charAt(0));
        if (firstCap) {
            with = Character.toUpperCase(with.charAt(0)) + with.substring(1);
        }
        return haystack.substring(0, idx) + with + haystack.substring(idx + needle.length());
    }
}

class TranslitArgs
{
    private final String s;
    private final boolean translate;
    
    public TranslitArgs(String s, boolean translate)
    {
        this.s = s;
        this.translate = translate;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((s == null) ? 0 : s.hashCode());
        result = prime * result + (translate ? 1231 : 1237);
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
        TranslitArgs other = (TranslitArgs) obj;
        if (s == null) {
            if (other.s != null)
                return false;
        } else if (!s.equals(other.s))
            return false;
        if (translate != other.translate)
            return false;
        return true;
    }
    
    
}
