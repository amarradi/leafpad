package com.git.amarradi.leafpad.fragment;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
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
import com.git.amarradi.leafpad.model.CategoryRepository;
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
    private OnBackPressedCallback backCallback;
    private List<Long> initialSelectedCategoryIds = null;
    private boolean initialSnapshotTaken = false;


    private MaterialToolbar hostToolbar;
    private Drawable prevNavIcon;
    private CharSequence prevTitle;
    private View.OnClickListener prevNavClickListener;

    private void closeSelf() {
        if (!isAdded()) return;

        List<Long> selectedCategoryIds = adapter.getSelectedCategoryIds();
        if (selectedCategoryIds == null) {
            selectedCategoryIds = new java.util.ArrayList<>();
        }
        if (initialSelectedCategoryIds == null) {
            initialSelectedCategoryIds = new java.util.ArrayList<>();
        }

        android.util.Log.d("CAT",
                "initial=" + initialSelectedCategoryIds + " selected=" + selectedCategoryIds);

        // Nur speichern, wenn sich wirklich etwas geändert hat
        if (getMode() == MODE_PICK_FOR_NOTE && noteViewModel != null) {
            if (!sameIds(initialSelectedCategoryIds, selectedCategoryIds)) {
                noteViewModel.setCategoriesForSelectedNote(selectedCategoryIds);
            }
        }

        // UI zurücksetzen (NoteEdit)
        if (getMode() == MODE_PICK_FOR_NOTE && getActivity() != null) {
            View fc = getActivity().findViewById(R.id.fragment_container);
            View bs = getActivity().findViewById(R.id.body_scroll);

            if (fc != null) fc.setVisibility(View.GONE);
            if (bs != null) bs.setVisibility(View.VISIBLE);

            if (getActivity() instanceof NoteEditActivity) {
                ((NoteEditActivity) getActivity()).restoreEditorToolbar();
            }

            getActivity().invalidateOptionsMenu();
        }

        getParentFragmentManager().popBackStack();
    }


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

                        List<Long> safeIds = (ids == null)
                                ? new java.util.ArrayList<>()
                                : new java.util.ArrayList<>(ids);

                        adapter.setSelectedCategoryIds(safeIds);

                        // Snapshot genau EINMAL setzen
                        if (!initialSnapshotTaken) {
                            initialSelectedCategoryIds = new java.util.ArrayList<>(safeIds);
                            initialSnapshotTaken = true;
                        }
                    });
        }
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
        // Im NoteEdit-Modus: Activity ist Owner der Toolbar
        if (getMode() == MODE_PICK_FOR_NOTE) {
            return;
        }

        // Im Settings-Modus: SettingsActivity ist Owner der Toolbar
        if (getMode() == MODE_MANAGE_ONLY) {
            return;
        }
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
                (name, colorHex) -> categoryViewModel
                        .createCategory(name, colorHex)
                        .observe(getViewLifecycleOwner(), result -> {
                            if (result == CategoryRepository.WriteResult.DUPLICATE) {
                                DialogHelper.showInfoDialog(
                                        requireContext(),
                                        getString(R.string.error),
                                        getString(R.string.category_already_exists)
                                );
                            } else if (result == CategoryRepository.WriteResult.EMPTY_NAME) {
                                DialogHelper.showInfoDialog(
                                        requireContext(),
                                        getString(R.string.error),
                                        getString(R.string.category_name_required)
                                );
                            } else if (result == CategoryRepository.WriteResult.DB_ERROR) {
                                DialogHelper.showInfoDialog(
                                        requireContext(),
                                        getString(R.string.error),
                                        getString(R.string.database_error_log)
                                );
                            }
                        })
        );
    }

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
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        if (getMode() == MODE_MANAGE_ONLY && getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.AppCompatActivity activity =
                    (androidx.appcompat.app.AppCompatActivity) getActivity();

            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.menu_settings);
            }
        }

        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeSelf();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                backCallback
        );
    }


    private void showEditCategoryDialog(CategoryEntity category) {
        DialogHelper.showCategoryAddOrEditDialog(
                this,
                getString(R.string.edit_category),
                category.name,
                category.colorHex,
                COLOR_PICKER_ID_EDIT,
                (name, colorHex) -> categoryViewModel
                        .updateCategory(category, name, colorHex)
                        .observe(getViewLifecycleOwner(), result -> {
                            // Optional: Feedback anzeigen
                            // (Strings musst du ggf. anlegen/verwenden)
                            if (result == CategoryRepository.WriteResult.DUPLICATE) {
                                DialogHelper.showInfoDialog(
                                        requireContext(),
                                        getString(R.string.error),
                                        getString(R.string.category_already_exists)
                                );
                            } else if (result == CategoryRepository.WriteResult.EMPTY_NAME) {
                                DialogHelper.showInfoDialog(
                                        requireContext(),
                                        getString(R.string.error),
                                        getString(R.string.category_name_required)
                                );
                            } else if (result == CategoryRepository.WriteResult.DB_ERROR) {
                                DialogHelper.showInfoDialog(
                                        requireContext(),
                                        getString(R.string.error),
                                        getString(R.string.database_error_log)
                                );
                            }
                            // OK -> Dialog schließt ohnehin (DialogHelper macht das typischerweise)
                        })
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHostTitle();
    }

    private void updateHostTitle() {
        if (!(requireActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            return;
        }

        androidx.appcompat.app.AppCompatActivity activity =
                (androidx.appcompat.app.AppCompatActivity) requireActivity();

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
            activity.getSupportActionBar().setTitle(R.string.manage_categories);
        }
    }

}
