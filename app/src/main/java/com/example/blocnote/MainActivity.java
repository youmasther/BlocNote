package com.example.blocnote;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        recyclerView = findViewById(R.id.recyclerNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton boutonAjouter = findViewById(R.id.boutonAjouter);
        boutonAjouter.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        chargerNotes(); // on recharge à chaque retour sur cet écran
    }

    private void chargerNotes() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> notes = db.noteDao().getNotesActives();
            runOnUiThread(() -> {
                NoteAdapter adapter = new NoteAdapter(notes, note -> {
                    Intent intent = new Intent(MainActivity.this, EditionActivity.class);
                    intent.putExtra("noteId", note.getId());
                    startActivity(intent);
                });
                recyclerView.setAdapter(adapter);
            });
        });
    }
}