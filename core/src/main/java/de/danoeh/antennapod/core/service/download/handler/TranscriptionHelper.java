package de.danoeh.antennapod.core.service.download.handler;

/**
 * <pre>
 *     author : whatsthat
 *     date   : 09/05/2023
 *     desc   : xxxx 描述
 * </pre>
 */
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

public class TranscriptionHelper {

    private static final String TAG = "TranscriptionHelper";
    private static final String TRANSCRIPTION_DIR = "transcription";

    private static final String FRAGMENT2REVIEW_DIR ="fragments2review";

    private static final String POSITIONS2REVIEW_DIR ="positions2review";

    public static HashSet<Integer> loadFragments2ReviewRecord(Context context, long id) {

        try {
            File recordDirectory = new File(context.getFilesDir(), FRAGMENT2REVIEW_DIR);
            File recordFile = new File(recordDirectory, fragment2reviewRecordName(id));
            FileInputStream fis = new FileInputStream(recordFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Set<Integer> set = (Set<Integer>) ois.readObject();
            ois.close();
            fis.close();
            return (HashSet<Integer>)set;
        } catch(Exception ioe) {
            return new HashSet<Integer>();
        }
    }

    public static HashSet<Integer> updateFragment2Review(Context context, HashSet<Integer> fragmentSet, long id, int fragment, boolean add2review){

        try {
            File directory = new File(context.getFilesDir(), FRAGMENT2REVIEW_DIR);
            if (!directory.exists()) {
                directory.mkdir();
            }
            File file = new File(directory, fragment2reviewRecordName(id));
            if(fragmentSet == null){
                fragmentSet = new HashSet<Integer>();
            }
            if(add2review){
                fragmentSet.add(fragment);
            }else{
                fragmentSet.remove(fragment);
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fragmentSet);
            oos.close();
            fos.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return  fragmentSet;
    }


    private static String fragment2reviewRecordName(long id) {
        return id + "_fragments_2_review.json";
    }



    public static HashSet<Integer> loadPositions2ReviewRecord(Context context, long id) {

        try {
            File recordDirectory = new File(context.getFilesDir(), POSITIONS2REVIEW_DIR);
            File recordFile = new File(recordDirectory, fragment2reviewRecordName(id));
            FileInputStream fis = new FileInputStream(recordFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Set<Integer> set = (Set<Integer>) ois.readObject();
            ois.close();
            fis.close();
            return (HashSet<Integer>)set;
        } catch(Exception ioe) {
            return new HashSet<Integer>();
        }
    }

    public static HashSet<Integer> updatePositions2Review(Context context, HashSet<Integer> positions, long id, int position, boolean add2review){

        try {
            File directory = new File(context.getFilesDir(), POSITIONS2REVIEW_DIR);
            if (!directory.exists()) {
                directory.mkdir();
            }
            File file = new File(directory, fragment2reviewRecordName(id));
            if(positions == null){
                positions = new HashSet<Integer>();
            }
            if(add2review){
                positions.add(position);
            }else{
                positions.remove(position);
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(positions);
            oos.close();
            fos.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return  positions;
    }


    private static String positions2reviewRecordName(long id) {
        return id + "_positions_2_review.json";
    }


    private static String transcriptFileName(long id, int fragment){
        return id + "_" + fragment + ".txt";
    }

    public static boolean saveTranscription(Context context, long id, int fragment, String transcription) {
        File directory = new File(context.getFilesDir(), TRANSCRIPTION_DIR);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                Log.e(TAG, "Failed to create transcription directory");
                return false;
            }
        }

        File file = new File(directory, transcriptFileName(id, fragment));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(transcription.getBytes());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing transcription to file", e);
            return false;
        }
    }

    public static String getTranscription(Context context, long id, int fragment) {
        File directory = new File(context.getFilesDir(), TRANSCRIPTION_DIR);
        File file = new File(directory, transcriptFileName(id, fragment));

        if (!file.exists()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading transcription from file", e);
            return "";
        }

        return sb.toString();
    }
}
