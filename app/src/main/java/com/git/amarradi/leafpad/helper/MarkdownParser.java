package com.git.amarradi.leafpad.helper;

public class MarkdownParser {

    public static String parse(String rawText) {
        StringBuilder result = new StringBuilder();
        String[] lines = rawText.split("\n");

        for (String line : lines) {
            String parsedLine = line;

            // Heading: "# Heading"
            if (parsedLine.startsWith("# ")) {
                parsedLine = "<h1>" + parsedLine.substring(2).trim() + "</h1>";
            }

            // Bullet point: "* bullet"
            else if (parsedLine.startsWith("* ")) {
                parsedLine = context.getString(R.string.bullet_point_symbol) + parsedLine.substring(2).trim();
            }

            // Underline: "__underlined__"
            parsedLine = parsedLine.replaceAll("__(.*?)__", "<u>$1</u>");

            // Link: [text](url)
            parsedLine = parsedLine.replaceAll("\\[(.+?)\\]\\((http.*?)\\)", "<a href=\"$2\">$1</a>");

            result.append(parsedLine).append("<br>");
        }

        return result.toString();
    }
}
