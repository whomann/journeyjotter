package com.example.jotterjourney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class HotelBookmarksActivity extends AppCompatActivity {

    ImageView hotelLoading,noBookmarksImageView; View loadingBookmarksView; RecyclerView recyclerView; private String type;Context context = this;private int locationId,nightsCount,adultsCount,selectedTripId; private int retriesCount=0; private SQLiteDatabase db; ArrayList<ArrayList<Object>> bookmarkedHotelsList = new ArrayList<>();  private String apiKey, returnDate, departureDate; ArrayList<Integer> bookmarksListIds = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_bookmarks);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            apiKey = properties.getProperty("hotellookApiKey");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageButton bookmarksBack=findViewById(R.id.bookmarksBack);
        bookmarksBack.setOnClickListener(v->{
            onBackPressed();
        });
        noBookmarksImageView=findViewById(R.id.noBookmarksImageView);
        noBookmarksImageView.setVisibility(View.GONE);
        hotelLoading=findViewById(R.id.hotelLoading);
        hotelLoading.setVisibility(View.VISIBLE);
        Glide.with(this).load(R.drawable.hotel_loading).into(hotelLoading);
        loadingBookmarksView=findViewById(R.id.loadingBookmarksView);
        loadingBookmarksView.setVisibility(View.VISIBLE);
        recyclerView=findViewById(R.id.recyclerViewBookmarks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        locationId=getIntent().getIntExtra("locationId", 0);
        nightsCount=getIntent().getIntExtra("nightsCount",1);
        adultsCount=getIntent().getIntExtra("adultsCount", 1);
        returnDate = getIntent().getStringExtra("returnDate");
        departureDate = getIntent().getStringExtra("departureDate");
        type=getIntent().getStringExtra("type");
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        Log.d("selectedTripId", String.valueOf(selectedTripId));
        Log.d("locationId", String.valueOf(locationId));
        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        readAndLogDataFromSQLite(selectedTripId);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    @SuppressLint("Range")
    private void readAndLogDataFromSQLite(int selectedTripId) {
        Cursor cursor = db.rawQuery("SELECT * FROM jjtrips1 WHERE userID = ?", new String[]{String.valueOf(selectedTripId)});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") String bookmarksList=cursor.getString(cursor.getColumnIndex("bookmarksList"));
                    Log.d("SQLite Data", "bookmarksList: " + bookmarksList);
                    if(bookmarksList != null && !bookmarksList.equals("null") && !bookmarksList.equals("")){
                        String[] bookmarksArray = bookmarksList.split("\\|");
                        for (String s : bookmarksArray) {
                            bookmarksListIds.add(Integer.parseInt(s));
                        }
                        if(!bookmarksListIds.isEmpty()) {
                            new SendHotelsRequestTask().execute();
                        }
                    }
                    else{
                        loadingBookmarksView.setVisibility(View.GONE);
                        hotelLoading.setVisibility(View.GONE);
                        noBookmarksImageView.setVisibility(View.VISIBLE);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
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
    private class SendHotelsRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String hotelRequestApi = "https://cors.eu.org/http://engine.hotellook.com/api/v2/static/hotels.json?locationId=" + locationId + "&token=" + apiKey;
            Log.d("Matching hotels:", hotelRequestApi);
            try {
                String response = makeHttpRequestWithOkHttp(hotelRequestApi);
                Log.d("no type hotels hotels", response);
                return response;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            boolean skipHotel = false;
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
                                int cntFloors = 1;
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
                                            priceFrom = (double) priceInt * 89 * nightsCount;
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
                                if (!skipHotel && bookmarksListIds.contains(hotelId)) {
                                    ArrayList<Object> hotelData = new ArrayList<>(Arrays.asList(hotelId, facilities, latitude, longitude, address, photos, cntFloors, cntRooms, poiDistance, distance, hotelName, stars, rating, propertyType, priceFrom, link));
                                    bookmarkedHotelsList.add(hotelData);
                                }
                                jsonReader.endObject();
                            }
                            jsonReader.endArray();
                            Log.d("bookmarkedHotelsList:", String.valueOf(bookmarkedHotelsList));
                            Log.d("bookmarkedHotelsList len:", String.valueOf(bookmarkedHotelsList.size()));
                            if (bookmarkedHotelsList.isEmpty()) {
                                loadingBookmarksView.setVisibility(View.GONE);
                                hotelLoading.setVisibility(View.GONE);
                                noBookmarksImageView.setVisibility(View.VISIBLE);
                                Toast.makeText(HotelBookmarksActivity.this, "В избранном пока нет отелей.", Toast.LENGTH_SHORT).show();
                            } else {
                                BookmarkedHotelAdapter bookmarkedHotelAdapter = new BookmarkedHotelAdapter(bookmarkedHotelsList);
                                recyclerView.setAdapter(bookmarkedHotelAdapter);
                                retriesCount = 0;
                                hotelLoading.setVisibility(View.GONE);
                                loadingBookmarksView.setVisibility(View.GONE);
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
                if (retriesCount < 4) {
                    retriesCount += 1;
                    progressBar.setVisibility(View.VISIBLE);
                    new SendHotelsRequestTask().execute();
                } else {
                    Toast.makeText(HotelBookmarksActivity.this, "Ошибка сети, проверьте подключение.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    public class BookmarkedHotelAdapter extends RecyclerView.Adapter<HotelBookmarksActivity.BookmarkedHotelAdapter.BookmarkedHotelViewHolder> {

        private ArrayList<ArrayList<Object>> bookmarkedHotelDataList;
        private AtomicInteger currentImageIndex;

        public BookmarkedHotelAdapter(ArrayList<ArrayList<Object>> bookmarkedHotelDataList) {
            this.bookmarkedHotelDataList = bookmarkedHotelDataList;
            this.currentImageIndex = new AtomicInteger(0);
        }
        public void clearData() {
            bookmarkedHotelDataList.clear();
            currentImageIndex.set(0);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public HotelBookmarksActivity.BookmarkedHotelAdapter.BookmarkedHotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hotel_card, parent, false);
            return new HotelBookmarksActivity.BookmarkedHotelAdapter.BookmarkedHotelViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull HotelBookmarksActivity.BookmarkedHotelAdapter.BookmarkedHotelViewHolder holder, int position) {
            ArrayList<Object> hotelData = bookmarkedHotelDataList.get(position);
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
            return bookmarkedHotelDataList.size();
        }

        public class BookmarkedHotelViewHolder extends RecyclerView.ViewHolder {
            public ImageView hotelImageView;
            public TextView hotelNameTextView;
            public TextView hotelPriceTextView;
            public TextView hotelRatingTextView;
            public TextView hotelTypeTextView;
            public ImageButton nextImageButton;
            public ImageButton previousImageButton;
            public TextView hotelDistanceTextView;

            public BookmarkedHotelViewHolder(View itemView) {
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
                        ArrayList<Object> clickedHotelData = bookmarkedHotelDataList.get(position);
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
                        intent.putExtra("nightsCount", Integer.parseInt(String.valueOf(nightsCount)));

                        itemView.getContext().startActivity(intent);
                    }
                });
            }
        }
    }
}