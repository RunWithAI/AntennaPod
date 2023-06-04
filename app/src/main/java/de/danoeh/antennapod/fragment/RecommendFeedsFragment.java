package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.FeedDiscoverAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.TransApi;
import de.danoeh.antennapod.core.util.Translator;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class RecommendFeedsFragment extends Fragment implements AdapterView.OnItemClickListener{
    private static final String TAG = "RecommendFeedsFragment";
    private static final int NUM_SUGGESTIONS = 12;

    private Disposable disposable;
    private FeedDiscoverAdapter adapter;
    private GridView recommendGridLayout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.recommend_feeds, container, false);

        recommendGridLayout = root.findViewById(R.id.recommend_grid);


        adapter = new FeedDiscoverAdapter((MainActivity) getActivity());
        recommendGridLayout.setAdapter(adapter);
        recommendGridLayout.setOnItemClickListener(this);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        if (screenWidthDp > 600) {
            recommendGridLayout.setNumColumns(6);
        } else {
            recommendGridLayout.setNumColumns(4);
        }


        List<PodcastSearchResult> dummies = new ArrayList<>();
        for (int i = 0; i < NUM_SUGGESTIONS; i++) {
            dummies.add(PodcastSearchResult.dummy());
        }

        adapter.updateData(dummies);

        disposable = Observable.fromCallable(() ->
                        loadRecommendation())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        podcasts -> {
                                adapter.updateData(podcasts);
                        }, error -> {

                        });
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private List<PodcastSearchResult> loadRecommendation() {
        List<PodcastSearchResult> lists = new ArrayList<>();
        try{
            TransApi api = new TransApi();
            String res = api.getPodcastRecommendation();
            JSONArray podcastArray = new JSONArray(res);
            for (int i = 0; i < podcastArray.length(); i ++){
                JSONObject podcast = (JSONObject) podcastArray.get(i);
                PodcastSearchResult psr = PodcastSearchResult.fromItunes(podcast);
                lists.add(psr);
            }
            return lists;
        }catch (Exception e){
        }
        return null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        PodcastSearchResult podcast = adapter.getItem(position);
        if (TextUtils.isEmpty(podcast.feedUrl)) {
            return;
        }
        Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, podcast.feedUrl);
        startActivity(intent);
    }
}
