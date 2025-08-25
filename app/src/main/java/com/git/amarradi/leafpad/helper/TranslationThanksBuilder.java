package com.git.amarradi.leafpad.helper;

import static android.graphics.Typeface.BOLD;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.git.amarradi.leafpad.R;

public class TranslationThanksBuilder {

    public static CharSequence build(Context context) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        appendBoldCountry(builder, context.getString(R.string.country_title_albanian));
        builder.append(context.getString(R.string.country_title_translator_albanian)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_arabic));
        builder.append(context.getString(R.string.country_title_translator_arabic)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_catalan));
        builder.append(context.getString(R.string.country_title_translator_catalan)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_chinese_simplified_han_script));
        builder.append(context.getString(R.string.country_title_translator_chinese_simplified_han_script)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_chinese_traditional_han_script));
        builder.append(context.getString(R.string.country_title_translator_chinese_simplified_han_script)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_czech));
        builder.append(context.getString(R.string.country_title_translator_czech)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_english));
        builder.append(context.getString(R.string.country_title_translator_english)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_estonian));
        builder.append(context.getString(R.string.country_title_translator_estonian)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_french));
        builder.append(context.getString(R.string.country_title_translator_french)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_german));
        builder.append(context.getString(R.string.country_title_translator_german)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_greek));
        builder.append(context.getString(R.string.country_title_translator_greek)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_hindi));
        builder.append(context.getString(R.string.country_title_translator_hindi)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_hindi_latin_script));
        builder.append(context.getString(R.string.country_title_translator_hindi_latin_script)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_hindi));
        builder.append(context.getString(R.string.country_title_translator_hindi)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_indonesian));
        builder.append(context.getString(R.string.country_title_translator_indonesian)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_italian));
        builder.append(context.getString(R.string.country_title_translator_italian)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_portuguese));
        builder.append(context.getString(R.string.country_title_translator_portuguese)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_russian));
        builder.append(context.getString(R.string.country_title_translator_russian)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_spanish));
        builder.append(context.getString(R.string.country_title_translator_spanish)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_tamil));
        builder.append(context.getString(R.string.country_title_translator_tamil)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_ukrainian));
        builder.append(context.getString(R.string.country_title_translator_ukrainian)).append("\n");

        appendBoldCountry(builder, context.getString(R.string.country_title_uzbek));
        builder.append(context.getString(R.string.country_title_translator_uzbek));
        return builder;
    }

    private static void appendBoldCountry(SpannableStringBuilder builder, String country) {
        int start = builder.length();
        builder.append(country);
        int end = builder.length();
        builder.setSpan(new StyleSpan(BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(": ");
    }



}
