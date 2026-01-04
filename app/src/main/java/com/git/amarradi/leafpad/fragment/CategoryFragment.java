package com.git.amarradi.leafpad.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.NoteEditActivity;
import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.adapter.CategoryAdapter;
import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.model.CategoryEntity;
import com.git.amarradi.leafpad.viewmodel.CategoryViewModel;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.util.List;

public class CategoryFragment extends Fragment implements ColorPickerDialogListener {

    private CategoryViewModel categoryViewModel;
    private CategoryAdapter adapter;

    private NoteViewModel noteViewModel;

    // Give your color picker dialog unique IDs if you have multiple dialogs.
    private static final int DIALOG_ID = 0;

    private static final int COLOR_PICKER_ID = 1001;
    private static final int COLOR_PICKER_ID_EDIT = 2002;

    private static final String ARG_MODE = "mode";
    public static final int MODE_PICK_FOR_NOTE = 0;
    public static final int MODE_MANAGE_ONLY = 1;

    private int selectedColor = Color.parseColor("#CCCCCC");
    private View colorPreview;
    private List<Long> initialSelectedCategoryIds;

    public static CategoryFragment newInstance(int mode) {
        CategoryFragment f = new CategoryFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_MODE, mode);
        f.setArguments(b);
        return f;
    }


    private int getMode() {
        Bundle b = getArguments();
        if (b == null) return MODE_PICK_FOR_NOTE;
        return b.getInt(ARG_MODE, MODE_PICK_FOR_NOTE);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_category, container, false);
        categoryViewModel = new ViewModelProvider(requireActivity())
                .get(CategoryViewModel.class);
        if (getMode() == MODE_PICK_FOR_NOTE) {
            noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        }
        // noteViewModel = new ViewModelProvider(requireActivity())
        //       .get(NoteViewModel.class);
        setupToolbar();
        setupMenu();
        setupRecyclerView(view);
        setupViewModel();

        if (getMode() == MODE_PICK_FOR_NOTE) {
            noteViewModel.getSelectedCategoryIds()
                    .observe(getViewLifecycleOwner(), ids -> {
                        adapter.setSelectedCategoryIds(ids);

                        if (initialSelectedCategoryIds == null) {
                            // Snapshot nur einmal setzen
                            initialSelectedCategoryIds = (ids == null) ? null : new java.util.ArrayList<>(ids);
                        }
                    });
        }

//        noteViewModel.getSelectedCategoryIds()
//                .observe(getViewLifecycleOwner(), ids -> {
//                    adapter.setSelectedCategoryIds(ids);
//                });



        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView rv = view.findViewById(R.id.categoryRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoryAdapter();
        int mode = getMode();
        adapter.setSelectionEnabled(mode == MODE_PICK_FOR_NOTE);

        adapter.setListener(new CategoryAdapter.Listener() {
            @Override
            public void onEditCategory(CategoryEntity category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onDeleteCategory(CategoryEntity category) {
                DialogHelper.showDeleteCategoryDialog(
                        requireContext(),
                        category.name,
                        () -> categoryViewModel.deleteCategory(category)
                );

            }
        });

        rv.setAdapter(adapter);
    }

    public void onColorPicked(int dialogId, int color) {
        if (dialogId == COLOR_PICKER_ID) {
            selectedColor = color;

            if (colorPreview != null) {
                colorPreview.setBackgroundColor(color);
            }
        }
    }


    private void setupViewModel() {
        categoryViewModel = new ViewModelProvider(requireActivity())
                .get(CategoryViewModel.class);

        categoryViewModel.getCategories().observe(
                getViewLifecycleOwner(),
                categories -> {
                    adapter.setCategories(categories);
                }
        );
    }

    private void setupToolbar() {
        // MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar == null) {
            toolbar = requireActivity().findViewById(R.id.setting_toolbar);
        }
        if (toolbar == null) {
            // kein Toolbar im Host gefunden → nichts konfigurieren
            return;
        }
       // toolbar.setTitle(R.string.categories);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        toolbar.setNavigationOnClickListener(v -> {
            if (getMode() == MODE_PICK_FOR_NOTE) {
                List<Long> selectedCategoryIds = adapter.getSelectedCategoryIds();
                if (!sameIds(initialSelectedCategoryIds, selectedCategoryIds)) {
                    noteViewModel.setCategoriesForSelectedNote(selectedCategoryIds);
                }
                //noteViewModel.setCategoriesForSelectedNote(selectedCategoryIds);
                getParentFragmentManager().popBackStack();
                View fc = requireActivity().findViewById(R.id.fragment_container);
                View bs = requireActivity().findViewById(R.id.body_scroll);

                if (fc != null) fc.setVisibility(View.GONE);
                if (bs != null) bs.setVisibility(View.VISIBLE);

                if (requireActivity() instanceof NoteEditActivity) {
                    ((NoteEditActivity) requireActivity()).restoreEditorToolbar();
                }

                requireActivity().invalidateOptionsMenu();
                return;

//                requireActivity().findViewById(R.id.fragment_container).setVisibility(View.GONE);
//                requireActivity().findViewById(R.id.body_scroll).setVisibility(View.VISIBLE);
//                ((NoteEditActivity) requireActivity()).restoreEditorToolbar();

                // toolbar.setNavigationIcon(null);
                //requireActivity().invalidateOptionsMenu();
            }
            getParentFragmentManager().popBackStack();
            //requireActivity().getSupportFragmentManager().popBackStack();
        } );

    }

    private boolean sameIds(List<Long> a, List<Long> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;

        java.util.ArrayList<Long> aa = new java.util.ArrayList<>(a);
        java.util.ArrayList<Long> bb = new java.util.ArrayList<>(b);
        java.util.Collections.sort(aa);
        java.util.Collections.sort(bb);
        return aa.equals(bb);
    }


    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.menu_category, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.item_add_category) {
                    showAddCategoryDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    private void showAddCategoryDialog() {
        DialogHelper.showCategoryAddOrEditDialog(
                this,
                getString(R.string.add_category),
                "",
                "#CCCCCC",
                COLOR_PICKER_ID,
                (name, colorHex) -> categoryViewModel.createCategory(name, colorHex)
        );
    }


    // 🔥 DAS WAR DER FEHLENDE TEIL
    @Override
    public void onColorSelected(int dialogId, int color) {
        if (dialogId == COLOR_PICKER_ID) {
            selectedColor = color;
            if (colorPreview != null) {
                colorPreview.setBackgroundColor(color);
            }
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {
        // optional
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        requireActivity().invalidateOptionsMenu();
        requireActivity().invalidateOptionsMenu();
    }

    private void showEditCategoryDialog(CategoryEntity category) {
        DialogHelper.showCategoryAddOrEditDialog(
                this,
                getString(R.string.edit_category),
                category.name,
                category.colorHex,
                COLOR_PICKER_ID_EDIT,
                (name, colorHex) -> {
                    CategoryEntity updated = category.copyForUpdate(name, colorHex);
                    categoryViewModel.updateCategory(updated);

                    updated.id = category.id;
                    updated.name = name;
                    updated.colorHex = colorHex;

                    // falls vorhanden:
                    updated.sortOrder = category.sortOrder;
                    updated.isArchived = category.isArchived;

                    //   categoryViewModel.updateCategory(updated);
                }
        );
    }
}
