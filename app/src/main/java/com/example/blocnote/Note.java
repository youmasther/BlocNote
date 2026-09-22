package com.example.blocnote;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String titre;
    private String contenu;
    private long dateModification;
    private boolean estSupprimee;

    public Note(String titre, String contenu) {
        this.titre = titre;
        this.contenu = contenu;
        this.dateModification = System.currentTimeMillis();
        this.estSupprimee = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public long getDateModification() { return dateModification; }
    public void setDateModification(long dateModification) { this.dateModification = dateModification; }
    public boolean isEstSupprimee() { return estSupprimee; }
    public void setEstSupprimee(boolean estSupprimee) { this.estSupprimee = estSupprimee; }
}