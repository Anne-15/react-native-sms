package com.react;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import android.content.ContentValues;
import android.os.Bundle;
import android.widget.Toast;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SmsModule extends ReactContextBaseJavaModule { //implements LoaderManager.LoaderCallBacks<Cursor>
    //private loader manager
    private Cursor smsCursor;
    private Map<Long, String> smsList;
    private Map<Long, Object> smsListBody;
    Activity mActivity = null;
    private static Context context;
    private ReactContext mReactContext;
    private Callback cb_autoSend_succ = null;
    private Callback cb_autoSend_err = null;

    public SmsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        smsList = new HashMap<Long, String>();
        context = reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "ABC";
    }

    @ReactMethod
    public void list(String filter, final Callback errorCallback, final Callback successCallback) {
        try {
            JSONObject filterJ = new JSONObject(filter);
            String uri_filter = filterJ.has("box") ? filterJ.optString("box") : "inbox";
            int fread = filterJ.has("read") ? filterJ.optInt("read") : -1;
            int fid = filterJ.has("_id") ? filterJ.optInt("_id") : -1;
            int ftid = filterJ.has("thread_id") ? filterJ.optInt("thread_id") : -1;
            String faddress = filterJ.optString("address");
            String fcontent = filterJ.optString("body");
            String fContentRegex = filterJ.optString("bodyRegex");
            int indexFrom = filterJ.has("indexFrom") ? filterJ.optInt("indexFrom") : 0;
            int maxCount = filterJ.has("maxCount") ? filterJ.optInt("maxCount") : -1;
            String selection = filterJ.has("selection") ? filterJ.optString("selection") : "";
            String sortOrder = filterJ.has("sortOrder") ? filterJ.optString("sortOrder") : null;
            long maxDate = filterJ.has("maxDate") ? filterJ.optLong("maxDate") : -1;
            long minDate = filterJ.has("minDate") ? filterJ.optLong("minDate") : -1;
            Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/" + uri_filter), null, selection, null, sortOrder);
            
            int c = 0;
            JSONArray jsons = new JSONArray();

            while (cursor != null && cursor.moveToNext()) {
                boolean matchFilter = true;
                if (fid > -1)
                    matchFilter = fid == cursor.getInt(cursor.getColumnIndex("_id"));
                else if (ftid > -1)
                    matchFilter = ftid == cursor.getInt(cursor.getColumnIndex("thread_id"));
                else if (fread > -1)
                    matchFilter = fread == cursor.getInt(cursor.getColumnIndex("read"));
                else if (faddress != null && !faddress.isEmpty())
                    matchFilter = faddress.equals(cursor.getString(cursor.getColumnIndex("address")).trim());
                else if (fcontent != null && !fcontent.isEmpty())
                    matchFilter = fcontent.equals(cursor.getString(cursor.getColumnIndex("body")).trim());

                if (fContentRegex != null && !fContentRegex.isEmpty())
                    matchFilter = matchFilter && cursor.getString(cursor.getColumnIndex("body")).matches(fContentRegex);
                if (maxDate > -1)
                    matchFilter = matchFilter && maxDate >= cursor.getLong(cursor.getColumnIndex("date"));
                if (minDate > -1)
                    matchFilter = matchFilter && minDate <= cursor.getLong(cursor.getColumnIndex("date"));
                if (matchFilter) {
                    if (c >= indexFrom) {
                        if (maxCount > 0 && c >= indexFrom + maxCount)
                            break;
                        // Long dateTime = Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
                        // String message = cursor.getString(cursor.getColumnIndex("body"));
                        JSONObject json;
                        json = getJsonFromCursor(cursor);
                        jsons.put(json);

                    }
                    c++;
                }
            }
            cursor.close();
            try {
                successCallback.invoke(c, jsons.toString());
            } catch (Exception e) {
                errorCallback.invoke(e.getMessage());
            }
        } catch (JSONException e) {
            errorCallback.invoke(e.getMessage());
            return;
        }
    }

    @ReactMethod
    public void show(){
        Toast.makeText(context, "Gotten a list of your data", Toast.LENGTH_LONG).show();
    }

    private JSONObject getJsonFromCursor(Cursor cur) {
        JSONObject json = new JSONObject();

        int nCol = cur.getColumnCount();
        String[] keys = cur.getColumnNames();
        try {
            for (int j = 0; j < nCol; j++)
                switch (cur.getType(j)) {
                    case Cursor.FIELD_TYPE_NULL:
                        json.put(keys[j], null);
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        json.put(keys[j], cur.getLong(j));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        json.put(keys[j], cur.getFloat(j));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        json.put(keys[j], cur.getString(j));
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        json.put(keys[j], cur.getBlob(j));
                }
        } catch (Exception e) {
            return null;
        }

        return json;
    }


}