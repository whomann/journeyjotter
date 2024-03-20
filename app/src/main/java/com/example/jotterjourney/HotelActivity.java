package com.example.jotterjourney;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.jotterjourney.databinding.ActivityMainBinding;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

import java.util.concurrent.atomic.AtomicInteger;

public class HotelActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://cors.eu.org/http://engine.hotellook.com/api/v2/lookup.json";
    private static final String TARGET_API_URL = "https://engine.hotellook.com/api/v2/lookup.json";
    private String proxy_api=""; private String corsApiKey="";ImageButton bookmarksButton; ImageView nothingHereImageView; CardView cardViewMap;long nightsCount; private String apiKey; Map<String, String> propertyTranslations = new HashMap<>(); private List<String> selectedEnglishPropertyTypes = new ArrayList<>(); private int selectedTripId; private double minPrice, maxPrice; RangeSlider slider; private ActivityMainBinding binding; private SQLiteDatabase db; private static final String TAG = "LocationId"; private int locationId; private int adultsCount; private int retriesCount; private String returnDate; private String departureDate; private String type; private ImageButton searchButton; Map<String, Integer> filterMap = new HashMap<>(); private String hotelInfo; private String targetLocation; List<String> selectedFilters = new ArrayList<>(); List<Integer> listOfIDs = new ArrayList<>(); ArrayList<ArrayList<Object>> hotelsList = new ArrayList<>(); ArrayList<ArrayList<Object>> matchingHotelsList = new ArrayList<>(); ArrayList<ArrayList<Object>> noTypeHotelsList = new ArrayList<>();  ArrayList<ArrayList<Object>> finalHotelList = new ArrayList<>(); private String hotelLat, hotelLon; private DrawerLayout drawerLayout; Context context = this; private static final int CONNECTION_TIMEOUT = 10000; private static final int READ_TIMEOUT = 15000; private GoogleMap mMap; private MapView mMapView; private int currentPage = 0; private int batchSize = 50; private CheckBox filterRussian, filterPool, filterFitness, filterLaundry, filterSpa, filterConcierge, filterBusinessCenter, filterSharedBathroom, filterSplitRoom, filterCoffee, filterSlippers, filterMiniBar, filterToiletInRoom, filterPublicWiFi, filterDailyCleaning, filterCleaning, filterSafe, filterTV, filterBath, filterShower, filterDisabled, filterPetsAllowed, filterFan, filterRestaurant, filterAirConditioner, filterCheckIn24hr, filterParking, filterBar, filterSmokingZones, filterPrivateBeach;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        drawerLayout = findViewById(R.id.drawerLayout);
        bookmarksButton=findViewById(R.id.bookmarksButton);
        bookmarksButton.setEnabled(false);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            apiKey = properties.getProperty("hotellookApiKey");
            proxy_api=properties.getProperty("PROXY_URL");
            corsApiKey=properties.getProperty("PROXY_API");
        } catch (IOException e) {
            e.printStackTrace();
        }
        addPropertyTranslations();
        getWindow().setStatusBarColor(Color.parseColor("#c2d5ff"));
        RecyclerView recyclerView = binding.recyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        retriesCount=0;
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        readAndLogDataFromSQLite(selectedTripId);
        currentPage = 0;

        //region filters region
        filterFan = findViewById(R.id.filterFan);
        nothingHereImageView=findViewById(R.id.nothingHereImageView);
        filterDisabled = findViewById(R.id.filterDisabled);
        filterPetsAllowed = findViewById(R.id.filterPetsAllowed);
        filterRestaurant = findViewById(R.id.filterRestaurant);
        filterParking= findViewById(R.id.filterParking);
        filterAirConditioner = findViewById(R.id.filterAirConditioner);
        filterCheckIn24hr = findViewById(R.id.CheckIn24hr);
        filterBar = findViewById(R.id.filterBar);
        filterSmokingZones = findViewById(R.id.filterSmokingZones);
        filterPrivateBeach = findViewById(R.id.filterPrivateBeach);
        filterPublicWiFi = findViewById(R.id.filterPublicWiFi);
        filterRussian = findViewById(R.id.filterRussian);
        filterSafe = findViewById(R.id.filterSafe);
        filterTV = findViewById(R.id.filterTV);
        filterBath = findViewById(R.id.filterBath);
        filterShower = findViewById(R.id.filterShower);
        filterCleaning = findViewById(R.id.filterCleaning);
        filterDailyCleaning = findViewById(R.id.filterDailyCleaning);
        filterMiniBar = findViewById(R.id.filterMiniBar);
        filterToiletInRoom = findViewById(R.id.filterToiletInRoom);
        filterCoffee = findViewById(R.id.filterCoffee);
        filterSlippers = findViewById(R.id.filterSlippers);
        filterSplitRoom = findViewById(R.id.filterSplitRoom);
        filterSharedBathroom = findViewById(R.id.filterSharedBathroom);
        filterBusinessCenter = findViewById(R.id.filterBusinessCenter);
        filterConcierge = findViewById(R.id.filterConcierge);
        filterLaundry = findViewById(R.id.filterLaundry);
        filterSpa = findViewById(R.id.filterSpa);
        filterFitness = findViewById(R.id.filterFitness);
        filterPool = findViewById(R.id.filterPool);
        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        Spinner sortSpinner = findViewById(R.id.sortSpinner);
        HorizontalScrollView horizontalScrollView=findViewById(R.id.horizontalScrollView);
        ImageButton nextPageButton = findViewById(R.id.nextPageButton);
        searchButton = findViewById(R.id.searchButton);
        slider=findViewById(R.id.rangeSlider);
        mMapView = findViewById(R.id.mapViewHotels);
        ImageView imageView7=findViewById(R.id.imageView7);
        imageView7.setImageResource(R.drawable.bg);
        type="Нет";
        Button hostelButton = findViewById(R.id.hostelTypeButton);
        Button hotelButton = findViewById(R.id.hotelTypeButton);
        Button apartmentButton = findViewById(R.id.apartmentTypeButton);
        Button farmTypeButton=findViewById(R.id.farmTypeButton);
        Button motelTypeButton=findViewById(R.id.motelTypeButton);
        Button villaTypeButton=findViewById(R.id.villaTypeButton);
        Button lodgeTypeButton=findViewById(R.id.lodgeTypeButton);
        Button guestHouseTypeButton=findViewById(R.id.guestHouseTypeButton);
        Button apartHotelTypeButton=findViewById(R.id.apartHotelTypeButton);
        cardViewMap=findViewById(R.id.cardViewMap);
        cardViewMap.setVisibility(View.GONE);
        mMapView.setVisibility(View.GONE);

        bookmarksButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HotelBookmarksActivity.class);
            type = typeSpinner.getSelectedItem().toString();
            if (type == null || type.equals("null") ||type.equals("") || type.isEmpty()) {
                type="Нет";
            }
            intent.putExtra("type", type);
            intent.putExtra("locationId", (int) locationId);
            intent.putExtra("selectedTripId", (int) selectedTripId);
            intent.putExtra("returnDate", returnDate);
            intent.putExtra("departureDate", departureDate);
            getNightsCount();
            intent.putExtra("nightsCount", nightsCount);
            intent.putExtra("adultsCount", adultsCount);
            startActivity(intent);
        });

        View.OnClickListener buttonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v instanceof Button) {
                    Button clickedButton = (Button) v;
                    String russianType = clickedButton.getText().toString();
                    String correspondingKey = null;
                    for (Map.Entry<String, String> entry : propertyTranslations.entrySet()) {
                        if (entry.getValue().equals(russianType)) {
                            correspondingKey = entry.getKey();
                            break;
                        }
                    }
                    if (correspondingKey != null) {
                        Log.d("russianType", russianType);
                        Log.d("englishType", correspondingKey);
                        if (!selectedEnglishPropertyTypes.contains(correspondingKey)) {
                            selectedEnglishPropertyTypes.add(correspondingKey);
                            clickedButton.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.accent_color));
                            clickedButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                            Log.d("SelectedType", correspondingKey);
                            Log.d("selectedEnglishPropertyTypes", String.valueOf(selectedEnglishPropertyTypes));
                        } else {
                            selectedEnglishPropertyTypes.remove(correspondingKey);
                            clickedButton.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.white));
                            clickedButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.accent_color));
                        }
                    } else {
                        Log.e("Error", "Key not found for Russian type: " + russianType);
                    }
                }
            }
        };

        hostelButton.setOnClickListener(buttonClickListener);
        hotelButton.setOnClickListener(buttonClickListener);
        apartmentButton.setOnClickListener(buttonClickListener);
        farmTypeButton.setOnClickListener(buttonClickListener);
        motelTypeButton.setOnClickListener(buttonClickListener);
        villaTypeButton.setOnClickListener(buttonClickListener);
        lodgeTypeButton.setOnClickListener(buttonClickListener);
        guestHouseTypeButton.setOnClickListener(buttonClickListener);
        apartHotelTypeButton.setOnClickListener(buttonClickListener);

        //endregion
        slider.setValues(1.0f,1000000.0f);
        searchButton.setEnabled(false);

        findViewById(R.id.nextPageButton).setOnClickListener(v ->{
            loadNextPage(noTypeHotelsList, true);
        });

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                type = typeSpinner.getSelectedItem().toString();
                if(!type.equals("Нет")){
                    horizontalScrollView.setVisibility(View.VISIBLE);
                }
                else{
                    horizontalScrollView.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this::onMapReady);

        findViewById(R.id.searchButton).setOnClickListener(v -> {
            imageView7.setImageResource(R.drawable.bginprogress);
            nothingHereImageView.setVisibility(View.VISIBLE);
            mMapView.setVisibility(View.GONE);
            cardViewMap.setVisibility(View.GONE);
            filterMap.clear();
            selectedFilters.clear();
            listOfIDs.clear();
            hotelsList.clear();
            matchingHotelsList.clear();
            finalHotelList.clear();
            noTypeHotelsList.clear();
            type = typeSpinner.getSelectedItem().toString();
            nextPageButton.setVisibility(View.GONE);
            mMap.clear();

            float minPriceFloat = slider.getValues().get(0);
            float maxPriceFloat = slider.getValues().get(1);
            minPrice = minPriceFloat;
            maxPrice = maxPriceFloat;

            HotelAdapter hotelAdapter = new HotelAdapter(new ArrayList<>());
            recyclerView.setAdapter(hotelAdapter);
            hotelAdapter.clearData();
            progressBar.setVisibility(View.VISIBLE);

            //region AppCompatCheckBoxes region
            AppCompatCheckBox filterFan = findViewById(R.id.filterFan);
            AppCompatCheckBox filterRestaurant = findViewById(R.id.filterRestaurant);
            AppCompatCheckBox filterAirConditioner = findViewById(R.id.filterAirConditioner);
            AppCompatCheckBox filterCheckIn24hr = findViewById(R.id.CheckIn24hr);
            AppCompatCheckBox filterDisabled = findViewById(R.id.filterDisabled);
            AppCompatCheckBox filterParking = findViewById(R.id.filterParking);
            AppCompatCheckBox filterPetsAllowed = findViewById(R.id.filterPetsAllowed);
            AppCompatCheckBox filterBar = findViewById(R.id.filterBar);
            AppCompatCheckBox filterSmokingZones = findViewById(R.id.filterSmokingZones);
            AppCompatCheckBox filterPrivateBeach = findViewById(R.id.filterPrivateBeach);
            AppCompatCheckBox filterPublicWiFi = findViewById(R.id.filterPublicWiFi);
            AppCompatCheckBox filterRussian = findViewById(R.id.filterRussian);
            AppCompatCheckBox filterSafe = findViewById(R.id.filterSafe);
            AppCompatCheckBox filterTV = findViewById(R.id.filterTV);
            AppCompatCheckBox filterBath = findViewById(R.id.filterBath);
            AppCompatCheckBox filterShower = findViewById(R.id.filterShower);
            AppCompatCheckBox filterCleaning = findViewById(R.id.filterCleaning);
            AppCompatCheckBox filterDailyCleaning = findViewById(R.id.filterDailyCleaning);
            AppCompatCheckBox filterMiniBar = findViewById(R.id.filterMiniBar);
            AppCompatCheckBox filterToiletInRoom = findViewById(R.id.filterToiletInRoom);
            AppCompatCheckBox filterCoffee = findViewById(R.id.filterCoffee);
            AppCompatCheckBox filterSlippers = findViewById(R.id.filterSlippers);
            AppCompatCheckBox filterSplitRoom = findViewById(R.id.filterSplitRoom);
            AppCompatCheckBox filterSharedBathroom = findViewById(R.id.filterSharedBathroom);
            AppCompatCheckBox filterBusinessCenter = findViewById(R.id.filterBusinessCenter);
            AppCompatCheckBox filterConcierge = findViewById(R.id.filterConcierge);
            AppCompatCheckBox filterLaundry = findViewById(R.id.filterLaundry);
            AppCompatCheckBox filterSpa = findViewById(R.id.filterSpa);
            AppCompatCheckBox filterFitness = findViewById(R.id.filterFitness);
            AppCompatCheckBox filterPool = findViewById(R.id.filterPool);
            //endregion

            //region selectedFilters region
            if (filterFan.isChecked()) {
                selectedFilters.add(filterFan.getText().toString());
            }
            if (filterRestaurant.isChecked()) {
                selectedFilters.add(filterRestaurant.getText().toString());
            }
            if (filterAirConditioner.isChecked()) {
                selectedFilters.add(filterAirConditioner.getText().toString());
            }
            if (filterCheckIn24hr.isChecked()) {
                selectedFilters.add(filterCheckIn24hr.getText().toString());
            }
            if (filterParking.isChecked()) {
                selectedFilters.add(filterParking.getText().toString());
            }
            if (filterDisabled.isChecked()) {
                selectedFilters.add(filterDisabled.getText().toString());
            }
            if (filterPetsAllowed.isChecked()) {
                selectedFilters.add(filterPetsAllowed.getText().toString());
            }
            if (filterBar.isChecked()) {
                selectedFilters.add(filterBar.getText().toString());
            }
            if (filterSmokingZones.isChecked()) {
                selectedFilters.add(filterSmokingZones.getText().toString());
            }
            if (filterPrivateBeach.isChecked()) {
                selectedFilters.add(filterPrivateBeach.getText().toString());
            }
            if (filterPublicWiFi.isChecked()) {
                selectedFilters.add(filterPublicWiFi.getText().toString());
            }
            if (filterRussian.isChecked()) {
                selectedFilters.add(filterRussian.getText().toString());
            }
            if (filterSafe.isChecked()) {
                selectedFilters.add(filterSafe.getText().toString());
            }
            if (filterTV.isChecked()) {
                selectedFilters.add(filterTV.getText().toString());
            }
            if (filterBath.isChecked()) {
                selectedFilters.add(filterBath.getText().toString());
            }
            if (filterShower.isChecked()) {
                selectedFilters.add(filterShower.getText().toString());
            }
            if (filterCleaning.isChecked()) {
                selectedFilters.add(filterCleaning.getText().toString());
            }
            if (filterDailyCleaning.isChecked()) {
                selectedFilters.add(filterDailyCleaning.getText().toString());
            }
            if (filterMiniBar.isChecked()) {
                selectedFilters.add(filterMiniBar.getText().toString());
            }
            if (filterToiletInRoom.isChecked()) {
                selectedFilters.add(filterToiletInRoom.getText().toString());
            }
            if (filterCoffee.isChecked()) {
                selectedFilters.add(filterCoffee.getText().toString());
            }
            if (filterSlippers.isChecked()) {
                selectedFilters.add(filterSlippers.getText().toString());
            }
            if (filterSplitRoom.isChecked()) {
                selectedFilters.add(filterSplitRoom.getText().toString());
            }
            if (filterSharedBathroom.isChecked()) {
                selectedFilters.add(filterSharedBathroom.getText().toString());
            }
            if (filterBusinessCenter.isChecked()) {
                selectedFilters.add(filterBusinessCenter.getText().toString());
            }
            if (filterConcierge.isChecked()) {
                selectedFilters.add(filterConcierge.getText().toString());
            }
            if (filterLaundry.isChecked()) {
                selectedFilters.add(filterLaundry.getText().toString());
            }
            if (filterSpa.isChecked()) {
                selectedFilters.add(filterSpa.getText().toString());
            }
            if (filterFitness.isChecked()) {
                selectedFilters.add(filterFitness.getText().toString());
            }
            if (filterPool.isChecked()) {
                selectedFilters.add(filterPool.getText().toString());
            }
            //endregion
            getNightsCount();

            if(Objects.equals(type, "Нет")){
                currentPage=0;
                new FetchNoTypesTask().execute(String.valueOf(locationId), type, departureDate, returnDate);
            }
            else{
                new FetchHotelsTask().execute(String.valueOf(locationId), type, departureDate, returnDate);
            }
        });
        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }
    private void getNightsCount(){
        LocalDate returnDateString = LocalDate.parse(returnDate);
        LocalDate departureDateString = LocalDate.parse(departureDate);
        nightsCount = ChronoUnit.DAYS.between(departureDateString, returnDateString);
    }
    private void addPropertyTranslations() {
        propertyTranslations.put("apartmen_hotel", "апарт-отель");
        propertyTranslations.put("apartment", "апартаменты");
        propertyTranslations.put("bed & breakfast", "ночлег и завтрак");
        propertyTranslations.put("bed_&_breakfast", "ночлег и завтрак");
        propertyTranslations.put("bed_and_breakfast", "ночлег и завтрак");
        propertyTranslations.put("bed_n_breakfast", "ночлег и завтрак");
        propertyTranslations.put("farm_stay", "ферма");
        propertyTranslations.put("motel", "мотель");
        propertyTranslations.put("guest_house", "гостевой дом");
        propertyTranslations.put("hostel", "хостел");
        propertyTranslations.put("hotel", "отель");
        propertyTranslations.put("lodge", "лодж");
        propertyTranslations.put("other", "другое");
        propertyTranslations.put("resort", "резорт");
        propertyTranslations.put("villa", "вилла");
    }
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng kremlin = new LatLng(55.753930, 37.620795);
        mMap.addMarker(new MarkerOptions().position(kremlin).title("Кремль"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kremlin, 12));
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
    private void showMapMarkers(final ArrayList<ArrayList<Object>> arrayList) {
        mMapView.setVisibility(View.VISIBLE);
        cardViewMap.setVisibility(View.VISIBLE);
        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        double hotelLatValueMap = 0.0, hotelLonValueMap = 0.0;

        for (int i = 0; i < arrayList.size(); i++) {
            ArrayList<Object> hotelDataMap = arrayList.get(i);
            String hotelNameMap = hotelDataMap.get(7).toString();
            hotelLatValueMap = (double) hotelDataMap.get(2);
            hotelLonValueMap = (double) hotelDataMap.get(3);
            String hotelLinkMap = hotelDataMap.get(12).toString();
            LatLng hotelLocation = new LatLng(hotelLatValueMap, hotelLonValueMap);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(hotelLocation)
                    .title(hotelNameMap)
                    .snippet(hotelLinkMap);
            mMap.addMarker(markerOptions);
            builder.include(hotelLocation);
        }

        if (hotelLatValueMap != 0.0 && hotelLonValueMap != 0.0) {
            mMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    LatLngBounds bounds = builder.build();
                    int padding = 50;
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.moveCamera(cameraUpdate);
                    mMapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    private void getTypes(Spinner typeSpinner) {
        String url = proxy_api+"https://yasen.hotellook.com/tp/public/available_selections.json?id=" + locationId + "&token=" + apiKey;
        Log.d("types url:", url);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        List<String> types = new ArrayList<>();
                        types.add("Нет");
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                types.add(response.getString(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(HotelActivity.this, android.R.layout.simple_spinner_item, types);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        typeSpinner.setAdapter(adapter);
                        Log.d("types response: ", String.valueOf(response));
                        searchButton.setEnabled(true);
                        bookmarksButton.setEnabled(true);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(HotelActivity.this, "Ошибка соединения.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error: " + error.toString());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-cors-api-key", corsApiKey);
                headers.put("Origin", "http://localhost/");
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(jsonArrayRequest);
    }

    private Map<String, Integer> loadFilters() {
        try {
            InputStream is = getAssets().open("filters.json");
            JsonReader jsonReader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                jsonReader.beginObject();
                String filterName = null;
                int filterId = -1;
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("ruTranslation")) {
                        filterName = jsonReader.nextString();
                    } else if (name.equals("id")) {
                        filterId = jsonReader.nextInt();
                    } else {
                        jsonReader.skipValue();
                    }
                }
                if (filterName != null && filterId != -1) {
                    filterMap.put(filterName, filterId);
                }
                jsonReader.endObject();
            }
            jsonReader.endArray();
            jsonReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filterMap;
    }
    private void getLocationId(String targetLocation) {
        String url = proxy_api + TARGET_API_URL + "?query=" + targetLocation + "&lang=ru&lookFor=city&limit=1&token="+apiKey;
        Log.d("loc url",url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d("airport", url);
                            JSONObject results = response.getJSONObject("results");
                            JSONArray locations = results.getJSONArray("locations");
                            if (locations.length() > 0) {
                                JSONObject firstLocation = locations.getJSONObject(0);
                                locationId = Integer.parseInt(firstLocation.getString("id"));
                                Log.d(TAG, "Location ID: " + locationId);
                                Spinner typeSpinner = findViewById(R.id.typeSpinner);
                                getTypes(typeSpinner);
                            } else {
                                Log.e(TAG, "No matching locations found.");
                                Toast.makeText(HotelActivity.this, "Отели в данной локации не найдены", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing JSON", e);
                            Toast.makeText(HotelActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error getting location ID: " + error.getMessage());
                        if (error.networkResponse != null) {
                            Log.e(TAG, "Error Response code: " + error.networkResponse.statusCode);
                            String errorMessage = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            Log.e(TAG, "Error Response body: " + errorMessage);

                            Map<String, String> headers = error.networkResponse.headers;
                            if (headers.containsKey("X-Ratelimit-Interval")) {
                                Log.d(TAG, "X-Ratelimit-Interval: " + headers.get("X-Ratelimit-Interval"));
                            }
                            if (headers.containsKey("X-Ratelimit-Remaining")) {
                                Log.d(TAG, "X-Ratelimit-Remaining: " + headers.get("X-Ratelimit-Remaining"));
                            }
                            if (headers.containsKey("X-Ratelimit-Limit")) {
                                Log.d(TAG, "X-Ratelimit-Limit: " + headers.get("X-Ratelimit-Limit"));
                            }
                        }
                        Toast.makeText(HotelActivity.this, "CORS proxy is down", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-cors-api-key", corsApiKey);
                headers.put("Origin", "http://localhost/");
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }


    private void loadNextPage(ArrayList<ArrayList<Object>> arrayList, boolean loadNextPage) {
        mMapView.setVisibility(View.VISIBLE);
        nothingHereImageView.setVisibility(View.GONE);
        cardViewMap.setVisibility(View.VISIBLE);
        ArrayList<ArrayList<Object>> nextPageData = new ArrayList<>();
        ImageButton nextPageButton = findViewById(R.id.nextPageButton);
        if(Objects.equals(type, "Нет")){
            nextPageButton.setVisibility(View.VISIBLE);
        }
        else {
            nextPageButton.setVisibility(View.GONE);
        }
        if (loadNextPage) {
            while(nextPageData.isEmpty()) {
                int startIndex = currentPage;
                int endIndex = currentPage + 50;
                int arraySize=arrayList.size();
                if(arraySize!=0) {
                    nextPageData = new ArrayList<>(arrayList.subList(startIndex, Math.min(endIndex, arraySize)));
                    nextPageData = filterHotelsWithEmptyPhotos(nextPageData);
                    Log.d("start index", String.valueOf(startIndex));
                    Log.d("end index", String.valueOf(endIndex));
                    Log.d("next page data", String.valueOf(nextPageData));
                    Log.d("next page data len", String.valueOf(nextPageData.size()));
                    currentPage += 50;
                    if ((arraySize - endIndex) < 50) {
                        nextPageButton.setVisibility(View.GONE);
                    }
                    else{
                        ProgressBar progressBar= findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        }
        if(!nextPageData.isEmpty()) {
            showMapMarkers(nextPageData);
            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            HotelAdapter hotelAdapter = new HotelAdapter(nextPageData);
            recyclerView.setAdapter(hotelAdapter);
        }
    }

    private ArrayList<ArrayList<Object>> filterHotelsWithEmptyPhotos(ArrayList<ArrayList<Object>> hotelDataList) {
        ArrayList<ArrayList<Object>> filteredList = new ArrayList<>();
        for (ArrayList<Object> hotelData : hotelDataList) {
            ArrayList<String> imageUrls = (ArrayList<String>) hotelData.get(5);
            ArrayList<String> facilities = (ArrayList<String>) hotelData.get(1);
            double price = (double) hotelData.get(14);
            String address = (String) hotelData.get(4);
            if (!imageUrls.isEmpty() && !facilities.isEmpty() && price!=0.0 && !address.equals("")) {
                filteredList.add(hotelData);
            }
        }
        return filteredList;
    }

    @SuppressLint("Range")
    private void readAndLogDataFromSQLite(int selectedTripId) {
        Cursor cursor = db.rawQuery("SELECT * FROM jjtrips1 WHERE userID = ?", new String[]{String.valueOf(selectedTripId)});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int userID = cursor.getInt(cursor.getColumnIndex("userID"));
                    departureDate = cursor.getString(cursor.getColumnIndex("departureDate"));
                    returnDate = cursor.getString(cursor.getColumnIndex("returnDate"));
                    adultsCount = cursor.getInt(cursor.getColumnIndex("adultsCount"));
                    hotelLat = cursor.getString(cursor.getColumnIndex("hotelLat"));
                    hotelLon = cursor.getString(cursor.getColumnIndex("hotelLon"));
                    hotelInfo = cursor.getString(cursor.getColumnIndex("hotelInfo"));
                    targetLocation = cursor.getString(cursor.getColumnIndex("targetLocation"));
                    String departureLocation = cursor.getString(cursor.getColumnIndex("departureLocation"));
                    String requestUrl = "https://www.travelpayouts.com/widgets_suggest_params?q=Из%20" +
                            departureLocation + "%20в%20" + targetLocation;
                    Log.d("loc request", requestUrl);
                    new SendRequestTask().execute(requestUrl);

                    Log.d("SQLite Data", "User ID: " + userID);
                    Log.d("SQLite Data", "Departure Date: " + departureDate);
                    Log.d("SQLite Data", "Return Date: " + returnDate);
                    Log.d("SQLite Data", "Adults Count: " + adultsCount);
                    Log.d("SQLite Data", "Hotel Latitude: " + hotelLat);
                    Log.d("SQLite Data", "Hotel Longitude: " + hotelLon);
                    Log.d("SQLite Data", "Hotel Info: " + hotelInfo);
                    Log.d("SQLite Data", "Target Location: " + targetLocation);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
    private class SendRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseStringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseStringBuilder.append(line);
                }
                reader.close();
                connection.disconnect();
                return responseStringBuilder.toString();
            } catch (IOException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    Log.d("Location response",response);
                    JSONObject destination = jsonResponse.getJSONObject("destination");
                    String targetIata = destination.getString("iata");
                    Log.d("Iata Codes", "Target Iata: " + targetIata);
                    getLocationId(targetIata);
                } catch (JSONException e) {
                    Log.e("JSON parsing error: ", e.getMessage());
                }
            } else {
                Log.e("No data available", "Error retrieving data");
                Toast.makeText(context, "Отели в данной локации не найдены.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FetchNoTypesTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String locationId = params[0];

            String hotelRequestApi = "https://cors.eu.org/https://engine.hotellook.com/api/v2/static/hotels.json?locationId=" + locationId + "&token="+apiKey;
            Log.d("Matching hotels:", hotelRequestApi);
            try {
                String response = makeHttpRequestWithOkHttp(hotelRequestApi);
                Log.d("no type hotels hotels", response);
                Log.d("minprice", String.valueOf(minPrice));
                Log.d("maxprice", String.valueOf(maxPrice));
                return response;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onPostExecute(String response) {
            boolean skipHotel=false;
            Log.d("number of nights", String.valueOf(nightsCount));

            if (response != null) {
                    try {
                        JsonReader jsonReader = new JsonReader(new StringReader(response));
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) {
                                String name = jsonReader.nextName();
                                if (name.equals("hotels")) {
                                    jsonReader.beginArray();
                                    while (jsonReader.hasNext()) {
                                        jsonReader.beginObject();
                                        int hotelId = -1;
                                        int cntRooms = 1;
                                        int cntFloors=1;
                                        List<String> facilities = new ArrayList<>();
                                        List<String> photos = new ArrayList<>();
                                        ArrayList<Integer> poiDistance = new ArrayList<>();
                                        double latitude = 0.0;
                                        double longitude = 0.0;
                                        String address = "";
                                        double distance = 0.0;
                                        String hotelName = "n/a", propertyType = "hotel";
                                        int stars = 3, rating = 0;
                                        String link = "";
                                        double priceFrom = 0.0;
                                        while (jsonReader.hasNext()) {
                                            skipHotel = false;
                                            String hotelProperty = jsonReader.nextName();
                                            switch (hotelProperty) {
                                                case "id":
                                                    int idValue = jsonReader.nextInt();
                                                    if (idValue == -1) {
                                                        skipHotel = true;
                                                    }
                                                    hotelId = idValue;
                                                    break;
                                                case "distance":
                                                    distance = jsonReader.nextDouble();
                                                    break;
                                                case "name":
                                                    jsonReader.beginObject();
                                                    String ruName = null;
                                                    String enName = null;
                                                    while (jsonReader.hasNext()) {
                                                        String nameType = jsonReader.nextName();
                                                        if (nameType.equals("ru")) {
                                                            ruName = jsonReader.nextString();
                                                        } else if (nameType.equals("en")) {
                                                            enName = jsonReader.nextString();
                                                        } else {
                                                            jsonReader.skipValue();
                                                        }
                                                    }
                                                    jsonReader.endObject();
                                                    hotelName = (ruName != null) ? ruName : (enName != null) ? enName : "N/A";
                                                    if (hotelName.equals("N/A")) {
                                                    }
                                                    break;
                                                case "stars":
                                                    stars = jsonReader.nextInt();
                                                    break;
                                                case "pricefrom":
                                                    int priceInt = jsonReader.nextInt();
                                                    priceFrom = (double) priceInt*89*nightsCount;
                                                    break;
                                                case "rating":
                                                    rating = jsonReader.nextInt();
                                                    break;
                                                case "propertyType":
                                                    propertyType = jsonReader.nextString();
                                                    break;
                                                case "cntRooms":
                                                    if (jsonReader.peek() == JsonToken.NULL) {
                                                        jsonReader.skipValue();
                                                    } else {
                                                        cntRooms = jsonReader.nextInt();
                                                    }
                                                    break;
                                                case "cntFloors":
                                                    if (jsonReader.peek() == JsonToken.NULL) {
                                                        jsonReader.skipValue();
                                                    } else {
                                                        cntFloors = jsonReader.nextInt();
                                                    }
                                                    break;
                                                case "address":
                                                    jsonReader.beginObject();
                                                    String ruAddress = null;
                                                    String enAddress = null;
                                                    while (jsonReader.hasNext()) {
                                                        String addressType = jsonReader.nextName();
                                                        if (addressType.equals("ru")) {
                                                            ruAddress = jsonReader.nextString();
                                                        } else if (addressType.equals("en")) {
                                                            enAddress = jsonReader.nextString();
                                                        } else {
                                                            jsonReader.skipValue();
                                                        }
                                                    }
                                                    jsonReader.endObject();
                                                    address = (ruAddress != null) ? ruAddress : (enAddress != null) ? enAddress : "N/A";
                                                    break;
                                                case "location":
                                                    jsonReader.beginObject();
                                                    while (jsonReader.hasNext()) {
                                                        String locationProperty = jsonReader.nextName();
                                                        if (locationProperty.equals("lat")) {
                                                            latitude = jsonReader.nextDouble();
                                                        } else if (locationProperty.equals("lon")) {
                                                            longitude = jsonReader.nextDouble();
                                                        } else {
                                                            jsonReader.skipValue();
                                                        }
                                                    }
                                                    jsonReader.endObject();
                                                    break;
                                                case "facilities":
                                                    jsonReader.beginArray();
                                                    while (jsonReader.hasNext()) {
                                                        facilities.add(jsonReader.nextString());
                                                    }
                                                    jsonReader.endArray();
                                                    break;
                                                case "photos":
                                                    jsonReader.beginArray();
                                                    while (jsonReader.hasNext()) {
                                                        jsonReader.beginObject();
                                                        while (jsonReader.hasNext()) {
                                                            String photoProperty = jsonReader.nextName();
                                                            if (photoProperty.equals("url")) {
                                                                String photoUrl = jsonReader.nextString();
                                                                photos.add(photoUrl);
                                                            } else {
                                                                jsonReader.skipValue();
                                                            }
                                                        }
                                                        jsonReader.endObject();
                                                    }
                                                    jsonReader.endArray();
                                                    break;
                                                case "poi_distance":
                                                    jsonReader.beginObject();
                                                    while (jsonReader.hasNext()) {
                                                        String poiIdString = jsonReader.nextName();
                                                        int poiId = Integer.parseInt(poiIdString);
                                                        int distancePoi = jsonReader.nextInt();
                                                        poiDistance.add(poiId);
                                                        poiDistance.add(distancePoi);
                                                    }
                                                    jsonReader.endObject();
                                                    break;
                                                default:
                                                    jsonReader.skipValue();
                                                    break;
                                            }
                                                if (!skipHotel) {
                                                    link = "https://search.hotellook.com/?hotelId=" + hotelId + "&checkIn=" + departureDate +
                                                            "&checkOut=" + returnDate + "&adults=" + adultsCount + "&locale=ru_RU";
                                                }
                                        }
                                        loadFilters();
                                        if (!skipHotel) {
                                            boolean isHotelMatching = true;
                                            for (String selectedFilter : selectedFilters) {
                                                int filterId = filterMap.get(selectedFilter);
                                                if (!facilities.contains(String.valueOf(filterId))) {
                                                    isHotelMatching = false;
                                                    break;
                                                }
                                            }
                                            if (isHotelMatching && priceFrom >= minPrice && priceFrom <= maxPrice) {
                                                ArrayList<Object> noTypeHotelData = new ArrayList<>(Arrays.asList(hotelId, facilities, latitude, longitude, address, photos, cntFloors, cntRooms, poiDistance, distance, hotelName, stars, rating, propertyType, priceFrom, link));
                                                noTypeHotelsList.add(noTypeHotelData);
                                            }
                                        }
                                        jsonReader.endObject();
                                    }
                                    jsonReader.endArray();

                                    Spinner sortSpinner = findViewById(R.id.sortSpinner);
                                    String sorting = sortSpinner.getSelectedItem().toString();
                                    noTypeHotelsList = sortHotels(noTypeHotelsList, sorting);
                                    Log.d("SortedData NO Type:", String.valueOf(noTypeHotelsList));
                                    Log.d("SortedData NO Type len:", String.valueOf(noTypeHotelsList.size()));
                                    if(noTypeHotelsList.isEmpty()){
                                        ProgressBar progressBar = findViewById(R.id.progressBar);
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(context, "Отели не найдены.", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        loadNextPage(noTypeHotelsList, true);
                                        ProgressBar progressBar = findViewById(R.id.progressBar);
                                        progressBar.setVisibility(View.GONE);
                                        retriesCount = 0;
                                    }
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                        jsonReader.endObject();
                        jsonReader.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            } else {
                Log.d("response is null", "network error");
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setVisibility(View.GONE);
                if(retriesCount<4) {
                    retriesCount += 1;
                    progressBar.setVisibility(View.VISIBLE);
                    new FetchNoTypesTask().execute(String.valueOf(locationId), type, departureDate, returnDate);
                }
                else{
                    Toast.makeText(context, "Ошибка сети, проверьте подключение.", Toast.LENGTH_SHORT).show();
                }
            }
            }

        private ArrayList<ArrayList<Object>> sortHotels(ArrayList<ArrayList<Object>> hotels, String sorting) {
            if (sorting.equals("по цене")) {
                Collections.sort(hotels, new Comparator<ArrayList<Object>>() {
                    @Override
                    public int compare(ArrayList<Object> hotel1, ArrayList<Object> hotel2) {
                        double price1 = (double) hotel1.get(14);
                        double price2 = (double) hotel2.get(14);
                        return Double.compare(price1, price2);
                    }
                });
            } else if (sorting.equals("по расстоянию от центра")) {
                Collections.sort(hotels, new Comparator<ArrayList<Object>>() {
                    @Override
                    public int compare(ArrayList<Object> hotel1, ArrayList<Object> hotel2) {
                        double distance1 = (double) hotel1.get(9);
                        double distance2 = (double) hotel2.get(9);
                        return Double.compare(distance1, distance2);
                    }
                });
            } else if (sorting.equals("по рейтингу")) {
                Collections.sort(hotels, new Comparator<ArrayList<Object>>() {
                    @Override
                    public int compare(ArrayList<Object> hotel1, ArrayList<Object> hotel2) {
                        int rating1 = (int) hotel1.get(12);
                        int rating2 = (int) hotel2.get(12);
                        double rating1Double = (double) rating1;
                        double rating2Double = (double) rating2;
                        return Double.compare(rating2Double, rating1Double);
                    }
                });
            }
            return hotels;
        }
        private String makeHttpRequestWithOkHttp(String urlString) throws IOException {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(urlString)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                okhttp3.ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    return responseBody.string();
                } else {
                    return null;
                }
            }
        }
    }

        private class FetchHotelsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String locationId = params[0];
            String type = params[1];
            String departureDate = params[2];
            String returnDate = params[3];

            String hotelRequestApi = proxy_api+"https://yasen.hotellook.com/tp/public/widget_location_dump.json?currency=rub&language=ru&limit=100&id=" + locationId +
                    "&type=" + type + "&check_in=" + departureDate + "&check_out=" + returnDate + "&token="+apiKey;

            Log.d("Hotels:", hotelRequestApi);
            try {
                return makeHttpRequest(hotelRequestApi);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                Log.d("Response: ", response);
                try {
                    JSONObject dataHotel = new JSONObject(response);
                    JSONArray popularityHotels = dataHotel.getJSONArray(type);
                    for (int i = 0; i < popularityHotels.length(); i++) {
                        JSONObject hotel = popularityHotels.getJSONObject(i);
                        int hotelId = hotel.getInt("hotel_id");
                        double distance = hotel.getDouble("distance");
                        String name = hotel.getString("name");
                        int stars = hotel.optInt("stars", 3);
                        int rating = hotel.getInt("rating");
                        String propertyType = hotel.getString("property_type");
                        double price = 0.0;

                        if (hotel.has("last_price_info") && !hotel.isNull("last_price_info")) {
                            JSONObject lastPriceInfo = hotel.getJSONObject("last_price_info");

                            if (lastPriceInfo.has("price")) {
                                price = lastPriceInfo.getDouble("price");
                            } else if (lastPriceInfo.has("old_price")) {
                                price = lastPriceInfo.getDouble("old_price");
                            } else if (lastPriceInfo.has("price_pn")) {
                                price = lastPriceInfo.getDouble("price_pn");
                            } else if (lastPriceInfo.has("old_price_pn")) {
                                price = lastPriceInfo.getDouble("old_price_pn");
                            }
                        } else {
                            price = 55000.0;
                        }
                        String link = "https://search.hotellook.com/?hotelId=" + hotelId + "&checkIn=" + departureDate + "&checkOut=" + returnDate + "&adults=" + adultsCount + "&locale=ru_RU";
                        if (price >= minPrice && price <= maxPrice && (selectedEnglishPropertyTypes.isEmpty() || selectedEnglishPropertyTypes.contains(propertyType))) {
                            ArrayList<Object> hotelData = new ArrayList<>(Arrays.asList(hotelId, distance, name, stars, rating, propertyType, price, link));
                            hotelsList.add(hotelData);
                        }
                    }
                    if(!hotelsList.isEmpty()) {
                        Log.d("selectedEnglishPropertyTypes", String.valueOf(selectedEnglishPropertyTypes));
                        List<Integer> listOfIDs = new ArrayList<>();
                        for (ArrayList<Object> data : hotelsList) {
                            listOfIDs.add((int) data.get(0));
                        }
                        String listOfIDsString = TextUtils.join(",", listOfIDs);
                        new FetchMatchingHotelsTask().execute(String.valueOf(locationId), listOfIDsString);
                        Log.d("hotelList:", String.valueOf(hotelsList));
                        Log.d("hotelListLen:", String.valueOf(hotelsList.size()));
                    }
                    else{
                        Toast.makeText(context, "Отелей по выбранным категориям не найдено.", Toast.LENGTH_SHORT).show();
                        ProgressBar progressBar = findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        private class FetchMatchingHotelsTask extends AsyncTask<String, List<Integer>, String> {
            @Override
            protected String doInBackground(String... params) {
                String locationId = params[0];
                String listOfIDsString = params[1];
                String[] idStrings = listOfIDsString.split(",");
                for (String idString : idStrings) {
                    listOfIDs.add(Integer.parseInt(idString));
                }
                String hotelRequestApi = "https://cors.eu.org/https://engine.hotellook.com/api/v2/static/hotels.json?locationId=" + locationId + "&token="+apiKey;
                Log.d("Matching hotels:", hotelRequestApi);
                try {
                    String response = makeHttpRequestWithOkHttp(hotelRequestApi);
                    Log.d("Matching hotels", response);
                    return response;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String response) {
                if (response != null) {
                        try {
                            JsonReader jsonReader = new JsonReader(new StringReader(response));
                            jsonReader.beginObject();
                            Log.d("List of IDs: ", listOfIDs.toString());
                            while (jsonReader.hasNext()) {
                                String name = jsonReader.nextName();
                                if (name.equals("hotels")) {
                                    jsonReader.beginArray();

                                    while (jsonReader.hasNext()) {
                                        jsonReader.beginObject();

                                        int hotelId = -1;
                                        int cntRooms=1;
                                        int cntFloors=1;
                                        List<String> facilities = new ArrayList<>();
                                        List<String> photos = new ArrayList<>();
                                        ArrayList<Integer> poiDistance = new ArrayList<>();
                                        double latitude = 0.0;
                                        double longitude = 0.0;
                                        String address = "";

                                        while (jsonReader.hasNext()) {
                                            String hotelProperty = jsonReader.nextName();
                                            switch (hotelProperty) {
                                                case "id":
                                                    hotelId = jsonReader.nextInt();
                                                    break;
                                                case "address":
                                                    jsonReader.beginObject();
                                                    String ruAddress = null;
                                                    String enAddress = null;
                                                    while (jsonReader.hasNext()) {
                                                        String addressType = jsonReader.nextName();
                                                        if (addressType.equals("ru")) {
                                                            ruAddress = jsonReader.nextString();
                                                        } else if (addressType.equals("en")) {
                                                            enAddress = jsonReader.nextString();
                                                        } else {
                                                            jsonReader.skipValue();
                                                        }
                                                    }
                                                    jsonReader.endObject();
                                                    address = (ruAddress != null) ? ruAddress : (enAddress != null) ? enAddress : "N/A";
                                                    break;
                                                case "location":
                                                    jsonReader.beginObject();
                                                    while (jsonReader.hasNext()) {
                                                        String locationProperty = jsonReader.nextName();
                                                        if (locationProperty.equals("lat")) {
                                                            latitude = jsonReader.nextDouble();
                                                        } else if (locationProperty.equals("lon")) {
                                                            longitude = jsonReader.nextDouble();
                                                        } else {
                                                            jsonReader.skipValue();
                                                        }
                                                    }
                                                    jsonReader.endObject();
                                                    break;
                                                case "facilities":
                                                    jsonReader.beginArray();
                                                    while (jsonReader.hasNext()) {
                                                        facilities.add(jsonReader.nextString());
                                                    }
                                                    jsonReader.endArray();
                                                    break;
                                                case "cntRooms":
                                                    if (jsonReader.peek() == JsonToken.NULL) {
                                                        jsonReader.skipValue();
                                                    } else {
                                                        cntRooms = jsonReader.nextInt();
                                                    }
                                                    break;
                                                case "cntFloors":
                                                    if (jsonReader.peek() == JsonToken.NULL) {
                                                        jsonReader.skipValue();
                                                    } else {
                                                        cntFloors = jsonReader.nextInt();
                                                    }
                                                    break;
                                                case "poi_distance":
                                                    jsonReader.beginObject();
                                                    while (jsonReader.hasNext()) {
                                                        String poiIdString = jsonReader.nextName();
                                                        int poiId = Integer.parseInt(poiIdString);
                                                        int distancePoi = jsonReader.nextInt();
                                                        poiDistance.add(poiId);
                                                        poiDistance.add(distancePoi);
                                                    }
                                                    jsonReader.endObject();
                                                    break;
                                                case "photos":
                                                    jsonReader.beginArray();
                                                    while (jsonReader.hasNext()) {
                                                        jsonReader.beginObject();
                                                        while (jsonReader.hasNext()) {
                                                            String photoProperty = jsonReader.nextName();
                                                            if (photoProperty.equals("url")) {
                                                                String photoUrl = jsonReader.nextString();
                                                                photos.add(photoUrl);
                                                            } else {
                                                                jsonReader.skipValue();
                                                            }
                                                        }
                                                        jsonReader.endObject();
                                                    }
                                                    jsonReader.endArray();
                                                    break;
                                                default:
                                                    jsonReader.skipValue();
                                                    break;
                                            }
                                        }
                                        loadFilters();
                                        if (listOfIDs.contains(hotelId)) {
                                            boolean isHotelMatching = true;
                                            for (String selectedFilter : selectedFilters) {
                                                int filterId = filterMap.get(selectedFilter);
                                                if (!facilities.contains(String.valueOf(filterId))) {
                                                    isHotelMatching = false;
                                                    break;
                                                }
                                            }
                                            if (isHotelMatching) {
                                                ArrayList<Object> matchingHotelData = new ArrayList<>(Arrays.asList(hotelId, facilities, latitude, longitude, address, photos, cntFloors, cntRooms, poiDistance));
                                                matchingHotelsList.add(matchingHotelData);
                                            }
                                        }
                                        jsonReader.endObject();
                                    }
                                    jsonReader.endArray();
                                    Log.d("MatchingArray:", String.valueOf(matchingHotelsList));
                                    Log.d("MatchingArrayLen:", String.valueOf(matchingHotelsList.size()));
                                    mergeHotelData(matchingHotelsList, hotelsList, finalHotelList);
                                    Log.d("MergedData:", String.valueOf(finalHotelList));
                                    Log.d("MergedDataLen:", String.valueOf(finalHotelList.size()));

                                    Spinner sortSpinner = findViewById(R.id.sortSpinner);
                                    String sorting = sortSpinner.getSelectedItem().toString();
                                    finalHotelList = sortHotels(finalHotelList, sorting);
                                    Log.d("SortedData matching:", String.valueOf(finalHotelList));
                                    if(finalHotelList.isEmpty()){
                                        Toast.makeText(HotelActivity.this, "По таким критериям отели не найдены.", Toast.LENGTH_SHORT).show();
                                        ProgressBar progressBar = findViewById(R.id.progressBar);
                                        progressBar.setVisibility(View.GONE);
                                    }
                                    else {
                                        showMapMarkers(finalHotelList);
                                        Log.d("selectedEnglishPropertyTypes", String.valueOf(selectedEnglishPropertyTypes));
                                        retriesCount = 0;
                                        RecyclerView recyclerView = findViewById(R.id.recyclerView);
                                        HotelAdapter hotelAdapter = new HotelAdapter(finalHotelList);
                                        recyclerView.setAdapter(hotelAdapter);
                                        nothingHereImageView.setVisibility(View.GONE);
                                        ProgressBar progressBar = findViewById(R.id.progressBar);
                                        progressBar.setVisibility(View.GONE);
                                    }
                                } else {
                                    jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                            jsonReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                } else {
                    if(retriesCount<4) {
                        retriesCount += 1;
                        new FetchHotelsTask().execute(String.valueOf(locationId), type, departureDate, returnDate);
                    }
                    else{
                        Toast.makeText(context, "Ошибка сети, проверьте подключение и повторите попытку.", Toast.LENGTH_SHORT).show();
                    }
                }
            }


            public void mergeHotelData(ArrayList<ArrayList<Object>> matchingHotelsList, ArrayList<ArrayList<Object>> hotelsList, ArrayList<ArrayList<Object>> finalHotelList) {
                for (ArrayList<Object> matchingHotelData : matchingHotelsList) {
                    for (ArrayList<Object> hotelData : hotelsList) {
                        Object matchingHotelId = matchingHotelData.get(0);
                        Object hotelId = hotelData.get(0);

                        if (matchingHotelId.equals(hotelId)) {
                            ArrayList<Object> combinedData = new ArrayList<>();
                            combinedData.add(matchingHotelId);
                            combinedData.addAll(matchingHotelData.subList(1, matchingHotelData.size()));
                            combinedData.addAll(hotelData.subList(1, hotelData.size()));
                            finalHotelList.add(combinedData);
                        }
                    }
                }
            }
            private String makeHttpRequestWithOkHttp(String urlString) throws IOException {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(urlString)
                        .build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    okhttp3.ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        return responseBody.string();
                    } else {
                        return null;
                    }
                }
            }
        }

        private String makeHttpRequest(String urlString) throws IOException {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            Scanner scanner = null;

            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("x-cors-api-key",corsApiKey);
                connection.setRequestProperty("Origin", "http://localhost/");
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("response code", String.valueOf(responseCode));
                    inputStream = connection.getInputStream();
                    scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
                    if (scanner.hasNext()) {
                        return scanner.next();
                    }
                } else {
                    Log.e("Request Error", "Response code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        private ArrayList<ArrayList<Object>> sortHotels(ArrayList<ArrayList<Object>> hotels, String sorting) {
            if (sorting.equals("по цене")) {
                Collections.sort(hotels, new Comparator<ArrayList<Object>>() {
                    @Override
                    public int compare(ArrayList<Object> hotel1, ArrayList<Object> hotel2) {
                        double price1 = (double) hotel1.get(14);
                        double price2 = (double) hotel2.get(14);
                        return Double.compare(price1, price2);
                    }
                });
            } else if (sorting.equals("по расстоянию от центра")) {
                Collections.sort(hotels, new Comparator<ArrayList<Object>>() {
                    @Override
                    public int compare(ArrayList<Object> hotel1, ArrayList<Object> hotel2) {
                        double distance1 = (double) hotel1.get(9);
                        double distance2 = (double) hotel2.get(9);
                        return Double.compare(distance1, distance2);
                    }
                });
            } else if (sorting.equals("по рейтингу")) {
                Collections.sort(hotels, new Comparator<ArrayList<Object>>() {
                    @Override
                    public int compare(ArrayList<Object> hotel1, ArrayList<Object> hotel2) {
                        int rating1 = (int) hotel1.get(12);
                        int rating2 = (int) hotel2.get(12);
                        double rating1Double = (double) rating1;
                        double rating2Double = (double) rating2;
                        return Double.compare(rating2Double, rating1Double);
                    }
                });
            }
            return hotels;
        }
    }
    public class HotelAdapter extends RecyclerView.Adapter<HotelAdapter.HotelViewHolder> {

        private List<ArrayList<Object>> hotelDataList;
        private AtomicInteger currentImageIndex;

        public HotelAdapter(List<ArrayList<Object>> hotelDataList) {
            this.hotelDataList = hotelDataList;
            this.currentImageIndex = new AtomicInteger(0);
        }
        public void clearData() {
            hotelDataList.clear();
            currentImageIndex.set(0);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hotel_card, parent, false);
            return new HotelViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
            ImageButton nextPageButton = findViewById(R.id.nextPageButton);

            ArrayList<Object> hotelData = hotelDataList.get(position);
            String facilities = TextUtils.join(", ", (List<String>) hotelData.get(1));
            ArrayList<String> imageUrls = (ArrayList<String>) hotelData.get(5);
            double hotelDistance = (double) hotelData.get(9);
            String hotelName = hotelData.get(10).toString();
            int stars = (int) hotelData.get(11);
            int rating = (int) hotelData.get(12);
            String propertyType = hotelData.get(13).toString().trim();
            double price = (double) hotelData.get(14);
            if(stars==0){
                stars=1;
            }
            if (!imageUrls.isEmpty()) {
                updateImage(holder.hotelImageView, imageUrls.get(currentImageIndex.get()));
            } else {
                holder.hotelImageView.setImageResource(R.drawable.placeholder);
            }

            //region filters
            String json;
            try {
                InputStream is = context.getAssets().open("filters.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Map<String, String> facilityTranslations = new HashMap<>();
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String id = item.getString("id");
                    String ruTranslation = item.getString("ruTranslation");
                    facilityTranslations.put(id, ruTranslation);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            String[] facilityIds = facilities.split(", ");
            List<String> translatedFacilities = new ArrayList<>();
            for (String id : facilityIds) {
                String translation = facilityTranslations.get(id);
                if (translation != null) {
                    translatedFacilities.add(translation);
                }
            }
            //endregion
            String starsSymbol = "\u2605";
            if(Objects.equals(type, "Нет")){
                try {
                    InputStream is = context.getAssets().open("roomTypes.json");
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    json = new String(buffer, "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                try {
                    JSONArray jsonArray = new JSONArray(json);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        String typeId = item.getString("id");
                        if(propertyType.equals(typeId)){
                            propertyType = item.getString("name");
                            holder.hotelTypeTextView.setText(propertyType.toLowerCase());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }
            else{
                String translatedPropertyType = propertyTranslations.get(propertyType);
                if (translatedPropertyType != null) {
                    holder.hotelTypeTextView.setText(translatedPropertyType);
                } else {
                    holder.hotelTypeTextView.setText("другое");
                }
                nextPageButton.setVisibility(View.GONE);
            }

            holder.previousImageButton.setOnClickListener(v -> {
                int previousIndex = currentImageIndex.get();
                if (previousIndex > 0) {
                    updateImage(holder.hotelImageView, imageUrls.get(previousIndex - 1));
                    currentImageIndex.decrementAndGet();
                }
            });

            holder.nextImageButton.setOnClickListener(v -> {
                int nextIndex = currentImageIndex.get();
                if (nextIndex < imageUrls.size() - 1) {
                    updateImage(holder.hotelImageView, imageUrls.get(nextIndex + 1));
                    currentImageIndex.incrementAndGet();
                }
            });
            holder.hotelNameTextView.setText(hotelName);
            holder.hotelRatingTextView.setText(String.valueOf((double) rating / 10 / 2));
            holder.hotelPriceTextView.setText("от "+price+ " руб.");
            holder.hotelDistanceTextView.setText(hotelDistance+" км");
        }
        private void updateImage(ImageView imageView, String imageUrl) {
            Picasso.get().load(imageUrl).into(imageView);
        }

        @Override
        public int getItemCount() {
            return hotelDataList.size();
        }

        public class HotelViewHolder extends RecyclerView.ViewHolder {
            public ImageView hotelImageView;
            public TextView hotelNameTextView;
            public TextView hotelPriceTextView;
            public TextView hotelRatingTextView;
            public TextView hotelTypeTextView;
            public ImageButton  nextImageButton;
            public ImageButton previousImageButton;
            public TextView hotelDistanceTextView;

            public HotelViewHolder(View itemView) {
                super(itemView);
                hotelImageView = itemView.findViewById(R.id.hotelImage);
                hotelNameTextView = itemView.findViewById(R.id.hotelName);
                hotelPriceTextView = itemView.findViewById(R.id.hotelPrice);
                hotelRatingTextView = itemView.findViewById(R.id.hotelRating);
                hotelTypeTextView = itemView.findViewById(R.id.hotelType);
                nextImageButton = itemView.findViewById(R.id.nextImageButton);
                previousImageButton = itemView.findViewById(R.id.previousImageButton);
                hotelDistanceTextView=itemView.findViewById(R.id.hotelDistanceTextView);

                itemView.setOnClickListener(view -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ArrayList<Object> clickedHotelData = hotelDataList.get(position);
                        Log.d("poi list d", String.valueOf(clickedHotelData.get(8)));
                        Intent intent = new Intent(itemView.getContext(), HotelInformationActivity.class);
                        intent.putExtra("facilities", TextUtils.join(", ", (List<String>) clickedHotelData.get(1)));
                        intent.putExtra("hotelLatValue", (double) clickedHotelData.get(2));
                        intent.putExtra("hotelLonValue", (double) clickedHotelData.get(3));
                        intent.putExtra("address", clickedHotelData.get(4).toString());
                        intent.putStringArrayListExtra("imageUrls", (ArrayList<String>) clickedHotelData.get(5));
                        intent.putExtra("cntFloors", (int) clickedHotelData.get(6));
                        intent.putExtra("cntRooms", (int) clickedHotelData.get(7));
                        intent.putExtra("poiDistance", (ArrayList<Integer>) clickedHotelData.get(8));
                        intent.putExtra("hotelDistance", (double) clickedHotelData.get(9));
                        intent.putExtra("hotelName", clickedHotelData.get(10).toString());
                        intent.putExtra("stars", (int) clickedHotelData.get(11));
                        intent.putExtra("rating", (int) clickedHotelData.get(12));
                        intent.putExtra("propertyType", clickedHotelData.get(13).toString());
                        intent.putExtra("price", (double) clickedHotelData.get(14));
                        intent.putExtra("hotelLink", clickedHotelData.get(15).toString());
                        intent.putExtra("locationID", locationId);
                        intent.putExtra("selectedTripId", selectedTripId);
                        intent.putExtra("type", type);
                        intent.putExtra("hotelId", (int) clickedHotelData.get(0));
                        intent.putExtra("nightsCount", Integer.parseInt(String.valueOf(nightsCount)));

                        itemView.getContext().startActivity(intent);
                    }
                });
            }
        }
    }
}