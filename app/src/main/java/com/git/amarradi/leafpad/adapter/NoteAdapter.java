package com.git.amarradi.leafpad.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.MainActivity;
import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.helper.ColorUtilsHelper;
import com.git.amarradi.leafpad.model.CategoryEntity;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.model.ReleaseNote;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
    private final NoteViewModel noteViewModel;
    private final LifecycleOwner lifecycleOwner;
    private java.util.Map<String, List<CategoryEntity>> categoriesByNoteId = new java.util.HashMap<>();



    public enum LayoutMode {
        LIST, GRID
    }

    private final static String BIBLEVERSE_URL_REGEX = "(?i)\\b(?:https?://)?(?:www\\.)?(bible\\.(com|org)|bibleserver\\.com)(/\\S*)?";
    private static final Pattern BIBLE_PATTERN = Pattern.compile(BIBLEVERSE_URL_REGEX);

    // Obergrenze für den Vorschautext. Die Zeilenbegrenzung (maxLines) und das "…" am Ende
    // übernimmt das TextView; diese Grenze verhindert nur, dass sehr lange Notizen komplett
    // in jede Karte gesetzt werden.
    private static final int PREVIEW_MAX_CHARS = 1000;

    public NoteAdapter(
            Context context,
            List<Note> noteList,
            MainActivity.NoteClickListener listener,
            OnReleaseNoteCloseListener releaseNoteCloseListener,
            NoteViewModel noteViewModel,
            LifecycleOwner lifecycleOwner
    ) {
        this.context = context;
        this.noteList = noteList;
        this.fullNoteList = noteList;
        this.listener = listener;
        this.releaseNoteCloseListener = releaseNoteCloseListener;
        this.noteViewModel = noteViewModel;
        this.lifecycleOwner = lifecycleOwner;
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


        // ------------------------------------------------------------
        // ReleaseNotes (unverändert)
        // ------------------------------------------------------------
        if (getItemViewType(position) == VIEWTYPE_RELEASE_NOTE && item instanceof ReleaseNote) {
            ((ReleaseNoteViewHolder) holder)
                    .bind((ReleaseNote) item, releaseNoteCloseListener);
            return;
        }

        // ------------------------------------------------------------
        // NOTE
        // ------------------------------------------------------------
        if (!(item instanceof Note)) {
            return;
        }

        Note note = (Note) item;
        NoteViewHolder noteHolder = (NoteViewHolder) holder;

        // ------------------------------------------------------------
        // Titel
        // ------------------------------------------------------------
        noteHolder.titleText.setText(note.getTitle());

        // ------------------------------------------------------------
        // Body-Vorschau
        // ------------------------------------------------------------
        String fullBody = note.getBody() != null ? note.getBody() : "";
        String preview = fullBody.trim();
        if (preview.length() > PREVIEW_MAX_CHARS) {
            preview = preview.substring(0, PREVIEW_MAX_CHARS) + "…";
        }
        noteHolder.bodyPreview.setText(preview);
        final boolean hasPreview = !preview.isEmpty();

        // ------------------------------------------------------------
        // Datum / Uhrzeit
        // ------------------------------------------------------------
        noteHolder.dateText.setText(note.getDate());
        noteHolder.timeText.setText(note.getTime());

        // ------------------------------------------------------------
        // Kategorien (NEU – sauber)
        // ------------------------------------------------------------
        ChipGroup chipGroup = noteHolder.categoryChipGroup;

        List<CategoryEntity> categories = categoriesByNoteId.get(note.getId());
        bindChips(chipGroup, categories);

        // ------------------------------------------------------------
        // Bibel-Link-Erkennung
        // ------------------------------------------------------------
        Matcher matcher = BIBLE_PATTERN.matcher(fullBody);
        noteHolder.bibleIcon.setVisibility(matcher.find() ? View.VISIBLE : View.GONE);

        // ------------------------------------------------------------
        // Clicks
        // ------------------------------------------------------------
        noteHolder.itemView.setOnClickListener(
                v -> listener.onNoteClicked(note)
        );

        noteHolder.itemView.setOnLongClickListener(v -> {
            listener.onNoteIconClicked(note, noteHolder.itemView);
            return true; // Event konsumiert, kein zusätzlicher Klick-Effekt (z. B. Ripple-Konflikt)
        });

        // Auf-/Zuklappen der Vorschau
        // Hinweis: Leafpad.getCollapsedNotes() enthält die IDs der AUFGEKLAPPTEN Notizen
        // (der Name ist irreführend). Sichtbarkeit und Pfeil nutzen dieselbe Quelle.
        Leafpad leafpad = Leafpad.getInstance();
        boolean isExpanded = hasPreview && leafpad.getCollapsedNotes().contains(note.getId());
        noteHolder.expandableContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        noteHolder.expandArrow.setRotation(isExpanded ? 180f : 0f);
        // Ohne Text gibt es nichts aufzuklappen
        noteHolder.expandArrow.setVisibility(hasPreview ? View.VISIBLE : View.GONE);

        noteHolder.expandArrow.setOnClickListener(v -> {
            boolean nowExpanded = !leafpad.getCollapsedNotes().contains(note.getId());
            leafpad.setCollapsedNotes(note.getId(), nowExpanded);
            noteHolder.expandableContent.setVisibility(nowExpanded ? View.VISIBLE : View.GONE);
            noteHolder.expandArrow.animate()
                    .rotation(nowExpanded ? 180f : 0f)
                    .setDuration(200)
                    .start();
        });
        // Größere Touch-Fläche für den kleinen Pfeil-Button, ohne die Optik zu ändern
        if (!hasPreview) {
            // Recycelter Holder: alte TouchDelegate entfernen, sonst reagiert die Karte links oben
            android.view.View arrowParent = (android.view.View) noteHolder.expandArrow.getParent();
            if (arrowParent != null) arrowParent.setTouchDelegate(null);
            return;
        }
        noteHolder.expandArrow.post(() -> {
            android.view.View parent = (android.view.View) noteHolder.expandArrow.getParent();
            if (parent == null) return;

            android.graphics.Rect rect = new android.graphics.Rect();
            noteHolder.expandArrow.getHitRect(rect);

            int extraPx = (int) (12 * noteHolder.itemView.getContext().getResources().getDisplayMetrics().density);
            rect.top -= extraPx;
            rect.bottom += extraPx;
            rect.left -= extraPx;
            rect.right += extraPx;

            parent.setTouchDelegate(new android.view.TouchDelegate(rect, noteHolder.expandArrow));
        });
    }

    private void bindChips(ChipGroup chipGroup, List<CategoryEntity> categories) {
        // Nur die alten Chips entfernen. In der Listenansicht liegt in der ChipGroup
        // zusätzlich der Datum/Uhrzeit-Block, der stehen bleiben muss.
        for (int i = chipGroup.getChildCount() - 1; i >= 0; i--) {
            if (chipGroup.getChildAt(i) instanceof Chip) {
                chipGroup.removeViewAt(i);
            }
        }

        if (categories == null || categories.isEmpty()) {
            // Gibt es noch andere Kinder (Datum/Uhrzeit), bleibt die Gruppe sichtbar
            chipGroup.setVisibility(chipGroup.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            return;
        }

        chipGroup.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(chipGroup.getContext());

        for (CategoryEntity c : categories) {
            Chip chip = (Chip) inflater.inflate(
                    R.layout.item_category_chip,
                    chipGroup,
                    false
            );

            chip.setText(c.name);
            //chip.setTextSize(10f);

            float d = chip.getResources().getDisplayMetrics().density; // dp → px

// Höhe
            chip.setChipMinHeight(18 * d);
            chip.setMinHeight((int) (18 * d));

// Abstand Text ↔ Rand links/rechts
            chip.setChipStartPadding(2 * d);
            chip.setChipEndPadding(2 * d);
            chip.setTextStartPadding(2 * d);
            chip.setTextEndPadding(2 * d);
//            chip.setChipMinHeight(ColorUtilsHelper.dpToPx(chip.getContext(), 18));
//            chip.setMinHeight(ColorUtilsHelper.dpToPx(chip.getContext(), 18));

            int baseColor;
            try {
                baseColor = Color.parseColor(c.colorHex);
            } catch (Exception e) {
                baseColor = Color.GRAY;
            }

            int bgColor = com.git.amarradi.leafpad.helper.ColorUtilsHelper.lightenColor(baseColor, 0.35f);

            chip.setChipStrokeWidth(
                    com.git.amarradi.leafpad.helper.ColorUtilsHelper.dpToPx(chip.getContext(), 1)
            );
            chip.setChipStrokeColor(ColorStateList.valueOf(baseColor));
            chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));

            boolean darkBg = androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) < 0.5;
            int textColor = darkBg ? Color.WHITE : Color.BLACK;
            chip.setTextColor(textColor);

            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setCloseIconVisible(false);
            chip.setEnsureMinTouchTargetSize(false);

            chipGroup.addView(chip);
        }
    }



    // ViewHolder für ReleaseNote-Header
    public static class ReleaseNoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, date, time;
        ImageButton closeButton;
        View expandableContent;



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
        ImageButton expandArrow;

        View expandableContent;       // neu
        ChipGroup categoryChipGroup;

        NoteViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            bodyPreview = itemView.findViewById(R.id.note_preview);
            dateText = itemView.findViewById(R.id.created_at);
            timeText = itemView.findViewById(R.id.time_txt);
            //categoryText = itemView.findViewById(R.id.category_txt);
            bibleIcon = itemView.findViewById(R.id.bible);
            //categoryIcon = itemView.findViewById(R.id.category_icon);
            expandArrow = itemView.findViewById(R.id.expand_arrow);
            expandableContent = itemView.findViewById(R.id.expandable_content);
            categoryChipGroup = itemView.findViewById(R.id.category_chip_group);
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

    public void setCategoriesByNoteId(java.util.Map<String, List<CategoryEntity>> map) {
        categoriesByNoteId = (map != null) ? map : new java.util.HashMap<>();
        notifyDataSetChanged();
    }

}
