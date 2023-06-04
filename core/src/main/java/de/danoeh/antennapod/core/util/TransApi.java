package de.danoeh.antennapod.core.util;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TransApi {
    private static final String TRANSLATE_API_URL = "http://podcast.kejikeji.com/translate";
    private static final String DICT_API_URL =   "https://ecdict.reading.kejikeji.com/translate";
    private static final String TRANSCRIBE_API_URL = "http://podcast.kejikeji.com/transcribe";
    public static final String APP_USER_GROUP_URL = "http://podcast.kejikeji.com/qrcode";
    public static final String APP_VERSION_UPDATE_URL = "http://podcast.kejikeji.com/version";
    public static final String PODCAST_RECOMMENDATION_URL = "http://podcast.kejikeji.com/recommendation";

    public TransApi() {

    }


    public String podcastTranscribe(String podcastTitle, String podcastWebsite, String episodeTitle, Date episodePubDate, String downloadUrl, Long feedMediaId, Long fileSize, String action, int fragment){
        try{
            JSONObject data = new JSONObject();
            data.put("podcast_title", podcastTitle == null? "":podcastTitle);
            data.put("podcast_website", podcastWebsite == null? "":podcastWebsite);
            data.put("episode_title", episodeTitle == null? "":episodeTitle);
            if(episodePubDate != null){
                String pubDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(episodePubDate);
                data.put("episode_pub_date",pubDate);
            }else{
                data.put("episode_pub_date","2009-10-10 00:00:00");
            }

            data.put("downloadUrl", downloadUrl);
            data.put("feedMediaId", feedMediaId);
            data.put("fileSize", fileSize);
            data.put("action", action);
            data.put("fragment", fragment);
            return HttpGet.post(TRANSCRIBE_API_URL, data);
        }catch (Exception e){
            return null;
        }
    }

    public String baiduTranslate(String query){
        try{
            JSONObject data = new JSONObject();
            data.put("query", query);
            return HttpGet.post(TRANSLATE_API_URL, data);
        }catch (Exception e){
            return null;
        }
    }

    public String getDictResult(String word){
        Map<String, String> params = new HashMap<String, String>();
        params.put("word", word);
        return HttpGet.get(DICT_API_URL, params);
    }

    public String getPodcastRecommendation(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("query", "nothing");
        return HttpGet.get(PODCAST_RECOMMENDATION_URL, params);
    }

}