package com.avx.nfcprobe;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class ProbeStore {
    static final String PREFS="avx_probe", KEY_LOG="log", KEY_COUNT="count", KEY_CARD_INFO="card_info";
    static final String ACTION_UPDATED="com.avx.nfcprobe.LOG_UPDATED";
    private ProbeStore() {}

    static synchronized void append(Context context, String message) {
        SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String old=sp.getString(KEY_LOG,"");
        String ts=new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
        String next=ts+"  "+message+(old.isEmpty()?"":"\n"+old);
        if(next.length()>30000) next=next.substring(0,30000);
        sp.edit().putString(KEY_LOG,next).apply();
        notifyUpdated(context);
    }

    static synchronized void incrementApdu(Context context) {
        SharedPreferences sp=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_COUNT,sp.getInt(KEY_COUNT,0)+1).apply();
    }

    static void setCardInfo(Context context,String info) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_CARD_INFO,info).apply();
        notifyUpdated(context);
    }

    static String getCardInfo(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_CARD_INFO,"No physical card scanned in this app yet.");
    }

    static String getLog(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_LOG,"No APDU received yet.");
    }

    static int getCount(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(KEY_COUNT,0);
    }

    static void clearLog(Context context) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(KEY_LOG).remove(KEY_COUNT).apply();
        notifyUpdated(context);
    }

    private static void notifyUpdated(Context context) {
        context.sendBroadcast(new android.content.Intent(ACTION_UPDATED).setPackage(context.getPackageName()));
    }
}
