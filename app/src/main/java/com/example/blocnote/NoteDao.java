package com.example.blocnote;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    void inserer(Note note);

    @Update
    void mettreAJour(Note note);

    @Query("SELECT * FROM notes WHERE estSupprimee = 0 ORDER BY dateModification DESC")
    List<Note> getNotesActives();

    @Query("SELECT * FROM notes WHERE estSupprimee = 1 ORDER BY dateModification DESC")
    List<Note> getNotesSupprimees();

    @Query("SELECT * FROM notes WHERE id = :noteId")
    Note getNoteParId(int noteId);
}