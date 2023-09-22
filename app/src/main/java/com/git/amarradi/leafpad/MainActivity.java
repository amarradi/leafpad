package com.git.amarradi.leafpad;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_NOTE_ID = "com.git.amarradi.leafpad";

    private final static String COUNT = "startcounter";
    private final static Integer count = 10;

    public ArrayList<Note> notes;
    public SimpleAdapter adapter;
    public ListView listView;

    List<Map<String, String>> data = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = findViewById(R.id.toolbar);


        setSupportActionBar(toolbar);

        updateDataset();

        adapter = new SimpleAdapter(this, data,
                R.layout.note_list_item,
                new String[] {"title", "body", "date", "time", "create"},
                new int[]{R.id.text1,
                        R.id.text2,
                        R.id.date_txt,
                        R.id.time_txt,
                        R.id.created_at});


        listView = findViewById(R.id.note_list_view);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((adapter, v, position, id) -> {
           // Intent intent = new Intent(MainActivity.this, NoteReadActivity.class);
            //intent.putExtra(EXTRA_NOTE_ID, notes.get(position).getId());
            //startActivity(intent);
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(EXTRA_NOTE_ID, notes.get(position).getId());
            startActivity(intent);
        });


        FloatingActionButton fab = findViewById(R.id.fab_action_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(EXTRA_NOTE_ID, Note.makeId());
            startActivity(intent);
        });
        countStart();
    }

    private void countStart() {
        SharedPreferences sharedPreferences = getSharedPreferences(COUNT,Context.MODE_PRIVATE);
        Integer integer = sharedPreferences.getInt(COUNT,0);
        integer++;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(COUNT,integer);
        editor.apply();
        if (integer.equals(count)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.satisfied);
            builder.setMessage(R.string.rate_the_app)
                    .setPositiveButton(R.string.feedback_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            showFeedbackDialog();
                        }
                    })
                    .setNegativeButton(R.string.feedback_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void showFeedbackDialog() {
        ReviewManager manager = ReviewManagerFactory.create(getApplicationContext());
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
           if(task.isSuccessful()) {
               ReviewInfo reviewInfo = task.getResult();
               Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
               flow.addOnCompleteListener(voidtask -> {
                   Toast.makeText(this, getString(R.string.review_seved), Toast.LENGTH_SHORT).show();
               });

           } else {
               @ReviewErrorCode int reviewErrorCode = ((ReviewException) task.getException()).getErrorCode();
           }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void updateListView() {
        updateDataset();
        ((SimpleAdapter) listView.getAdapter()).notifyDataSetChanged();
    }


    public void updateDataset() {
        notes = Leaf.loadAll(this);

        for (Note note : notes) {
            if (note.getTitle().isEmpty()) {
                String[] splits = note.getBody().split(" ");

                StringBuilder stringBuilder = new StringBuilder();
                int substringLength = 0;
                for (int i = 0; i < splits.length; i++) {
                    if (substringLength + splits[i].length() > 25) {
                        break;
                    }
                    substringLength += splits[i].length();
                    stringBuilder.append(splits[i]);
                    if (i + 1 != splits.length) {
                        if (!(substringLength + splits[i + 1].length() > 25)) {
                            stringBuilder.append(" ");
                        }
                    }
                }
                if (!(note.getBody().length() == stringBuilder.toString().length())) {
                    stringBuilder.append("...");
                }
                note.setTitle(stringBuilder.toString());
            }
            if(note.getDate().isEmpty() || note.getTime().isEmpty()) {
                note.setNotetime();
                note.setNotedate();
            }

        }

        data.clear();
        for (Note note : notes) {
            Map<String, String> datum = new HashMap<>(4);
            datum.put("title", note.getTitle());
            datum.put("body", note.getBody());
            datum.put("date", note.getDate());
            datum.put("time", note.getTime());
            datum.put("create",note.getCreateDate());
            data.add(datum);
        }
    }
/*
    public String joinArray(String[] array, char delimiter) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            stringBuilder.append(array[i]);
            if (i < array.length - 1) {
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.toString();
    }

 */
}
