package com.git.amarradi.leafpad.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.MainActivity;
import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.model.ReleaseNote;
import com.google.android.material.card.MaterialCardView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEWTYPE_RELEASE_NOTE = 100;
    public static final int VIEWTYPE_NOTE_LIST = 0;
    public static final int VIEWTYPE_NOTE_GRID = 1;

    private final Context context;
    private List<Note> noteList;
    private List<Note> fullNoteList;
    private boolean showOnlyHidden = false;
    private LayoutMode layoutMode = LayoutMode.LIST;
    private ReleaseNote releaseNoteHeader = null;

    private List<Object> currentList = new ArrayList<>();
    private final MainActivity.NoteClickListener listener;
    private final OnReleaseNoteCloseListener releaseNoteCloseListener;

    public enum LayoutMode {
        LIST, GRID
    }

    private final static String BIBLEVERSE_URL_REGEX = "(?i)\\b(?:https?://)?(?:www\\.)?(bible\\.(com|org)|bibleserver\\.com)(/\\S*)?";

    public NoteAdapter(Context context, List<Note> noteList, MainActivity.NoteClickListener listener, OnReleaseNoteCloseListener releaseNoteCloseListener) {
        this.context = context;
        this.noteList = noteList;
        this.fullNoteList = noteList;
        this.listener = listener;
        this.releaseNoteCloseListener = releaseNoteCloseListener;
        setHasStableIds(true);
        applyFilterAndUpdate();
    }

    // Setter für ReleaseNote-Header (aufrufen, wenn angezeigt werden soll)
    public void setReleaseNoteHeader(ReleaseNote releaseNote) {
        this.releaseNoteHeader = releaseNote;
        buildCombinedListAndNotify();
    }

    public boolean isShowOnlyHidden() {
        return showOnlyHidden;
    }


    // Setter für kombinierte Liste (z. B. bei MVVM direkt vom ViewModel)
    public void setCombinedList(List<Object> list) {
        if (list != null) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NoteMixedDiffCallback(currentList, list));
            currentList = new ArrayList<>(list);
            diffResult.dispatchUpdatesTo(this);
        } else {
            currentList = new ArrayList<>();
            notifyDataSetChanged();
        }
    }

    public void setLayoutMode(LayoutMode mode) {
        this.layoutMode = mode;
        notifyDataSetChanged();
    }

    public void setShowOnlyHidden(boolean showHidden) {
        this.showOnlyHidden = showHidden;
        applyFilterAndUpdate();
    }

    public void updateNotes(List<Note> newNotes) {
        this.fullNoteList = newNotes;
        applyFilterAndUpdate();
    }

    public boolean isFilteredListEmpty() {
        return noteList == null || noteList.isEmpty();
    }

    // Kombiniert ReleaseNote (sofern gesetzt) und die Notizen zu einer Liste
    private void buildCombinedListAndNotify() {
        List<Object> newCombined = new ArrayList<>();
        if (releaseNoteHeader != null) {
            newCombined.add(releaseNoteHeader);
        }
        if (noteList != null) {
            newCombined.addAll(noteList);
        }
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NoteMixedDiffCallback(currentList, newCombined));
        currentList = newCombined;
        diffResult.dispatchUpdatesTo(this);
    }

    private void applyFilterAndUpdate() {
        List<Note> newFilteredList = filterNotes(showOnlyHidden);
        this.noteList = newFilteredList;
        buildCombinedListAndNotify();
    }

    private List<Note> filterNotes(boolean showHidden) {
        List<Note> filtered = new ArrayList<>();
        if (fullNoteList != null) {
            for (Note note : fullNoteList) {
                if (showHidden && note.isHide()) {
                    filtered.add(note);
                } else if (!showHidden && !note.isHide()) {
                    filtered.add(note);
                }
            }
        }
        return filtered;
    }

    @Override
    public int getItemCount() {
        return currentList != null ? currentList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        Object item = currentList.get(position);
        if (item instanceof ReleaseNote) {
            return -1;
        } else if (item instanceof Note) {
            String id = ((Note) item).getId();
            if (id != null) {
                return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
            }
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = currentList.get(position);
        if (item instanceof ReleaseNote) {
            return VIEWTYPE_RELEASE_NOTE;
        }
        if (layoutMode == LayoutMode.GRID) {
            return VIEWTYPE_NOTE_GRID;
        }
        return VIEWTYPE_NOTE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEWTYPE_RELEASE_NOTE) {
            View v = LayoutInflater.from(context).inflate(R.layout.release_note_item, parent, false);
            return new ReleaseNoteViewHolder(v);
        }
        View view;
        if (viewType == VIEWTYPE_NOTE_LIST) {
            view = LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.note_grid_item, parent, false);
        }
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = currentList.get(position);
        if (getItemViewType(position) == VIEWTYPE_RELEASE_NOTE && item instanceof ReleaseNote) {
            ((ReleaseNoteViewHolder) holder).bind((ReleaseNote) item, releaseNoteCloseListener);
            return;
        }
        if (item instanceof Note) {
            Note note = (Note) item;
            NoteViewHolder noteHolder = (NoteViewHolder) holder;
            noteHolder.titleText.setText(note.getTitle());

            String body = note.getBody() != null ? note.getBody() : "";
            if (body.length() > 150) {
                body = body.substring(0, 150) + "...";
            }
            noteHolder.bodyPreview.setText(body);

            noteHolder.dateText.setText(note.getDate());
            noteHolder.timeText.setText(note.getTime());

            MaterialCardView cardView = noteHolder.itemView.findViewById(R.id.note_card_view);
            if (!TextUtils.isEmpty(note.getCategory())) {
                noteHolder.categoryText.setVisibility(View.VISIBLE);
                noteHolder.categoryIcon.setVisibility(View.VISIBLE);
                noteHolder.categoryText.setText(note.getCategory());
                cardView.setStrokeColor(context.getResources().getColor(R.color.md_theme_recipe, null));
            } else {
                cardView.setStrokeColor(context.getResources().getColor(R.color.md_theme_primaryContainer, null));
                noteHolder.categoryText.setVisibility(View.GONE);
                noteHolder.categoryIcon.setVisibility(View.GONE);
            }

            Pattern biblePattern = Pattern.compile(BIBLEVERSE_URL_REGEX);
            Matcher matcher = biblePattern.matcher(note.getBody());
            if (matcher.find()) {
                noteHolder.bibleIcon.setVisibility(View.VISIBLE);
            } else {
                noteHolder.bibleIcon.setVisibility(View.GONE);
            }

            noteHolder.itemView.setOnClickListener(v -> listener.onNoteClicked(note));
            noteHolder.actionButton.setOnClickListener(v -> listener.onNoteIconClicked(note, noteHolder.actionButton));
        }
    }

    // ViewHolder für ReleaseNote-Header
    public static class ReleaseNoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, date, time;
        ImageButton closeButton;

        public ReleaseNoteViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.release_title);
            content = itemView.findViewById(R.id.release_content);
            date = itemView.findViewById(R.id.release_date);
            time = itemView.findViewById(R.id.release_time);
            closeButton = itemView.findViewById(R.id.release_note_close_button);
        }

        public void bind(ReleaseNote note, OnReleaseNoteCloseListener listener) {
            title.setText(note.getTitle());
            content.setText(note.getContent());
            date.setText(note.getDate());
            time.setText(note.getTime());
            closeButton.setOnClickListener(v -> {
                // Speicherflag wird im ViewModel/Activity gesetzt!


                if (listener != null) listener.onReleaseNoteClosed();
            });
        }
    }

    // Notiz-ViewHolder
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, bodyPreview, dateText, timeText, categoryText;
        ImageView bibleIcon, categoryIcon;
        ImageButton actionButton;

        NoteViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            bodyPreview = itemView.findViewById(R.id.note_preview);
            dateText = itemView.findViewById(R.id.created_at);
            timeText = itemView.findViewById(R.id.time_txt);
            categoryText = itemView.findViewById(R.id.category_txt);
            bibleIcon = itemView.findViewById(R.id.bible);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            actionButton = itemView.findViewById(R.id.image_button);
        }
    }

    // DiffUtil für gemischte Listen (ReleaseNote + Notes)
    public static class NoteMixedDiffCallback extends DiffUtil.Callback {
        private final List<Object> oldList;
        private final List<Object> newList;

        public NoteMixedDiffCallback(List<Object> oldList, List<Object> newList) {
            this.oldList = oldList != null ? oldList : new ArrayList<>();
            this.newList = newList != null ? newList : new ArrayList<>();
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof ReleaseNote && newItem instanceof ReleaseNote)
                return true;
            if (oldItem instanceof Note && newItem instanceof Note)
                return ((Note) oldItem).getId().equals(((Note) newItem).getId());
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof ReleaseNote && newItem instanceof ReleaseNote) {
                return oldItem.equals(newItem);
            }
            if (oldItem instanceof Note && newItem instanceof Note) {
                return oldItem.equals(newItem);
            }
            return false;
        }
    }

    // (Optional) Suche/Filter in den Notizen
    public void filter(String query) {
        List<Note> filtered = new ArrayList<>();
        if (fullNoteList != null) {
            for (Note note : fullNoteList) {
                if (!showOnlyHidden && note.isHide()) continue;
                if (showOnlyHidden && !note.isHide()) continue;
                String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
                String body = note.getBody() != null ? note.getBody().toLowerCase() : "";
                String category = note.getCategory() != null ? note.getCategory().toLowerCase() : "";
                if (title.contains(query.toLowerCase())
                        || body.contains(query.toLowerCase())
                        || category.contains(query.toLowerCase())) {
                    filtered.add(note);
                }
            }
        }
        this.noteList = filtered;
        buildCombinedListAndNotify();
    }
}
