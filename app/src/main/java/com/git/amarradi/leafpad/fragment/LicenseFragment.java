package com.git.amarradi.leafpad.fragment;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.git.amarradi.leafpad.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicenseFragment extends Fragment {

    public LicenseFragment() {
        super(R.layout.fragment_license);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textView = view.findViewById(R.id.text_licenses);
        textView.setText(loadLicenseText());
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        if (requireActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) requireActivity();

            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(R.string.open_source_licenses);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private String loadLicenseText() {
        StringBuilder builder = new StringBuilder();

        InputStream is = null;
        BufferedReader reader = null;

        try {
            is = requireContext().getAssets().open("licenses.txt");
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

        } catch (IOException e) {
            builder.append(getString(R.string.error_loading_licenses));
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ignored) {
            }

            try {
                if (is != null) is.close();
            } catch (IOException ignored) {
            }
        }

        return builder.toString();
    }
}