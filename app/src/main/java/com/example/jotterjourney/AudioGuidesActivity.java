package com.example.jotterjourney;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AudioGuidesActivity extends AppCompatActivity {
    private SQLiteDatabase db; private  String targetLocation, cityName; private int cityId=0; ArrayList<ArrayList<Object>> guidesList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guides);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        readAndLogDataFromSQLite();
    }
    @SuppressLint("Range")
    private void readAndLogDataFromSQLite() {
        Cursor cursor = db.rawQuery("SELECT * FROM jjtrips1", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int userID = cursor.getInt(cursor.getColumnIndex("userID"));
                    targetLocation = cursor.getString(cursor.getColumnIndex("targetLocation"));
                    Log.d("SQLite Data", "User ID: " + userID);
                    Log.d("SQLite Data", "Target Location: " + targetLocation);
                    String apiUrl = "https://app.wegotrip.com/api/v2/search/?query="+targetLocation;
                    new SendRequestTask().execute(apiUrl);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
    public class SendRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept-Language", "ru");
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else {
                    Log.e("HTTP Request", "Error: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("HTTP Request", "IOException: " + e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    Log.d("Response", response);
                    JSONObject dataObject = jsonResponse.getJSONObject("data");
                    JSONArray resultsArray = dataObject.getJSONArray("results");
                    for (int i = 0; i < resultsArray.length(); i++) {
                        JSONObject resultObject = resultsArray.getJSONObject(i);
                        JSONObject cityObject = resultObject.optJSONObject("city");
                        if (cityObject != null && !cityObject.isNull("id")) {
                            cityId = cityObject.getInt("id");
                            cityName = cityObject.getString("name");
                            Log.d("City Info", "City ID: " + cityId);
                            Log.d("City Info", "City Name: " + cityName);
                            break;
                        }
                    }
                    if (cityId != 0) {
                        String secondApiUrl = "https://app.wegotrip.com/api/v2/products/popular/?city=" + cityId + "&lang=ru&currency=RUB";
                        new SendGuidesRequestTask().execute(secondApiUrl);
                    } else {
                        Log.e("City ID not found", "No city ID found in the response");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("JSON parsing error", e.getMessage());
                }
            } else {
                Log.e("No data available", "Error retrieving data");
            }
        }
    }


    public class SendGuidesRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept-Language", "ru");
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else {
                    Log.e("HTTP Request", "Error: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("HTTP Request", "IOException: " + e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONArray results = data.getJSONArray("results");
                    ArrayList<ArrayList<Object>> guidesList = new ArrayList<>();

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject guide = results.getJSONObject(i);
                        String id = guide.getString("id");
                        String title = guide.getString("title");
                        String slug = guide.getString("slug");
                        String duration = guide.getString("duration");
                        double price = guide.getDouble("price");
                        double rating = guide.optDouble("rating", 0.0);
                        String cover = guide.getString("cover");
                        String category = guide.getString("category");
                        String locale = guide.getString("locale");
                        String link = "https://wegotrip.com/checkout/"+slug+"-p"+id+"/booking/";

                        ArrayList<Object> guideInfo = new ArrayList<>();
                        guideInfo.add(id);
                        guideInfo.add(title);
                        guideInfo.add(slug);
                        guideInfo.add(duration);
                        guideInfo.add(price);
                        guideInfo.add(rating);
                        guideInfo.add(cover);
                        guideInfo.add(category);
                        guideInfo.add(locale);
                        guideInfo.add(link);
                        guidesList.add(guideInfo);
                    }
                    Log.d("Guides List", guidesList.toString());
                } catch (JSONException e) {
                    Log.e("JSON parsing error", e.getMessage());
                }
            } else {
                Log.e("No data available", "Error retrieving data");
            }
        }
    }
}