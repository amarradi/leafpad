package com.git.amarradi.leafpad;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String GITHUBPATH = "https://github.com/amarradi/leafpad";
    public static final String WEBLATEPATH = "https://hosted.weblate.org/projects/leafpad/";

    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setupActivityResultLaunchers();
        setupPreferences();
    }

    private void setupPreferences() {
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            }
        }

        setupClickListener("save", v -> {
            startExportIntent();
            return true;
        });

        setupClickListener("restore", v -> {
            startImportIntent();
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
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUBPATH)));
            return true;
        });

        setupClickListener("translate", v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEBLATEPATH)));
            return true;
        });
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
        String fileName = NoteBackupHelper.BASE_NAME +" "+ NoteBackupHelper.generateTimestamp() + ".xml";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/xml");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        exportLauncher.launch(intent);
    }

    private void startImportIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/xml");
        importLauncher.launch(intent);
    }

    private void backupToUri(Uri uri) {
        new Thread(() -> {
            requireActivity().runOnUiThread(() -> showSnackbar(getString(R.string.backupIsrunning)));
            try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    NoteBackupHelper.backupNotesToStream(requireContext(), outputStream);
                    requireActivity().runOnUiThread(() -> showSnackbar(getString(R.string.backupfinish)));
                    Log.d("Backup", "Backup erfolgreich geschrieben.");
                }
            } catch (Exception e) {
                Log.e("Backup", "Fehler beim Schreiben des Backups", e);
            }
        }).start();
    }
    private void restoreFromUri(Uri uri) {

        try {
            AtomicReference<InputStream> inputStream = new AtomicReference<>(requireContext().getContentResolver().openInputStream(uri));
            if (inputStream.get() == null) {
                showToast(getString(R.string.fileOpeningError));
                return;
            }

            boolean isValid = NoteBackupHelper.isValidLeafpadBackup(inputStream.get());
            inputStream.get().close();

            if (!isValid) {
                showToast(getString(R.string.invalidFile));
                return;
            }

            DialogHelper.showRestoreConfirmation(requireContext(), () -> {
                try {
                    inputStream.set(requireContext().getContentResolver().openInputStream(uri));
                    if (inputStream.get() == null) {
                        showToast(getString(R.string.fileOpeningError));
                        return;
                    }
                    Leaf.deleteAll(requireContext());
                    List<Note> restored = NoteBackupHelper.restoreNotesFromStream(requireContext(), inputStream.get());
                    int count = restored.size();
                    String message = getResources().getQuantityString(R.plurals.notes_imported, count, count);
                    showSnackbar(message);
                    inputStream.get().close();
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast(getString(R.string.importError));
                    Log.e("Restore", "Fehler beim Wiederherstellen", e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showToast(getString(R.string.importError));
            Log.e("Restore", "Fehler beim Wiederherstellen", e);
        }
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
            if (preference != null && !(preference instanceof CheckBoxPreference)) {
                setPreferenceSummary(preference, sharedPreferences.getString(key, ""));
            }
        }
    }

    private void showToast(String text) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void showSnackbar(String text) {
        Snackbar.make(requireView(), text, Snackbar.LENGTH_SHORT).show();
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
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}
