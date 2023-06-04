package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.service.download.handler.TranscribeCallable;
import de.danoeh.antennapod.core.service.download.handler.TranscriptionHelper;
import de.danoeh.antennapod.core.storage.NoteAdapter;
import de.danoeh.antennapod.core.storage.NotesDBHelper;
import de.danoeh.antennapod.core.storage.WordNote;
import de.danoeh.antennapod.core.storage.WordNoteClickedEvent;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;
import de.danoeh.antennapod.core.util.Translator;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.event.MarkFragment2ReviewEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlayerStatus;

public class TranscriptionFragment extends Fragment {

    private  static String TAG = "TranscriptionFragment";

    private LinearLayout transcriptList = null;
    private List<TextView> transcriptLines = new ArrayList<>();
    private JSONArray transcriptArray = new JSONArray();
    private double transcriptTotalDuration = 0;

    private TextView transcriptView;
    private LinearLayout statusLayout;
    private SwitchCompat switchAutoScroll;

    private PlaybackController controller;

    private int currentAudioFragment = -1;
    private long currentMediaId = -1;

    private boolean audioDownloadRejected = false;

    private int transcriptContentHeight = 0;

    //transcription related variables
    private static final long INTERVAL_MS = 60000; // 60 seconds


    private SpannableStringBuilder transcriptStringBuilder = null;
    private ForegroundColorSpan highlightSpan = null;
    private boolean transcriptStringBuilderContentSet = false;
    private double highlightedSpanEndPosition = 0;
    private double highlightedSpanStartPosition = 0;

    private int transcriptScrollPosition = 0;


    private final Handler handler = new Handler();
    private ExecutorService executorService;

    private static final int  CODE_AUDIO_QUERY_FOUND = 1;
    private static final int  CODE_AUDIO_QUERY_NOTFOUND = 2;
    private static final int  CODE_AUDIO_DOWNLOAD_FOUND = 3;
    private static final int  CODE_AUDIO_DOWNLOAD_ACCEPTED = 4;
    private static final int  CODE_AUDIO_DOWNLOAD_REJECTED = 5;
    private static final int  CODE_TRANS_AUDIO_NOT_FOUND = 6;
    private static final int  CODE_TRANS_FOUND = 7;
    private static final int  CODE_TRANS_ACCEPTED = 8;
    private static final int  CODE_TRANS_REJECTED = 9;
    private static final int  CODE_TRANS_AUDIO_DOWNLOADING = 10;
    private static final int  CODE_TRANS_FAILED = 11;

    private static final int  AUDIO_REQUESTED = 0;
    private static final int  AUDIO_DOWNLOADING = 1;
    private static final int  AUDIO_DOWNLOADED = 2;
    private static final int  AUDIO_DOWNLOAD_PAUSED = 3;
    private static final int  AUDIO_DOWNLOAD_CANCELED = 4;
    private static final int  AUDIO_DELETED = 5;
    private static final int  AUDIO_DOWNLOAD_FAILED = 6;
    private static final int  TRANS_REQUESTED = 0;
    private static final int  TRANS_ONGOING = 1;
    private static final int  TRANS_FINISHED = 2;
    private static final int  TRANS_PAUSED = 3;
    private static final int  TRANS_CANCELED = 4;
    private static final int  TRANS_DELETED = 5;
    private static final int  TRANS_FAILED = 6;


    private TextView executedActionView;
    private TextView executedResultView;
    private TextView nextActionView;
    private TextView nextActionExecuteCountDownView;


    private CountDownTimer countDownTimer;

    private boolean fragmentDestroyed = false;

    private TranscribeService transcribeService = null;

    private TextToSpeech tts;

    private BottomNavigationView bottomNavigationView;
    private ScrollView transcriptScrollView;

    private ScrollView audioMarksScrollView;

    private HashSet<Integer> positions2Review;
    private LinearLayout marksLinearLayout;

    private NotesDBHelper notesDBHelper;


