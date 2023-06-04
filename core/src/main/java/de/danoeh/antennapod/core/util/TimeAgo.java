package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.R;

public class TimeAgo {

    public static String getTimeAgo(Context context, long timeInSec) {
        Date now = new Date();
        long seconds = System.currentTimeMillis()/1000 - timeInSec;
        int minute = 60;
        int hour = minute * 60;
        int day = hour * 24;
        int week = day * 7;
        int month = day * 30;
        int year = day * 365;
        Log.d("Transcript", "timeInSec " + timeInSec + ", now " + System.currentTimeMillis()/1000);
        if (seconds < minute) {
            return context.getString(R.string.transcript_word_review_just_now);
        } else if (seconds < hour) {
            return seconds / minute + context.getString(R.string.transcript_word_review_minutes_ago);
        } else if (seconds < day) {
            return seconds / hour + context.getString(R.string.transcript_word_review_hours_ago);
        } else if (seconds < week) {
            return seconds / day + context.getString(R.string.transcript_word_review_days_ago);
        } else if (seconds < month) {
            return seconds / week + context.getString(R.string.transcript_word_review_weeks_ago);
        } else if (seconds < year) {
            return seconds / month + context.getString(R.string.transcript_word_review_months_ago);
        } else {
            return seconds / year + context.getString(R.string.transcript_word_review_years_ago);
        }
    }
}

