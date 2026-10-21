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
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EditionActivity extends AppCompatActivity {

    private AppDatabase db;
    private EditText editTitre;
    private EditText editContenu;
    private Note noteActuelle;

    private final Deque<String> pileUndo = new ArrayDeque<>();
    private final Deque<String> pileRedo = new ArrayDeque<>();
    private boolean enTrainDeRestaurer = false;

    private final ActivityResultLauncher<String[]> demandePermissionLocalisation =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultat -> {
            });

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
            noteActuelle =  sauvegarderNote();
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

        if (!aPermissionLocalisation()) {
            demandePermissionLocalisation.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
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

    private Note sauvegarderNote() {
        if (noteActuelle == null) return null;

        String titre = editTitre.getText().toString().trim();
        String contenu = editContenu.getText().toString().trim();

        if (titre.isEmpty() && contenu.isEmpty()) return null;

        noteActuelle.setTitre(titre.isEmpty() ? "Sans titre" : titre);
        noteActuelle.setContenu(contenu);
        noteActuelle.setDateModification(System.currentTimeMillis());

        // Position : uniquement si la note n'en a pas encore
        Location position = (noteActuelle.getLatitude() == null) ? obtenirPosition() : null;

        Executors.newSingleThreadExecutor().execute(() -> {
            if (position != null) {
                noteActuelle.setLatitude(position.getLatitude());
                noteActuelle.setLongitude(position.getLongitude());
                noteActuelle.setLocalisation(nommerPosition(position)); // réseau : thread secondaire obligatoire
            }

            if (noteActuelle.getId() == 0) {
                db.noteDao().inserer(noteActuelle);
                noteActuelle = db.noteDao().getLastNote();
            } else {
                db.noteDao().mettreAJour(noteActuelle);
            }
        });
        return noteActuelle;
    }

    private boolean aPermissionLocalisation() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private Location obtenirPosition() {
        if (!aPermissionLocalisation()) return null;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location meilleure = null;

        for (String fournisseur : lm.getProviders(true)) { // true = seulement les fournisseurs activés
            try {
                Location l = lm.getLastKnownLocation(fournisseur);
                if (l != null && (meilleure == null || l.getTime() > meilleure.getTime())) {
                    meilleure = l;
                }
            } catch (SecurityException e) {

            }
        }
        return meilleure;
    }

    private String nommerPosition(Location position) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> adresses = geocoder.getFromLocation(position.getLatitude(), position.getLongitude(), 1);
            if (adresses != null && !adresses.isEmpty() && adresses.get(0).getLocality() != null) {
                return adresses.get(0).getLocality();
            }
        } catch (IOException e) {

        }
        return String.format(Locale.US, "%.4f, %.4f", position.getLatitude(), position.getLongitude());
    }

}
