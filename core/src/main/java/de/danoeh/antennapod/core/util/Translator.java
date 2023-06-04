package de.danoeh.antennapod.core.util;

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;


public class Translator implements Callable<JSONObject> {
    private String toTrans;
    private String sentence;

    public Translator(String toTrans, String sentence) {
        this.toTrans = toTrans.trim();
        this.sentence = sentence;
    }

    public Translator(String toTrans) {
        this.toTrans = toTrans.trim();
        this.sentence = "";
    }

    @Override
    public JSONObject call() {

        TransApi api = new TransApi();
        String[] words = this.toTrans.split("\\s+");
        int resultCode = 0;
        String error = "";
        String transResult = "";
        String transType = "word";

        try{

            if (words.length > 1) {
                transType = "sentence";
                String apiResponse = api.baiduTranslate(toTrans);
                JSONObject jsonObject = new JSONObject(apiResponse);
                if (jsonObject.has("error_code")) {
                    error = "error : " + jsonObject.getString("error_code");
                    resultCode = -1;
                }
                if (jsonObject.has("trans_result")) {
                    JSONArray results = jsonObject.getJSONArray("trans_result");
                    transResult = results.getJSONObject(0).getString("dst");
                }
            } else if (words.length == 1) {

                String word = words[0].replaceAll("\\p{Punct}", "");
                String dickRes = api.getDictResult(word);
                JSONObject resObject = new JSONObject(dickRes);
                resultCode = resObject.getInt("result_code");
                transResult = dickRes;

            } else {
                resultCode = -1;
                error = "unsupported words length";
            }

            JSONObject resObj = new JSONObject();

            resObj.put("resultCode", resultCode);
            resObj.put("transResult", transResult);
            resObj.put("transType", transType);
            resObj.put("word", toTrans);
            resObj.put("sentence", sentence);
            resObj.put("error", error);
            return resObj;
        }catch (Exception e){
            return null;
        }

    }
}