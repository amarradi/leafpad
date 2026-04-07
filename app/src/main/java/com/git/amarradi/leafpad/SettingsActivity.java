package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.git.amarradi.leafpad.fragment.SettingsFragment;
import com.git.amarradi.leafpad.helper.DialogHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity implements ColorPickerDialogListener {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MyApp);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.setting_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment_container, new SettingsFragment())
                    .commit();
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setting_toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        DialogHelper.onColorSelectedForCategoryDialog(dialogId, color);
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }
}
