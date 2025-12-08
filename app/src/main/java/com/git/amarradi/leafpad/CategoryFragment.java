package com.git.amarradi.leafpad;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.adapter.CategoryAdapter;
import com.git.amarradi.leafpad.viewmodel.CategoryViewModel;
import com.google.android.material.appbar.MaterialToolbar;
public class CategoryFragment extends Fragment {

    private CategoryViewModel categoryViewModel;
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_category, container, false);

        setupToolbar();
        setupMenu();
        setupRecyclerView(view);
        setupViewModel();

        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView rv = view.findViewById(R.id.categoryRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoryAdapter();
        rv.setAdapter(adapter);
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
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
       // toolbar.setTitle(R.string.categories);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        toolbar.setNavigationOnClickListener(v -> {
                getParentFragmentManager().popBackStack();
                //requireActivity().getSupportFragmentManager().popBackStack();
                requireActivity().findViewById(R.id.fragment_container).setVisibility(View.GONE);
                requireActivity().findViewById(R.id.body_scroll).setVisibility(View.VISIBLE);
            ((NoteEditActivity) requireActivity()).restoreEditorToolbar();

           // toolbar.setNavigationIcon(null);
            requireActivity().invalidateOptionsMenu();
        } );
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
                    Toast.makeText(requireContext(), "Neue Kategorie", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        requireActivity().invalidateOptionsMenu();
        requireActivity().invalidateOptionsMenu();
    }
}
