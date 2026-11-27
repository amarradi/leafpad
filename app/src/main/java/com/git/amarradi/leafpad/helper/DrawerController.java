package com.git.amarradi.leafpad.helper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.git.amarradi.leafpad.AboutActivity;
import com.git.amarradi.leafpad.BuildConfig;
import com.git.amarradi.leafpad.CategoryActivity;
import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.LicenseActivity;
import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.backup.LeafpadBackupManager;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;

/**
 * Sidebar der Hauptansicht. Enthält alle Funktionen des früheren Einstellungs-Bildschirms:
 * Theme, Backup/Restore, Kategorien, Änderungshinweis, Bildschirm-an und die Links.
 *
 * Muss in onCreate() der Activity erzeugt werden, weil hier ActivityResultLauncher
 * registriert werden.
 */
public class DrawerController {

    /** Aktionen, die die Hauptansicht selbst ausführen muss. */
    public interface Listener {
        /** Zwischen Liste und Rasteransicht umschalten. */
        void onToggleLayout();
    }

    public static final String GITHUB_URL = "https://github.com/amarradi/leafpad/issues";
    public static final String WEBLATE_URL = "https://hosted.weblate.org/projects/leafpad/";
    public static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details";

    private final AppCompatActivity activity;
    private final DrawerLayout drawerLayout;
    private final NavigationView navigationView;
    private final View snackbarAnchor;
    private final Listener listener;
    private final LeafpadBackupManager backupManager = new LeafpadBackupManager();

    private final ActivityResultLauncher<Intent> exportLauncher;
    private final ActivityResultLauncher<Intent> importLauncher;
    private int headerBaseTop;

