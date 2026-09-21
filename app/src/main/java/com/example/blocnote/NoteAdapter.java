package com.example.blocnote;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteLongClick(Note note);
    }

    private List<Note> listeNotes;
    private OnNoteClickListener listener;

    public NoteAdapter(List<Note> listeNotes, OnNoteClickListener listener) {
        this.listeNotes = listeNotes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vue = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(vue);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = listeNotes.get(position);
        holder.textTitre.setText(note.getTitre());

        SimpleDateFormat format = new SimpleDateFormat("d MMM yyyy HH:mm", Locale.FRENCH);
        holder.textDate.setText(format.format(note.getDateModification()));

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));

        holder.itemView.setOnLongClickListener(v -> {
            listener.onNoteLongClick(note);
            return true; // true = l'événement est "consommé", pas de comportement par défaut
        });
    }

    @Override
    public int getItemCount() {
        return listeNotes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textTitre;
        TextView textDate;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitre = itemView.findViewById(R.id.textTitre);
            textDate = itemView.findViewById(R.id.textDate);
        }
    }

}