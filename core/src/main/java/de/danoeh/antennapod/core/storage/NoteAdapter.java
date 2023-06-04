package de.danoeh.antennapod.core.storage;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.util.TimeAgo;


public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private List<WordNote> noteList;

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewWord;

        public TextView noteReviewedAt;
        public TextView textViewTranslation;
        public ImageView noteDelete;
        private WordNote wordNote;

        public RelativeLayout wordLayout;

        public NoteViewHolder(View v) {
            super(v);
            textViewWord = v.findViewById(R.id.note_word);
            textViewTranslation = v.findViewById(R.id.note_translation);
            noteDelete = v.findViewById(R.id.note_delete);
            noteReviewedAt = v.findViewById(R.id.note_reviewed_at);
            wordLayout = v.findViewById(R.id.word_layout);
        }
    }

    public NoteAdapter(List<WordNote> noteList) {
        this.noteList = noteList;
    }

    public void setNoteList(List<WordNote> noteList){
        this.noteList = noteList;
    }

    @Override
    public NoteAdapter.NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.word_note_item, parent, false);
        return new NoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        holder.wordNote = noteList.get(position);
        holder.textViewWord.setText(holder.wordNote.getWord());
        holder.textViewTranslation.setText(holder.wordNote.getTranslation());
        holder.noteReviewedAt.setText(TimeAgo.getTimeAgo(holder.itemView.getContext(), holder.wordNote.reviewed_at));

        holder.noteDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Here you would call your method to delete the item from the database.
            }
        });

        holder.wordLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WordNoteClickedEvent clickedEvent = new WordNoteClickedEvent(holder.wordNote, holder.getAbsoluteAdapterPosition());
                EventBus.getDefault().post(clickedEvent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }
}