    public DrawerController(@NonNull AppCompatActivity activity,
                            @NonNull DrawerLayout drawerLayout,
                            @NonNull NavigationView navigationView,
                            @NonNull View snackbarAnchor,
                            @NonNull Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.drawerLayout = drawerLayout;
        this.navigationView = navigationView;
        this.snackbarAnchor = snackbarAnchor;

        exportLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                            && result.getData().getData() != null) {
                        backupToUri(result.getData().getData());
                    }
                });

        importLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                            && result.getData().getData() != null) {
                        restoreFromUri(result.getData().getData());
                    }
                });

        navigationView.setNavigationItemSelectedListener(this::onItemSelected);
        setupInsets();
        setupVersionFooter();
        setupBackHandling();
        refreshSwitches();
    }

    /** Öffnet die Sidebar (Klick auf das Menü-Symbol in der Toolbar). */
    public void open() {
        refreshSwitches();
        applyHeaderInset();
        drawerLayout.openDrawer(GravityCompat.START);
    }

    /** Schalter an den gespeicherten Wert anpassen, z. B. nach onResume. */
    public void refreshSwitches() {
        setSwitch(R.id.nav_change_notification, Leafpad.isChangeNotificationEnabled(activity));
        setSwitch(R.id.nav_keep_screen_on, Leafpad.isKeepScreenOnEnabled(activity));
        setSwitch(R.id.nav_grid_view, !LayoutModeHelper.isListMode(activity));
    }

    // ------------------------------------------------------------------
    // Klicks
    // ------------------------------------------------------------------

    private boolean onItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Schalter: Sidebar offen lassen, damit man den neuen Zustand sieht
        if (id == R.id.nav_grid_view) {
            listener.onToggleLayout();
            setSwitch(id, !LayoutModeHelper.isListMode(activity));
            return false;
        }
        if (id == R.id.nav_change_notification) {
            boolean enabled = !Leafpad.isChangeNotificationEnabled(activity);
            Leafpad.setChangeNotificationEnabled(activity, enabled);
            setSwitch(id, enabled);
            return false;
        }
        if (id == R.id.nav_keep_screen_on) {
            toggleKeepScreenOn();
            return false;
        }

        close();

        if (id == R.id.nav_theme) {
            // saveTheme() setzt den NightMode; AppCompat baut die Activity dann selbst neu auf
            DialogHelper.showThemeSelectionDialog(activity, null);
        } else if (id == R.id.nav_backup) {
            startExportIntent();
        } else if (id == R.id.nav_restore) {
            startImportIntent();
        } else if (id == R.id.nav_categories) {
            activity.startActivity(new Intent(activity, CategoryActivity.class));
        } else if (id == R.id.nav_rating) {
            launchAppStore(activity, activity.getPackageName());
        } else if (id == R.id.nav_github) {
            openUrl(GITHUB_URL);
        } else if (id == R.id.nav_translate) {
            openUrl(WEBLATE_URL);
        } else if (id == R.id.nav_licenses) {
            activity.startActivity(new Intent(activity, LicenseActivity.class));
        } else if (id == R.id.nav_about) {
            activity.startActivity(new Intent(activity, AboutActivity.class));
        }
        // false: kein Eintrag bleibt dauerhaft markiert, es sind Aktionen, keine Ziele
        return false;
    }

    private void toggleKeepScreenOn() {
        if (Leafpad.isKeepScreenOnEnabled(activity)) {
            Leafpad.setKeepScreenOnEnabled(activity, false);
            setSwitch(R.id.nav_keep_screen_on, false);
        } else {
            // Einschalten nur nach Bestätigung (Akku-Warnung), wie bisher in den Einstellungen
            DialogHelper.showKeepScreenOnWarningDialog(activity, () -> {
                Leafpad.setKeepScreenOnEnabled(activity, true);
                setSwitch(R.id.nav_keep_screen_on, true);
            });
        }
    }

    /**
     * Edge-to-Edge: DrawerLayout würde sonst die Statusleiste mit colorPrimaryDark (grün)
     * hinterlegen, und der Sidebar-Kopf läge unter der Statusleiste.
     */
    private void setupInsets() {
        drawerLayout.setStatusBarBackground(null);
        if (navigationView.getHeaderCount() > 0) {
            headerBaseTop = navigationView.getHeaderView(0).getPaddingTop();
        }
    }

    /**
     * Abstand des Sidebar-Kopfs zur Statusleiste. Die NavigationView reicht die Insets
     * nicht an den Kopf weiter, daher beim Öffnen aus den Root-Insets setzen.
     */
    private void applyHeaderInset() {
        if (navigationView.getHeaderCount() == 0) return;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(drawerLayout);
        if (insets == null) return;
        int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        View header = navigationView.getHeaderView(0);
        header.setPadding(header.getPaddingLeft(), headerBaseTop + top,
                header.getPaddingRight(), header.getPaddingBottom());
    }

    /** Unterster Eintrag: "Version 1.25" aus der Build-Konfiguration. */
    private void setupVersionFooter() {
        MenuItem item = navigationView.getMenu().findItem(R.id.nav_version);
        if (item != null) {
            item.setTitle(activity.getString(R.string.version) + " " + BuildConfig.VERSION_NAME);
        }
    }

    private void close() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void setupBackHandling() {
        // Zurück schließt zuerst die Sidebar
        OnBackPressedCallback callback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, callback);

        // Statusleisten-Symbole: über dem grünen Sidebar-Kopf hell, sonst wie vom Theme vorgegeben
        final WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        final boolean themeLightStatusBar = insetsController.isAppearanceLightStatusBars();

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                boolean overHeader = slideOffset > 0.5f;
                insetsController.setAppearanceLightStatusBars(!overHeader && themeLightStatusBar);
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                callback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                callback.setEnabled(false);
            }
        });
    }

    private void setSwitch(int itemId, boolean checked) {
        MenuItem item = navigationView.getMenu().findItem(itemId);
        if (item != null && item.getActionView() instanceof MaterialSwitch) {
            ((MaterialSwitch) item.getActionView()).setChecked(checked);
        }
    }

    // ------------------------------------------------------------------
    // Backup / Restore
    // ------------------------------------------------------------------

    private void startExportIntent() {
        String fileName = LeafpadBackupManager.BASE_NAME + "_"
                + LeafpadBackupManager.generateTimestamp() + ".zip";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        exportLauncher.launch(intent);
    }

    private void startImportIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
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
        final Context appContext = activity.getApplicationContext();
        showResult(appContext, appContext.getString(R.string.backupIsrunning));

        backupManager.exportBackup(appContext, uri, new LeafpadBackupManager.Callback() {
            @Override
            public void onSuccess(int noteCount) {
                showResult(appContext, appContext.getResources().getQuantityString(
                        R.plurals.notes_exported, noteCount, noteCount));
            }

            @Override
            public void onError(String message) {
                showResult(appContext, message);
            }
        });
    }

    private void restoreFromUri(Uri uri) {
        final Context appContext = activity.getApplicationContext();

        DialogHelper.showRestoreConfirmation(activity, () ->
                backupManager.restoreBackup(appContext, uri, new LeafpadBackupManager.Callback() {
                    @Override
                    public void onSuccess(int noteCount) {
                        showResult(appContext, appContext.getResources().getQuantityString(
                                R.plurals.notes_imported, noteCount, noteCount));
                    }

                    @Override
                    public void onError(String message) {
                        showResult(appContext, message);
                    }
                }));
    }

    /**
     * Ergebnis anzeigen: als Snackbar, solange die Hauptansicht da ist; wurde sie
     * inzwischen beendet (Backup läuft im Hintergrund weiter), als Toast.
     */
    private void showResult(Context appContext, String message) {
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            NotificationHelper.showSnackbar(snackbarAnchor, message);
        } else {
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    // Links
    // ------------------------------------------------------------------

    private void openUrl(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
            // kein Browser installiert
        }
    }

    public static void launchAppStore(Activity activity, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(GOOGLE_PLAY_URL + "?id=" + packageName)));
            } catch (ActivityNotFoundException ignored) {
                // weder Store noch Browser vorhanden
            }
        }
    }
}
