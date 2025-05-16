package com.git.amarradi.leafpad;

import androidx.appcompat.widget.SearchView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;
import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;


public class SearchActivity extends AppCompatActivity {
	private SearchView searchView;
	private ListView listView;
	private MaterialToolbar toolbar;
	private SimpleAdapter adapter;
	private List<Note> notes;
	private List<Map<String,String>> data = new ArrayList<>();

	private final int PREVIEW = 25;

	@Override
	protected void onCreate(Bundle savedInstanceState)  {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);

		searchView = findViewById(R.id.search_view);
		listView = findViewById(R.id.list_search_results);
		toolbar = findViewById(R.id.search_toolbar);

		setSupportActionBar(toolbar);
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

		notes = Leaf.loadAll(this,true);

		adapter = new SimpleAdapter(this, data, R.layout.note_list_item, new String[]{"title","body","date","time","category"},
							new int[]{R.id.title_text, R.id.note_preview, R.id.created_at, R.id.time_txt, R.id.category_txt});

		listView.setAdapter(adapter);


		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {	
				filterNotes(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText)  {
				filterNotes(newText);
				return true;
			}
		});

		filterNotes("");

	}

	private void filterNotes(String query) {
		String lowerQuery = query.toLowerCase();
		data.clear();

		for(Note note: notes) {
			if (note.getTitle().toLowerCase().contains(lowerQuery) || note.getBody().toLowerCase().contains(lowerQuery) ) {
				Map<String, String> datum = new HashMap<>();
				datum.put("id", note.getId());
				datum.put("title", note.getTitle());

				String previewText = note.getBody();
				if (previewText.length()> PREVIEW) {
					previewText = previewText.substring(0, PREVIEW) + "...";
				}
				datum.put("body", previewText);
				datum.put("date", note.getDate());
				datum.put("time", note.getTime());
				datum.put("category", note.getCategory());
				data.add(datum);
			}
		}

		adapter.notifyDataSetChanged();
	}		
}