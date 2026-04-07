package com.git.amarradi.leafpad.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.git.amarradi.leafpad.AboutActivity;
import com.git.amarradi.leafpad.LicenseActivity;
import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.backup.LeafpadBackupManager;
import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.NotificationHelper;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String GITHUBPATH = "https://github.com/amarradi/leafpad/issues";

    public static final String WEBLATEPATH = "https://hosted.weblate.org/projects/leafpad/";

    public static final String GOOGLEPLAYPATH = "https://play.google.com/store/apps/details";

    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    private final LeafpadBackupManager backupManager = new LeafpadBackupManager();


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setupActivityResultLaunchers();
        setupPreferences();
        setupKeepScreenOnSwitch();
    }

    private void setupPreferences() {
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if (!(preference instanceof CheckBoxPreference)) {
                String value = Objects.requireNonNull(sharedPreferences).getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            }
        }

        setupClickListener("theme", preference -> {
            DialogHelper.showThemeSelectionDialog(requireContext(), () -> requireActivity().recreate());
            return true;
        });


        setupClickListener("save", v -> {
            startExportIntent();
            return true;
        });

        setupClickListener("restore", v -> {
            startImportIntent();
            return true;
        });

        setupClickListener("change", v -> {
            return true;
        });

        setupClickListener("about", v -> {
            startActivity(new Intent(getActivity(), AboutActivity.class));
            return true;
        });

        setupClickListener("rating", v -> {
            launchAppStore(requireActivity(), requireContext().getPackageName());
            return true;
        });

        setupClickListener("github", v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUBPATH.trim())));
            return true;
        });

        setupClickListener("translate", v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEBLATEPATH)));
            return true;
        });
        setupClickListener("manage_categories", pref -> {
            openManageCategories();
            return true;
        });
        setupClickListener("licenses", v -> {
            startActivity(new Intent(requireActivity(), LicenseActivity.class));
            return true;
        });
    }

    private void openManageCategories() {
        // Variante A: du hast eine SettingsActivity mit einem Fragment-Container
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_fragment_container, CategoryFragment.newInstance(CategoryFragment.MODE_MANAGE_ONLY))
                .addToBackStack("CategoryFragment")
                .commit();


        if (requireActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.AppCompatActivity activity =
                    (androidx.appcompat.app.AppCompatActivity) requireActivity();

            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.category);
            }
        }
    }


    private void setupClickListener(String key, Preference.OnPreferenceClickListener listener) {
        Preference pref = findPreference(key);
        if (pref != null) pref.setOnPreferenceClickListener(listener);
    }

    private void setupActivityResultLaunchers() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            backupToUri(uri);
                        }
                    }
                }
        );

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            restoreFromUri(uri);
                        }
                    }
                }
        );
    }

    private void startExportIntent() {
        String fileName = LeafpadBackupManager.BASE_NAME + "_" + LeafpadBackupManager.generateTimestamp() + ".zip";
        // String fileName = LegacyXmlBackupHelper.BASE_NAME +" "+ LegacyXmlBackupHelper.generateTimestamp() + ".xml";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        exportLauncher.launch(intent);
    }

    private void startImportIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("text/xml");
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream",   // manche Dateimanager liefern das für ZIP
                "text/xml"
        });
        importLauncher.launch(intent);
    }

    private void backupToUri(Uri uri) {
        NotificationHelper.showSnackbar(requireView(), getString(R.string.backupIsrunning));

        backupManager.exportBackup(requireContext(), uri, new LeafpadBackupManager.Callback() {
            @Override
            public void onSuccess(int noteCount) {
                String message = getResources().getQuantityString(
                        R.plurals.notes_exported, noteCount, noteCount
                );
                NotificationHelper.showSnackbar(requireView(), message);
                NotificationHelper.showSnackbar(requireView(), getString(R.string.backupfinish));
            }

            @Override
            public void onError(String message) {
                NotificationHelper.showSnackbar(requireView(), message);
            }
        });
    }

    private void restoreFromUri(Uri uri) {
        DialogHelper.showRestoreConfirmation(requireContext(), () -> {
            backupManager.restoreBackup(requireContext(), uri, new LeafpadBackupManager.Callback() {
                @Override
                public void onSuccess(int noteCount) {
                    String message = getResources().getQuantityString(
                            R.plurals.notes_imported, noteCount, noteCount
                    );
                    NotificationHelper.showSnackbar(requireView(), message);
                }

                @Override
                public void onError(String message) {
                    NotificationHelper.showSnackbar(requireView(), message);
                }
            });
        });
    }


    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference listPref) {
            int index = listPref.findIndexOfValue(value);
            if (index >= 0) listPref.setSummary(listPref.getEntries()[index]);
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(value);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key != null) {
            Preference preference = findPreference(key);
            if (preference != null) {
                if (preference instanceof SwitchPreferenceCompat || preference instanceof CheckBoxPreference) {
                    boolean value = sharedPreferences.getBoolean(key, false);
                } else {
                    String value = sharedPreferences.getString(key, "");
                    setPreferenceSummary(preference, value);
                }
            }
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
    }

    public static void launchAppStore(Activity activity, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLEPLAYPATH + "?id=" + packageName)));
        }
    }

    private void setupKeepScreenOnSwitch() {
        androidx.preference.SwitchPreferenceCompat keepScreenOnSwitch = findPreference("keep_screen_on");
        if (keepScreenOnSwitch != null) {
            keepScreenOnSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    DialogHelper.showKeepScreenOnWarningDialog(requireContext(), () -> {
                        keepScreenOnSwitch.setChecked(true);
                    });
                    return false;
                }
                return true;
            });
        }
    }
}