    private RecyclerView notesRecyclerView;
    private ProgressBar loadingPB;
    private NestedScrollView noteScrollView;
    private SwipeRefreshLayout noteScrollLayout;
    private NoteAdapter noteAdapter;
    private ArrayList<WordNote> noteList;

    private int loadedNotesPage = 0;
    private int notePageSize = 20;


    // Call this method to start the timer
    public void startTimer(int duration) {
        if(countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(duration * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                if(nextActionView.getText().length() > 0){
                    nextActionExecuteCountDownView.setText("," + (millisUntilFinished / 1000) + "秒后");
                }
            }
            public void onFinish() {
                Log.d("Timer", "done!");
            }
        }.start();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.transcription_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        executedActionView = view.findViewById(R.id.executedAction);
        executedResultView = view.findViewById(R.id.executedResult);
        nextActionView = view.findViewById(R.id.nextAction);
        nextActionExecuteCountDownView = view.findViewById(R.id.nextActionExecuteCountDown);

        transcriptView = view.findViewById(R.id.transcriptView);

        transcriptView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Clear the existing menu items
                menu.clear();
                // Add new item
                int groupId = 0;  // Group ID. This can be useful if you have more than one set of menu items that need to be managed separately.
                int itemId = 123; // Unique item ID.
                int order = Menu.NONE; // The order for the item (see Android Menu documentation for more info)
                CharSequence title =  getString(R.string.transcript_trans_tts);// The text for the menu item.
                menu.add(groupId, itemId, order, title);
                menu.add(0, 125, Menu.NONE, getString(R.string.transcript_trans_translate));
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Return false if nothing is done
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                CharSequence selectedText = transcriptView.getText().subSequence(transcriptView.getSelectionStart(), transcriptView.getSelectionEnd());

                if (item.getItemId() == 123) {
                    if(!tts.isSpeaking()){
                        tts.speak(selectedText, TextToSpeech.QUEUE_FLUSH, new Bundle(), "a2s");
                    }
                    mode.finish();
                    return true;
                }else if(item.getItemId() == 125) {
                    translateSelectText(selectedText.toString());
                    mode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        executorService = Executors.newSingleThreadExecutor();

        if(transcriptList != null){
            transcriptList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    transcriptContentHeight = v.getHeight(); // Fetches the height after the layout is updated
                }
            });
        }

        statusLayout = view.findViewById(R.id.statusLayout);

        transcriptStringBuilder = new SpannableStringBuilder();
        highlightSpan = new ForegroundColorSpan(Color.RED);

        tts = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                    // Set pitch level
                    tts.setPitch(1.0f); // 1.0 is normal. Lower values decrease pitch and higher values increase pitch.
                    // Set speed rate
                    tts.setSpeechRate(1.0f); // 1.0 is normal. Lower values slow down speech and higher values accelerate it.
                }
            }
        });

        bottomNavigationView = view.findViewById(R.id.transcript_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.transcript_nav_transcript:
                        audioMarksScrollView.setVisibility(View.GONE);
                        noteScrollLayout.setVisibility(View.INVISIBLE);
                        noteScrollView.setVisibility(View.INVISIBLE);
                        transcriptScrollView.setVisibility(View.VISIBLE);
                        break;
                    case R.id.transcript_nav_bookmarks:
                        audioMarksScrollView.setVisibility(View.VISIBLE);
                        noteScrollLayout.setVisibility(View.INVISIBLE);
                        noteScrollView.setVisibility(View.INVISIBLE);
                        transcriptScrollView.setVisibility(View.VISIBLE);
                        break;
                    case R.id.transcript_nav_notes:
                        audioMarksScrollView.setVisibility(View.GONE);
                        noteScrollLayout.setVisibility(View.VISIBLE);
                        noteScrollView.setVisibility(View.VISIBLE);
                        transcriptScrollView.setVisibility(View.INVISIBLE);
                        break;
                }
                return true;
            }
        });

        transcriptScrollView = view.findViewById(R.id.transcriptScrollView);
        audioMarksScrollView = view.findViewById(R.id.audioMarksScrollView);
        marksLinearLayout = view.findViewById(R.id.marksLinearLayout);

        loadingPB = view.findViewById(R.id.idPBLoading);
        notesDBHelper = new NotesDBHelper(getContext());
        notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        noteScrollView = view.findViewById(R.id.noteScrollView);
        noteScrollLayout = view.findViewById(R.id.noteScrollLayout);

        noteScrollLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ArrayList<WordNote> pageList = notesDBHelper.getWordList("reviewed_at", "asc", 0, notePageSize);
                noteList.clear();
                noteList.addAll(0, pageList);
                noteAdapter.notifyDataSetChanged();
                noteScrollLayout.setRefreshing(false);
            }
        });

        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        notesRecyclerView.setLayoutManager(manager);
        noteScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // on scroll change we are checking when users scroll as bottom.
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    loadedNotesPage = loadedNotesPage + 1;
                    int listCount = noteList.size();
                    ArrayList<WordNote> pageList = notesDBHelper.getWordList("reviewed_at", "asc", loadedNotesPage * notePageSize, notePageSize);
                    noteList.addAll(pageList);
                    noteAdapter.notifyItemRangeInserted(listCount, pageList.size());
                }
            }
        });

        noteList = notesDBHelper.getWordList("reviewed_at", "asc", 0, notePageSize);
        noteAdapter = new NoteAdapter(noteList);
        notesRecyclerView.setAdapter(noteAdapter);

    }

    private  void translateSelectText(String selectedText){
        Translator translator = new Translator(selectedText.toString());
        Future<JSONObject> future = executorService.submit(translator);
        try {
            JSONObject transResponse = future.get(5, TimeUnit.SECONDS);
            if(transResponse.getInt("resultCode") == 0 ){
                String dlgTitle = selectedText;
                String dlgMsg = transResponse.getString("transResult");
                Boolean transForSentence = transResponse.getString("transType") == "sentence";
                if(transForSentence){
                    dlgTitle = getString(R.string.transcript_trans_title);
                    dlgMsg = transResponse.getString("transResult") + "\n\n" + selectedText;
                    new AlertDialog.Builder(getContext())
                            .setTitle(dlgTitle)
                            .setMessage(dlgMsg)
                            .setPositiveButton(getString(R.string.transcript_trans_got_it), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "sentence translation result ok clicked ");
                                }
                            }).show();
                }else{

                    JSONObject transResultObj = new JSONObject(dlgMsg);
                    final String translation = transResultObj.getJSONObject("translation").getString("translation");
                    final String title = dlgTitle;

                    new AlertDialog.Builder(getContext())
                            .setTitle(title)
                            .setMessage(translation)
                            .setPositiveButton(getString(R.string.transcript_trans_add_note), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    int now = (int) System.currentTimeMillis()/1000;
                                    FeedMedia media = (FeedMedia)controller.getMedia();
                                    WordNote note = notesDBHelper.addWord((int) media.getId(), title, translation,  1);
                                    if(note != null){
                                        noteList.add(noteList.size(), note);
                                        noteAdapter.notifyItemInserted(noteList.size() - 1);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.transcript_trans_got_it, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "word translation result cancel clicked ");
                                }
                            })
                            .show();
                }
            }else{
                Log.d(TAG, "= translation failed with " + transResponse.getInt("result_code"));
            }

        } catch (Exception e) {
            Log.d(TAG, "word translation failed with " + e.toString());
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "fragment started");
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
            }
        };
        controller.init();
        EventBus.getDefault().register(this);

        if(transcribeService == null){
            transcribeService = new TranscribeService();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "fragment stopped ");
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
//        clearTranscriptList();
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if(countDownTimer != null) {
            countDownTimer.cancel();
        }
        fragmentDestroyed = true;
        super.onDestroy();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updatePosition(PlaybackPositionEvent event) {

        if (controller == null) {
            Log.d(TAG, "updatePosition without controller");
            return;
        }

        TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
        int currentPosition = converter.convert(event.getPosition());
        int duration = converter.convert(event.getDuration());

        if (currentPosition == Playable.INVALID_TIME || duration == Playable.INVALID_TIME) {
            Log.w(TAG, "Could not react to position observer update because of invalid time");
            return;
        }

        FeedMedia media = (FeedMedia)controller.getMedia();
        int fragment = event.getPosition() / 180000;
        if(currentAudioFragment != fragment ||currentMediaId != media.getId()){
            currentAudioFragment = fragment;
            audioDownloadRejected = false;
            if(currentMediaId != media.getId()){
                currentMediaId = media.getId();
                reCreatePositions2ReviewLayout();
            }
            Log.d(TAG, "playing fragment change to " + currentAudioFragment);
            Log.d(TAG, "playing media's  image location  " + media.getImageLocation());
            clearTranscriptList();
            if(!loadTranscriptFromLocal()){
                startTranscriptService();
            }
        }

        if(transcriptArray.length() == 0){
            transcriptView.setVisibility(View.INVISIBLE);
            statusLayout.setVisibility(View.VISIBLE);
            if(!handler.hasCallbacks(transcribeService)){
                if(!loadTranscriptFromLocal()){
                    Log.d(TAG, "transcript array is empty, try to get transcript again");
                    if(!audioDownloadRejected){
                        startTranscriptService();
                    }
                }
            }else{
                Log.d(TAG, "transcript array is empty, busy with getting transcript, ignored this request");
            }
            return;
        }else{
            statusLayout.setVisibility(View.INVISIBLE);
        }

        double realPosition = event.getPosition()/1000;
        Log.d(TAG, "current position: " + realPosition + ", current highlightedSpanPosition: (" + highlightedSpanStartPosition + ", " + highlightedSpanEndPosition + ")");
        if(realPosition > highlightedSpanEndPosition || realPosition < highlightedSpanStartPosition){
            double[] spanPositions = highlightCurrentPosition(realPosition);
            highlightedSpanStartPosition = spanPositions[0];
            highlightedSpanEndPosition = spanPositions[1];
            Log.d(TAG, "renew highlightedSpanPosition: (" + highlightedSpanStartPosition + ", " + highlightedSpanEndPosition + ")");
        }
    }

    private void addTranscriptLine(String content){

        for (TextView textView : transcriptLines) {
            textView.setTextColor(Color.BLACK);
        }
        // Create a new TextView with red text
        TextView textView = new TextView(getContext());
        textView.setText(content);
        textView.setTextColor(Color.RED);
        textView.setTextSize(20);
        // Add the new TextView to the LinearLayout and to the list
        transcriptList.addView(textView);
        transcriptLines.add(textView);
    }


    public void clearTranscriptList() {
        transcriptStringBuilder.clear();
        transcriptStringBuilderContentSet = false;
        transcriptView.setText(transcriptStringBuilder,TextView.BufferType.SPANNABLE);
        highlightedSpanEndPosition = 0;
        highlightedSpanStartPosition = 0;

        transcriptArray = new JSONArray();
        transcriptTotalDuration = 0;

        initTranscriptStatus();
    }

    private boolean loadTranscriptFromLocal(){
        Playable media = controller.getMedia();

        Log.d(TAG, "try to load transcript, episode title: " + media.getEpisodeTitle()  + "\n feed title: " + media.getFeedTitle() + "\n website link: " + media.getWebsiteLink());

        if( !(media instanceof FeedMedia)){
            return false;
        }
        FeedMedia feedMedia = (FeedMedia)media;
        String transcription = TranscriptionHelper.getTranscription(getContext(), feedMedia.getId(), currentAudioFragment);
        try{
            JSONObject transcript = new JSONObject(transcription);
            transcriptTotalDuration = transcript.getDouble("total_duration");
            transcriptArray = transcript.getJSONArray("segments");
            setTranscriptStatus("localQuery", "成功", "");
            Log.d(TAG, "this podcast's transcription is ready ");
            return true;
        }catch (Exception e){

        }
        return false;
    }

    private void initTranscriptStatus(){
        executedActionView.setText("");
        executedResultView.setText("");
        nextActionView.setText("");
        nextActionExecuteCountDownView.setText("");
    }

    private void errorTranscriptStatus(String error){
    }

    private void setTranscriptStatus(String executedAction, String executedResult, String nextAction){
        String executedActionText = getString(R.string.transcript_fetch_action_query);
        switch (executedAction){
            case "query":
            case "localQuery":
                executedActionText = getString(R.string.transcript_fetch_action_query);
                break;
            case "download":
                executedActionText = getString(R.string.transcript_fetch_action_download);
                break;
            case "transcribe":
                executedActionText = getString(R.string.transcript_fetch_action_transcribe);
                break;
        }

        executedActionView.setText(executedActionText);
        executedResultView.setText(executedResult);

        String nextActionText = "";
        switch (nextAction){
            case "query":
            case "localQuery":
                nextActionText = getString(R.string.transcript_fetch_next_action_query);
                break;
            case "download":
                nextActionText = getString(R.string.transcript_fetch_next_action_download);;
                break;
            case "transcribe":
                nextActionText = getString(R.string.transcript_fetch_next_action_transcribe);;
                break;
        }

        if(!nextActionText.isEmpty()){
            nextActionView.setText("," + nextActionText);
        }else{
            nextActionView.setText("");
            nextActionExecuteCountDownView.setText("");
        }
    }

    private void startTranscriptService() {
        clearTranscriptList();
        Playable media = controller.getMedia();
        if( !(media instanceof FeedMedia)){
            return;
        }
        FeedMedia feedMedia = (FeedMedia)media;
        transcribeService.setServiceParameters(
                feedMedia.getFeedTitle(),
                feedMedia.getWebsiteLink(),
                feedMedia.getEpisodeTitle(),
                feedMedia.getPubDate(),
                feedMedia.getDownload_url(),
                feedMedia.getId(),
                feedMedia.getSize(),
                currentAudioFragment,
                "query");
        handler.postDelayed(transcribeService, 1000);
    }

    private double[] highlightCurrentPosition(double position){

        double[] spanPositions = new double[2];

        try{
            int highlightStart = 0;
            int highlightEnd = 0;
            boolean spanLocated = false;
            for(int i = 0; i < transcriptArray.length(); i++){
                JSONObject jsonObject = transcriptArray.getJSONObject(i);
                double start = jsonObject.getDouble("start") + currentAudioFragment * 180;
                double end = jsonObject.getDouble("end") + currentAudioFragment * 180;
                if(position >= start &&  position <= end ){
                    highlightStart = highlightEnd;
                    highlightEnd = highlightStart + jsonObject.get("text").toString().length();
                    spanLocated = true;
                    spanPositions[0] = start;
                    spanPositions[1] = end;
                    Log.d(TAG, "highlight spn hit with start: " + start + " end: " + end + " position " + position);
                }else{
                    if(!spanLocated){
                        highlightEnd = highlightEnd + jsonObject.get("text").toString().length();
                    }
                }
                if(!transcriptStringBuilderContentSet){
                    transcriptStringBuilder.append((CharSequence) jsonObject.get("text"));
                }
            }
            transcriptStringBuilderContentSet = true;
            if(spanLocated && !transcriptView.hasSelection()){
                transcriptScrollPosition = transcriptScrollView.getScrollY();
                transcriptView.clearFocus();
                transcriptStringBuilder.clearSpans();
                transcriptStringBuilder.setSpan(highlightSpan, highlightStart, highlightEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                transcriptView.setText(transcriptStringBuilder,TextView.BufferType.SPANNABLE);
                transcriptView.setVisibility(View.VISIBLE);
                transcriptScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        transcriptScrollView.setScrollY(transcriptScrollPosition);
                    }
                });
            }else{
                Log.d(TAG, "span not located for this position " + position);
            }

        }catch(Exception e){
            Log.d(TAG, "highlight transcript failed with " + e.toString());
        }
        return spanPositions;
    }

    private void setTranscript(long id, int fragment, JSONObject transcript) {
        try{

            TranscriptionHelper.saveTranscription(getContext(), id, fragment, transcript.toString());
            FeedMedia media = (FeedMedia)controller.getMedia();
            if(fragment == currentAudioFragment && id == media.getId()){
                transcriptArray = transcript.getJSONArray("segments");
                transcriptTotalDuration = transcript.getInt("total_duration");
            }else{
                Log.d(TAG, "transcript fetched, but not for current fragment, saved");
            }
        }catch (Exception e){
            Log.d(TAG, "transcript fetched, try to set failed with " + e.toString());
        }
    }

    private class TranscribeService implements  Runnable{
        private String downloadUrl ;
        private long id;
        private long fileSize;
        private int fragment;
        private String action;
        private String podcastTitle;
        private String podcastWebsite;
        private String episodeTitle;
        private Date episodePubDate;

        private long interval = 30000;


        public void setServiceParameters(String podcastTitle, String podcastWebsite, String episodeTitle, Date episodePubDate, String url, long id, long fileSize, int fragment, String action){
            this.podcastTitle = podcastTitle;
            this.podcastWebsite = podcastWebsite;
            this.episodeTitle = episodeTitle;
            this.episodePubDate = episodePubDate;

            this.downloadUrl = url;
            this.id = id;
            this.fileSize = fileSize;
            this.fragment = fragment;
            this.action = action;
        }

        TranscribeService(){

        }

        @Override
        public void run() {

            if(fragmentDestroyed){
                return;
            }
            if( currentAudioFragment != fragment || currentMediaId != id){
                Log.d(TAG, "TranscribeService run with currentAudioFragment -1 or not equals to current fragment, ignored");
                return;
            }

            Log.d(TAG, "TranscribeService run with downloadUrl: " + downloadUrl + ", id: " + id + ", fileSize: " + fileSize
                    + ",fragment: " + fragment + ", action: " + action);


            TranscribeCallable callable = new TranscribeCallable(podcastTitle, podcastWebsite,episodeTitle, episodePubDate,
                    downloadUrl,
                    id,
                    fileSize,
                    fragment,
                    action);
            Future<String> future = executorService.submit(callable);
            String nextAction = "";
            try {
                String resultString = future.get(5, TimeUnit.SECONDS); // Set a timeout to avoid blocking indefinitely
                Log.d(TAG, "TranscribeService run with result: " + resultString);

                JSONObject result = new JSONObject(resultString);
                if(result.has("transcript_text")) {
                    JSONObject transcript_text = result.optJSONObject("transcript_text");
                    if (transcript_text.has("text")) {
                        //finished
                        JSONObject audioFile = result.getJSONObject("audio_file");
                        int duration = audioFile.getInt("duration");
                        transcript_text.put("total_duration", duration);
                        setTranscript(id, fragment, transcript_text);
                        setTranscriptStatus("query", "成功", "");
                        return;
                    }
                }

                Integer resultCode = result.optInt("code");

                switch (resultCode){
                    case CODE_AUDIO_QUERY_FOUND:
                        JSONObject audioFile = result.getJSONObject("audio_file");
                        Integer downloaded = audioFile.optInt("downloaded", 0);
                        if(downloaded != 100) {
                            if(audioFile.getInt("status") == AUDIO_DOWNLOADING){
                                nextAction = "query";
                                interval = 5000;
                                setTranscriptStatus(action, getString(R.string.transcript_fetch_audio_download_progress)+downloaded, nextAction);
                            }else if(audioFile.getInt("status") == AUDIO_REQUESTED){
                                nextAction = "query";
                                interval = 5000;
                                setTranscriptStatus(action, getString(R.string.transcript_fetch_audio_download_ready), nextAction);
                            }
                            else if(audioFile.getInt("status") == AUDIO_DOWNLOAD_FAILED){
                                nextAction = "download";
                                interval = 3000;
                                setTranscriptStatus(action, getString(R.string.transcript_fetch_audio_download_failed), nextAction);
                            }
                            else{
                                nextAction = "";
                                interval = 15000;
                                setTranscriptStatus(action, getString(R.string.transcript_fetch_audio_download_failed), nextAction);
                            }
                        }else {
                            if(audioFile.getInt("status") == AUDIO_DOWNLOAD_FAILED){
                                nextAction = "";
                                interval = 3000;
                                setTranscriptStatus(action, getString(R.string.transcript_fetch_audio_download_failed), nextAction);
                            }else if(result.has("transcript_file")){
                                JSONObject transcript_file = result.getJSONObject("transcript_file");
                                if (transcript_file.has("id")) {
                                    nextAction = "query";
                                    interval = 10000;
                                    setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_ongoing), nextAction);
                                }else {
                                    nextAction = "transcribe";
                                    interval = 1000;
                                    setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_ready), nextAction);
                                }
                            }else{
                                nextAction = "transcribe";
                                interval = 1000;
                                setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_ready), nextAction);
                            }
                        }
                        break;
                    case CODE_AUDIO_DOWNLOAD_REJECTED:
                        audioDownloadRejected = true;
                        nextAction = "";
                        setTranscriptStatus(action, getString(R.string.transcript_fetch_audio_download_failed), nextAction);
                        break;
                    case CODE_AUDIO_QUERY_NOTFOUND:
                    case CODE_TRANS_AUDIO_NOT_FOUND:
                        nextAction = "download";
                        interval = 1000;
                        setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_audio_missing), nextAction);
                        break;
                    case CODE_AUDIO_DOWNLOAD_ACCEPTED:
                        nextAction = "query";
                        interval = 30000;
                        setTranscriptStatus(action,getString(R.string.transcript_fetch_transcribe_audio_downloading), nextAction);
                        break;
                    case CODE_TRANS_ACCEPTED:
                        nextAction = "query";
                        interval = 25000;
                        setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_ongoing), nextAction);
                        break;
                    case CODE_AUDIO_DOWNLOAD_FOUND:
                        nextAction = "transcribe";
                        interval = 3000;
                        setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_audio_downloaded), nextAction);
                        break;
                    case CODE_TRANS_AUDIO_DOWNLOADING:
                        nextAction = "transcribe";
                        interval = 10000;
                        setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_audio_downloading), nextAction);
                        break;
                    case CODE_TRANS_REJECTED:
                        nextAction = "transcribe";
                        interval = 10000;
                        setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_busy), nextAction);
                        break;
                    case CODE_TRANS_FAILED:
                        Log.d(TAG, "TranscribeService request transcribe failed");
                        setTranscriptStatus(action, getString(R.string.transcript_fetch_transcribe_failed), "");
                    default:
                        break;
                }
            } catch (Exception e) {
                Log.d(TAG, "TranscribeService run with exception " + e.toString());
                nextAction = action;
                interval = 10000;
                setTranscriptStatus(action, getString(R.string.transcript_fetch_server_error) , "");
            }
            if(!nextAction.isEmpty()){
                transcribeService.setServiceParameters(
                        podcastTitle,
                        podcastWebsite,
                        episodeTitle,
                        episodePubDate,
                        downloadUrl,
                        id,
                        fileSize,
                        fragment,
                        nextAction);
                handler.postDelayed(transcribeService, interval);
                startTimer((int) (interval/1000));
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void markFragment2Review(MarkFragment2ReviewEvent event) {

        if(event.position < 0 ){
            return;
        }
        addPositions2ReviewLayout(event.position);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void reCreatePositions2ReviewLayout(){

        positions2Review = TranscriptionHelper.loadPositions2ReviewRecord(getContext(), currentMediaId);
        Log.d(TAG, "reCreatePositions2ReviewLayout, positions load from file " + positions2Review);

        TextView tip = (TextView) marksLinearLayout.getChildAt(0);
        tip.setVisibility(positions2Review.size() == 0 ? View.VISIBLE: View.GONE);

        int toRemoveCount = marksLinearLayout.getChildCount() -1;

        for(int i = 0; i < toRemoveCount; i++){
            Log.d(TAG, "reCreatePositions2ReviewLayout, remove positions, current item at " + (marksLinearLayout.getChildCount() -1 ));
            marksLinearLayout.removeViewAt(marksLinearLayout.getChildCount() -1 );
        }

        Integer[] positionArr = positions2Review.toArray(new Integer[0]);
        Arrays.sort(positionArr);

        for (int i = 0; i < positionArr.length; i++) {
            Log.d(TAG, "reCreatePositions2ReviewLayout, add positions, current item at -1, position " + positionArr[i]);
            addPosition2Review(marksLinearLayout, positionArr[i], -1);
        }
    }


    private void addPosition2Review(LinearLayout parentLayout, int position, int insertAt){

        View myLayout = getLayoutInflater().inflate(R.layout.position_2_review, null);
        myLayout.setTag(position);
        TextView textView = myLayout.findViewById(R.id.position);
        textView.setText(Converter.getDurationStringLong(position));
        ImageView play = myLayout.findViewById(R.id.play);
        play.setTag(position);
        play.setOnClickListener(view -> {
            Integer p = (Integer) view.getTag();
            controller.seekTo(p);
            if(controller.getStatus() != PlayerStatus.PLAYING){
                controller.playPause();
            }
        });

        ImageView delete = myLayout.findViewById(R.id.delete);
        delete.setTag(position);
        delete.setOnClickListener(view -> {
            Integer p = (Integer) view.getTag();
            deletePositions2ReviewLayout(p);
        });
        parentLayout.addView(myLayout,insertAt);
    }

    private void deletePositions2ReviewLayout(int position){

        positions2Review = TranscriptionHelper.loadPositions2ReviewRecord(getContext(), currentMediaId);

        for(int i = 1 ; i < marksLinearLayout.getChildCount(); i++){
            View child = marksLinearLayout.getChildAt(i);
            if((Integer)child.getTag() == position){
                marksLinearLayout.removeViewAt(i);
                Log.d(TAG, "deletePositions2ReviewLayout, remove positions, current item at " + i + ", position " + position);
                break;
            }
        }

        marksLinearLayout.getChildAt(0).setVisibility(marksLinearLayout.getChildCount() > 1 ? View.GONE: View.VISIBLE);
        positions2Review = TranscriptionHelper.updatePositions2Review(getContext(), positions2Review, currentMediaId, position, false);

    }
    private void addPositions2ReviewLayout(int position){

        positions2Review = TranscriptionHelper.loadPositions2ReviewRecord(getContext(), currentMediaId);

        marksLinearLayout.getChildAt(0).setVisibility(View.GONE);

        positions2Review = TranscriptionHelper.updatePositions2Review(getContext(), positions2Review, currentMediaId, position, true);

        if(marksLinearLayout.getChildCount() == 1){
            addPosition2Review(marksLinearLayout, position, -1);
            return;
        }

        for(int i = 1 ; i < marksLinearLayout.getChildCount(); i++){

            int childPosition = (Integer)(marksLinearLayout.getChildAt(i).getTag());
            if(childPosition > position){
                addPosition2Review(marksLinearLayout, position, i);
                return;
            }
        }
        addPosition2Review(marksLinearLayout, position, -1);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void wordNoteClicked(WordNoteClickedEvent event){
        final int noteId = event.wordNote.note_id;
        new AlertDialog.Builder(getContext())
                .setTitle(event.wordNote.word)
                .setMessage(event.wordNote.translation + "\n\n" + getString(R.string.transcript_word_delete_or_review_confirm))
                .setPositiveButton(getString(R.string.transcript_word_mastered), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        notesDBHelper.deleteWord(noteId);
                        noteList.remove(event.position);
                        noteAdapter.notifyItemRemoved(event.position);
                    }
                })
                .setNegativeButton(getString(R.string.transcript_word_review_later), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        notesDBHelper.updateWordReviewTime(event.wordNote.word);
                        noteList.remove(event.position);
                        noteAdapter.notifyItemRemoved(event.position);
                    }
                }).show();
    }
}



