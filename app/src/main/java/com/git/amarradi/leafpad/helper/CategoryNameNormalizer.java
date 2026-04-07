package com.git.amarradi.leafpad.helper;

import java.util.Locale;

public final class CategoryNameNormalizer {

    private CategoryNameNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.trim().replaceAll("\\s+", " ");
        return s.toLowerCase(Locale.ROOT);
    }
}
