package com.git.amarradi.leafpad.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MasonrySpacingDecoration extends RecyclerView.ItemDecoration {

    private final int verticalSpacing;
    private final int horizontalSpacing;

    public MasonrySpacingDecoration(int verticalSpacing, int horizontalSpacing) {
        this.verticalSpacing = verticalSpacing;
        this.horizontalSpacing = horizontalSpacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        // immer 5dp oben und unten
        outRect.top = verticalSpacing;
        outRect.bottom = verticalSpacing;

        // links/rechts gleichmäßig verteilen
        outRect.left = horizontalSpacing / 2;
        outRect.right = horizontalSpacing / 2;
    }
}
