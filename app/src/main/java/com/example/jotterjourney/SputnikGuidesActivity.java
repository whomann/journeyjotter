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
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class SputnikGuidesActivity extends AppCompatActivity {
    private String sputnikUsername, sputnikApiKey; private int selectedTripId; ImageButton previousSputnikPageButton; ProgressBar progressBar; RecyclerView recyclerView; ArrayList<ArrayList<Object>> guidesList = new ArrayList<>(); private int regionId; private SQLiteDatabase db; private String targetLocation; private int page = 1; ImageButton searchSputnikTourButton, nextSputnikPageButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sputnik_guides);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            sputnikUsername = properties.getProperty("sputnikUsername");
            sputnikApiKey = properties.getProperty("sputnikApiKey");
        } catch (IOException e) {
            e.printStackTrace();
        }
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        previousSputnikPageButton=findViewById(R.id.previousSputnikPageButton);
        previousSputnikPageButton.setVisibility(View.GONE);
        progressBar = findViewById(R.id.progressBarGuide);
        progressBar.setVisibility(View.VISIBLE);
        nextSputnikPageButton=findViewById(R.id.nextSputnikPageButton);
        nextSputnikPageButton.setVisibility(View.GONE);
        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        readAndLogDataFromSQLite(selectedTripId);
        recyclerView=findViewById(R.id.recyclerViewGuides);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        previousSputnikPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GuidesAdapter guidesAdapter= new GuidesAdapter(new ArrayList<>());
                recyclerView.setAdapter(guidesAdapter);
                guidesAdapter.clearData();

                page -= 1;
                if(page>0){
                    progressBar.setVisibility(View.VISIBLE);
                    new SputnikApiTask().execute();
                }
                else{
                    Toast.makeText(SputnikGuidesActivity.this, "Это первая страница", Toast.LENGTH_SHORT).show();
                }
            }
        });
        nextSputnikPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GuidesAdapter guidesAdapter= new GuidesAdapter(new ArrayList<>());
                recyclerView.setAdapter(guidesAdapter);
                guidesAdapter.clearData();

                page += 1;
                progressBar.setVisibility(View.VISIBLE);
                new SputnikApiTask().execute();
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
                    targetLocation = cursor.getString(cursor.getColumnIndex("targetLocation"));
                    String activitiesList= cursor.getString(cursor.getColumnIndex("activitiesList"));
                    Log.d("SQLite Data", "targetLocation " + targetLocation);
                    Log.d("SQLite Data", "activitiesList " + activitiesList);
                    new SputnikLocationApiTask().execute();
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
    public class SputnikLocationApiTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL("https://api.sputnik8.com/v1/cities?api_key="+sputnikApiKey+"&username="+sputnikUsername);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStreamReader reader = new InputStreamReader(urlConnection.getInputStream());
                    JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                        if (jsonObject.has("region_id") && jsonObject.has("name")) {
                            int region_Id = jsonObject.get("region_id").getAsInt();
                            String name = jsonObject.get("name").getAsString();
                            if (name.equals(targetLocation)) {
                                regionId=region_Id;
                            }
                        } else {
                            Log.d("Response is missing expected fields",targetLocation);
                            Toast.makeText(SputnikGuidesActivity.this, "Экскурсии кончились!", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("Error performing HTTP request", String.valueOf(e));
                Toast.makeText(SputnikGuidesActivity.this, "Ошибка соединения.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void unused) {
            Log.d("loc id", String.valueOf(regionId));
            if(regionId!=0) {
                page = 1;
                progressBar.setVisibility(View.VISIBLE);
                GuidesAdapter guidesAdapter = new GuidesAdapter(new ArrayList<>());
                recyclerView.setAdapter(guidesAdapter);
                guidesAdapter.clearData();
                nextSputnikPageButton.setVisibility(View.GONE);
                previousSputnikPageButton.setVisibility(View.GONE);
                new SputnikApiTask().execute();
            }
            else{
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SputnikGuidesActivity.this, "Этот регион не поддерживается.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private class SputnikApiTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL("https://api.sputnik8.com/v1/products?city_id="+regionId+"&limit=20&page="+page+"&api_key="+sputnikApiKey+"&username="+sputnikUsername);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(in);
                    JsonReader jsonReader = new JsonReader(reader);
                    guidesList.clear();
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        parseGuide(jsonReader);
                    }
                    jsonReader.endArray();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("SputnikApiTask", "Error performing HTTP request", e);
            }
            return null;
        }

        private void parseGuide(JsonReader jsonReader) throws IOException {
            jsonReader.beginObject();
            String title="",language="",description="",url="",mainPhotoUrl="",whatIncluded="",beginPlaceAddress="",placesToSee="",price="",duration="";
            double reviewRating=0.0;

            while (jsonReader.hasNext()) {
                String fieldName = jsonReader.nextName();
                switch (fieldName) {
                    case "title":
                        title = jsonReader.nextString();
                        break;
                    case "languages":
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            language = jsonReader.nextString();
                        }
                        jsonReader.endArray();
                        break;
                    case "description":
                        description = jsonReader.nextString();
                        break;
                    case "url":
                        url = jsonReader.nextString();
                        break;
                    case "main_photo":
                        if (jsonReader.peek() == JsonToken.NULL) {
                            jsonReader.nextNull();
                            mainPhotoUrl = "https://i.imgur.com/lqaLd4a.png";
                        } else {
                            jsonReader.beginObject();
                            mainPhotoUrl = "https://i.imgur.com/lqaLd4a.png";
                            while (jsonReader.hasNext()) {
                                String photoFieldName = jsonReader.nextName();
                                if (photoFieldName.equals("big")) {
                                    mainPhotoUrl = jsonReader.nextString();
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        }
                        break;
                    case "customers_review_rating":
                        reviewRating = jsonReader.nextDouble();
                        break;
                    case "what_included":
                        whatIncluded = jsonReader.nextString();
                        break;
                    case "begin_place":
                        if (jsonReader.peek() == JsonToken.NULL) {
                            jsonReader.nextNull();
                            Log.d("begin_place_address", "NULL");
                        } else {
                            jsonReader.beginObject();
                            beginPlaceAddress = "";
                            while (jsonReader.hasNext()) {
                                String placeFieldName = jsonReader.nextName();
                                if (placeFieldName.equals("address")) {
                                    beginPlaceAddress = jsonReader.nextString();
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        }
                        break;
                    case "places_to_see":
                        placesToSee = jsonReader.nextString();
                        break;
                    case "duration":
                        duration = jsonReader.nextString();
                        break;
                    case "price":
                        price = jsonReader.nextString();
                        break;
                    default:
                        jsonReader.skipValue();
                        break;
                }
            }
            jsonReader.endObject();
            ArrayList<Object> guidesData = new ArrayList<>(Arrays.asList(title,language,description,url,mainPhotoUrl,whatIncluded,beginPlaceAddress,placesToSee,price,duration,reviewRating));
            guidesList.add(guidesData);
        }
        @Override
        protected void onPostExecute(Void result) {
            if(guidesList.isEmpty()) {
                Toast.makeText(SputnikGuidesActivity.this, "Экскурсий не нашлось.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
            else{
                if(page>1){
                    previousSputnikPageButton.setVisibility(View.VISIBLE);
                }
                else{
                    previousSputnikPageButton.setVisibility(View.GONE);
                }
                if(guidesList.size()<20){
                    nextSputnikPageButton.setVisibility(View.GONE);
                }
                else{
                    nextSputnikPageButton.setVisibility(View.VISIBLE);
                }
                progressBar.setVisibility(View.GONE);
                RecyclerView recyclerView = findViewById(R.id.recyclerViewGuides);
                GuidesAdapter guidesAdapter = new GuidesAdapter(guidesList);
                recyclerView.setAdapter(guidesAdapter);
            }
        }
    }
    public class GuidesAdapter extends RecyclerView.Adapter<SputnikGuidesActivity.GuidesAdapter.GuidesViewHolder> {

        private List<ArrayList<Object>> guidesDataList;

        public GuidesAdapter(List<ArrayList<Object>> guidesDataList) {
            this.guidesDataList = guidesDataList;
        }
        public void clearData() {
            guidesDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SputnikGuidesActivity.GuidesAdapter.GuidesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sputnik_guide_card, parent, false);
            return new SputnikGuidesActivity.GuidesAdapter.GuidesViewHolder(view);
        }
        private void openGuideLink(String link) {
            if (link != null && !link.isEmpty()) {
                Uri uri = Uri.parse(link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        }

        @SuppressLint("Range")
        @Override
        public void onBindViewHolder(@NonNull SputnikGuidesActivity.GuidesAdapter.GuidesViewHolder holder, int position) {
            ArrayList<Object> guidesData = guidesDataList.get(position);
            //title,language,description,url,mainPhotoUrl,whatIncluded,beginPlaceAddress,placesToSee,price,duration,reviewRating));
            String title = guidesData.get(0).toString().trim();
            String language = guidesData.get(1).toString().trim();
            String description = guidesData.get(2).toString().trim();
            String link = guidesData.get(3).toString()+"#to_order_form";
            String mainPhotoUrl = guidesData.get(4).toString();
            String whatIncluded = guidesData.get(5).toString().trim();
            String beginPlaceAddress = guidesData.get(6).toString().trim();
            String placesToSee = guidesData.get(7).toString().trim();
            String price = guidesData.get(8).toString();
            String duration = guidesData.get(9).toString().trim();
            String reviewRating = guidesData.get(10).toString().trim();

            updateImage(holder.guideImageView, mainPhotoUrl);
            holder.titleTextView.setText(title+" • "+duration);
            holder.ratingTextView.setText(reviewRating);
            holder.descriptionTextView.setText(description);
            holder.placesToSeeTextView.setText(placesToSee);
            holder.whatIncludedTextView.setText(whatIncluded);
            holder.beginPlaceTextView.setText("Начальная точка: "+beginPlaceAddress);
            holder.languageTextView.setText(language);
            holder.guidePriceTextView.setText(price+"\nза человека");

            holder.buyGuideButton.setOnClickListener(v -> {
                openGuideLink(link);
            });
            holder.boughtGuideButton.setOnClickListener(v -> {
                String updatedActivitiesInfo = title+" • "+duration+" за "+price+" ("+mainPhotoUrl+"), link: "+link+"\n";
                int userId = selectedTripId;
                SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
                ContentValues values = new ContentValues();
                Cursor cursor = db.rawQuery("SELECT activitiesList FROM jjtrips1 WHERE userId=?", new String[]{String.valueOf(userId)});
                if (cursor != null && cursor.moveToFirst()) {
                    String existingActivitiesInfo = cursor.getString(cursor.getColumnIndex("activitiesList"));
                    if (existingActivitiesInfo == null) {
                        existingActivitiesInfo = "";
                    }
                    updatedActivitiesInfo = existingActivitiesInfo + updatedActivitiesInfo;
                }
                if (cursor != null) {
                    cursor.close();
                }
                values.put("activitiesList", updatedActivitiesInfo);
                int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
                if (affectedRows > 0) {
                    Log.d("Data updated successfully.", "Data updated successfully.");
                } else {
                    Log.e("Error updating data:", "No data updated.");
                }
                db.close();
                Toast.makeText(SputnikGuidesActivity.this, "Экскурсия забронирована!", Toast.LENGTH_SHORT).show();
            });

            holder.descriptionTextView.post(() -> {
                int maxHeight = 90;
                int height = holder.descriptionTextView.getLineHeight() * holder.descriptionTextView.getLineCount();
                int maxHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxHeight, holder.itemView.getResources().getDisplayMetrics());
                ViewGroup.LayoutParams params = holder.descriptionTextView.getLayoutParams();
                if (height > maxHeightPx) {
                    params.height = maxHeightPx;
                    holder.descriptionTextView.setLayoutParams(params);
                    holder.descriptionTextView.setTag(true);
                    showExpandIndicator(holder.descriptionTextView);
                } else {
                    hideExpandIndicator(holder.descriptionTextView);
                }
            });
            holder.descriptionTextView.setOnClickListener(v -> {
                toggleTextView(holder.descriptionTextView);
            });
        }

        private void showExpandIndicator(TextView textView) {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.downpurple);
        }

        private void hideExpandIndicator(TextView textView) {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        public void toggleTextView(TextView descriptionTextView) {
            int originalHeight = descriptionTextView.getLineHeight() * descriptionTextView.getLineCount();
            int maxHeight = 90;
            int maxHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxHeight, descriptionTextView.getResources().getDisplayMetrics());
            ViewGroup.LayoutParams params = descriptionTextView.getLayoutParams();
            if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT || params.height == originalHeight) {
                params.height = maxHeightPx;
                hideExpandIndicator(descriptionTextView);
            } else {
                params.height = originalHeight;
                showExpandIndicator(descriptionTextView);
            }
            descriptionTextView.setLayoutParams(params);
        }
        @Override
        public int getItemCount() {
            return guidesDataList.size();
        }
        private void updateImage(ImageView imageView, String imageUrl) {
            Picasso.get().load(imageUrl).into(imageView);
        }
        public class GuidesViewHolder extends RecyclerView.ViewHolder {

            public ImageView guideImageView;
            public TextView titleTextView;
            public TextView ratingTextView;
            public TextView whatIncludedTextView;
            public TextView descriptionTextView;
            public TextView placesToSeeTextView;
            public TextView beginPlaceTextView;
            public TextView languageTextView;
            public TextView guidePriceTextView;
            public Button buyGuideButton;
            public Button boughtGuideButton;

            public GuidesViewHolder(View itemView) {
                super(itemView);
                guideImageView = itemView.findViewById(R.id.guideImageView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                ratingTextView = itemView.findViewById(R.id.ratingTextView);
                descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
                placesToSeeTextView = itemView.findViewById(R.id.placesToSeeTextView);
                beginPlaceTextView = itemView.findViewById(R.id.beginPlaceTextView);
                languageTextView = itemView.findViewById(R.id.languageTextView);
                guidePriceTextView = itemView.findViewById(R.id.guidePriceTextView);
                whatIncludedTextView = itemView.findViewById(R.id.whatIsIncludedTextView);
                buyGuideButton = itemView.findViewById(R.id.buyGuideButton);
                boughtGuideButton = itemView.findViewById(R.id.boughtGuideButton);
            }
        }
    }
}