package com.example.blocnote;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;

public class EditionActivity extends AppCompatActivity {

    private AppDatabase db;
    private EditText editTitre;
    private EditText editContenu;
    private Note noteActuelle;

    private final Deque<String> pileUndo = new ArrayDeque<>();
    private final Deque<String> pileRedo = new ArrayDeque<>();
    private boolean enTrainDeRestaurer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edition);

        db = AppDatabase.getInstance(this);
        editTitre = findViewById(R.id.editTitre);
        editContenu = findViewById(R.id.editContenu);

        ImageButton boutonRetour = findViewById(R.id.boutonRetour);
        ImageButton boutonUndo = findViewById(R.id.boutonUndo);
        ImageButton boutonRedo = findViewById(R.id.boutonRedo);
        ImageButton boutonSauvegarder = findViewById(R.id.boutonSauvegarder);

        boutonRetour.setOnClickListener(v -> finish()); // déclenche onPause -> sauvegarde auto

        boutonSauvegarder.setOnClickListener(v -> {
            sauvegarderNote();
            Toast.makeText(this, "Note sauvegardée", Toast.LENGTH_SHORT).show();
        });

        boutonUndo.setOnClickListener(v -> annuler());
        boutonRedo.setOnClickListener(v -> refaire());

        editContenu.addTextChangedListener(new TextWatcher() {
            private String texteAvantModification = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                texteAvantModification = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (enTrainDeRestaurer) return;
                pileUndo.push(texteAvantModification);
                pileRedo.clear();
            }
        });

        int noteId = getIntent().getIntExtra("noteId", -1);

        if (noteId == -1) {
            noteActuelle = new Note("", "");
        } else {
            Executors.newSingleThreadExecutor().execute(() -> {
                noteActuelle = db.noteDao().getNoteParId(noteId);
                runOnUiThread(() -> {
                    editTitre.setText(noteActuelle.getTitre());
                    editContenu.setText(noteActuelle.getContenu());
                });
            });
        }
    }

    private void annuler() {
        if (pileUndo.isEmpty()) return;
        String etatPrecedent = pileUndo.pop();
        pileRedo.push(editContenu.getText().toString());
        restaurerTexte(etatPrecedent);
    }

    private void refaire() {
        if (pileRedo.isEmpty()) return;
        String etatSuivant = pileRedo.pop();
        pileUndo.push(editContenu.getText().toString());
        restaurerTexte(etatSuivant);
    }

    private void restaurerTexte(String texte) {
        enTrainDeRestaurer = true;
        editContenu.setText(texte);
        editContenu.setSelection(texte.length());
        enTrainDeRestaurer = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        sauvegarderNote();
    }

    private void sauvegarderNote() {
        if (noteActuelle == null) return;

        String titre = editTitre.getText().toString().trim();
        String contenu = editContenu.getText().toString().trim();

        if (titre.isEmpty() && contenu.isEmpty()) return;

        noteActuelle.setTitre(titre.isEmpty() ? "Sans titre" : titre);
        noteActuelle.setContenu(contenu);
        noteActuelle.setDateModification(System.currentTimeMillis());

        Executors.newSingleThreadExecutor().execute(() -> {
            if (noteActuelle.getId() == 0) {
                db.noteDao().inserer(noteActuelle);
            } else {
                db.noteDao().mettreAJour(noteActuelle);
            }
        });
    }
}
