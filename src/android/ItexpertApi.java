package pl.itexpert.cordova;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class ItexpertApi extends CordovaPlugin {

    private static final String LOG_TAG = "ItexpertApi";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(LOG_TAG, "Action: " + action);
        if (action.equals("getIntent")) {
            if (args.length() != 0) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }
            Intent intent = cordova.getActivity().getIntent();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, getIntentJson(intent)));
            return true;
        } else if (action.equals("sendDatabase")) {
            if (args.length() != 2) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }
            sendDatabaseInBackground((String) args.get(0), (String) args.get(1));

            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject()));
            return true;
        }

        return false;
    }

    private void sendDatabaseInBackground (final String url, final String databaseName) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                sendDatabase(url, databaseName);
            }
        });
    }

    private void sendDatabase(String url, String databaseName) {
        Log.d(LOG_TAG, "Sending database file: " + databaseName + " to url: " + url);
        File dbFile = new File("/data/data/" + cordova.getActivity().getPackageName() + "/databases/" + databaseName);
        if (!dbFile.exists() || !dbFile.canRead()) {
            LOG.e(LOG_TAG, "File not found or not readable");
            return;
        }
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();

            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";

            // Indicate that we want to write to the HTTP request body
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            // Indicate that we want to write some data as the HTTP request body
            urlConnection.setDoOutput(true);

            //OutputStream outputStreamToRequestBody = urlConnection.getOutputStream();
            DataOutputStream httpRequestBodyWriter = new DataOutputStream(
                    urlConnection.getOutputStream());

            // Include the section to describe the file
            httpRequestBodyWriter.writeBytes(twoHyphens + boundary + crlf);
            httpRequestBodyWriter.writeBytes("Content-Disposition: form-data; name=\"database\";filename=\"" +
                    databaseName + "\"" + crlf + "\"");
            httpRequestBodyWriter.writeBytes("Content-Transfer-Encoding: binary" + crlf);
            httpRequestBodyWriter.writeBytes(crlf);
            httpRequestBodyWriter.flush();

            // Write the actual file contents
            FileInputStream inputStreamToLogFile = new FileInputStream(dbFile);

            int bytesRead;
            byte[] dataBuffer = new byte[1024];
            while ((bytesRead = inputStreamToLogFile.read(dataBuffer)) != -1) {
                httpRequestBodyWriter.write(dataBuffer, 0, bytesRead);
            }

            httpRequestBodyWriter.flush();

            // Mark the end of the multipart http request
            httpRequestBodyWriter.writeBytes(crlf);
            httpRequestBodyWriter.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
            httpRequestBodyWriter.flush();

            // Close the streams
            httpRequestBodyWriter.close();
            Log.d(LOG_TAG, "Database send response code: " + urlConnection.getResponseCode() + " message: " + urlConnection.getResponseMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
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
        }
        try {
            intentJSON = new JSONObject();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (items != null) {
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