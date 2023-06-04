package de.danoeh.antennapod.core.storage;

public class WordNote {
    public int note_id;
    public int media_id;
    public String word;
    public String translation;
    public long created_at;
    public long updated_at;
    public long reviewed_at;
    public int difficulty_level;

    public String getWord(){
        return word;
    }

    public String getTranslation(){
        return translation;
    }
}
