package com.git.amarradi.leafpad;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.git.amarradi.leafpad.fragment.CategoryFragment;
import com.git.amarradi.leafpad.helper.DialogHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.util.Objects;

/**
 * "Kategorien verwalten" aus der Sidebar. Zeigt CategoryFragment im Verwaltungsmodus.
 *
 * Das Fragment fängt Zurück selbst ab und poppt sich vom Back-Stack (closeSelf()).
 * Ist der Back-Stack danach leer, wird die Activity beendet.
 */
public class CategoryActivity extends AppCompatActivity implements ColorPickerDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category);

        MaterialToolbar toolbar = findViewById(R.id.category_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.manage_categories);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                finish();
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.category_fragment_container,
                            CategoryFragment.newInstance(CategoryFragment.MODE_MANAGE_ONLY))
                    .addToBackStack("category")
                    .commit();
        }
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        DialogHelper.onColorSelectedForCategoryDialog(dialogId, color);
    }

    @Override
    public void onDialogDismissed(int dialogId) {
    }
}
