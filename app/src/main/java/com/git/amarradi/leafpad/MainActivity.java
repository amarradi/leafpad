package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.git.amarradi.leafpad.adapter.NoteAdapter;
import com.git.amarradi.leafpad.adapter.OnReleaseNoteCloseListener;
import com.git.amarradi.leafpad.fragment.NoteActionsBottomSheet;
import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.ShareHelper;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, OnReleaseNoteCloseListener {

    public RecyclerView recyclerView;
    public NoteAdapter noteAdapter;
    private NoteViewModel noteViewModel;

    private int lastScrollPosition = 0;

    private boolean firstNotesLoad = true;

    private ImageView toolbarTitleIcon;

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        noteViewModel = new ViewModelProvider(
                this,
                new ViewModelProvider.AndroidViewModelFactory(getApplication())
        ).get(NoteViewModel.class);

        noteViewModel.checkAndLoadReleaseNote(this);
        noteViewModel.getReleaseNote().observe(this, releaseNote -> {
            if (!Leafpad.isReleaseNoteClosed(this) ||
                    Leafpad.getCurrentVersionCode(this) > Leafpad.getCurrentLeafpadVersionCode(this)) {
                noteViewModel.setReleaseNoteHeader(releaseNote);
                Leafpad.resetReleaseNoteClosed(this);
                updateEmptyState();
            } else {
                noteViewModel.setReleaseNoteHeader(null);
            }
        });
        boolean savedShowHidden = Leafpad.getInstance().getSavedShowHidden();
        noteViewModel.setShowHidden(savedShowHidden);

        noteViewModel.getShowHidden().observe(this, showHidden -> {
            updateEmptyState();
            updateToolbarForHiddenState(showHidden);
        });

        noteViewModel.getCombinedNotes().observe(this, combinedList -> {
            noteAdapter.setCombinedList(combinedList);

            if (firstNotesLoad) {
                recyclerView.post(() -> recyclerView.scrollToPosition(0));
                firstNotesLoad = false;
            }

            updateEmptyState();
        });

        setupSharedPreferences();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);

        View customTitleView = getLayoutInflater().inflate(R.layout.toolbar_title_with_icon, toolbar, false);

        toolbarTitleIcon = customTitleView.findViewById(R.id.toolbar_title_icon);
        toolbar.addView(customTitleView);
        recyclerView = findViewById(R.id.note_list_view);

        noteAdapter = new NoteAdapter(this, new ArrayList<>(), new NoteClickListener() {
            @Override
            public void onNoteClicked(Note note) {
                // Aktuelle Scrollposition merken
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    lastScrollPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                } else if (layoutManager instanceof StaggeredGridLayoutManager staggered) {
                    int[] firstVisibleItems = staggered.findFirstVisibleItemPositions(null);
                    if (firstVisibleItems != null && firstVisibleItems.length > 0) {
                        lastScrollPosition = firstVisibleItems[0];
                    }
                }

                noteViewModel.selectNote(note);
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(Leafpad.EXTRA_NOTE_ID, note.getId());
                intent.putExtra("is_new_note", false);
                noteEditLauncher.launch(intent);
            }


            @Override
            public void onNoteIconClicked(Note note, View anchor) {
                showNoteActionsBottomSheet(note);
            }
        },this,
                noteViewModel,
                this);


        recyclerView.setAdapter(noteAdapter);
        noteViewModel.getCategoriesByNoteId().observe(this, map -> {
            noteAdapter.setCategoriesByNoteId(map);
        });

        Leafpad.getInstance().applyCurrentLayoutMode(recyclerView, noteAdapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_action_add);

        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
            lp.bottomMargin = bottomInset + fabMargin;
            v.setLayoutParams(lp);
            return insets;
        });

        fab.setOnClickListener(v -> {
            String newNoteId = Note.makeId();
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(Leafpad.EXTRA_IS_NEW_NOTE, true);
            intent.putExtra(Leafpad.EXTRA_NOTE_ID, newNoteId);
            noteEditLauncher.launch(intent);
        });
        if (BuildConfig.DEBUG) {
            fab.setOnLongClickListener(v -> {
                seedTestNotes(25);
                return true;
            });
        }
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy > 0 && fab.isExtended()) {
                    fab.shrink();
                } else if (dy < 0 && !fab.isExtended()) {
                    fab.extend();
                }
            }
        });
    }

    private void seedTestNotes(int count) {
        new Thread(() -> {
            String[] sampleCategories = {"Rezept", "Test"};
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            String today = dateFormat.format(new java.util.Date());
            String now = timeFormat.format(new java.util.Date());

            com.git.amarradi.leafpad.model.AppDatabase db =
                    com.git.amarradi.leafpad.model.AppDatabase.getInstance(getApplicationContext());

            for (int i = 1; i <= count; i++) {
                String id = com.git.amarradi.leafpad.model.Note.makeId();

                com.git.amarradi.leafpad.model.NoteEntity entity = new com.git.amarradi.leafpad.model.NoteEntity(
                        id,
                        "Testnotiz " + i,
                        "Das ist der Inhalt von Testnotiz Nummer " + i + ". Lorem ipsum dolor sit amet.",
                        today,
                        now,
                        today,
                        false
                );
                db.noteDao().insert(entity);
            }

            runOnUiThread(() -> noteViewModel.loadNotes());
        }).start();
    }
    private final ActivityResultLauncher<Intent> noteEditLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            Note updatedNote = data.getParcelableExtra("updated_note");
                            boolean isNewNote = data.getBooleanExtra("is_new_note", false);

                            if (updatedNote != null) {
                                noteViewModel.updateSingleNote(updatedNote);

                                if (isNewNote) {
                                    recyclerView.post(() -> recyclerView.scrollToPosition(0));
                                } else {
                                    recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                            if (recyclerView.getLayoutManager() != null &&
                                                    noteAdapter.getItemCount() > lastScrollPosition) {
                                                recyclerView.scrollToPosition(lastScrollPosition);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });

    private void updateToolbarForHiddenState(boolean showHidden) {
        if (showHidden) {
            toolbarTitleIcon.setImageResource(R.drawable.btn_hide);
            toolbarTitleIcon.setVisibility(View.VISIBLE);
        } else {
            toolbarTitleIcon.setVisibility(View.GONE);
        }
    }
    @Override
    public void onReleaseNoteClosed() {
        Leafpad.setReleaseNoteClosed(this);
        Leafpad.setCurrentLeafpadVersionCode(this);
        noteViewModel.setReleaseNoteHeader(null);
        noteViewModel.loadNotes();
        recyclerView.post(this::updateEmptyState);
    }
    private void updateEmptyState() {
        int count = noteAdapter.getItemCount();
        Boolean showOnlyHiddenValue = noteViewModel.getShowHidden().getValue();
        boolean showOnlyHidden = showOnlyHiddenValue != null && showOnlyHiddenValue;
        ImageView emptyElement = findViewById(R.id.emptyElement);
        if (count == 0) {
            emptyElement.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
            if(showOnlyHidden) {
                emptyElement.setImageResource(R.drawable.ic_olive_leafhidden);
            } else {
                emptyElement.setImageResource(R.drawable.ic_olive_leaf);
            }
        } else {
            emptyElement.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void showNoteActionsBottomSheet(Note note) {
        NoteActionsBottomSheet sheet = NoteActionsBottomSheet.newInstance(note, new NoteActionsBottomSheet.OnNoteActionListener() {
            @Override
            public void onHideToggle(Note note) {
                noteViewModel.selectNote(note);
                noteViewModel.setNoteHide();
                Note updatedNote = noteViewModel.getSelectedNote().getValue();
                if (updatedNote != null) {
                    noteViewModel.saveNote(MainActivity.this, updatedNote);
                }
            }

            @Override
            public void onShare(Note note) {
                ShareHelper.shareNote(MainActivity.this, note);
            }

            @Override
            public void onRemove(Note note) {
                DialogHelper.showDeleteSingleNoteDialog(MainActivity.this, () -> noteViewModel.deleteNote(MainActivity.this, note));
            }
        });
        sheet.show(getSupportFragmentManager(), "note_actions");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("theme".equals(key)) {
            String newValue = sharedPreferences.getString("theme", "system");
            Leafpad.getInstance().saveTheme(newValue);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Leafpad.getInstance().applyCurrentLayoutMode(recyclerView, noteAdapter);
        noteViewModel.loadNotes();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onCreateOptionsMenu(@NonNull android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        android.view.MenuItem item = menu.findItem(R.id.item_show_hidden);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedLayout = sharedPreferences.getString(Leafpad.PREF_LAYOUT_MODE, "list");
        Boolean showHidden = noteViewModel.getShowHidden().getValue();
        if (showHidden == null) {
            showHidden = false;
        }
        if (showHidden) {
            item.setIcon(getDrawable(R.drawable.btn_hide));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.btn_show));
            item.setTitle(getString(R.string.show_hidden));
        }
        MenuItem layoutItem = menu.findItem(R.id.item_toggle_layout);
        if ("grid".equals(savedLayout)) {
            layoutItem.setIcon(R.drawable.ic_listview);
        } else {
            layoutItem.setIcon(R.drawable.ic_gridview);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.item_show_hidden:
                toggleShowHidden(item);
                return true;
            case R.id.item_toggle_layout:
                Leafpad.getInstance().toggleLayoutMode(recyclerView, noteAdapter);
                invalidateOptionsMenu();
                return true;
            case R.id.action_search:
                Intent searchIntent = new Intent(this, SearchActivity.class);
                startActivity(searchIntent);
                return true;
            case R.id.item_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleShowHidden(MenuItem item) {

        Boolean current = noteViewModel.getShowHidden().getValue();
        if (current == null) {
            current = false;
        }

        boolean newValue = !current;
        noteViewModel.setShowHidden(newValue);

        if (newValue) {
            item.setIcon(getDrawable(R.drawable.btn_hide));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.btn_show));
            item.setTitle(getString(R.string.show_hidden));
        }
        Leafpad.getInstance().saveShowHidden(newValue);
    }
    public interface NoteClickListener {
        void onNoteClicked(Note note);
        void onNoteIconClicked(Note note, View anchor);
    }
}