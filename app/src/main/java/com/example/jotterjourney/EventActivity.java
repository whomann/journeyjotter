package com.example.jotterjourney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class EventActivity extends AppCompatActivity {
    private String predictHqApiKey; private SQLiteDatabase db; private int selectedTripId; private boolean isOnCreate; private String cityId; private ImageButton previousEventPage, nextEventPage; private ProgressBar progressBarEvent; private RecyclerView recyclerView; private String targetIATA, returnDate,departureDate; ArrayList<ArrayList<Object>> eventList = new ArrayList<>(); private int offset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        progressBarEvent=findViewById(R.id.progressBarEvent);
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            predictHqApiKey = properties.getProperty("predictHqApiKey");
        } catch (IOException e) {
            e.printStackTrace();
        }
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        isOnCreate=true;
        recyclerView=findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList.clear();
        offset=0;
        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        readAndLogDataFromSQLite(selectedTripId);
        nextEventPage = findViewById(R.id.nextEventPage);
        previousEventPage = findViewById(R.id.previousEventPage);
        nextEventPage.setVisibility(View.INVISIBLE);
        previousEventPage.setVisibility(View.INVISIBLE);
        nextEventPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                offset+=50;
                progressBarEvent.setVisibility(View.VISIBLE);
                eventList.clear();
                EventsAdapter eventsAdapter= new EventsAdapter(new ArrayList<>());
                recyclerView.setAdapter(eventsAdapter);
                eventsAdapter.clearData();
                new PredictHQApiTask().execute(cityId);
            }
        });
        previousEventPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                offset-=50;
                progressBarEvent.setVisibility(View.VISIBLE);
                eventList.clear();
                EventsAdapter eventsAdapter= new EventsAdapter(new ArrayList<>());
                recyclerView.setAdapter(eventsAdapter);
                eventsAdapter.clearData();
                new PredictHQApiTask().execute(cityId);
            }
        });
    }

    @SuppressLint("Range")
    private void readAndLogDataFromSQLite(int selectedTripId) {
        Cursor cursor = db.rawQuery("SELECT * FROM jjtrips1 WHERE userID = ?", new String[]{String.valueOf(selectedTripId)});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int userID = cursor.getInt(cursor.getColumnIndex("userID"));
                    String targetLocation = cursor.getString(cursor.getColumnIndex("targetLocation"));
                    departureDate = cursor.getString(cursor.getColumnIndex("departureDate"));
                    returnDate = cursor.getString(cursor.getColumnIndex("returnDate"));
                    targetIATA = cursor.getString(cursor.getColumnIndex("targetIATA"));
                    String targetLocName = getEnNameFromCitiesJson(targetLocation);
                    Log.d("targetLocName", targetLocName);
                    new GetCityIdTask().execute(targetLocName);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    private String getEnNameFromCitiesJson(String targetLocation) {
        progressBarEvent.setVisibility(View.VISIBLE);
        try {
            InputStream is = getAssets().open("cities_sorted.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            String jsonString = stringBuilder.toString();
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject cityObject = jsonArray.getJSONObject(i);
                String cityName = cityObject.optString("name", null);
                if (cityName.equals(targetLocation)) {
                    return cityObject.optString("name_translations", null);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String formatDateTimeForAndroid(String inputDateTime) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        try {
            Date date = inputFormat.parse(inputDateTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return inputDateTime;
        }
    }
    public class GetCityIdTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }
            String targetName = params[0];
            String apiUrl = "https://api.predicthq.com/v1/places/?q=" + targetName;

            try {
                eventList.clear();
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Bearer " + predictHqApiKey);
                urlConnection.setRequestProperty("Accept", "application/json");

                int responseCode = urlConnection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream in = urlConnection.getInputStream();
                    return readStream(in);
                } else {
                    Log.e("GetCityIdTask", "HTTP error: " + responseCode);
                    Toast.makeText(EventActivity.this, "Ошибка соединения.", Toast.LENGTH_SHORT).show();
                    progressBarEvent.setVisibility(View.GONE);
                }
            } catch (IOException e) {
                Log.e("GetCityIdTask", "Error making API request", e);
                Toast.makeText(EventActivity.this, "События не найдены.", Toast.LENGTH_SHORT).show();
                progressBarEvent.setVisibility(View.GONE);
            }
            return null;
        }

        private String readStream(InputStream in) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("result", result);
            if (result != null) {
                cityId = parseCityId(result);
                if (cityId != null) {
                    Log.d("cityId", cityId);
                    new PredictHQApiTask().execute(cityId);
                } else {
                    Log.e("GetCityIdTask", "Failed to obtain city ID from API response.");
                    Toast.makeText(EventActivity.this, "События не найдены.", Toast.LENGTH_SHORT).show();
                    progressBarEvent.setVisibility(View.GONE);
                }
            } else {
                Log.e("GetCityIdTask", "API response is null.");
                Toast.makeText(EventActivity.this, "События не найдены.", Toast.LENGTH_SHORT).show();
                progressBarEvent.setVisibility(View.GONE);
            }
        }

        private String parseCityId(String response) {
            try {
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("results")) {
                    JSONArray resultsArray = jsonResponse.getJSONArray("results");
                    if (resultsArray.length() > 0) {
                        JSONObject firstResult = resultsArray.getJSONObject(0);
                        return firstResult.optString("id", null);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
        public class PredictHQApiTask extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {
                if (params.length == 0) {
                    return null;
                }
                String cityId = params[0];
                String result = null;
                try {
                    URL url = new URL("https://api.predicthq.com/v1/events/?" +
                            "&place.scope=" + cityId +
                            "&limit=1000" +
                            "&end.lte=" +returnDate+
                            "&start.gte="+departureDate+
                            "&offset="+offset);
                    Log.d("url", String.valueOf(url));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("Authorization", "Bearer " + predictHqApiKey);
                    urlConnection.setRequestProperty("Accept", "application/json");

                    int responseCode = urlConnection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream in = urlConnection.getInputStream();
                        result = readStream(in);
                    } else {
                        Log.e("PredictHQApiTask", "HTTP error: " + responseCode);
                    }

                } catch (IOException e) {
                    Log.e("PredictHQApiTask", "Error making API request", e);
                    progressBarEvent.setVisibility(View.GONE);
                }
                return result;
            }

            private String readStream(InputStream in) throws IOException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    int totalCount=0;
                    Log.d("result", result);
                    try {
                        JSONObject jsonResponse = new JSONObject(result);
                        totalCount = jsonResponse.optInt("count", 0);
                        Log.d("PredictHQApiTask", "Total Events Count: " + totalCount);

                        JSONArray resultsArray = jsonResponse.getJSONArray("results");
                        if (resultsArray.length() > 0) {
                            for (int i = 0; i < resultsArray.length(); i++) {
                                JSONObject eventObject = resultsArray.getJSONObject(i);
                                ArrayList<Object> eventInfoList = new ArrayList<>();
                                eventInfoList.add(eventObject.optString("title", ""));
                                eventInfoList.add(eventObject.optString("start", ""));
                                eventInfoList.add(eventObject.optString("category", ""));
                                JSONArray entitiesArray = eventObject.getJSONArray("entities");
                                if (entitiesArray.length() > 0) {
                                    JSONObject firstEntity = entitiesArray.getJSONObject(0);
                                    String formattedAddress = firstEntity.optString("formatted_address", "");
                                    formattedAddress = formattedAddress.replace("\n", ", ");
                                    eventInfoList.add(formattedAddress);
                                } else {
                                    eventInfoList.add("Локальный праздник");
                                }
                                eventList.add(eventInfoList);
                            }
                            if (offset + resultsArray.length() < totalCount) {
                                nextEventPage.setVisibility(View.VISIBLE);
                            } else {
                                nextEventPage.setVisibility(View.INVISIBLE);
                            }
                            if (offset - 50 >= 0) {
                                previousEventPage.setVisibility(View.VISIBLE);
                            } else {
                                previousEventPage.setVisibility(View.INVISIBLE);
                            }
                            Log.d("eventList", String.valueOf(eventList));
                            progressBarEvent.setVisibility(View.GONE);
                            RecyclerView recyclerView = findViewById(R.id.recyclerViewEvents);
                            EventsAdapter eventsAdapter = new EventsAdapter(eventList);
                            recyclerView.setAdapter(eventsAdapter);
                        } else {
                            Log.d("PredictHQApiTask", "No events found in the response.");
                            nextEventPage.setVisibility(View.INVISIBLE);
                            Toast.makeText(EventActivity.this, "Больше событий не найдено.", Toast.LENGTH_SHORT).show();
                            progressBarEvent.setVisibility(View.GONE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        nextEventPage.setVisibility(View.INVISIBLE);
                    }
                } else {
                    Log.e("PredictHQApiTask", "API response is null.");
                    Toast.makeText(EventActivity.this, "События не найдены.", Toast.LENGTH_SHORT).show();
                    progressBarEvent.setVisibility(View.GONE);
                }
        }
    }
    public class EventsAdapter extends RecyclerView.Adapter<EventActivity.EventsAdapter.EventsViewHolder> {

        private List<ArrayList<Object>> eventsDataList;

        public EventsAdapter(List<ArrayList<Object>> eventsDataList) {
            this.eventsDataList = eventsDataList;
        }
        public void clearData() {
            eventsDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EventActivity.EventsAdapter.EventsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card_layout, parent, false);
            return new EventActivity.EventsAdapter.EventsViewHolder(view);
        }

        @SuppressLint("Range")
        @Override
        public void onBindViewHolder(@NonNull EventActivity.EventsAdapter.EventsViewHolder holder, int position) {
            ArrayList<Object> eventsData = eventsDataList.get(position);
            String title = eventsData.get(0).toString().trim();
            String startAt = eventsData.get(1).toString().trim();
            String startAtFormatted = formatDateTimeForAndroid(startAt);
            String category = eventsData.get(2).toString().trim();
            String address = eventsData.get(3).toString().trim();
            switch(category){
                case "concerts":
                    holder.eventIcon.setImageResource(R.drawable.music);
                    break;
                case "academic":
                    holder.eventIcon.setImageResource(R.drawable.academic);
                    break;
                case "school-holidays":
                    holder.eventIcon.setImageResource(R.drawable.school_holidays);
                    break;
                case "observances":
                    holder.eventIcon.setImageResource(R.drawable.observances);
                    break;
                case "politics":
                    holder.eventIcon.setImageResource(R.drawable.politics);
                    break;
                case "conferences":
                    holder.eventIcon.setImageResource(R.drawable.conferences);
                    break;
                case "expos":
                    holder.eventIcon.setImageResource(R.drawable.expos);
                    break;
                case "festivals":
                    holder.eventIcon.setImageResource(R.drawable.festivals);
                    break;
                case "performing-arts":
                    holder.eventIcon.setImageResource(R.drawable.performing_arts);
                    break;
                case "sports":
                    holder.eventIcon.setImageResource(R.drawable.sports);
                    break;
                case "community":
                    holder.eventIcon.setImageResource(R.drawable.community);
                    break;
                case "daylight-savings":
                    holder.eventIcon.setImageResource(R.drawable.daylight_savings);
                    break;
                case "airport-delays":
                    holder.eventIcon.setImageResource(R.drawable.airport_delays);
                    break;
                case "severe-weather":
                    holder.eventIcon.setImageResource(R.drawable.severe_weaither);
                    break;
                case "disasters":
                    holder.eventIcon.setImageResource(R.drawable.disasters);
                    break;
                case "terror":
                    holder.eventIcon.setImageResource(R.drawable.terror);
                    break;
                case "health-warnings":
                    holder.eventIcon.setImageResource(R.drawable.health_warnings);
                    break;
                case "public-holidays":
                    holder.eventIcon.setImageResource(R.drawable.public_holidays);
                    break;
                default:
                    holder.eventIcon.setImageResource(R.drawable.festivals);
                    break;
            }

            holder.eventTextView.setText(title);
            holder.eventAddressTextView.setText(address.trim());
            holder.eventStart.setText("Начало: "+startAtFormatted);

            holder.addEvent.setOnClickListener(v -> {
                String updatedEventsInfo = category+"|"+title+"|"+startAtFormatted +"|"+address+"\n";
                int userId = selectedTripId;
                SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
                ContentValues values = new ContentValues();
                Cursor cursor = db.rawQuery("SELECT eventsList FROM jjtrips1 WHERE userId=?", new String[]{String.valueOf(userId)});
                if (cursor != null && cursor.moveToFirst()) {
                    String existingEventsInfo = cursor.getString(cursor.getColumnIndex("eventsList"));
                    if (existingEventsInfo == null) {
                        existingEventsInfo = "";
                    }
                    updatedEventsInfo = existingEventsInfo + updatedEventsInfo;
                }
                if (cursor != null) {
                    cursor.close();
                }
                values.put("eventsList", updatedEventsInfo);
                int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
                if (affectedRows > 0) {
                    Log.d("Data updated successfully.", "Data updated successfully.");
                } else {
                    Log.e("Error updating data:", "No data updated.");
                }
                db.close();
                Toast.makeText(EventActivity.this, "Добавлено в планы!", Toast.LENGTH_SHORT).show();
            });
        }
        @Override
        public int getItemCount() {
            return eventsDataList.size();
        }
        public class EventsViewHolder extends RecyclerView.ViewHolder {
            public ImageView eventIcon;
            public TextView eventTextView;
            public TextView eventAddressTextView;
            public TextView eventStart;
            public ImageButton addEvent;

            public EventsViewHolder(View itemView) {
                super(itemView);
                eventTextView = itemView.findViewById(R.id.eventTextView);
                eventAddressTextView = itemView.findViewById(R.id.eventAddressTextView);
                eventStart = itemView.findViewById(R.id.eventStart);
                eventIcon = itemView.findViewById(R.id.eventIcon);
                addEvent = itemView.findViewById(R.id.addEvent);
            }
        }
    }
}