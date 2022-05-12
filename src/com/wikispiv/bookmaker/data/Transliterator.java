package com.wikispiv.bookmaker.data;

import java.util.HashMap;

import com.wikispiv.bookmaker.Main;
import com.wikispiv.bookmaker.Utils;

public class Transliterator
{
    private static HashMap<TranslitArgs, String> cache = new HashMap<>();

    public static String transliterate(String s, boolean translate)
    {
        if (s == null) {
            return null;
        }
        if (!Main.getPrefs().getShouldTransliterate()) {
            return s;
        }

        TranslitArgs ta = new TranslitArgs(s, translate);
        if (cache.containsKey(ta)) {
            return cache.get(ta);
        }

        // WikiSpiv translation rules
        if (translate) {
            s = replacePreserveCase(s, "Гори-гори любовь цыганки", "Gari-gari lyubov tsyganki");
            s = s.replaceAll("(Р|р)осійська (Ц|ц)иганська (П|п)існя", "Russian Romani song");
            s = replacePreserveCase(s, "Іреней Коваль", "Ireneus Kowal");
            s = s.replaceAll("Пісня ЮМПЗ", "Song of International Plast Jamboree");
            s = s.replaceAll(", ЛЧ", "");  // Nuke Plast kurin designators from names
            s = s.replaceAll(", ЧМ", "");
            s = s.replaceAll(", ЗГ", "");
            s = s.replaceAll("Ю2", "Oldest Plast Camp");
            s = s.replaceAll("УПЮів", "Older Plast Boys");
            s = s.replaceAll("УПЮок", "Older Plast Girls");
            s = s.replaceAll("УПНів", "Younger Plast Boys");
            s = s.replaceAll("УПНок", "Younger Plast Girls");
            s = s.replaceAll("УПЮ", "Older Plastuny");
            s = s.replaceAll("УПН", "Younger Plastuny");
            s = s.replaceAll("(\\d)р(\\.|)", "$1");  // Remove "р." or "р" from the end of years 
            s = replacePreserveCase(s, "Приспів", "Chorus");
            s = replacePreserveCase(s, "Стрічка", "Verse");
            s = replacePreserveCase(s, "Стрічки", "Verses");
            s = replacePreserveCase(s, "Пластові", "Plast");
            s = replacePreserveCase(s, "Необов'язково", "Optional");
            s = replacePreserveCase(s, "Слова", "Lyrics");
            s = replacePreserveCase(s, "Мелодія", "Melody");
            s = replacePreserveCase(s, "Музика", "Music");
            s = replacePreserveCase(s, " І ", " And ");
            s = replacePreserveCase(s, "Народна", "Folk");
            s = replacePreserveCase(s, "Народні", "Folk");
            s = replacePreserveCase(s, "Невідомий", "Unknown");
            s = replacePreserveCase(s, "Обробка", "Arrangement");
            s = replacePreserveCase(s, "Переклад", "Translation");
            s = replacePreserveCase(s, "Зміст", "Index");
            s = replacePreserveCase(s, "Від:", "From:");
            s = replacePreserveCase(s, "Від ", "From ");
            s = replacePreserveCase(s, " до ", " to ");
            s = replacePreserveCase(s, "можливо", "perhaps");
            s = replacePreserveCase(s, "версія", "version");
            s = replacePreserveCase(s, "пісня з кіно", "song from the movie");
            s = replacePreserveCase(s, "обробка слів", "Lyrics arrangement");
            s = replacePreserveCase(s, "Пісня куреня", "Song from the kurin");
            s = replacePreserveCase(s, "головний", "main");
            s = replacePreserveCase(s, "варіант", "version");
            s = replacePreserveCase(s, "варіянт", "version");
            s = replacePreserveCase(s, "на основі", "based on");
            s = replacePreserveCase(s, "ч.", "no.");
            s = replacePreserveCase(s, "ім.", "named for");
            s = replacePreserveCase(s, "Англ.", "English");
            s = replacePreserveCase(s, "кавер", "cover");
            s = replacePreserveCase(s, "рефрен", "refrain");
            s = replacePreserveCase(s, "вступ", "intro");
            s = s.replaceAll("(сум|СУМ|Сум)івський", "CYM");
            s = s.replaceAll("Дмитрий Богемский", "Dmitry Bogemsky");
            s = s.replaceAll("Папа и Султан", "Papa i Sultan");
            s = s.replaceAll("(У|у)країнські (С|с)ічові (С|с)трільці", "Ukrainian Sich Riflemen");
            s = s.replaceAll("(У|у)країнської", "Ukrainian");
            s = s.replaceAll("(Н|н)імецької", "German");
            s = s.replaceAll("(Р|р)осійської", "Russian");
            s = s.replaceAll("(П|п)ольска", "Polish");
            s = s.replaceAll("(Л|л)атинської", "Latin");
            s = s.replace("о.", "Fr.");
            s = s.replace("О.", "Fr.");
        }

        // Special WikiSpiv exceptions
        s = replacePreserveCase(s, "йде", "ide");
        s = replacePreserveCase(s, "йдіть", "idit");

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
        s = replacePreserveCase(s, "ай", "ai");
        s = replacePreserveCase(s, "та й", "tai");

        // UNT Simplified (only do this if we're confident we're inside ukie text,
        // to preserve in English contractions and such)
        if (Utils.containsUkie(s)) {
            s = replacePreserveCase(s, "'", "");
        }

        // BGN/PCGN
        // s = replacePreserveCase(s, "Ь", "'");

        // Nevermind, we're going to remove these
        s = replacePreserveCase(s, "Ь", "");

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
        if (firstCap && with.length() > 0) {
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
