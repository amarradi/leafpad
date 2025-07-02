package com.git.amarradi.leafpad.util;

import androidx.recyclerview.widget.DiffUtil;

import com.git.amarradi.leafpad.model.Note;

import java.util.List;
import java.util.Objects;

public class NoteDiffCallback extends DiffUtil.Callback {
    private final List<Note> oldList;
    private final List<Note> newList;

    public NoteDiffCallback(List<Note> oldList, List<Note> newList) {
        this.oldList = oldList;
        this.newList = newList;
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
        return Objects.equals(oldList.get(oldItemPosition).getId(), newList.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
