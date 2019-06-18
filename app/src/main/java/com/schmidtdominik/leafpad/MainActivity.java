package com.schmidtdominik.leafpad;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_NOTE_ID = "com.mycompany.myfirstapp.NOTE_ID";

    public ArrayList<Note> notes;
    public SimpleAdapter adapter;
    public ListView listView;
    List<Map<String, String>> data = new ArrayList<Map<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateDataset();

        adapter = new SimpleAdapter(this, data,
                R.layout.note_list_item,
                new String[] {"title", "body"},
                new int[]{R.id.text1,
                        R.id.text2});

        listView = (ListView) findViewById(R.id.note_list_view);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(EXTRA_NOTE_ID, notes.get(position).getId());
                startActivity(intent);
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_action_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(EXTRA_NOTE_ID, Note.makeId());
                startActivity(intent);
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            default:
                return super.onOptionsItemSelected(item);
        }
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
        }

        data.clear();
        for (Note note : notes) {
            Map<String, String> datum = new HashMap<String, String>(2);
            datum.put("title", note.getTitle());
            datum.put("body", note.getBody());
            data.add(datum);
        }
    }

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
}
