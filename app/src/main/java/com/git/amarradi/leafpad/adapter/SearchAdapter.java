package com.git.amarradi.leafpad.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.model.Note;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    private List<Note> notes;
    private final OnNoteClickListener listener;

    public SearchAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.title.setText(note.getTitle() != null ? note.getTitle() : "(kein Titel)");

        String preview = note.getBody() != null ? note.getBody() : "";
        if (preview.length() > 100) preview = preview.substring(0, 100) + "...";
        holder.body.setText(preview);

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, body;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.search_result_title);
            body = itemView.findViewById(R.id.search_result_body);
        }
    }
}
