package de.danoeh.antennapod.core.storage;

/**
 * <pre>
 *     author : whatsthat
 *     date   : 28/05/2023
 *     desc   : xxxx 描述
 * </pre>
 */
public class WordNoteClickedEvent {
    public WordNote wordNote;

    public int position;

    public WordNoteClickedEvent(WordNote note, int position){
        this.wordNote = note;
        this.position = position;
    }
}
