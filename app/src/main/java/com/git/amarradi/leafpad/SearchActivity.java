package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.adapter.SearchAdapter;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity {

	private TextInputEditText searchInput;
	private RecyclerView recyclerView;
	private TextView noResultsText;

	private SearchAdapter searchAdapter;
	private NoteViewModel noteViewModel;

	@SuppressLint("RestrictedApi")
    @Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		EdgeToEdge.enable(this);
		MaterialToolbar searchtoolbar = findViewById(R.id.searchtoolbar);
		setSupportActionBar(searchtoolbar);
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
		searchtoolbar.setTitle(getString(R.string.search));
		searchInput = findViewById(R.id.search_input);
		recyclerView = findViewById(R.id.search_results_recycler_view);
		noResultsText = findViewById(R.id.no_results_text);
		noResultsText.setVisibility(View.GONE); // Start: nicht anzeigen

		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		searchAdapter = new SearchAdapter(new ArrayList<>(), note -> {
			Intent intent = new Intent(SearchActivity.this, NoteEditActivity.class);
			intent.putExtra("fromSearch", true); // Herkunft mitgeben
			intent.putExtra(Leafpad.EXTRA_NOTE_ID, note.getId());
			//intent.putExtra("noteId", note.getId());
			startActivity(intent);
		});
		recyclerView.setAdapter(searchAdapter);

		noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
		noteViewModel.loadNotes();

		noteViewModel.getSearchResults().observe(this, results -> {
			searchAdapter.updateNotes(results);

			String currentQuery = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
			boolean queryIsEmpty = currentQuery.isEmpty();

			if (!queryIsEmpty && (results == null || results.isEmpty())) {
				noResultsText.setVisibility(View.VISIBLE);
			} else {
				noResultsText.setVisibility(View.GONE);
			}
		});

		searchInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				noteViewModel.setSearchQuery(s.toString());
			}

			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});

		searchInput.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				String query = searchInput.getText() != null ? searchInput.getText().toString() : "";
				noteViewModel.setSearchQuery(query);
				return true;
			}
			return false;
		});

	}


}
