package com.example.jotterjourney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class LandmarksActivity extends AppCompatActivity {
    ImageView landmarkLoading, loadingLandmarkGif; HashMap<String, String> tagTranslations = new HashMap<>(); private String googleMapsApiKey,englishTargetName; private int selectedTripId; private String targetLocation; private SQLiteDatabase db; ProgressBar progressBarLandmarks;private GoogleMap mMap; ViewPager2 viewPager;  RecyclerView recyclerView; ArrayList<ArrayList<Object>> landmarksList=new ArrayList<>(); private MapView mMapView;

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
        landmarkLoading=findViewById(R.id.landmarkLoading);
        loadingLandmarkGif=findViewById(R.id.loadingLandmarkGif);
        loadingLandmarkGif.setVisibility(View.VISIBLE);
        landmarkLoading.setVisibility(View.VISIBLE);
        Glide.with(this).load(R.drawable.loadingscreen).into(loadingLandmarkGif);
        addTagsTranslations();
        ImageButton landmarksBack=findViewById(R.id.landmarksBack);
        landmarksBack.setOnClickListener(v->{
            onBackPressed();
        });
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        englishTargetName=getIntent().getStringExtra("englishTargetName");
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

    private void addTagsTranslations() {
        tagTranslations.put("accounting", "бухгалтерия");
        tagTranslations.put("airport", "аэропорт");
        tagTranslations.put("amusement_park", "парк развлечений");
        tagTranslations.put("aquarium", "аквариум");
        tagTranslations.put("art_gallery", "художественная галерея");
        tagTranslations.put("atm", "банкомат");
        tagTranslations.put("bakery", "пекарня");
        tagTranslations.put("bank", "банк");
        tagTranslations.put("bar", "бар");
        tagTranslations.put("beauty_salon", "салон красоты");
        tagTranslations.put("bicycle_store", "магазин велосипедов");
        tagTranslations.put("book_store", "книжный магазин");
        tagTranslations.put("bowling_alley", "боулинг");
        tagTranslations.put("bus_station", "автобусная станция");
        tagTranslations.put("cafe", "кафе");
        tagTranslations.put("campground", "кемпинг");
        tagTranslations.put("car_dealer", "автосалон");
        tagTranslations.put("car_rental", "прокат автомобилей");
        tagTranslations.put("car_repair", "авторемонт");
        tagTranslations.put("car_wash", "автомойка");
        tagTranslations.put("casino", "казино");
        tagTranslations.put("cemetery", "кладбище");
        tagTranslations.put("church", "церковь");
        tagTranslations.put("city_hall", "городская ратуша");
        tagTranslations.put("clothing_store", "магазин одежды");
        tagTranslations.put("convenience_store", "удобный магазин");
        tagTranslations.put("courthouse", "суд");
        tagTranslations.put("dentist", "стоматолог");
        tagTranslations.put("department_store", "универмаг");
        tagTranslations.put("doctor", "врач");
        tagTranslations.put("drugstore", "аптека");
        tagTranslations.put("electrician", "электрик");
        tagTranslations.put("electronics_store", "магазин электроники");
        tagTranslations.put("embassy", "посольство");
        tagTranslations.put("fire_station", "пожарная станция");
        tagTranslations.put("florist", "цветочный магазин");
        tagTranslations.put("funeral_home", "похоронное бюро");
        tagTranslations.put("furniture_store", "магазин мебели");
        tagTranslations.put("gas_station", "заправка");
        tagTranslations.put("gym", "тренажерный зал");
        tagTranslations.put("hair_care", "парикмахерская");
        tagTranslations.put("hardware_store", "хозяйственный магазин");
        tagTranslations.put("hindu_temple", "храм индуизма");
        tagTranslations.put("home_goods_store", "товары для дома");
        tagTranslations.put("hospital", "больница");
        tagTranslations.put("insurance_agency", "страховое агентство");
        tagTranslations.put("jewelry_store", "ювелирный магазин");
        tagTranslations.put("laundry", "прачечная");
        tagTranslations.put("lawyer", "юрист");
        tagTranslations.put("library", "библиотека");
        tagTranslations.put("light_rail_station", "железнодорожный вокзал");
        tagTranslations.put("liquor_store", "винный магазин");
        tagTranslations.put("local_government_office", "офис местного правительства");
        tagTranslations.put("locksmith", "слесарь");
        tagTranslations.put("lodging", "жилье");
        tagTranslations.put("meal_delivery", "доставка еды");
        tagTranslations.put("meal_takeaway", "еда на вынос");
        tagTranslations.put("mosque", "мечеть");
        tagTranslations.put("movie_rental", "прокат фильмов");
        tagTranslations.put("movie_theater", "кинотеатр");
        tagTranslations.put("moving_company", "грузоперевозки");
        tagTranslations.put("museum", "музей");
        tagTranslations.put("night_club", "ночной клуб");
        tagTranslations.put("painter", "художник");
        tagTranslations.put("park", "парк");
        tagTranslations.put("parking", "парковка");
        tagTranslations.put("pet_store", "зоомагазин");
        tagTranslations.put("pharmacy", "аптека");
        tagTranslations.put("physiotherapist", "физиотерапевт");
        tagTranslations.put("plumber", "сантехник");
        tagTranslations.put("police", "полиция");
        tagTranslations.put("post_office", "почта");
        tagTranslations.put("primary_school", "начальная школа");
        tagTranslations.put("real_estate_agency", "агентство недвижимости");
        tagTranslations.put("restaurant", "ресторан");
        tagTranslations.put("roofing_contractor", "кровельщик");
        tagTranslations.put("rv_park", "автокемпинг");
        tagTranslations.put("school", "школа");
        tagTranslations.put("secondary_school", "средняя школа");
        tagTranslations.put("shoe_store", "обувной магазин");
        tagTranslations.put("shopping_mall", "торговый центр");
        tagTranslations.put("spa", "спа");
        tagTranslations.put("stadium", "стадион");
        tagTranslations.put("storage", "хранение");
        tagTranslations.put("store", "магазин");
        tagTranslations.put("subway_station", "станция метро");
        tagTranslations.put("supermarket", "супермаркет");
        tagTranslations.put("synagogue", "синагога");
        tagTranslations.put("taxi_stand", "остановка такси");
        tagTranslations.put("tourist_attraction", "достопримечательность");
        tagTranslations.put("train_station", "железнодорожная станция");
        tagTranslations.put("transit_station", "транзитная станция");
        tagTranslations.put("travel_agency", "турагентство");
        tagTranslations.put("university", "университет");
        tagTranslations.put("veterinary_care", "ветеринарная помощь");
        tagTranslations.put("zoo", "заповедник");
        tagTranslations.put("administrative_area_level_1", "административный уровень 1");
        tagTranslations.put("administrative_area_level_2", "административный уровень 2");
        tagTranslations.put("administrative_area_level_3", "административный уровень 3");
        tagTranslations.put("administrative_area_level_4", "административный уровень 4");
        tagTranslations.put("administrative_area_level_5", "административный уровень 5");
        tagTranslations.put("administrative_area_level_6", "административный уровень 6");
        tagTranslations.put("administrative_area_level_7", "административный уровень 7");
        tagTranslations.put("archipelago", "архипелаг");
        tagTranslations.put("colloquial_area", "разговорная зона");
        tagTranslations.put("continent", "континент");
        tagTranslations.put("country", "страна");
        tagTranslations.put("establishment", "учреждение");
        tagTranslations.put("finance", "финансы");
        tagTranslations.put("floor", "этаж");
        tagTranslations.put("food", "продукты");
        tagTranslations.put("general_contractor", "генеральный подрядчик");
        tagTranslations.put("geocode", "геокод");
        tagTranslations.put("health", "здоровье");
        tagTranslations.put("intersection", "пересечение");
        tagTranslations.put("landmark", "ориентир");
        tagTranslations.put("locality", "населенный пункт");
        tagTranslations.put("natural_feature", "естественная среда");
        tagTranslations.put("neighborhood", "район");
        tagTranslations.put("place_of_worship", "место поклонения");
        tagTranslations.put("plus_code", "плюс-код");
        tagTranslations.put("point_of_interest", "точка интереса");
        tagTranslations.put("political", "политика");
        tagTranslations.put("post_box", "почтовый ящик");
        tagTranslations.put("postal_code", "почтовый индекс");
        tagTranslations.put("postal_code_prefix", "префикс почтового индекса");
        tagTranslations.put("postal_code_suffix", "суффикс почтового индекса");
        tagTranslations.put("postal_town", "почтовый город");
        tagTranslations.put("premise", "помещение");
        tagTranslations.put("room", "комната");
        tagTranslations.put("route", "маршрут");
        tagTranslations.put("street_address", "улица, дом");
        tagTranslations.put("street_number", "номер дома");
        tagTranslations.put("sublocality", "местность");
        tagTranslations.put("sublocality_level_1", "местность, уровень 1");
        tagTranslations.put("sublocality_level_2", "местность, уровень 2");
        tagTranslations.put("sublocality_level_3", "местность, уровень 3");
        tagTranslations.put("sublocality_level_4", "местность, уровень 4");
        tagTranslations.put("sublocality_level_5", "местность, уровень 5");
        tagTranslations.put("subpremise", "подпомещение");
        tagTranslations.put("town_square", "городская площадь");
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
    public void onBackPressed() {
        super.onBackPressed();
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
                    landmarksLabel.setText(targetLocation);
                    Log.d("SQLite Data", "targetLocation " + targetLocation);
                    String locationForApiUrl = (englishTargetName != null && !englishTargetName.isEmpty()) ? englishTargetName : targetLocation;
                    String apiUrl="https://maps.googleapis.com/maps/api/place/textsearch/json?query=tourist+attractions+in+"+locationForApiUrl+"&key="+googleMapsApiKey+"&language=ru&fields=photos&pagetoken=";
                    Log.d("apiUrl 1",apiUrl);
                    String nextPageToken="";
                    new PlaceDetailsAsyncTask(landmarksList, nextPageToken,locationForApiUrl).execute(apiUrl);
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
        private String nextPageToken;
        private String locationForApiUrl;

        public PlaceDetailsAsyncTask(ArrayList<ArrayList<Object>> landmarksList, String nextPageToken,String locationForApiUrl) {
            this.landmarksList = landmarksList;
            this.nextPageToken = nextPageToken;
            this.locationForApiUrl=locationForApiUrl;
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
                        Log.d("nextPageToken",nextPageToken);
                    }
                    if (placeDetailsObject.has("results")) {
                        JSONArray resultsArray = placeDetailsObject.getJSONArray("results");
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject result = resultsArray.getJSONObject(i);
                            String name = result.optString("name", "");
                            String formattedAddress = result.optString("formatted_address", "");
                            int ratingCount = result.optInt("user_ratings_total", 0);

                            ArrayList<String> typesList = new ArrayList<>();
                            if (result.has("types")) {
                                JSONArray typesArray = result.getJSONArray("types");
                                for (int j = 0; j < typesArray.length(); j++) {
                                    typesList.add(typesArray.getString(j));
                                }
                            }

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
                            placeDetails.add(ratingCount);
                            placeDetails.add(latitude);
                            placeDetails.add(longitude);
                            placeDetails.add(typesList);
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
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String apiUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=tourist+attractions+in+"+locationForApiUrl+"&key="+googleMapsApiKey+"&language=ru&fields=photos&pagetoken=" + nextPageToken;
                    Log.d("apiUrl2", apiUrl);
                    Log.d("landmarksList 1", String.valueOf(landmarksList));
                    new PlaceDetailsPageTwoAsyncTask(new ArrayList<>(),nextPageToken).execute(apiUrl);
                }
            }, 3000);
        }
    }
    public class PlaceDetailsPageTwoAsyncTask extends AsyncTask<String, Void, Void> {

        private ArrayList<ArrayList<Object>> landmarksListPageTwo;
        String nextPageToken;

        public PlaceDetailsPageTwoAsyncTask(ArrayList<ArrayList<Object>> landmarksListPageTwo, String nextPageToken) {
            this.landmarksListPageTwo = landmarksListPageTwo;
            this.nextPageToken=nextPageToken;
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
                            int ratingCount = result.optInt("user_ratings_total", 0);

                            ArrayList<String> typesList = new ArrayList<>();
                            if (result.has("types")) {
                                JSONArray typesArray = result.getJSONArray("types");
                                for (int j = 0; j < typesArray.length(); j++) {
                                    typesList.add(typesArray.getString(j));
                                }
                            }

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
                            placeDetailsPageTwo.add(ratingCount);
                            placeDetailsPageTwo.add(latitude);
                            placeDetailsPageTwo.add(longitude);
                            placeDetailsPageTwo.add(typesList);
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
            if(!nextPageToken.equals("")) {
                Log.d("landmarks list 2", String.valueOf(landmarksListPageTwo));
                landmarksList.addAll(landmarksListPageTwo);
            }
            Log.d("landmarksList result", String.valueOf(landmarksList));
            Log.d("landmarksList len", String.valueOf(landmarksList.size()));
            showMapMarkers(landmarksList);
            progressBarLandmarks.setVisibility(View.GONE);
            landmarkLoading.setVisibility(View.GONE);
            loadingLandmarkGif.setVisibility(View.GONE);
            landmarksList=sortAttractions(landmarksList);
            Log.d("sortedlandmarksList", String.valueOf(landmarksList));
            View mainLayout = findViewById(R.id.relativeLayout);
            LandmarkAdapter adapter = new LandmarkAdapter(landmarksList,mainLayout);
            recyclerView.setAdapter(adapter);
            PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
            pagerSnapHelper.attachToRecyclerView(recyclerView);
        }
    }

    private ArrayList<ArrayList<Object>> sortAttractions(ArrayList<ArrayList<Object>> attractions) {
            Collections.sort(attractions, new Comparator<ArrayList<Object>>() {
                @Override
                public int compare(ArrayList<Object> attraction1, ArrayList<Object> attraction2) {
                    int count1 = (int) attraction1.get(3);
                    int count2 = (int) attraction2.get(3);
                    return Integer.compare(count2, count1);
                }
            });
        return attractions;
    }

    public class LandmarkAdapter extends RecyclerView.Adapter<LandmarksActivity.LandmarkAdapter.LandmarksViewHolder> {

        private List<ArrayList<Object>> landmarksDataList;
        private View mainLayout;

        public LandmarkAdapter(List<ArrayList<Object>> landmarksDataList, View mainLayout) {
            this.landmarksDataList = landmarksDataList;
            this.mainLayout = mainLayout;
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
            String placeholderPath="android.resource://" + holder.itemView.getContext().getPackageName() + "/drawable/placeholder";
            String mainPhotoUrl = landmarkData.get(1) != null ? landmarkData.get(1).toString() : placeholderPath;
            ArrayList<String> tagsList = (ArrayList<String>) landmarkData.get(6);
            Log.d("tagsList", String.valueOf(tagsList));

            int maxCardsToShow = 5;
            for (int i = 1; i <= maxCardsToShow; i++) {
                int cardId = getResources().getIdentifier("landmarkTag" + i, "id", getPackageName());
                CardView cardView = mainLayout.findViewById(cardId);
                if (i <= tagsList.size()) {
                    cardView.setVisibility(View.VISIBLE);
                    int textViewId = getResources().getIdentifier("attractionIcon" + i + "TV", "id", getPackageName());
                    int imageViewId=getResources().getIdentifier("attractionIcon" + i, "id", getPackageName());
                    TextView textView = cardView.findViewById(textViewId);
                    ImageView imageView = cardView.findViewById(imageViewId);
                    String tag = tagsList.get(i - 1);
                    if (tagTranslations.containsKey(tag)) {
                        textView.setText(tagTranslations.get(tag));
                        int drawableResourceId = getResources().getIdentifier(tag.toLowerCase(), "drawable", getPackageName());
                        if (drawableResourceId != 0) {
                            imageView.setImageResource(drawableResourceId);
                        }
                        else if(tag.equals("administrative_area_level_1") || tag.equals("administrative_area_level_2")||tag.equals("administrative_area_level_3")||tag.equals("administrative_area_level_4")||tag.equals("administrative_area_level_5")||tag.equals("administrative_area_level_6")||tag.equals("administrative_area_level_7")){
                                imageView.setImageResource(R.drawable.administrative_area_level);
                        }
                        else if(tag.equals("sublocality_level_1")||tag.equals("sublocality_level_2")||tag.equals("sublocality_level_3")||tag.equals("sublocality_level_4")||tag.equals("sublocality_level_5")){
                            imageView.setImageResource(R.drawable.sublocality_level);
                        }
                        else {
                            imageView.setImageResource(R.drawable.establishment);
                        }
                    } else {
                        textView.setText(tag);
                    }
                } else {
                    cardView.setVisibility(View.GONE);
                }
            }

            if (!mainPhotoUrl.equals(placeholderPath)) {
                int targetWidth = 750;
                setImageViewAspectRatio(holder.landmarkImageView, targetWidth);
                updateImage(holder.landmarkImageView, mainPhotoUrl);
            } else {
                int targetWidth = 750;
                setImageViewAspectRatio(holder.landmarkImageView, targetWidth);
                updateImage(holder.landmarkImageView, placeholderPath);
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
                    Toast.makeText(LandmarksActivity.this, "Добавлено в планы!", Toast.LENGTH_SHORT).show();
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