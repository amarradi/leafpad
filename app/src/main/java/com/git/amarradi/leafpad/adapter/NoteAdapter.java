package com.git.amarradi.leafpad.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.MainActivity;
import com.git.amarradi.leafpad.Note;
import com.git.amarradi.leafpad.NoteEditActivity;
import com.git.amarradi.leafpad.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context context;
    private List<Note> noteList;
    private boolean showOnlyHidden = false;
    private List<Note> fullNoteList = new ArrayList<>();
    private boolean isGridMode = false;


    private final static String BIBLEVERSE_URL_REGEX = "(?i)\\b(?:https?://)?(?:www\\.)?(bible\\.(com|org)|bibleserver\\.com)(/\\S*)?";

    public NoteAdapter(Context context, List<Note> noteList) {
        this.context = context;
        this.noteList = noteList;
        this.noteList = filterNotes(showOnlyHidden);
    }

    public void setLayoutMode(boolean isGrid) {
        this.isGridMode = isGrid;
    }
    @Override
    public int getItemViewType(int position) {
        // Wenn Grid-Ansicht aktiv ist, dann Rückgabe 1
        // Wenn Listen-Ansicht aktiv ist, dann Rückgabe 0
        if (isGridMode) {
            return 1; // das steht für das Grid-Layout
        } else {
            return 0; // das steht für das Listen-Layout
        }
    }



    public void setShowOnlyHidden(boolean showHidden) {
        this.showOnlyHidden = showHidden;
        this.noteList = filterNotes(showHidden);
        notifyDataSetChanged();
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


    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
         if (viewType == 1) {
             view = LayoutInflater.from(context).inflate(R.layout.note_grid_item, parent, false);
         } else {
             view = LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
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
        if (!TextUtils.isEmpty(note.getCategory())) {
            holder.categoryText.setVisibility(View.VISIBLE);
            holder.categoryIcon.setVisibility(View.VISIBLE);
            holder.categoryText.setText(note.getCategory());
            cardView.setStrokeColor(context.getResources().getColor(R.color.md_theme_recipe, null));
        } else {
            cardView.setStrokeColor(context.getResources().getColor(R.color.md_theme_primaryContainer, null));
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
            Intent intent = new Intent(context, NoteEditActivity.class);
            intent.putExtra(MainActivity.EXTRA_NOTE_ID, note.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public void updateNotes(List<Note> newNotes) {
        this.fullNoteList = newNotes;
        this.noteList = filterNotes(showOnlyHidden);
        notifyDataSetChanged();
    }
    public boolean isFilteredListEmpty() {
        return noteList == null || noteList.isEmpty();
    }


    static class NoteViewHolder extends RecyclerView.ViewHolder {
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