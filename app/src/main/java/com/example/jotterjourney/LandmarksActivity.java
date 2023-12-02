package com.example.jotterjourney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class LandmarksActivity extends AppCompatActivity {
    private String googleMapsApiKey; private int selectedTripId; private String targetLocation; private SQLiteDatabase db; ProgressBar progressBarLandmarks;private GoogleMap mMap; ViewPager2 viewPager; private String nextPageToken=""; RecyclerView recyclerView; ArrayList<ArrayList<Object>> landmarksList=new ArrayList<>(); private MapView mMapView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landmarks);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            googleMapsApiKey = properties.getProperty("googleMapsApiKey");
        } catch (IOException e) {
            e.printStackTrace();
        }
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        recyclerView = findViewById(R.id.recyclerViewLandmarks);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        mMapView = findViewById(R.id.mapViewLandmarks);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this::onMapReady);
        progressBarLandmarks=findViewById(R.id.progressBarLandmarks);
        progressBarLandmarks.setVisibility(View.VISIBLE);
        readAndLogDataFromSQLite(selectedTripId);
    }
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }
    @SuppressLint("Range")
    private void readAndLogDataFromSQLite(int selectedTripId) {
        Cursor cursor = db.rawQuery("SELECT * FROM jjtrips1 WHERE userID = ?", new String[]{String.valueOf(selectedTripId)});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int userID = cursor.getInt(cursor.getColumnIndex("userID"));
                    targetLocation = cursor.getString(cursor.getColumnIndex("targetLocation"));
                    TextView landmarksLabel=findViewById(R.id.landmarksLabel);
                    landmarksLabel.setText("Достопримечательности в городе "+targetLocation);
                    Log.d("SQLite Data", "targetLocation " + targetLocation);
                    String apiUrl="https://maps.googleapis.com/maps/api/place/textsearch/json?query=tourist+attractions+in+"+targetLocation+"&key="+googleMapsApiKey+"&language=ru&fields=photos&pagetoken="+nextPageToken;
                    Log.d("apiUrl",apiUrl);
                    new PlaceDetailsAsyncTask(landmarksList).execute(apiUrl);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
    private void showMapMarkers(ArrayList<ArrayList<Object>> arrayList){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < arrayList.size(); i++) {
            ArrayList<Object> landmarkDataMap = arrayList.get(i);
            String landmarkNameMap = landmarkDataMap.get(0).toString();
            double landmarkLatValueMap = (double) landmarkDataMap.get(4);
            double landmarkLonValueMap = (double) landmarkDataMap.get(5);
            LatLng landmarkLocation = new LatLng(landmarkLatValueMap, landmarkLonValueMap);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(landmarkLocation)
                    .title(landmarkNameMap);
            mMap.addMarker(markerOptions);
            builder.include(landmarkLocation);
        }
        LatLngBounds bounds = builder.build();
        int padding = 50;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.moveCamera(cameraUpdate);
    }
    private String getPlaceDetailsJson(String placeDetailsUrl) {
        try {
            URL url = new URL(placeDetailsUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = urlConnection.getInputStream();
                Scanner scanner = new Scanner(in);
                scanner.useDelimiter("\\A");

                boolean hasInput = scanner.hasNext();
                if (hasInput) {
                    return scanner.next();
                } else {
                    return null;
                }
            } finally {
                urlConnection.disconnect();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class PlaceDetailsAsyncTask extends AsyncTask<String, Void, Void> {

        private ArrayList<ArrayList<Object>> landmarksList;

        public PlaceDetailsAsyncTask(ArrayList<ArrayList<Object>> landmarksList) {
            this.landmarksList = landmarksList;
        }

        @Override
        protected Void doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }
            String placeDetailsJsonString = getPlaceDetailsJson(params[0]);
            if (placeDetailsJsonString != null) {
                try {
                    JSONObject placeDetailsObject = new JSONObject(placeDetailsJsonString);
                    Log.d("placeDetailsObject 1", String.valueOf(placeDetailsObject));
                    if (placeDetailsObject.has("next_page_token")) {
                        nextPageToken = placeDetailsObject.getString("next_page_token");
                    }
                    if (placeDetailsObject.has("results")) {
                        JSONArray resultsArray = placeDetailsObject.getJSONArray("results");
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject result = resultsArray.getJSONObject(i);
                            String name = result.optString("name", "");
                            String formattedAddress = result.optString("formatted_address", "");
                            double rating = result.optDouble("rating", Double.NaN);

                            String photoReference = null;
                            JSONArray photosArray = result.optJSONArray("photos");
                            if (photosArray != null && photosArray.length() > 0) {
                                JSONObject firstPhoto = photosArray.getJSONObject(0);
                                photoReference = firstPhoto.optString("photo_reference", null);
                                photoReference = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + photoReference + "&key="+googleMapsApiKey;
                            }
                            double latitude=0.0; double longitude=0.0;
                            JSONObject geometry = result.optJSONObject("geometry");
                            if (geometry != null) {
                                JSONObject location = geometry.optJSONObject("location");
                                if (location != null) {
                                    latitude = location.optDouble("lat", Double.NaN);
                                    longitude = location.optDouble("lng", Double.NaN);
                                }
                            }
                            ArrayList<Object> placeDetails = new ArrayList<>();
                            placeDetails.add(name);
                            placeDetails.add(photoReference);
                            placeDetails.add(formattedAddress);
                            placeDetails.add(rating);
                            placeDetails.add(latitude);
                            placeDetails.add(longitude);
                            landmarksList.add(placeDetails);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                Toast.makeText(LandmarksActivity.this, "Не найдены достопримечательности в выбранной локации.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            String apiUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=tourist+attractions+in+"+targetLocation+"&key="+googleMapsApiKey+"&language=ru&fields=photos&pagetoken=" + nextPageToken;
            Log.d("apiUrl", apiUrl);
            Log.d("landmarksList 1", String.valueOf(landmarksList));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    new PlaceDetailsPageTwoAsyncTask(new ArrayList<>()).execute(apiUrl);
                }
            }, 3000);
        }
    }
    public class PlaceDetailsPageTwoAsyncTask extends AsyncTask<String, Void, Void> {

        private ArrayList<ArrayList<Object>> landmarksListPageTwo;

        public PlaceDetailsPageTwoAsyncTask(ArrayList<ArrayList<Object>> landmarksListPageTwo) {
            this.landmarksListPageTwo = landmarksListPageTwo;
        }

        @Override
        protected Void doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }
            String placeDetailsJsonString = getPlaceDetailsJson(params[0]);
            if (placeDetailsJsonString != null) {
                try {
                    JSONObject placeDetailsObject = new JSONObject(placeDetailsJsonString);
                    Log.d("placeDetailsObject 2", String.valueOf(placeDetailsObject));
                    if (placeDetailsObject.has("results")) {
                        JSONArray resultsArray = placeDetailsObject.getJSONArray("results");
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject result = resultsArray.getJSONObject(i);
                            String name = result.optString("name", "");
                            String formattedAddress = result.optString("formatted_address", "");
                            double rating = result.optDouble("rating", Double.NaN);

                            String photoReference = null;
                            JSONArray photosArray = result.optJSONArray("photos");
                            if (photosArray != null && photosArray.length() > 0) {
                                JSONObject firstPhoto = photosArray.getJSONObject(0);
                                photoReference = firstPhoto.optString("photo_reference", null);
                                photoReference="https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference="+photoReference+"&key="+googleMapsApiKey;
                            }
                            double latitude=0.0; double longitude=0.0;
                            JSONObject geometry = result.optJSONObject("geometry");
                            if (geometry != null) {
                                JSONObject location = geometry.optJSONObject("location");
                                if (location != null) {
                                    latitude = location.optDouble("lat", Double.NaN);
                                    longitude = location.optDouble("lng", Double.NaN);
                                }
                            }
                            ArrayList<Object> placeDetailsPageTwo = new ArrayList<>();
                            placeDetailsPageTwo.add(name);
                            placeDetailsPageTwo.add(photoReference);
                            placeDetailsPageTwo.add(formattedAddress);
                            placeDetailsPageTwo.add(rating);
                            placeDetailsPageTwo.add(latitude);
                            placeDetailsPageTwo.add(longitude);
                            landmarksListPageTwo.add(placeDetailsPageTwo);
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("landmarks list 2", String.valueOf(landmarksListPageTwo));
            landmarksList.addAll(landmarksListPageTwo);
            Log.d("landmarksList result", String.valueOf(landmarksList));
            Log.d("landmarksList len", String.valueOf(landmarksList.size()));
            showMapMarkers(landmarksList);
            progressBarLandmarks.setVisibility(View.GONE);
            LandmarkAdapter adapter = new LandmarkAdapter(landmarksList);
            recyclerView.setAdapter(adapter);
            PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
            pagerSnapHelper.attachToRecyclerView(recyclerView);
        }
    }
    public class LandmarkAdapter extends RecyclerView.Adapter<LandmarksActivity.LandmarkAdapter.LandmarksViewHolder> {

        private List<ArrayList<Object>> landmarksDataList;

        public LandmarkAdapter(List<ArrayList<Object>> landmarksDataList) {
            this.landmarksDataList = landmarksDataList;
        }

        public void clearData() {
            landmarksDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LandmarksActivity.LandmarkAdapter.LandmarksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.landmark_card, parent, false);
            return new LandmarksActivity.LandmarkAdapter.LandmarksViewHolder(view);
        }
        private void setImageViewAspectRatio(ImageView imageView, int targetWidth) {
            int height = (int) (targetWidth / 1.5);
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.width = targetWidth;
            params.height = height;
            imageView.setLayoutParams(params);
        }

        @SuppressLint("Range")
        @Override
        public void onBindViewHolder(@NonNull LandmarksActivity.LandmarkAdapter.LandmarksViewHolder holder, int position) {
            ArrayList<Object> landmarkData = landmarksDataList.get(position);
            String title = landmarkData.get(0).toString();
            String address = landmarkData.get(2).toString();
            String mainPhotoUrl = landmarkData.get(1) != null ? landmarkData.get(1).toString() : "file:///android_asset/placeholder.png";
            if (!mainPhotoUrl.equals("file:///android_asset/placeholder.png")) {
                int targetWidth = 850;
                setImageViewAspectRatio(holder.landmarkImageView, targetWidth);
                updateImage(holder.landmarkImageView, mainPhotoUrl);
            } else {
                holder.landmarkImageView.setImageResource(R.drawable.placeholder);
            }

            holder.landmarkTitle.setText(title);
            holder.landmarkAddress.setText(address);

            holder.addLandmarkButton.setOnClickListener(v -> {
                String updatedLandmarksInfo = title+" ("+mainPhotoUrl+"), "+address+"\n";
                int userId = selectedTripId;
                SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
                ContentValues values = new ContentValues();
                Cursor cursor = db.rawQuery("SELECT landmarksList FROM jjtrips1 WHERE userId=?", new String[]{String.valueOf(userId)});
                if (cursor != null && cursor.moveToFirst()) {
                    String existingLandmarksInfo = cursor.getString(cursor.getColumnIndex("landmarksList"));
                    if (existingLandmarksInfo == null) {
                        existingLandmarksInfo = "";
                    }
                    updatedLandmarksInfo = existingLandmarksInfo + updatedLandmarksInfo;
                }
                if (cursor != null) {
                    cursor.close();
                }
                values.put("landmarksList", updatedLandmarksInfo);
                int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
                if (affectedRows > 0) {
                    Log.d("Data updated successfully.", "Data updated successfully.");
                } else {
                    Log.e("Error updating data:", "No data updated.");
                }
                db.close();
            });
        }

        @Override
        public int getItemCount() {
            return landmarksDataList.size();
        }

        private void updateImage(ImageView imageView, String imageUrl) {
            Picasso.get().load(imageUrl).into(imageView);
        }

        public class LandmarksViewHolder extends RecyclerView.ViewHolder {

            public ImageView landmarkImageView;
            public TextView landmarkTitle;
            public TextView landmarkAddress;
            public Button addLandmarkButton;

            public LandmarksViewHolder(View itemView) {
                super(itemView);
                landmarkImageView = itemView.findViewById(R.id.imageViewPhoto);
                landmarkTitle = itemView.findViewById(R.id.textViewName);
                landmarkAddress = itemView.findViewById(R.id.textViewAddress);
                addLandmarkButton = itemView.findViewById(R.id.addLandmarkButton);
            }
        }
    }
}