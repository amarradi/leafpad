package com.git.amarradi.leafpad.ui.category;

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

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.adapter.CategoryAdapter;
import com.git.amarradi.leafpad.viewmodel.CategoryViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class CategoryFragment extends Fragment {

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---------------------------
        // Menü der Activity ausblenden
        // ---------------------------
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear(); // ALLE Icons der NoteEditActivity ausblenden
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false; // Fragment hat keine Menüs
            }
        }, getViewLifecycleOwner());

        // ---------------------------
        // RecyclerView Setup
        // ---------------------------
        RecyclerView recyclerView = view.findViewById(R.id.categoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoryAdapter();
        recyclerView.setAdapter(adapter);

        // ---------------------------
        // ViewModel holen
        // ---------------------------
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            adapter.setCategories(categories);
        });

        // ---------------------------
        // Toolbar steuern (Titel + Zurück-Pfeil)
        // ---------------------------
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Kategorien");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Toolbar zurücksetzen auf Activity-Standard
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);
        toolbar.setTitle(R.string.app_name);
    }
}
