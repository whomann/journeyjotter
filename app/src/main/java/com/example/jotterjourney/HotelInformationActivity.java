package com.example.jotterjourney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.JsonReader;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class HotelInformationActivity extends AppCompatActivity {

    private String hotellookApiKey; ImageView hotelInfoImageView; ArrayList<String> imageUrls; AtomicInteger currentImageIndex; TextView poisNamesTextView,poisDistanceTextView,hotelLanguagesTextView; private double hotelLatValue; private double hotelLonValue; private String hotelName; private String hotelLink; RecyclerView recyclerViewHotelInfo; private GoogleMap mMap; private MapView mMapView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_information);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            hotellookApiKey = properties.getProperty("hotellookApiKey");
        } catch (IOException e) {
            e.printStackTrace();
        }
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.accent_color));
        Map<String, String> propertyTranslations = new HashMap<>();
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
        poisNamesTextView=findViewById(R.id.poisNamesTextView);
        TextView poisLabel=findViewById(R.id.poisLabel);
        hotelLanguagesTextView=findViewById(R.id.hotelLanguagesTextView);
        poisDistanceTextView=findViewById(R.id.poisDistanceTextView);

        currentImageIndex = new AtomicInteger(0);
        hotelName = getIntent().getStringExtra("hotelName");
        double hotelDistance = getIntent().getDoubleExtra("hotelDistance", 0.0);
        String facilities = getIntent().getStringExtra("facilities");
        String address = getIntent().getStringExtra("address");
        int stars = getIntent().getIntExtra("stars", 0);
        int rating = getIntent().getIntExtra("rating", 0);
        String propertyType = getIntent().getStringExtra("propertyType").trim();
        double price = getIntent().getDoubleExtra("price", 0.0);
        hotelLink = getIntent().getStringExtra("hotelLink");
        hotelLatValue = getIntent().getDoubleExtra("hotelLatValue", 0.0);
        hotelLonValue = getIntent().getDoubleExtra("hotelLonValue", 0.0);
        imageUrls = getIntent().getStringArrayListExtra("imageUrls");
        int locationId = getIntent().getIntExtra("locationID", 0);
        int cntFloors = getIntent().getIntExtra("cntFloors", 0);
        int cntRooms = getIntent().getIntExtra("cntRooms", 0);
        int selectedTripId=getIntent().getIntExtra("selectedTripId",1);
        String type=getIntent().getStringExtra("type");
        ArrayList<Integer> poiList = getIntent().getIntegerArrayListExtra("poiDistance");
        Log.d("poiList", String.valueOf(poiList));
        if(stars==0){
            stars=1;
        }

        if(!poiList.isEmpty()) {
            FetchPOIsTask fetchPOIsTask = new FetchPOIsTask(poiList);
            fetchPOIsTask.execute(String.valueOf(locationId));
            poisNamesTextView.setVisibility(View.VISIBLE);
            poisLabel.setVisibility(View.VISIBLE);
            poisDistanceTextView.setVisibility(View.VISIBLE);
        }
        else{
            poisNamesTextView.setVisibility(View.GONE);
            poisLabel.setVisibility(View.GONE);
            poisDistanceTextView.setVisibility(View.GONE);
        }
        TextView hotelInfoNameTextView=findViewById(R.id.hotelInfoNameTextView);
        TextView hotelInfoAddressTextView=findViewById(R.id.hotelInfoAddressTextView);
        hotelInfoImageView=findViewById(R.id.hotelInfoImageView);
        Button bookedHotelButton= findViewById(R.id.bookedHotelButton);
        Button bookHotelButton=findViewById(R.id.bookHotelButton);
        TextView hotelInfoPriceTextView=findViewById(R.id.hotelInfoPriceTextView);
        TextView ratingTextViewHotel=findViewById(R.id.ratingTextViewHotel);
        TextView hotelInfoTextView=findViewById(R.id.hotelInfoTextView);
        ImageButton backToHotelsPageButton=findViewById(R.id.backToHotelsPageButton);
        RecyclerView recyclerViewHotelInfo=findViewById(R.id.recyclerViewHotelInfo);

        recyclerViewHotelInfo.setLayoutManager(new LinearLayoutManager(this));
        mMapView = findViewById(R.id.hotelMapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this::onMapReady);

        //region types
        String json;
        if(Objects.equals(type, "Нет")){
            try {
                InputStream is = this.getAssets().open("roomTypes.json");
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
                propertyType=translatedPropertyType;
            } else {
                propertyType="другое";
            }
        }
        //endregion

        //region facilities
        try {
            InputStream is = this.getAssets().open("filters.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        List<Map<String, Object>> facilityTranslations = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                String id = item.getString("id");
                String ruTranslation = item.getString("ruTranslation");
                String icon = item.getString("icon");
                Map<String, Object> filterData = new HashMap<>();
                filterData.put("id", id);
                filterData.put("name", ruTranslation);
                filterData.put("icon", icon);
                facilityTranslations.add(filterData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        String[] facilityIds = facilities.split(", ");
        ArrayList<ArrayList<Object>> translatedFacilities = new ArrayList<>();
        for (String id : facilityIds) {
            for (Map<String, Object> filterData : facilityTranslations) {
                if (id.equals(filterData.get("id"))) {
                    ArrayList<Object> translatedFilterData = new ArrayList<>();
                    translatedFilterData.add(filterData.get("name"));
                    translatedFilterData.add(filterData.get("icon"));
                    translatedFacilities.add(translatedFilterData);
                    break;
                }
            }
        }
        FiltersAdapter filtersAdapter = new FiltersAdapter(translatedFacilities,hotelLanguagesTextView);
        recyclerViewHotelInfo.setAdapter(filtersAdapter);
        //endregion

        String floorsString;
        if(cntFloors==0 || cntRooms==0){
            floorsString="";
        }
        else{
            floorsString="\nВ отеле "+cntRooms+" комнат, "+cntFloors+" этажей";
        }
        if(propertyType.equals("0")){
            propertyType="отель";
        }
        String starsSymbol = "\u2605";
        hotelInfoAddressTextView.setText(address);
        ratingTextViewHotel.setText(String.valueOf((double) rating / 10));
        hotelInfoPriceTextView.setText("от "+String.valueOf(price)+ " руб.");
        hotelInfoNameTextView.setText(hotelName + " • "+starsSymbol.repeat(stars));
        hotelInfoTextView.setText("Тип: "+propertyType.toLowerCase()+floorsString+"\nРасстояние от центра: "+hotelDistance+" км");
        if (!imageUrls.isEmpty()) {
            updateImage(hotelInfoImageView, imageUrls.get(currentImageIndex.get()));
        } else {
            hotelInfoImageView.setImageResource(R.drawable.placeholder);
        }

        backToHotelsPageButton.setOnClickListener(v->{
            onBackPressed();
        });

        bookHotelButton.setOnClickListener(v->{
            openHotelLink(hotelLink);
        });

        GestureDetector gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        hotelInfoImageView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        bookedHotelButton.setOnClickListener(v -> {
            String updatedHotelInfo = hotelName + ": " + address + ", за " + price + " руб.\nСсылка на Hotellook: "+hotelLink+"\nimage: "+imageUrls.get(0);
            int userId = selectedTripId;
            SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);

            ContentValues values = new ContentValues();
            values.put("hotelInfo", updatedHotelInfo);
            values.put("hotelLat", hotelLatValue);
            values.put("hotelLon", hotelLonValue);

            int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
            if (affectedRows > 0) {
                Log.d("Data updated successfully.", "Data updated successfully.");
                Toast.makeText(this, "Отель забронирован!", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("Error updating data:", "No data updated.");
                Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show();
            }
            db.close();
        });
    }
    private void showMapMarkers() {
        if (mMap != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            LatLng hotelLocation = new LatLng(hotelLatValue, hotelLonValue);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(hotelLocation)
                    .title(hotelName)
                    .snippet(hotelLink);
            mMap.addMarker(markerOptions);
            builder.include(hotelLocation);
            LatLngBounds bounds = builder.build();
            LatLng center = new LatLng((bounds.southwest.latitude + bounds.northeast.latitude) / 2, (bounds.southwest.longitude + bounds.northeast.longitude) / 2);
            float zoomLevel = 12.0f;

            ViewTreeObserver viewTreeObserver = mMapView.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mMapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(center, zoomLevel);
                        mMap.moveCamera(cameraUpdate);
                    }
                });
            }
        }
    }
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng kremlin = new LatLng(55.753930, 37.620795);
        mMap.addMarker(new MarkerOptions().position(kremlin).title("Кремль"));
        showMapMarkers();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    private void openHotelLink(String hotelLink) {
        if (hotelLink != null && !hotelLink.isEmpty()) {
            Uri uri = Uri.parse(hotelLink);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }
    private void updateImage(ImageView imageView, String imageUrl) {
        Picasso.get().load(imageUrl).into(imageView);
    }
    private class FetchPOIsTask extends AsyncTask<String, Void, String> {
        private final ArrayList<Integer> poiList;

        public FetchPOIsTask(ArrayList<Integer> poiList) {
            this.poiList = poiList;
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

        @Override
        protected String doInBackground(String... params) {
            String locationId = params[0];
            String hotelRequestApi = "https://cors.eu.org/http://engine.hotellook.com/api/v2/static/hotels.json?locationId=" + locationId + "&token="+hotellookApiKey;
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
                    Log.d("List of IDs: ", poiList.toString());
                    while (jsonReader.hasNext()) {
                        String name = jsonReader.nextName();
                        if (name.equals("pois")) {
                            jsonReader.beginArray();
                            while (jsonReader.hasNext()) {
                                jsonReader.beginObject();
                                int poiId = -1;
                                String poiName = "";
                                while (jsonReader.hasNext()) {
                                    String poiProperty = jsonReader.nextName();
                                    switch (poiProperty) {
                                        case "id":
                                            poiId = jsonReader.nextInt();
                                            break;
                                        case "name":
                                            poiName = jsonReader.nextString();
                                            break;
                                        default:
                                            jsonReader.skipValue();
                                            break;
                                    }
                                }
                                for (Integer poiInfo : poiList) {
                                    if (poiInfo.equals(poiId)) {
                                        if (poiList.indexOf(poiInfo) + 1 < poiList.size()) {
                                            if(poisNamesTextView.getText().toString().trim().contains("загрузка...")){
                                                poisNamesTextView.setText("");
                                            }
                                            if(poisDistanceTextView.getText().toString().trim().contains("загрузка...")){
                                                poisDistanceTextView.setText("");
                                            }
                                            int distance = poiList.get(poiList.indexOf(poiInfo) + 1);
                                            double distanceInKilometers = distance / 1000.0;
                                            String formattedDistance = String.format("%.1f км", distanceInKilometers);
                                            SpannableString spannableString = new SpannableString(poiName+"\n");
                                            spannableString.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), poiName.length() + 1, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            poisNamesTextView.append(spannableString);
                                            poisDistanceTextView.append(formattedDistance+"\n");
                                            Log.d("poi name", poiName+ ", poi distance"+formattedDistance);
                                        } else {
                                            Log.e("POI", "Distance information missing or out of bounds for ID: " + poiInfo);
                                        }
                                        break;
                                    }
                                }
                                jsonReader.endObject();
                            }
                            jsonReader.endArray();
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public class FiltersAdapter extends RecyclerView.Adapter<HotelInformationActivity.FiltersAdapter.FiltersViewHolder> {

        private List<ArrayList<Object>> filtersDataList;
        private TextView hotelLanguagesTextView;
        private List<String> languages = new ArrayList<>();

        public FiltersAdapter(List<ArrayList<Object>> filtersDataList,TextView hotelLanguagesTextView) {
            this.filtersDataList = filtersDataList;
            this.hotelLanguagesTextView = hotelLanguagesTextView;
        }
        public void clearData() {
            filtersDataList.clear();
            notifyDataSetChanged();
        }
        @NonNull
        @Override
        public HotelInformationActivity.FiltersAdapter.FiltersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.filter_card, parent, false);
            return new HotelInformationActivity.FiltersAdapter.FiltersViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull HotelInformationActivity.FiltersAdapter.FiltersViewHolder holder, int position) {
            ArrayList<Object> filterData = filtersDataList.get(position);
            String filterName = filterData.get(0).toString();
            String iconName = filterData.get(1).toString();

            if (filterName.contains("язык")) {
                String language = filterName.substring(0, filterName.indexOf("язык") + 4);
                if (!languages.contains(language)) {
                    languages.add(language);
                    hotelLanguagesTextView.setVisibility(View.VISIBLE);
                    if (hotelLanguagesTextView.getText().toString().endsWith("Персонал знает: ")) {
                        hotelLanguagesTextView.append(language.toLowerCase());
                    } else {
                        hotelLanguagesTextView.append(", " + language.toLowerCase());
                    }
                }
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.getLayoutParams().height = 0;
            } else {
                updateImage(holder.iconView, iconName);
                holder.filterName.setText(filterName);
                holder.itemView.setVisibility(View.VISIBLE);
                hotelLanguagesTextView.setVisibility(View.GONE);
                holder.itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        }
        private void updateImage(ImageView imageView, String iconName) {
            int resourceId = imageView.getContext().getResources().getIdentifier(iconName, "drawable", imageView.getContext().getPackageName());
            if (resourceId != 0) {
                imageView.setImageResource(resourceId);
            } else {
                imageView.setImageResource(R.drawable.community);
            }
        }

        @Override
        public int getItemCount() {
            return filtersDataList.size();
        }

        public class FiltersViewHolder extends RecyclerView.ViewHolder {
            public ImageView iconView;
            public TextView filterName;

            public FiltersViewHolder(View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.iconView);
                filterName=itemView.findViewById(R.id.filterName);
            }
        }
    }
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)
                    && Math.abs(diffX) > SWIPE_THRESHOLD
                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    showPreviousImage();
                } else {
                    showNextImage();
                }
                return true;
            }
            return false;
        }
    }

    private void showPreviousImage() {
        if (currentImageIndex.get() > 0) {
            currentImageIndex.decrementAndGet();
            updateImage(hotelInfoImageView, imageUrls.get(currentImageIndex.get()));
        }
    }

    private void showNextImage() {
        if (currentImageIndex.get() < imageUrls.size() - 1) {
            currentImageIndex.incrementAndGet();;
            updateImage(hotelInfoImageView, imageUrls.get(currentImageIndex.get()));
        }
    }
}