package com.git.amarradi.leafpad.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.Note;
import com.git.amarradi.leafpad.NoteDiffCallback;
import com.git.amarradi.leafpad.OnNoteClickListener;
import com.git.amarradi.leafpad.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {


    private List<Note> noteList = new ArrayList<>();
    private boolean showOnlyHidden = false;
    private List<Note> fullNoteList = new ArrayList<>();
    private boolean isGridMode = false;

    private final OnNoteClickListener clickListener;

    private final Context context;


    private final static String BIBLEVERSE_URL_REGEX = "(?i)\\b(?:https?://)?(?:www\\.)?(bible\\.(com|org)|bibleserver\\.com)(/\\S*)?";

    public NoteAdapter(OnNoteClickListener listener, Context context) {
        this.clickListener = listener;
        this.context = context;
        setHasStableIds(true);  // WICHTIG für DiffUtil & RecyclerView-Stabilität
    }
    public NoteAdapter(Context context, OnNoteClickListener listener, Context context1) {
        this.clickListener = listener;
        this.context = context1;

        this.noteList = noteList;
        this.noteList = filterNotes(showOnlyHidden);
    }

    public void setLayoutMode(boolean isGrid) {
        this.isGridMode = isGrid;
    }
    @Override
    public int getItemViewType(int position) {
        if (isGridMode) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public long getItemId(int position) {
        Note note = noteList.get(position);
        return note.getId() != null ? note.getId().hashCode() : RecyclerView.NO_ID;
    }


    public void setShowOnlyHidden(boolean showHidden) {
        this.showOnlyHidden = showHidden;
        //this.noteList = filterNotes(showHidden);
        updateNotes(this.fullNoteList);
    }

    private List<Note> filterNotes(boolean showHidden) {
        List<Note> filtered = new ArrayList<>();
        for (Note note : fullNoteList) {
            if (showHidden && note.isHide()) {
                filtered.add(note);
            } else if (!showHidden && !note.isHide()) {
                filtered.add(note);
            }
        }
        return filtered;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_grid_item, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_list_item, parent, false);
        }
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        holder.titleText.setText(note.getTitle());

        String body = note.getBody();
        if (body.length() > 25) {
            body = body.substring(0, 25) + "...";
        }
        holder.bodyPreview.setText(body);

        holder.dateText.setText(note.getDate());
        holder.timeText.setText(note.getTime());

        MaterialCardView cardView = holder.itemView.findViewById(R.id.note_card_view);
        Context context = cardView.getContext(); // sichere Methode!
        if (!TextUtils.isEmpty(note.getCategory())) {

            holder.categoryText.setVisibility(View.VISIBLE);
            holder.categoryIcon.setVisibility(View.VISIBLE);
            holder.categoryText.setText(note.getCategory());
            cardView.setStrokeColor(ContextCompat.getColor(context, R.color.md_theme_recipe));
        } else {
            cardView.setStrokeColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer));
            holder.categoryText.setVisibility(View.GONE);
            holder.categoryIcon.setVisibility(View.GONE);
        }

        Pattern biblePattern = Pattern.compile(BIBLEVERSE_URL_REGEX);
        Matcher matcher = biblePattern.matcher(note.getBody());
        if (matcher.find()) {
            holder.bibleIcon.setVisibility(View.VISIBLE);
        } else {
            holder.bibleIcon.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> {
                clickListener.onNoteClick(note);
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public void updateNotes(List<Note> newNotes) {
        List<Note> newFullList = new ArrayList<>();
        if (newNotes != null) {
            newFullList.addAll(newNotes);
        }

        List<Note> newFilteredList = filterNotesFromList(showOnlyHidden, newFullList);  // Erst filtern

        NoteDiffCallback diffCallback = new NoteDiffCallback(this.noteList, newFilteredList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        //fullNoteList.clear();
        if(newNotes != null) {
            newFullList.addAll(newNotes);
        }


        // Diese Zuweisung NICHT nachträglich modifizieren!
        this.fullNoteList = newFullList; // Alternativ: clear + addAll, wenn du List final hältst
        this.noteList = newFilteredList; // NEUE LISTE STATT clear/addAll


        diffResult.dispatchUpdatesTo(this);
    }
    private List<Note> filterNotesFromList(boolean showHidden, List<Note> sourceList) {
        List<Note> filtered = new ArrayList<>();
        for (Note note : sourceList) {
            if (showHidden && note.isHide()) {
                filtered.add(note);
            } else if (!showHidden && !note.isHide()) {
                filtered.add(note);
            }
        }

        return filtered;
    }

    public boolean isFilteredListEmpty() {
        return noteList == null || noteList.isEmpty();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, bodyPreview, dateText, timeText, categoryText;
        ImageView bibleIcon, categoryIcon;

        NoteViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            bodyPreview = itemView.findViewById(R.id.note_preview);
            dateText = itemView.findViewById(R.id.created_at);
            timeText = itemView.findViewById(R.id.time_txt);
            categoryText = itemView.findViewById(R.id.category_txt);
            bibleIcon = itemView.findViewById(R.id.bible);
            categoryIcon = itemView.findViewById(R.id.category_icon);
        }
    }
}