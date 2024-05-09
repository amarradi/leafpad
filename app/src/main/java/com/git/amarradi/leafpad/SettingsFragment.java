package com.git.amarradi.leafpad;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String GITHUBPATH = "https://github.com/amarradi/leafpad";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int count = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if(!( preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences != null ? sharedPreferences.getString(preference.getKey(), "") : null;

                setPreferenceSummary(preference,value);
            }
        }

        Preference about = findPreference("about");
        assert about != null;
        Objects.requireNonNull(about).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent about_intent = new Intent(getActivity(),AboutActivity.class);
                startActivity(about_intent);
                return false;
            }
        });

        Preference license = findPreference("license");
        assert license != null;
        license.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent license_intent = new Intent(getActivity(), OssLicensesMenuActivity.class);
                startActivity(license_intent);

                return false;
            }
        });


        Preference rating = findPreference("rating");
        assert rating != null;
        Objects.requireNonNull(rating).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                launchAppStore(requireActivity(), getContext().getPackageName());

                return false;
            }
        });

        Preference github = findPreference("github");
        assert github != null;
        Objects.requireNonNull(github).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUBPATH));
                startActivity(browserIntent);
                return false;
            }
        });


    }

    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int preferenceIndex = listPreference.findIndexOfValue(value);
            if (preferenceIndex >= 0) {
                listPreference.setSummary(listPreference.getEntries()[preferenceIndex]);
            }
        } else if(preference instanceof EditTextPreference){
            preference.setSummary(value);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).
                registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).
                unregisterOnSharedPreferenceChangeListener(this);

    }

    @NonNull
    @Override
    public CreationExtras getDefaultViewModelCreationExtras() {
        return super.getDefaultViewModelCreationExtras();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {

        assert key != null;
        Preference preference = findPreference(key);
        if(null != preference) {
            if(!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(),"");
                setPreferenceSummary(preference,value);
            }
        }
    }

    public static void launchAppStore(Activity activity, String packageName) {
        Intent intent;
        try {
            //Log.d("market", "launchAppStore: market://details?id="+packageName);
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            activity.startActivity(intent);
        } catch (android.content.ActivityNotFoundException anfe) {
            //Log.d("uri", "launchAppStore: https://play.google.com/store/apps/details?id=" + packageName);
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));

        }
    }

}