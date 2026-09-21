package com.example.blocnote;

import android.content.Intent;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import java.util.List;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView recyclerView;
    private DrawerLayout drawerLayout;
    private TextView textTitreEcran;
    private boolean afficherCorbeille = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        recyclerView = findViewById(R.id.recyclerNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        drawerLayout = findViewById(R.id.drawerLayout);
        textTitreEcran = findViewById(R.id.textTitreEcran);
        ImageButton boutonMenu = findViewById(R.id.boutonMenu);
        NavigationView navigationView = findViewById(R.id.navigationView);

        boutonMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int idItem = item.getItemId();

            if (idItem == R.id.menuMesNotes) {
                afficherCorbeille = false;
                textTitreEcran.setText("Mes notes");
                chargerNotes();
            } else if (idItem == R.id.menuCorbeille) {
                afficherCorbeille = true;
                textTitreEcran.setText("Corbeille");
                chargerNotes();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        FloatingActionButton boutonAjouter = findViewById(R.id.boutonAjouter);
        boutonAjouter.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        chargerNotes();
    }

    private void chargerNotes() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> notes = afficherCorbeille
                    ? db.noteDao().getNotesSupprimees()
                    : db.noteDao().getNotesActives();

            runOnUiThread(() -> {
                NoteAdapter adapter = new NoteAdapter(notes, new NoteAdapter.OnNoteClickListener() {
                    @Override
                    public void onNoteClick(Note note) {
                        if (afficherCorbeille) return; // pas d'édition depuis la corbeille
                        Intent intent = new Intent(MainActivity.this, EditionActivity.class);
                        intent.putExtra("noteId", note.getId());
                        startActivity(intent);
                    }

                    @Override
                    public void onNoteLongClick(Note note) {
                        if (afficherCorbeille) {
                            afficherDialogueCorbeille(note);
                        } else {
                            afficherDialogueSuppression(note);
                        }
                    }
                });
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void afficherDialogueSuppression(Note note) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Supprimer la note")
                .setMessage("Envoyer \"" + note.getTitre() + "\" à la corbeille ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.noteDao().mettreALaCorbeille(note.getId());
                        runOnUiThread(this::chargerNotes);
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void afficherDialogueCorbeille(Note note) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(note.getTitre())
                .setItems(new CharSequence[]{"Restaurer", "Supprimer définitivement"}, (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (which == 0) {
                            db.noteDao().restaurerDeLaCorbeille(note.getId());
                        } else {
                            db.noteDao().supprimerDefinitivement(note.getId());
                        }
                        runOnUiThread(this::chargerNotes);
                    });
                })
                .show();
    }
}