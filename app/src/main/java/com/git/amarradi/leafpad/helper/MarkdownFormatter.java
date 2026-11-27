package com.git.amarradi.leafpad.helper;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Einfache Textformatierung im Markdown-Stil. Gespeichert wird immer reiner Text:
 *
 * <pre>
 *   **fett**    *kursiv*    ++unterstrichen++    "- " am Zeilenanfang = Aufzählung
 *   "# ", "## ", "### " am Zeilenanfang = Überschrift H1, H2, H3
 * </pre>
 *
 * Im Editor werden die Formatierungen live per Spans angezeigt, die Zeichen bleiben
 * (gedimmt) sichtbar. Für Vorschauen liefert {@link #render(CharSequence)} formatierten
 * Text ohne Zeichen.
 */
public final class MarkdownFormatter {

    public static final String BOLD = "**";
    public static final String ITALIC = "*";
    public static final String UNDERLINE = "++";
    public static final String BULLET = "- ";

    private static final String BULLET_DISPLAY = "• ";

    // Inhalt: darf über Zeilen gehen, aber nicht über eine Leerzeile (= neuer Absatz),
    // damit ein vergessenes Zeichen nicht den ganzen Rest der Notiz formatiert.
    private static final String CONTENT = "((?:(?!\\n[ \\t]*\\n)[\\s\\S])+?)";

    // Inhalt darf nicht mit Leerraum beginnen/enden
    private static final Pattern BOLD_PATTERN =
            Pattern.compile("\\*\\*(?=\\S)" + CONTENT + "(?<=\\S)\\*\\*");
    private static final Pattern UNDERLINE_PATTERN =
            Pattern.compile("\\+\\+(?=\\S)" + CONTENT + "(?<=\\S)\\+\\+");
    // Einzelnes * – nicht Teil von ** und nicht mitten in einem Wort
    private static final Pattern ITALIC_PATTERN =
            Pattern.compile("(?<![*\\w])\\*(?=[^\\s*])" + CONTENT + "(?<=[^\\s*])\\*(?![*\\w])");
    private static final Pattern BULLET_PATTERN =
            Pattern.compile("^- ", Pattern.MULTILINE);
    // Gruppe 1: die #-Zeichen samt Leerzeichen, Gruppe 2: der Überschriftentext
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("^(#{1,3} )(.*)$", Pattern.MULTILINE);

    /** Schriftgröße relativ zum Fließtext für H1, H2, H3. */
    private static final float[] HEADING_SIZES = {1.6f, 1.35f, 1.15f};

    private MarkdownFormatter() {
    }

    // ------------------------------------------------------------------
    // Spans (eigene Klassen, damit nur unsere Spans entfernt werden)
    // ------------------------------------------------------------------

    private static final class MdStyleSpan extends StyleSpan {
        MdStyleSpan(int style) {
            super(style);
        }
    }

    private static final class MdUnderlineSpan extends UnderlineSpan {
    }

    private static final class MdSizeSpan extends RelativeSizeSpan {
        MdSizeSpan(float proportion) {
            super(proportion);
        }
    }

    private static final class MdMarkerSpan extends ForegroundColorSpan {
        MdMarkerSpan(int color) {
            super(color);
        }
    }

    /** Zeichnet "• " anstelle von "- " (der gespeicherte Text bleibt "- "). */
    private static final class MdBulletSpan extends ReplacementSpan {
        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                           @Nullable Paint.FontMetricsInt fm) {
            if (fm != null) {
                Paint.FontMetricsInt m = paint.getFontMetricsInt();
                fm.ascent = m.ascent;
                fm.descent = m.descent;
                fm.top = m.top;
                fm.bottom = m.bottom;
            }
            return Math.round(paint.measureText(BULLET_DISPLAY));
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                         float x, int top, int y, int bottom, @NonNull Paint paint) {
            canvas.drawText(BULLET_DISPLAY, x, y, paint);
        }
    }

    // ------------------------------------------------------------------
    // Editor
    // ------------------------------------------------------------------

    /**
     * Hängt die Live-Formatierung an ein Textfeld: Spans nach jeder Änderung neu setzen,
     * Aufzählungen bei Enter automatisch fortsetzen bzw. beenden.
     *
     * @param markerColor Farbe für die Formatierungszeichen (gedimmt)
     */
    public static void attach(@NonNull EditText editText, int markerColor) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean busy = false;
            private int newlineAt = -1;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Genau ein Zeilenumbruch getippt?
                newlineAt = (!busy && before == 0 && count == 1 && s.charAt(start) == '\n')
                        ? start : -1;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (busy) return;
                busy = true;
                try {
                    if (newlineAt >= 0) {
                        continueBullet(editText, s, newlineAt);
                        newlineAt = -1;
                    }
                    applyEditorSpans(s, markerColor);
                } finally {
                    busy = false;
                }
            }
        });
        applyEditorSpans(editText.getText(), markerColor);
    }

    /** Enter in einer Aufzählung: nächsten Punkt anlegen; Enter auf leerem Punkt: Liste beenden. */
    private static void continueBullet(EditText editText, Editable s, int newlinePos) {
        int lineStart = lineStart(s, newlinePos);
        String line = s.subSequence(lineStart, newlinePos).toString();
        if (!line.startsWith(BULLET)) return;

        if (line.equals(BULLET)) {
            // leerer Punkt: Marker und den gerade getippten Umbruch entfernen
            s.delete(lineStart, newlinePos + 1);
            editText.setSelection(lineStart);
        } else {
            s.insert(newlinePos + 1, BULLET);
            editText.setSelection(newlinePos + 1 + BULLET.length());
        }
    }

    /** Entfernt unsere Spans und setzt sie passend zum aktuellen Text neu. */
    public static void applyEditorSpans(@Nullable Editable text, int markerColor) {
        if (text == null) return;
        for (Class<?> type : new Class<?>[]{MdStyleSpan.class, MdUnderlineSpan.class,
                MdMarkerSpan.class, MdBulletSpan.class, MdSizeSpan.class}) {
            for (Object span : text.getSpans(0, text.length(), type)) {
                text.removeSpan(span);
            }
        }

        String plain = text.toString();
        markInline(text, plain, BOLD_PATTERN, BOLD.length(), markerColor,
                () -> new MdStyleSpan(Typeface.BOLD));
        markInline(text, plain, ITALIC_PATTERN, ITALIC.length(), markerColor,
                () -> new MdStyleSpan(Typeface.ITALIC));
        markInline(text, plain, UNDERLINE_PATTERN, UNDERLINE.length(), markerColor,
                MdUnderlineSpan::new);

        Matcher m = BULLET_PATTERN.matcher(plain);
        while (m.find()) {
            text.setSpan(new MdBulletSpan(), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Matcher h = HEADING_PATTERN.matcher(plain);
        while (h.find()) {
            int level = h.group(1).length() - 1; // "# " -> 1
            int lineStart = h.start();
            int lineEnd = h.end();
            // Größe auf die ganze Zeile (auch die #-Zeichen), damit sie zusammenpassen
            text.setSpan(new MdSizeSpan(HEADING_SIZES[level - 1]), lineStart, lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new MdMarkerSpan(markerColor), lineStart, h.end(1),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (h.end(2) > h.start(2)) {
                text.setSpan(new MdStyleSpan(Typeface.BOLD), h.start(2), h.end(2),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private interface SpanFactory {
        Object create();
    }

    private static void markInline(Spannable text, String plain, Pattern pattern, int markerLen,
                                   int markerColor, SpanFactory factory) {
        Matcher m = pattern.matcher(plain);
        while (m.find()) {
            int s = m.start();
            int e = m.end();
            text.setSpan(factory.create(), s + markerLen, e - markerLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new MdMarkerSpan(markerColor), s, s + markerLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new MdMarkerSpan(markerColor), e - markerLen, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    // ------------------------------------------------------------------
    // Formatierungsleiste
    // ------------------------------------------------------------------

    /**
     * Setzt die Markierung um die Auswahl bzw. entfernt sie, wenn die Auswahl schon
     * davon umschlossen ist. Ohne Auswahl wird ein leeres Paar eingefügt und der
     * Cursor in die Mitte gesetzt.
     */
    public static void toggleInline(@NonNull EditText editText, @NonNull String marker) {
        Editable text = editText.getText();
        if (text == null) return;
        int start = Math.min(editText.getSelectionStart(), editText.getSelectionEnd());
        int end = Math.max(editText.getSelectionStart(), editText.getSelectionEnd());
        if (start < 0) start = end = text.length();
        int len = marker.length();

        // Mehrzeilige Auswahl: jede Zeile einzeln formatieren (hinter einem "- ")
        if (text.subSequence(start, end).toString().indexOf('\n') >= 0) {
            toggleInlinePerLine(editText, text, start, end, marker);
            return;
        }

        // Cursor direkt vor dem schließenden Zeichen (z. B. "**neu|**"):
        // erneuter Druck verlässt die Formatierung, statt ein neues Paar einzufügen.
        if (start == end && start >= 1 && end + len <= text.length()
                && text.subSequence(end, end + len).toString().equals(marker)
                && !text.subSequence(Math.max(0, end - len), end).toString().equals(marker)
                && !(ITALIC.equals(marker) && end + 1 < text.length() && text.charAt(end + 1) == '*')) {
            editText.setSelection(end + len);
            return;
        }

        boolean wrapped = start >= len && end + len <= text.length()
                && text.subSequence(start - len, start).toString().equals(marker)
                && text.subSequence(end, end + len).toString().equals(marker);
        // "*" nicht als Teil von "**" missverstehen
        if (wrapped && ITALIC.equals(marker)) {
            boolean partOfBold = (start - 2 >= 0 && text.charAt(start - 2) == '*')
                    || (end + 1 < text.length() && text.charAt(end + 1) == '*');
            if (partOfBold) wrapped = false;
        }

        if (wrapped) {
            text.delete(end, end + len);
            text.delete(start - len, start);
            editText.setSelection(start - len, end - len);
        } else {
            text.insert(end, marker);
            text.insert(start, marker);
            editText.setSelection(start + len, end + len);
        }
    }

    /**
     * Mehrzeilige Auswahl: Haben alle (nicht leeren) Zeilen die Markierung schon,
     * wird sie entfernt, sonst bei den übrigen ergänzt. Ein führendes "- " bleibt
     * vor der Markierung, damit die Aufzählung erhalten bleibt.
     */
    private static void toggleInlinePerLine(EditText editText, Editable text, int start, int end,
                                            String marker) {
        int blockStart = lineStart(text, start);
        int blockEnd = lineEnd(text, end);
        String[] lines = text.subSequence(blockStart, blockEnd).toString().split("\n", -1);
        int len = marker.length();

        boolean allWrapped = true;
        boolean anyContent = false;
        for (String line : lines) {
            String content = contentOf(line);
            if (content.trim().isEmpty()) continue;
            anyContent = true;
            if (!isWrapped(content, marker)) {
                allWrapped = false;
                break;
            }
        }
        if (!anyContent) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String prefix = line.startsWith(BULLET) ? BULLET : "";
            String content = line.substring(prefix.length());
            if (content.trim().isEmpty()) {
                sb.append(line);
            } else if (allWrapped) {
                int a = 0, b = content.length();
                while (a < b && Character.isWhitespace(content.charAt(a))) a++;
                while (b > a && Character.isWhitespace(content.charAt(b - 1))) b--;
                sb.append(prefix).append(content, 0, a).append(content, a + len, b - len)
                        .append(content.substring(b));
            } else if (isWrapped(content, marker)) {
                sb.append(line);
            } else {
                // Leerraum am Rand bleibt außerhalb der Markierung (sonst greift das Muster nicht)
                int a = 0, b = content.length();
                while (a < b && Character.isWhitespace(content.charAt(a))) a++;
                while (b > a && Character.isWhitespace(content.charAt(b - 1))) b--;
                sb.append(prefix).append(content, 0, a).append(marker)
                        .append(content, a, b).append(marker).append(content.substring(b));
            }
            if (i < lines.length - 1) sb.append('\n');
        }
        text.replace(blockStart, blockEnd, sb.toString());
        editText.setSelection(blockStart, blockStart + sb.length());
    }

    private static String contentOf(String line) {
        return line.startsWith(BULLET) ? line.substring(BULLET.length()) : line;
    }

    private static boolean isWrapped(String content, String marker) {
        String c = content.trim();
        int len = marker.length();
        if (c.length() < 2 * len + 1 || !c.startsWith(marker) || !c.endsWith(marker)) return false;
        // "**x**" ist fett, nicht kursiv
        if (ITALIC.equals(marker) && c.startsWith(BOLD) && !c.startsWith("***")) return false;
        return true;
    }

    /**
     * Schaltet die Überschrift für die Zeilen der Auswahl weiter:
     * normal → H1 → H2 → H3 → normal. Maßgeblich ist die erste Zeile; alle Zeilen
     * bekommen dieselbe Stufe. Eine Aufzählung wird dabei durch die Überschrift ersetzt.
     */
    public static void cycleHeading(@NonNull EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;
        int selStart = Math.max(0, Math.min(editText.getSelectionStart(), editText.getSelectionEnd()));
        int selEnd = Math.max(0, Math.max(editText.getSelectionStart(), editText.getSelectionEnd()));

        int blockStart = lineStart(text, selStart);
        int blockEnd = lineEnd(text, selEnd);
        String[] lines = text.subSequence(blockStart, blockEnd).toString().split("\n", -1);

        int next = (headingLevel(lines[0]) + 1) % 4; // 0 = keine Überschrift
        String prefix = next == 0 ? "" : "###".substring(0, next) + " ";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String content = stripLinePrefix(lines[i]);
            if (content.isEmpty() && lines.length > 1) {
                sb.append(content); // Leerzeilen in einer Mehrfachauswahl nicht zu "# " machen
            } else {
                sb.append(prefix).append(content);
            }
            if (i < lines.length - 1) sb.append('\n');
        }
        text.replace(blockStart, blockEnd, sb.toString());

        if (lines.length == 1) {
            editText.setSelection(blockStart + sb.length()); // Cursor ans Zeilenende
        } else {
            editText.setSelection(blockStart, blockStart + sb.length());
        }
    }

    private static int headingLevel(String line) {
        Matcher m = HEADING_PATTERN.matcher(line);
        return m.lookingAt() ? m.group(1).length() - 1 : 0;
    }

    /** Entfernt "# "/"## "/"### " bzw. "- " am Zeilenanfang. */
    private static String stripLinePrefix(String line) {
        int level = headingLevel(line);
        if (level > 0) return line.substring(level + 1);
        if (line.startsWith(BULLET)) return line.substring(BULLET.length());
        return line;
    }

    /**
     * Entfernt alle Formatierungen (fett, kursiv, unterstrichen, Überschrift, Aufzählung)
     * in den markierten Zeilen bzw. ohne Auswahl in der aktuellen Zeile. Der Text bleibt.
     */
    public static void clearFormatting(@NonNull EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;
        int selStart = Math.max(0, Math.min(editText.getSelectionStart(), editText.getSelectionEnd()));
        int selEnd = Math.max(0, Math.max(editText.getSelectionStart(), editText.getSelectionEnd()));
        boolean hadSelection = selStart != selEnd;

        int blockStart = lineStart(text, selStart);
        int blockEnd = lineEnd(text, selEnd);
        String block = text.subSequence(blockStart, blockEnd).toString();

        // Zeilenanfang: Überschrift bzw. Aufzählung
        String[] lines = block.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(stripLinePrefix(lines[i]));
            if (i < lines.length - 1) sb.append('\n');
        }
        // Inline-Markierungen; wiederholen für verschachtelte Fälle wie ***x***
        String cleaned = sb.toString();
        String before;
        do {
            before = cleaned;
            cleaned = BOLD_PATTERN.matcher(cleaned).replaceAll("$1");
            cleaned = UNDERLINE_PATTERN.matcher(cleaned).replaceAll("$1");
            cleaned = ITALIC_PATTERN.matcher(cleaned).replaceAll("$1");
        } while (!cleaned.equals(before));

        if (cleaned.equals(block)) return;
        text.replace(blockStart, blockEnd, cleaned);
        if (hadSelection) {
            editText.setSelection(blockStart, blockStart + cleaned.length());
        } else {
            editText.setSelection(blockStart + cleaned.length());
        }
    }

    /**
     * Schaltet die Aufzählung für alle Zeilen der Auswahl um: Haben alle Zeilen schon
     * "- ", wird es entfernt, sonst bei den übrigen ergänzt.
     */
    public static void toggleBullet(@NonNull EditText editText) {
        Editable text = editText.getText();
        if (text == null) return;
        int selStart = Math.max(0, Math.min(editText.getSelectionStart(), editText.getSelectionEnd()));
        int selEnd = Math.max(0, Math.max(editText.getSelectionStart(), editText.getSelectionEnd()));

        int blockStart = lineStart(text, selStart);
        int blockEnd = lineEnd(text, selEnd);
        String[] lines = text.subSequence(blockStart, blockEnd).toString().split("\n", -1);

        boolean allBulleted = true;
        for (String line : lines) {
            if (!line.startsWith(BULLET)) {
                allBulleted = false;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        List<Integer> deltas = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (allBulleted) {
                sb.append(line.substring(BULLET.length()));
                deltas.add(-BULLET.length());
            } else if (line.startsWith(BULLET)) {
                sb.append(line);
                deltas.add(0);
            } else {
                sb.append(BULLET).append(line);
                deltas.add(BULLET.length());
            }
            if (i < lines.length - 1) sb.append('\n');
        }
        text.replace(blockStart, blockEnd, sb.toString());

        int total = 0;
        for (int d : deltas) total += d;
        int firstDelta = deltas.isEmpty() ? 0 : deltas.get(0);
        int newStart = Math.max(blockStart, selStart + firstDelta);
        int newEnd = Math.max(newStart, Math.min(text.length(), selEnd + total));
        editText.setSelection(newStart, newEnd);
    }

    // ------------------------------------------------------------------
    // Vorschau
    // ------------------------------------------------------------------

    /** Formatierter Text ohne Markdown-Zeichen, Überschriften vergrößert (Leseansicht). */
    @NonNull
    public static CharSequence render(@Nullable CharSequence source) {
        return render(source, true);
    }

    /**
     * Formatierter Text ohne Markdown-Zeichen.
     *
     * @param scaleHeadings false für kompakte Vorschauen (Notizliste, Suche):
     *                      Überschriften dann nur fett, nicht vergrößert
     */
    @NonNull
    public static CharSequence render(@Nullable CharSequence source, boolean scaleHeadings) {
        SpannableStringBuilder sb = new SpannableStringBuilder(source == null ? "" : source);

        // Überschriften: "# " entfernen, Text groß und fett
        Matcher headings = HEADING_PATTERN.matcher(sb.toString());
        List<int[]> heads = new ArrayList<>();
        while (headings.find()) {
            heads.add(new int[]{headings.start(), headings.end(1), headings.end(),
                    headings.group(1).length() - 1});
        }
        for (int i = heads.size() - 1; i >= 0; i--) {
            int[] hd = heads.get(i); // start, Ende der #-Zeichen, Zeilenende, Stufe
            if (hd[2] > hd[1]) {
                if (scaleHeadings) {
                    sb.setSpan(new RelativeSizeSpan(HEADING_SIZES[hd[3] - 1]), hd[1], hd[2],
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                sb.setSpan(new StyleSpan(Typeface.BOLD), hd[1], hd[2], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.delete(hd[0], hd[1]);
        }

        // Aufzählungen: "- " -> "• "
        Matcher bullets = BULLET_PATTERN.matcher(sb.toString());
        List<Integer> bulletStarts = new ArrayList<>();
        while (bullets.find()) bulletStarts.add(bullets.start());
        for (int i = bulletStarts.size() - 1; i >= 0; i--) {
            int s = bulletStarts.get(i);
            sb.replace(s, s + BULLET.length(), BULLET_DISPLAY);
        }

        renderInline(sb, BOLD_PATTERN, BOLD.length(), () -> new StyleSpan(Typeface.BOLD));
        renderInline(sb, UNDERLINE_PATTERN, UNDERLINE.length(), UnderlineSpan::new);
        renderInline(sb, ITALIC_PATTERN, ITALIC.length(), () -> new StyleSpan(Typeface.ITALIC));
        return sb;
    }

    private static void renderInline(SpannableStringBuilder sb, Pattern pattern, int markerLen,
                                     SpanFactory factory) {
        Matcher m = pattern.matcher(sb.toString());
        List<int[]> ranges = new ArrayList<>();
        while (m.find()) ranges.add(new int[]{m.start(), m.end()});
        // von hinten nach vorn, damit die Positionen gültig bleiben
        for (int i = ranges.size() - 1; i >= 0; i--) {
            int s = ranges.get(i)[0];
            int e = ranges.get(i)[1];
            sb.setSpan(factory.create(), s + markerLen, e - markerLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.delete(e - markerLen, e);
            sb.delete(s, s + markerLen);
        }
    }

    // ------------------------------------------------------------------

    private static int lineStart(CharSequence text, int pos) {
        int i = Math.min(pos, text.length());
        while (i > 0 && text.charAt(i - 1) != '\n') i--;
        return i;
    }

    private static int lineEnd(CharSequence text, int pos) {
        int i = Math.max(0, pos);
        while (i < text.length() && text.charAt(i) != '\n') i++;
        return i;
    }
}
