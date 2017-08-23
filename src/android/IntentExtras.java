package pl.itexpert.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import android.content.Intent;
import android.util.Log;
import org.apache.cordova.PluginResult;
import android.content.ClipData;
import org.json.JSONArray;
import android.content.ContentResolver;
import android.webkit.MimeTypeMap;
import android.os.Build;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;

public class IntentExtras extends CordovaPlugin {

    private static final String LOG_TAG = "IntentExtras";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(LOG_TAG, "Action: " + action);
        if (action.equals("getIntent")) {
            if(args.length() != 0) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }
            Intent intent = cordova.getActivity().getIntent();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, getIntentJson(intent)));
            return true;
        }

        return false;
    }

    private JSONObject getIntentJson(Intent intent) {
        JSONObject intentJSON = null;
        ClipData clipData = null;
        JSONObject[] items = null;
        ContentResolver cR = this.cordova.getActivity().getApplicationContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipData = intent.getClipData();
            if (clipData != null) {
                int clipItemCount = clipData.getItemCount();
                items = new JSONObject[clipItemCount];

                for (int i = 0; i < clipItemCount; i++) {

                    ClipData.Item item = clipData.getItemAt(i);

                    try {
                        items[i] = new JSONObject();
                        items[i].put("htmlText", item.getHtmlText());
                        items[i].put("intent", item.getIntent());
                        items[i].put("text", item.getText());
                        items[i].put("uri", item.getUri());

                        if (item.getUri() != null) {
                            String type = cR.getType(item.getUri());
                            String extension = mime.getExtensionFromMimeType(cR.getType(item.getUri()));

                            items[i].put("type", type);
                            items[i].put("extension", extension);
                        }

                    } catch (JSONException e) {
                        Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
                        Log.d(LOG_TAG, e.getMessage());
                        Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));
                    }

                }
            }

            try {
                intentJSON = new JSONObject();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if(items != null) {
                        intentJSON.put("clipItems", new JSONArray(items));
                    }
                }

                intentJSON.put("type", intent.getType());
                intentJSON.put("extras", toJsonObject(intent.getExtras()));
                intentJSON.put("action", intent.getAction());
                intentJSON.put("categories", intent.getCategories());
                intentJSON.put("flags", intent.getFlags());
                intentJSON.put("component", intent.getComponent());
                intentJSON.put("data", intent.getData());
                intentJSON.put("package", intent.getPackage());

                return intentJSON;
            } catch (JSONException e) {
                Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
                Log.d(LOG_TAG, e.getMessage());
                Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));

                return null;
            }
        }
    }

    private static JSONObject toJsonObject(Bundle bundle) {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        try {
            return (JSONObject) toJsonValue(bundle);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot convert bundle to JSON: " + e.getMessage(), e);
        }
    }

    private static Object toJsonValue(final Object value) throws JSONException {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        if (value == null) {
            return null;
        } else if (value instanceof Bundle) {
            final Bundle bundle = (Bundle) value;
            final JSONObject result = new JSONObject();
            for (final String key : bundle.keySet()) {
                result.put(key, toJsonValue(bundle.get(key)));
            }
            return result;
        } else if (value.getClass().isArray()) {
            final JSONArray result = new JSONArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; ++i) {
                result.put(i, toJsonValue(Array.get(value, i)));
            }
            return result;
        } else if (
                value instanceof String
                        || value instanceof Boolean
                        || value instanceof Integer
                        || value instanceof Long
                        || value instanceof Double) {
            return value;
        } else {
            return String.valueOf(value);
        }
    }
}