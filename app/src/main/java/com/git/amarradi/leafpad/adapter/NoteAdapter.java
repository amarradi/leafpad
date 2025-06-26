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

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.model.Note;
import com.google.android.material.card.MaterialCardView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 9999; // eindeutiger Wert für Header
    private static final int VIEW_TYPE_NOTE = 0;

    private final Context context;
    private List<Note> noteList = new ArrayList<>();
    private boolean showReleaseNoteHeader = false;

    // Für die Anzeige im Header
    private String releaseNoteTitle = "";
    private String releaseNoteContent = "";
    private String releaseNoteDate = "";
    private String releaseNoteTime = "";

    // Listener für Klicks
    private NoteClickListener noteClickListener;
    private OnReleaseNoteCloseListener releaseNoteCloseListener;

    // Listener-Interfaces
    public interface NoteClickListener {
        void onNoteClicked(Note note);
        void onNoteIconClicked(Note note, View anchor);
    }
    public interface OnReleaseNoteCloseListener {
        void onReleaseNoteClose();
    }

    // Konstruktor
    public NoteAdapter(Context context, NoteClickListener noteClickListener) {
        this.context = context;
        this.noteClickListener = noteClickListener;
    }

    // Setter für ReleaseNotes
    public void setReleaseNoteHeader(String title, String content, String date, String time, boolean showHeader) {
        this.releaseNoteTitle = title != null ? title : "";
        this.releaseNoteContent = content != null ? content : "";
        this.releaseNoteDate = date != null ? date : "";
        this.releaseNoteTime = time != null ? time : "";
        this.showReleaseNoteHeader = showHeader;
        notifyDataSetChanged();
    }

    public void setOnReleaseNoteCloseListener(OnReleaseNoteCloseListener listener) {
        this.releaseNoteCloseListener = listener;
    }

    // Setter für Notizenliste
    public void setNotes(List<Note> notes) {
        if (notes == null) {
            this.noteList = new ArrayList<>();
        } else {
            this.noteList = new ArrayList<>(notes);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (showReleaseNoteHeader) {
            return noteList.size() + 1;
        } else {
            return noteList.size();
        }
    }

    @Override
    public long getItemId(int position) {
        if (showReleaseNoteHeader) {
            if (position == 0) {
                return RecyclerView.NO_ID;
            }
            int notePosition = position - 1;
            if (notePosition >= 0 && notePosition < noteList.size()) {
                Note note = noteList.get(notePosition);
                if (note.getId() != null) {
                    return UUID.nameUUIDFromBytes(note.getId().getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
                }
            }
        } else {
            if (position >= 0 && position < noteList.size()) {
                Note note = noteList.get(position);
                if (note.getId() != null) {
                    return UUID.nameUUIDFromBytes(note.getId().getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
                }
            }
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemViewType(int position) {
        if (showReleaseNoteHeader) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else {
                return VIEW_TYPE_NOTE;
            }
        } else {
            return VIEW_TYPE_NOTE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.release_note_item, parent, false);
            return new ReleaseNoteViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.note_list_item, parent, false);
            return new NoteViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (showReleaseNoteHeader) {
            if (position == 0) {
                ReleaseNoteViewHolder h = (ReleaseNoteViewHolder) holder;
                h.title.setText(releaseNoteTitle);
                h.content.setText(releaseNoteContent);
                h.date.setText(releaseNoteDate);
                h.time.setText(releaseNoteTime);
                h.closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (releaseNoteCloseListener != null) {
                            releaseNoteCloseListener.onReleaseNoteClose();
                        }
                    }
                });
                return;
            }
        }

        int notePosition;
        if (showReleaseNoteHeader) {
            notePosition = position - 1;
        } else {
            notePosition = position;
        }

        if (notePosition < 0 || notePosition >= noteList.size()) {
            // Sicherheitsabbruch
            return;
        }

        Note note = noteList.get(notePosition);
        NoteViewHolder h = (NoteViewHolder) holder;

        // Titel
        h.titleText.setText(note.getTitle());

        // Vorschautext für Body
        String body = note.getBody() != null ? note.getBody() : "";
        if (body.length() > 150) {
            body = body.substring(0, 150) + "...";
        }
        h.bodyPreview.setText(body);

        h.dateText.setText(note.getDate());
        h.timeText.setText(note.getTime());

        if (note.getCategory() != null && !note.getCategory().isEmpty()) {
            h.categoryText.setVisibility(View.VISIBLE);
            h.categoryText.setText(note.getCategory());
            h.categoryIcon.setVisibility(View.VISIBLE);
        } else {
            h.categoryText.setVisibility(View.GONE);
            h.categoryIcon.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noteClickListener != null) {
                    noteClickListener.onNoteClicked(note);
                }
            }
        });

        h.actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noteClickListener != null) {
                    noteClickListener.onNoteIconClicked(note, h.actionButton);
                }
            }
        });
    }

    // ViewHolder für Notes
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, bodyPreview, dateText, timeText, categoryText;
        ImageView categoryIcon;
        ImageButton actionButton;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            bodyPreview = itemView.findViewById(R.id.note_preview);
            dateText = itemView.findViewById(R.id.created_at);
            timeText = itemView.findViewById(R.id.time_txt);
            categoryText = itemView.findViewById(R.id.category_txt);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            actionButton = itemView.findViewById(R.id.image_button);
        }
    }

    // ViewHolder für ReleaseNote-Header
    public static class ReleaseNoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, date, time;
        ImageButton closeButton;

        public ReleaseNoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.release_title);
            content = itemView.findViewById(R.id.release_content);
            date = itemView.findViewById(R.id.release_date);
            time = itemView.findViewById(R.id.release_time);
            closeButton = itemView.findViewById(R.id.release_note_close_button);
        }
    }
}
