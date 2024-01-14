package com.example.jotterjourney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.icu.util.Calendar;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaunchActivity extends AppCompatActivity {
    ProgressBar progressBar3; private Map<String, String> cityToCountryCodeMap;private String photoTargetIata,englishTargetName; ImageButton showImageButton; private String yandexTaxiApiKey, yandexClid; private int selectedTripId; RecyclerView recyclerViewEventsLaunch; TextView visaTextView; private String countryCode; RecyclerView recyclerViewLaunchGuides,recyclerViewLaunchLandmarks; private String hotelLink; Button guideButton, landmarksButton; RadioButton planeRadioButton,trainRadioButton; RadioGroup transportationRadioGroup; String transport; ImageButton hotelLinkButton,taxiLinkButton; String hotelInfo, yandexTaxiURL; TextView transferInfoTextView; String targetLocation, locationNameForTaxi, targetIATA, originIATA; Button orderTransfer; Button chooseTicketButton; EditText adultCountEditText; AutoCompleteTextView departureLocationSelect, targetLocationSelect; TextView departureDateTextView; TextView returnDateTextView; private int returnYear; private int returnMonth; private int returnDay; private String returnDate, departureDate; private SQLiteDatabase db; private int adultsCount; private String hotelLat, hotelLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            yandexTaxiApiKey = properties.getProperty("yandexTaxiApiKey");
            yandexClid=properties.getProperty("yandexClid");
        } catch (IOException e) {
            e.printStackTrace();
        }
        progressBar3=findViewById(R.id.progressBar3);
        progressBar3.setVisibility(View.INVISIBLE);
        transport="plane";
        showImageButton=findViewById(R.id.showImageButton);
        landmarksButton=findViewById(R.id.landmarksButton);
        guideButton=findViewById(R.id.guideButton);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        Button selectReturnDateButton = findViewById(R.id.selectReturnDateButton);
        Button selectDepartureDateButton = findViewById(R.id.selectDepartureDateButton);
        planeRadioButton=findViewById(R.id.planeRadioButton);
        planeRadioButton.setChecked(true);
        trainRadioButton=findViewById(R.id.trainRadioButton);
        final Calendar calendar = Calendar.getInstance();
        returnYear = calendar.get(Calendar.YEAR);
        returnMonth = calendar.get(Calendar.MONTH);
        returnDay = calendar.get(Calendar.DAY_OF_MONTH);
        departureDateTextView = findViewById(R.id.departureDateTextView);
        returnDateTextView = findViewById(R.id.returnDateTextView);
        departureLocationSelect = findViewById(R.id.departureLocationSelect);
        targetLocationSelect = findViewById(R.id.targetLocationSelect);
        adultCountEditText = findViewById(R.id.adultCountEditText);
        chooseTicketButton=findViewById(R.id.chooseTicketButton);
        orderTransfer=findViewById(R.id.transferButton);
        taxiLinkButton=findViewById(R.id.taxiLinkButton);
        recyclerViewLaunchGuides=findViewById(R.id.recyclerViewLaunchGuides);
        recyclerViewLaunchLandmarks=findViewById(R.id.recyclerViewLaunchLandmarks);
        recyclerViewEventsLaunch=findViewById(R.id.recyclerViewEventsLaunch);
        recyclerViewLaunchLandmarks.setVisibility(View.GONE);
        recyclerViewLaunchGuides.setVisibility(View.GONE);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        LinearLayoutManager layoutManagerLandmarks = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        LinearLayoutManager layoutManagerEvents = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerViewLaunchGuides.setLayoutManager(layoutManager);
        recyclerViewLaunchLandmarks.setLayoutManager(layoutManagerLandmarks);
        recyclerViewEventsLaunch.setLayoutManager(layoutManagerEvents);
        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        PagerSnapHelper pagerSnapHelperLandmarks = new PagerSnapHelper();
        PagerSnapHelper pagerSnapHelperEvents = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(recyclerViewLaunchGuides);
        pagerSnapHelperEvents.attachToRecyclerView(recyclerViewEventsLaunch);
        pagerSnapHelperLandmarks.attachToRecyclerView(recyclerViewLaunchLandmarks);
        visaTextView=findViewById(R.id.visaTextView);
        hotelLinkButton = findViewById(R.id.hotelLinkButton);
        transportationRadioGroup = findViewById(R.id.transportationRadioGroup);
        taxiLinkButton.setVisibility(View.GONE);
        hotelLinkButton.setVisibility(View.GONE);
        showImageButton.setVisibility(View.GONE);
        ImageButton selectTripButton= findViewById(R.id.selectTripButton);
        Button countryInfoButton = findViewById(R.id.countryInfoButton);
        Button checkEventsButton= findViewById(R.id.checkEventsButton);
        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);

        SharedPreferences preferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        String value = preferences.getString("selectedTripId", null);
        if (value != null) {
            selectedTripId = Integer.parseInt(value);
        } else {
            selectedTripId = 1;
        }
        insertDataIntoSQLite(selectedTripId);

        selectTripButton.setOnClickListener(view -> showTripSelectionPopup());
        if (selectedTripId != -1) {
            readAndLogDataFromSQLite(selectedTripId);
        } else {
            selectedTripId = generateNewTripId();
            insertDataIntoSQLite(selectedTripId);
            readAndLogDataFromSQLite(selectedTripId);
        }

        departureLocationSelect.setOnClickListener(v -> departureLocationSelect.requestFocus());
        checkEventsButton.setOnClickListener(v -> {
            Intent intent = new Intent(LaunchActivity.this, EventActivity.class);
            intent.putExtra("selectedTripId", selectedTripId);
            startActivity(intent);
        });

        departureLocationSelect.setOnItemClickListener((parent, view, position, id) -> {
            departureLocationSelect.clearFocus();
            if(!departureLocationSelect.getText().toString().equals("") && !targetLocationSelect.getText().toString().equals("")){
                checkVisa();
            }
        });

        countryInfoButton.setOnClickListener(v -> {
            if(!targetLocationSelect.getText().toString().equals("")) {
                String countryCodeLocal=getCountryCode(targetLocationSelect.getText().toString());
                openCountryInfoPopup(countryCodeLocal);
            }
            else{
                Toast.makeText(LaunchActivity.this, "Выберите страну назначения", Toast.LENGTH_SHORT).show();
            }
        });

        targetLocationSelect.setOnClickListener(v -> targetLocationSelect.requestFocus());

        targetLocationSelect.setOnItemClickListener((parent, view, position, id) -> {
            targetLocationSelect.clearFocus();
            if(!departureLocationSelect.getText().toString().equals("") && !targetLocationSelect.getText().toString().equals("")){
                showImageButton.setVisibility(View.GONE);
                checkVisa();
                String requestUrl = "https://www.travelpayouts.com/widgets_suggest_params?q=Из%20Москва%20в%20" + targetLocation;
                Log.d("loc request", requestUrl);
                new SendIataRequestTask().execute(requestUrl);
            }
        });
        transportationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.planeRadioButton:
                    transport = "plane";
                    break;
                case R.id.trainRadioButton:
                    transport = "train";
                    break;
            }
        });
        selectReturnDateButton.setOnClickListener(v -> showDatePickerDialog(v, "returnDate"));

        selectDepartureDateButton.setOnClickListener(v -> showDatePickerDialog(v, "departureDate"));
        orderTransfer.setOnClickListener(view -> {
            if (!targetLocationSelect.getText().toString().equals("") && !departureLocationSelect.getText().toString().equals("") && !Objects.equals(hotelLat, "null") && !Objects.equals(hotelLon, "null") &&adultsCount!=0&& hotelLat != null&&hotelLon!=null) {
                if(countryCode==null){
                    countryCode=getCountryCode(targetLocationSelect.getText().toString());
                }
                if (Objects.equals(countryCode, "RU") || Objects.equals(countryCode, "RS") || Objects.equals(countryCode, "KZ") || Objects.equals(countryCode, "MD") || Objects.equals(countryCode, "LT") || Objects.equals(countryCode, "GE") || Objects.equals(countryCode, "AM") || Objects.equals(countryCode, "BY") || Objects.equals(countryCode, "KG") || Objects.equals(countryCode, "UZ")) {
                    String hotelName = extractHotelName(hotelInfo);
                    String locationNameForTaxiOrder;
                    if (Objects.equals(transport, "plane")) {
                        locationNameForTaxiOrder = "Аэропорт " + targetIATA + ", г. " + targetLocation;
                    } else {
                        locationNameForTaxiOrder = "Вокзал " + targetIATA + ", г. " + targetLocation;
                    }
                    double airportLatTaxi = 0.0;
                    double airportLonTaxi = 0.0;
                    LatLng coordinates = getCoordinates(locationNameForTaxiOrder);
                    if (coordinates != null) {
                        airportLatTaxi = coordinates.latitude;
                        airportLonTaxi = coordinates.longitude;
                        String appmetrica_tracking_id = "25395763362139037";
                        yandexTaxiURL = "https://3.redirect.appmetrica.yandex.com/route?start-lat=" + airportLatTaxi +
                                "&start-lon=" + airportLonTaxi + "&end-lat=" + hotelLat + "&end-lon=" + hotelLon +
                                "&tariffClass=econom&ref=trusttripcom&appmetrica_tracking_id=" + appmetrica_tracking_id + "&lang=ru";
                        Log.d("Yandex taxi url:", yandexTaxiURL);
                        WebView webView = new WebView(LaunchActivity.this);
                        new TaxiAsyncTask(LaunchActivity.this, webView, airportLatTaxi, airportLonTaxi, Double.parseDouble(hotelLon), Double.parseDouble(hotelLat), yandexTaxiURL, hotelName, locationNameForTaxiOrder, transferInfoTextView, selectedTripId,yandexTaxiApiKey, yandexClid).execute();
                    }
                } else {
                    Toast.makeText(LaunchActivity.this, "Сервис такси не работает в выбранной стране, попробуйте найти локальный трансфер.", Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(LaunchActivity.this, "Выберите отель и точки отправления и назначения.", Toast.LENGTH_SHORT).show();
            }
        });
        new LoadDataAsyncTask().execute();
        readAndLogDataFromSQLite(selectedTripId);
    }
    private void openCountryInfoPopup(String countryCode) {
        CountryInfoPopup countryInfoPopup = new CountryInfoPopup(this, countryCode);
        countryInfoPopup.show();
    }
    @SuppressLint("Range")
    private void showTripSelectionPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        SpannableString spannableTitle = new SpannableString("Выберите план путешествия");
        TypefaceSpan typefaceSpan = new TypefaceSpan("montserrat_bold");
        spannableTitle.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.accent_color)), 0, spannableTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableTitle.setSpan(typefaceSpan, 0, spannableTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setTitle(spannableTitle);
        Cursor cursor = db.rawQuery("SELECT userID FROM jjtrips1", null);
        List<Integer> tripIds = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                tripIds.add(cursor.getInt(cursor.getColumnIndex("userID")));
            } while (cursor.moveToNext());
            cursor.close();
        }
        Integer[] tripIdArray = tripIds.toArray(new Integer[0]);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tripIdArray);
        builder.setAdapter(adapter, (dialog, which) -> {
            selectedTripId = tripIdArray[which];
            SharedPreferences preferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("selectedTripId", String.valueOf(selectedTripId));
            editor.apply();
            readAndLogDataFromSQLite(selectedTripId);
        });
        SpannableString createNewPlan = new SpannableString("Создать новый план");
        int color = ContextCompat.getColor(this, R.color.accent_color);
        createNewPlan.setSpan(new StyleSpan(Typeface.BOLD), 0, createNewPlan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        createNewPlan.setSpan(new ForegroundColorSpan(color), 0, createNewPlan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setPositiveButton(createNewPlan, (dialog, which) -> {
            selectedTripId = generateNewTripId();
            insertDataIntoSQLite(selectedTripId);
            readAndLogDataFromSQLite(selectedTripId);
        });

        SpannableString deleteAllPlans = new SpannableString("Очистить");
        deleteAllPlans.setSpan(new StyleSpan(Typeface.BOLD), 0, deleteAllPlans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        deleteAllPlans.setSpan(new ForegroundColorSpan(color), 0, deleteAllPlans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setNegativeButton(deleteAllPlans, (dialog, which) -> {
            clearDatabase();
        });
        builder.create().show();
    }

    private void clearDatabase(){
        SharedPreferences shPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = shPreferences.edit();
        editor.clear();
        editor.apply();
        db.execSQL("DROP TABLE IF EXISTS jjtrips1");
        selectedTripId=1;
        insertDataIntoSQLite(selectedTripId);
        LandmarksLaunchAdapter landmarksLaunchAdapterClear = new LandmarksLaunchAdapter(new ArrayList<>());
        recyclerViewLaunchLandmarks.setAdapter(landmarksLaunchAdapterClear);
        landmarksLaunchAdapterClear.clearData();
        ToursLaunchAdapter toursLaunchAdapterClear=new ToursLaunchAdapter(new ArrayList<>());
        recyclerViewLaunchGuides.setAdapter(toursLaunchAdapterClear);
        toursLaunchAdapterClear.clearData();
        EventsLaunchAdapter eventsLaunchAdapterClear=new EventsLaunchAdapter(new ArrayList<>());
        recyclerViewEventsLaunch.setAdapter(eventsLaunchAdapterClear);
        eventsLaunchAdapterClear.clearData();
    }
    private int generateNewTripId() {
        Cursor cursor = db.rawQuery("SELECT MAX(userID) FROM jjtrips1", null);
        int maxUserId = 0;
        if (cursor != null && cursor.moveToFirst()) {
            maxUserId = cursor.getInt(0);
            cursor.close();
        }
        return maxUserId + 1;
    }

    private void checkVisa(){
        String countryCodeLocal=getCountryCode(targetLocationSelect.getText().toString());
        String departureCountryCodeLocal=getCountryCode(departureLocationSelect.getText().toString());
        if(!Objects.equals(countryCodeLocal, departureCountryCodeLocal)){
            String apiUrl = "https://rough-sun-2523.fly.dev/api/" +departureCountryCodeLocal + "/" + countryCodeLocal;
            Log.d("visa url",apiUrl);
            progressBar3.setVisibility(View.VISIBLE);
            new FetchStatusTask().execute(apiUrl);
        }
        else{
            visaTextView.setText("Виза: не нужна");
        }
    }
    private void initializeCityMap() {
        cityToCountryCodeMap = new HashMap<>();
        try {
            JSONArray citiesArray = loadCodeJSONFromAsset(LaunchActivity.this, "cities_sorted.json");
            for (int i = 0; i < citiesArray.length(); i++) {
                JSONObject city = citiesArray.getJSONObject(i);
                String cityName = city.getString("name");
                String countryCode = city.getString("country_code");
                Log.d("City and Country Code", cityName + ": " + countryCode);
                cityToCountryCodeMap.put(cityName, countryCode);
            }
        } catch (IOException | JSONException e) {
            Log.d("Error", String.valueOf(e));
        }
    }

    private String getCountryCode(String selectedCity) {
        if (cityToCountryCodeMap == null) {
            initializeCityMap();
        }
        if (selectedCity == null || selectedCity.isEmpty()) {
            return "";
        }
        return cityToCountryCodeMap.getOrDefault(selectedCity.trim(), "");
    }
    private JSONArray loadCodeJSONFromAsset(Context context, String fileName) throws IOException, JSONException {
        InputStream inputStream = context.getAssets().open(fileName);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();
        String json = new String(buffer, StandardCharsets.UTF_8);
        return new JSONArray(json);
    }

    private static String getExpectedPrice(String apiUrl, String yandexTaxiApiKey) {
        try {
            URL apiUrlObj = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) apiUrlObj.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Api-Key " + yandexTaxiApiKey);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                Log.d("response", String.valueOf(response));
                return parseApiResponse(response.toString());
            } else {
                System.out.println("API request failed. Response Code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private static String parseApiResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray optionsArray = jsonObject.getJSONArray("options");
            if (optionsArray.length() > 0) {
                JSONObject firstOption = optionsArray.getJSONObject(0);
                if (firstOption.has("price_text")) {
                    return firstOption.getString("price_text");
                } else if (firstOption.has("price")) {
                    return "от " + firstOption.getDouble("price") + " руб.";
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static String extractHotelName(String hotelInfo) {
        int colonIndex = hotelInfo.indexOf(':');
        int commaIndex = hotelInfo.indexOf(',');
        if (colonIndex != -1 && commaIndex != -1) {
            return hotelInfo.substring(colonIndex + 1, commaIndex).trim();
        } else {
            System.out.println("Invalid hotel information format");
            return null;
        }
    }
    public static void writeTransferDataToDB(Context context, String transferInfo, String locationNameForTaxiOrder, int selectedTripId){
        int userId = selectedTripId;
        SQLiteDatabase db = context.openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        ContentValues values = new ContentValues();
        values.put("transferInfo", transferInfo);
        values.put("locationNameForTaxi", locationNameForTaxiOrder);
        int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
        if (affectedRows > 0) {
            Log.d("Data updated successfully.", "Data updated successfully.");
        } else {
            Log.e("Error updating data:", "No data updated.");
        }
        db.close();
    }
    @Override
    protected void onResume() {
        super.onResume();
        departureLocationSelect.clearFocus();
        targetLocationSelect.clearFocus();
        String selectedCity = targetLocationSelect.getText().toString();
        countryCode=getCountryCode(selectedCity);
        readAndLogDataFromSQLite(selectedTripId);
    }
    private LatLng getCoordinates(String address) {
        Geocoder geocoder = new Geocoder(this);

        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);

            if (addresses.size() > 0) {
                Address location = addresses.get(0);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                return new LatLng(latitude, longitude);
            } else {
                Log.e("Geocoder", "Address not found");
                runOnUiThread(() -> Toast.makeText(LaunchActivity.this, "Address not found", Toast.LENGTH_SHORT).show());
            }
        } catch (IOException e) {
            Log.e("MapActivity", "Error fetching location data", e);
            runOnUiThread(() -> Toast.makeText(LaunchActivity.this, "An error occurred while fetching location data.", Toast.LENGTH_SHORT).show());
        }
        return null;
    }

    private void insertDataIntoSQLite(int selectedTripId) {
        //db.execSQL("DROP TABLE IF EXISTS jjtrips1");
        String createTableSQL = "CREATE TABLE IF NOT EXISTS jjtrips1 (userID INTEGER PRIMARY KEY, departureDate TEXT, returnDate TEXT, " +
                "adultsCount INTEGER, departureLocation TEXT, transferInfo TEXT, hotelLat REAL, hotelLon REAL, " +
                "hotelInfo TEXT, targetLocation TEXT, ticketInfo TEXT, transport TEXT, locationNameForTaxi TEXT, originIATA TEXT, targetIATA TEXT, activitiesList TEXT,landmarksList TEXT, eventsList TEXT, bookmarksList TEXT)";
        db.execSQL(createTableSQL);
        Cursor cursor = db.rawQuery("SELECT 1 FROM jjtrips1 WHERE userId=?", new String[]{String.valueOf(selectedTripId)});
        if (!cursor.moveToFirst()) {
            ContentValues initialValues = new ContentValues();
            initialValues.put("userID", selectedTripId);
            long newRowId = db.insert("jjtrips1", null, initialValues);
            if (newRowId != -1) {
                Log.d("Initial data inserted successfully.", "Initial data inserted successfully.");
            } else {
                Log.e("Error inserting initial data:", "No initial data inserted.");
            }
        }
        readAndLogDataFromSQLite(selectedTripId);
    }

    @SuppressLint("Range")
    private void readAndLogDataFromSQLite(int targetUserID) {
        String[] selectionArgs = {String.valueOf(targetUserID)};
        if (isTableExists(db, "jjtrips1")) {
            Cursor cursor = db.rawQuery("SELECT * FROM jjtrips1 WHERE userID = ?", selectionArgs);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        @SuppressLint("Range") int userID = cursor.getInt(cursor.getColumnIndex("userID"));
                        departureDate = cursor.getString(cursor.getColumnIndex("departureDate"));
                        returnDate = cursor.getString(cursor.getColumnIndex("returnDate"));
                        adultsCount = cursor.getInt(cursor.getColumnIndex("adultsCount"));
                        String departureLocation = cursor.getString(cursor.getColumnIndex("departureLocation"));
                        String transferInfo = cursor.getString(cursor.getColumnIndex("transferInfo"));
                        hotelLat = cursor.getString(cursor.getColumnIndex("hotelLat"));
                        hotelLon = cursor.getString(cursor.getColumnIndex("hotelLon"));
                        hotelInfo = cursor.getString(cursor.getColumnIndex("hotelInfo"));
                        targetLocation = cursor.getString(cursor.getColumnIndex("targetLocation"));
                        String ticketInfo = cursor.getString(cursor.getColumnIndex("ticketInfo"));
                        transport = cursor.getString(cursor.getColumnIndex("transport"));
                        locationNameForTaxi = cursor.getString(cursor.getColumnIndex("locationNameForTaxi"));
                        originIATA = cursor.getString(cursor.getColumnIndex("originIATA"));
                        targetIATA = cursor.getString(cursor.getColumnIndex("targetIATA"));
                        String activitiesList = cursor.getString(cursor.getColumnIndex("activitiesList"));
                        String landmarksList=cursor.getString(cursor.getColumnIndex("landmarksList"));
                        String eventsList = cursor.getString(cursor.getColumnIndex("eventsList"));
                        String bookmarksList=cursor.getString(cursor.getColumnIndex("bookmarksList"));
                        if(adultsCount==0){
                            adultsCount=1;
                        }

                        Log.d("SQLite Data", "User ID: " + userID);
                        Log.d("SQLite Data", "Departure Date: " + departureDate);
                        Log.d("SQLite Data", "Return Date: " + returnDate);
                        Log.d("SQLite Data", "Adults Count: " + adultsCount);
                        Log.d("SQLite Data", "Departure Location: " + departureLocation);
                        Log.d("SQLite Data", "Transfer Info: " + transferInfo);
                        Log.d("SQLite Data", "Hotel Latitude: " + hotelLat);
                        Log.d("SQLite Data", "Hotel Longitude: " + hotelLon);
                        Log.d("SQLite Data", "Hotel Info: " + hotelInfo);
                        Log.d("SQLite Data", "Target Location: " + targetLocation);
                        Log.d("SQLite Data", "Ticket Info: " + ticketInfo);
                        Log.d("SQLite Data", "Transport: " + transport);
                        Log.d("SQLite Data", "Location Name for Taxi: " + locationNameForTaxi);
                        Log.d("SQLite Data", "Target IATA: " + targetIATA);
                        Log.d("SQLite Data", "Origin IATA: " + originIATA);
                        Log.d("SQLite Data", "Activities List: " + activitiesList);
                        Log.d("SQLite Data", "Landmarks List: " + landmarksList);
                        Log.d("SQLite Data", "Events List: " + eventsList);
                        Log.d("SQLite Data", "Bookmarks List: " + bookmarksList);
                        if (transport != null) {
                            if (transport.trim().equalsIgnoreCase("train")) {
                                trainRadioButton.setChecked(true);
                            } else if (transport.trim().equalsIgnoreCase("plane") || transport.trim().isEmpty()) {
                                planeRadioButton.setChecked(true);
                            }
                        }
                        else{
                            transport="plane";
                            planeRadioButton.setChecked(true);
                        }
                        returnDateTextView.setText(returnDate);
                        departureDateTextView.setText(departureDate);
                        departureLocationSelect.setText(departureLocation);
                        targetLocationSelect.setText(targetLocation);
                        if(!targetLocationSelect.getText().toString().equals("") && !departureLocationSelect.getText().toString().equals("")){
                            checkVisa();
                            showImageButton.setVisibility(View.GONE);
                            String requestUrl = "https://www.travelpayouts.com/widgets_suggest_params?q=Из%20Москва%20в%20" + targetLocation;
                            Log.d("loc request", requestUrl);
                            new SendIataRequestTask().execute(requestUrl);
                        }
                        adultCountEditText.setText(String.valueOf(adultsCount));
                        if (hotelInfo != null) {
                            setHotelDisplay(hotelInfo);
                        }
                        else{
                            hotelLinkButton.setVisibility(View.GONE);
                            ImageView hotelImageView=findViewById(R.id.hotelImageView);
                            TextView hotelNameTextView=findViewById(R.id.hotelNameTextView);
                            TextView hotelAddressTextView=findViewById(R.id.hotelAddressTextView);
                            TextView hotelPriceTextView=findViewById(R.id.hotelPriceTextView);
                            hotelImageView.setImageResource(R.drawable.placeholder);
                            hotelNameTextView.setText("Отель не забронирован");
                            hotelAddressTextView.setText("Но пора это исправить");
                            hotelPriceTextView.setText("↓");
                        }
                        if(ticketInfo!=null && !Objects.equals(transport, "train")){
                            setTicketDisplay(ticketInfo);
                        }
                        else if(ticketInfo!=null && !transport.equals("plane")){
                            setTrainTicketInfo(ticketInfo);
                        }
                        else{
                            ImageView ticketBackgroundImageView=findViewById(R.id.ticketBackgroundImageView);
                            ticketBackgroundImageView.setImageResource(R.drawable.plane_background1);
                            TextView departureLine=findViewById(R.id.departureLine);
                            TextView departureDateLine=findViewById(R.id.departureDateLine);
                            TextView returnLine=findViewById(R.id.returnLine);
                            TextView returnDateLine=findViewById(R.id.returnDateLine);
                            departureLine.setText("Билет пока не выбран");
                            departureDateLine.setText("воспользуйтесь поиском билетов");
                            returnLine.setText("");
                            returnDateLine.setText("");
                        }

                        if(transferInfo!=null){
                            setTransferDisplay(transferInfo);
                        }
                        else{
                            taxiLinkButton.setVisibility(View.GONE);
                            TextView transferAddressLine=findViewById(R.id.transferAddressLine);
                            TextView transferPriceLine = findViewById(R.id.transferPriceLine);
                            transferAddressLine.setText("Трансфер\nв 20 странах мира");
                            transferPriceLine.setText("от 50 руб.");
                        }
                        if(eventsList!=null){
                            setEventsInfo(eventsList);
                        }
                        else{
                            recyclerViewEventsLaunch.setVisibility(View.GONE);
                        }

                        if(activitiesList!=null){
                            setActivitiesInfo(activitiesList);
                        }
                        else{
                            recyclerViewLaunchGuides.setVisibility(View.GONE);
                        }

                        if(landmarksList!=null){
                            setLandmarksInfo(landmarksList);
                        }
                        else{
                            recyclerViewLaunchLandmarks.setVisibility(View.GONE);
                        }
                        cursor.close();
                    } while (cursor.moveToNext());
                }
                cursor.close();
            } else {
                insertDataIntoSQLite(selectedTripId);
            }
        }
    }
    private void setLandmarksInfo(String landmarksInfo){
        recyclerViewLaunchLandmarks.setVisibility(View.VISIBLE);
        ArrayList<ArrayList<Object>> parsedLandmarksList = parseLandmarksString(landmarksInfo);
        Log.d("landmarksList", String.valueOf(parsedLandmarksList));
        LandmarksLaunchAdapter adapter = new LandmarksLaunchAdapter(parsedLandmarksList);
        recyclerViewLaunchLandmarks.setAdapter(adapter);
    }
    private static ArrayList<ArrayList<Object>> parseLandmarksString(String landmarksString) {
        ArrayList<ArrayList<Object>> landmarksList = new ArrayList<>();
        Pattern pattern = Pattern.compile("(.*?) \\(https://[^)]+\\), (.+)");
        Matcher matcher = pattern.matcher(landmarksString);
        while (matcher.find()) {
            String title = matcher.group(1);
            String address = matcher.group(2);
            String photoUrl = extractPhotoUrl(matcher.group());
            ArrayList<Object> landmarkInfo = new ArrayList<>();
            landmarkInfo.add(title.trim());
            landmarkInfo.add(photoUrl.trim());
            landmarkInfo.add(address.trim());
            landmarksList.add(landmarkInfo);
        }
        return landmarksList;
    }

    private void setEventsInfo(String eventsInfo){
        recyclerViewEventsLaunch.setVisibility(View.VISIBLE);
        ArrayList<ArrayList<Object>> parsedEventsList = parseEventInfo(eventsInfo);
        Log.d("parsedEventsList", String.valueOf(parsedEventsList));
        EventsLaunchAdapter adapter = new EventsLaunchAdapter(parsedEventsList);
        recyclerViewEventsLaunch.setAdapter(adapter);
    }
    private static ArrayList<ArrayList<Object>> parseEventInfo(String eventsInfo) {
        ArrayList<ArrayList<Object>> eventsList = new ArrayList<>();
        String[] events = eventsInfo.split("\\n");
        for (String event : events) {
            String[] fields = event.split("\\|");
            if (fields.length == 4) {
                ArrayList<Object> eventInfo = new ArrayList<>();
                eventInfo.add(fields[0].trim());
                eventInfo.add(fields[1].trim());
                eventInfo.add(fields[2].trim());
                eventInfo.add(fields[3].trim());
                eventsList.add(eventInfo);
            } else {
                Log.d("Invalid format", "Invalid event format: " + event);
            }
        }
        return eventsList;
    }
    private static String extractPhotoUrl(String input) {
        Pattern photoUrlPattern = Pattern.compile("\\((https://[^)]+)\\)");
        Matcher photoUrlMatcher = photoUrlPattern.matcher(input);
        if (photoUrlMatcher.find()) {
            return photoUrlMatcher.group(1);
        }
        return "";
    }

    private void setActivitiesInfo(String activitiesList){
        recyclerViewLaunchGuides.setVisibility(View.VISIBLE);
        ArrayList<ArrayList<Object>> toursList = parseToursString(activitiesList);
        ToursLaunchAdapter adapter = new ToursLaunchAdapter(toursList);
        recyclerViewLaunchGuides.setAdapter(adapter);
    }

    private static ArrayList<ArrayList<Object>> parseToursString(String toursString) {
        ArrayList<ArrayList<Object>> toursList = new ArrayList<>();
        String[] toursArray = toursString.split("\n");
        for (String tour : toursArray) {
            String[] tourInfo = tour.split(" • ");
            if (tourInfo.length == 2) {
                String[] details = tourInfo[1].split(" за ");
                if (details.length == 2) {
                    String title = tourInfo[0].trim();
                    String duration = details[0].trim();
                    String priceWithCurrency = details[1].trim();
                    String[] priceParts = priceWithCurrency.split("\\s+");
                    String price = (priceParts.length > 1) ? "от " + priceParts[1] + " " + priceParts[0] : "";

                    String photoUrl = priceWithCurrency.replaceAll(".*\\((.*?)\\).*", "$1").trim();
                    String link = tourInfo[1].replaceAll(".*link: ", "").trim();
                    ArrayList<Object> activityInfo = new ArrayList<>();
                    activityInfo.add(title);
                    activityInfo.add(duration);
                    activityInfo.add(price);
                    activityInfo.add(photoUrl);
                    activityInfo.add(link);

                    toursList.add(activityInfo);
                }
            }
        }
        return toursList;
    }

    private void setTransferDisplay(String transferInfo) {
        Pattern pattern = Pattern.compile("(.+?), от (\\d+[\\s\\D]+?)\\s+Ссылка YandexGo: (.+)");
        Matcher matcher = pattern.matcher(transferInfo);
        TextView transferAddressLine = findViewById(R.id.transferAddressLine);
        TextView transferPriceLine = findViewById(R.id.transferPriceLine);
        taxiLinkButton = findViewById(R.id.taxiLinkButton);
        String price = null, pathLine = null, link = null;
        if (matcher.find()) {
            pathLine = matcher.group(1);
            price = matcher.group(2);
            link = matcher.group(3);
            taxiLinkButton.setVisibility(View.VISIBLE);
        }
        transferAddressLine.setText(pathLine);
        transferPriceLine.setText("от " + price);
        String finalLink = link;
        taxiLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalLink));
                startActivity(browserIntent);
            }
        });
    }
    private void setTrainTicketInfo(String ticketInfo){
        Pattern pattern = Pattern.compile("Билет туда: (.+?)\n(.+?) - (.+?) \\((.+?)\\)\n\nБилет обратно: (.+?)\n(.+?) - (.+?) \\((.+?)\\)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(ticketInfo);
        String originTrainName = null;
        String originStation = null;
        String originArrivalStation = null;
        String returnTrainName = null;
        String originDate = null;
        String returnStation = null;
        String returnArrivalStation = null;
        String returnDatee = null;
        TextView departureLine = findViewById(R.id.departureLine);
        TextView returnLine=findViewById(R.id.returnLine);
        TextView departureDateLine = findViewById(R.id.departureDateLine);
        TextView returnDateLine=findViewById(R.id.returnDateLine);
        ImageView ticketBackgroundImageView=findViewById(R.id.ticketBackgroundImageView);
        ticketBackgroundImageView.setImageResource(R.drawable.train_background1);

        if (matcher.matches()) {
            originTrainName = matcher.group(1);
            originStation = matcher.group(2);
            originArrivalStation = matcher.group(3);
            originDate = matcher.group(4);

            returnTrainName = matcher.group(5);
            returnStation = matcher.group(6);
            returnArrivalStation = matcher.group(7);
            returnDatee = matcher.group(8);
        } else {
            Log.d("Invalid ticketInfo format", ticketInfo);
        }
        departureDateLine.setText(originDate);
        departureLine.setText(originTrainName + ": " + originStation + " - " + originArrivalStation);
        returnLine.setText(returnTrainName + ": " + returnStation + " - " + returnArrivalStation);
        returnDateLine.setText(returnDatee);

    }
    private void setTicketDisplay(String ticketInfo){
        TextView departureLine = findViewById(R.id.departureLine);
        TextView returnLine=findViewById(R.id.returnLine);
        TextView departureDateLine = findViewById(R.id.departureDateLine);
        TextView returnDateLine=findViewById(R.id.returnDateLine);
        ImageView ticketBackgroundImageView=findViewById(R.id.ticketBackgroundImageView);
        ticketBackgroundImageView.setImageResource(R.drawable.plane_background1);
        Pattern pattern = Pattern.compile("^(.*?) <-> (.*?) \\((.*?)\\)\\.\nВылет туда: (.*?),\nВылет обратно: (.*?)$");
        Matcher matcher = pattern.matcher(ticketInfo);
        if (matcher.matches()) {
            String originAirportCode = matcher.group(1);
            String destinationAirportCode = matcher.group(2);
            String airlineName = matcher.group(3);
            String departureAt = matcher.group(4);
            String returnAt = matcher.group(5);

            returnDateLine.setText(returnAt);
            departureDateLine.setText(departureAt);
            returnLine.setText(destinationAirportCode+" - "+originAirportCode+" ("+airlineName+")");
            departureLine.setText(originAirportCode+" - "+destinationAirportCode+" ("+airlineName+")");
        } else {
            Log.d("Invalid ticketInfo format", ticketInfo);
        }
    }
    private void setHotelDisplay(String hotelInfo){
            Pattern pattern = Pattern.compile("^(.*?): (.*), за (\\d+\\.\\d+) руб\\.\\s+Ссылка на Hotellook: (https://.*?)\\s+image: (https://.*?)$");
            Matcher matcher = pattern.matcher(hotelInfo);
            if (matcher.matches()) {
                hotelLinkButton.setVisibility(View.VISIBLE);
                String hotelName = matcher.group(1);
                String address = matcher.group(2);
                double price = Double.parseDouble(matcher.group(3));
                hotelLink = matcher.group(4);
                String imageUrl = matcher.group(5);
                ImageView hotelImageView = findViewById(R.id.hotelImageView);
                Picasso.get().load(imageUrl).into(hotelImageView);
                TextView hotelNameTextView = findViewById(R.id.hotelNameTextView);
                hotelNameTextView.setText(hotelName);
                TextView hotelAddressTextView = findViewById(R.id.hotelAddressTextView);
                hotelAddressTextView.setText(address);
                TextView hotelPriceTextView = findViewById(R.id.hotelPriceTextView);
                hotelPriceTextView.setText(price +" руб.");
            } else {
                Log.d("Invalid hotelInfo format", hotelInfo);
            }
        hotelLinkButton = findViewById(R.id.hotelLinkButton);
        hotelLinkButton.setOnClickListener(view -> {
            if(hotelLink!=null){
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(hotelLink));
                startActivity(browserIntent);
            } else {
                Toast.makeText(LaunchActivity.this, "URL not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private boolean isTableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
        boolean tableExists = cursor.getCount() > 0;
        cursor.close();
        return tableExists;
    }
    private JSONArray loadJSONFromAsset(Context context, String filename) throws IOException, JSONException {
        InputStream inputStream = context.getAssets().open(filename);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();
        return new JSONArray(stringBuilder.toString());
    }
    public void showDatePickerDialog(View view, final String targetDateVariable) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.CustomDatePickerDialogTheme, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                int userId = selectedTripId;
                SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
                ContentValues values = new ContentValues();
                String formattedDay = String.format("%02d", day);
                String formattedMonth = String.format("%02d", month + 1);
                String selectedDate = year + "-" + formattedMonth + "-" + formattedDay;
                if ("returnDate".equals(targetDateVariable)) {
                    returnDate = selectedDate;
                    returnDateTextView.setText(selectedDate);
                    values.put("returnDate", returnDate);
                } else if ("departureDate".equals(targetDateVariable)) {
                    departureDate = selectedDate;
                    departureDateTextView.setText(selectedDate);
                    values.put("departureDate", departureDate);
                }
                int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
                if (affectedRows > 0) {
                    Log.d("Data updated successfully.", "Data updated successfully.");
                } else {
                    Log.e("Error updating data:", "No data updated.");
                }
                db.close();
            }
        }, returnYear, returnMonth, returnDay);
        datePickerDialog.show();
    }
    public void startHotelApp(View view) {
        if(!targetLocationSelect.getText().toString().equals("") &&returnDate!=null&&departureDate!=null&&adultsCount!=0) {
            int userId = selectedTripId;
            SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
            ContentValues values = new ContentValues();
            Editable editable = adultCountEditText.getText();
            adultsCount = Integer.parseInt(String.valueOf(editable));
            values.put("adultsCount", adultsCount);
            values.put("targetLocation", String.valueOf(targetLocationSelect.getText()));
            values.put("departureLocation", String.valueOf(departureLocationSelect.getText()));
            int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
            if (affectedRows > 0) {
                Log.d("Data updated successfully.", "Data updated successfully.");
            } else {
                Log.e("Error updating data:", "No data updated.");
            }
            db.close();
            Intent intent = new Intent(this, HotelActivity.class);
            intent.putExtra("selectedTripId", userId);
            startActivity(intent);
        }
        else{
            Toast.makeText(this, "Выберите даты и точку назначения.", Toast.LENGTH_SHORT).show();
        }
    }
    public void startGuidesApp(View view) {
        if(!targetLocationSelect.getText().toString().equals("")&&adultsCount!=0) {
            Intent intent = new Intent(this, SputnikGuidesActivity.class);
            intent.putExtra("selectedTripId", selectedTripId);
            startActivity(intent);
        }
        else{
            Toast.makeText(this, "Выберите точку назначения.", Toast.LENGTH_SHORT).show();
        }
    }

    public void startLandmarksApp(View view) {
        if(!targetLocationSelect.getText().toString().equals("")&&adultsCount!=0) {
            Intent intent = new Intent(this, LandmarksActivity.class);
            intent.putExtra("selectedTripId", selectedTripId);
            intent.putExtra("englishTargetName",englishTargetName);
            startActivity(intent);
        }
        else{
            Toast.makeText(this, "Выберите точку назначения.", Toast.LENGTH_SHORT).show();
        }
    }

    public void startTicketApp(View view) {
        if(!departureLocationSelect.getText().toString().equals("") &&!targetLocationSelect.getText().toString().equals("") &&returnDate!=null&&departureDate!=null&&adultsCount!=0) {
            int userId = selectedTripId;
            SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
            ContentValues values = new ContentValues();
            Editable editable = adultCountEditText.getText();
            adultsCount = Integer.parseInt(String.valueOf(editable));
            values.put("adultsCount", adultsCount);
            values.put("targetLocation", targetLocationSelect.getText().toString());
            values.put("departureLocation", departureLocationSelect.getText().toString());
            values.put("transport", transport);

            int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
            if (affectedRows > 0) {
                Log.d("Data updated successfully.", "Data updated successfully.");
            } else {
                Log.e("Error updating data:", "No data updated.");
            }
            db.close();
            if (transport.equals("plane")) {
                Intent intent = new Intent(this, TicketActivity.class);
                intent.putExtra("selectedTripId", selectedTripId);
                startActivity(intent);
            } else {
                String selectedCity = targetLocationSelect.getText().toString();
                countryCode=getCountryCode(selectedCity);
                Log.d("selectedCity",selectedCity);
                Log.d("country code", countryCode);
                if (Objects.equals(countryCode, "RU") || Objects.equals(countryCode, "KZ") || Objects.equals(countryCode, "AZ") || Objects.equals(countryCode, "AM") || Objects.equals(countryCode, "BY") || Objects.equals(countryCode, "KG") || Objects.equals(countryCode, "TM") || Objects.equals(countryCode, "UZ") || Objects.equals(countryCode, "TJ")) {
                    Intent intent = new Intent(this, TrainTicketActivity.class);
                    intent.putExtra("selectedTripId", selectedTripId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Для путешествий за пределы СНГ, пожалуйста, поищите билеты на самолет.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else{
            Toast.makeText(this, "Выберите точку отправления и назначения.", Toast.LENGTH_SHORT).show();
        }
    }
    public class FetchStatusTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String urlString = urls[0];
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    InputStream in = urlConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    return result.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("FetchStatusTask", "Error fetching data", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            progressBar3.setVisibility(View.INVISIBLE);
            if (response != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    Log.d("visa", String.valueOf(jsonResponse));
                    String status = jsonResponse.getString("status");
                    Log.d("Status", status);
                    String translatedStatus;
                    switch(status){
                        case "VR":
                            translatedStatus="нужна";
                            break;
                        case "CB":
                            translatedStatus="въезд запрещен";
                            break;
                        case "VOA":
                            translatedStatus="оформляется при въезде в страну";
                            break;
                        case "VF":
                            translatedStatus="не нужна";
                            break;
                        default:
                            translatedStatus="уточняйте в посольстве";
                    }
                    visaTextView.setText("Виза: "+translatedStatus);
                } catch (JSONException e) {
                    Log.e("FetchStatusTask", "Error parsing JSON", e);
                }
            } else {
                Log.e("FetchStatusTask", "Response is null");
            }
        }
    }
    private static class LoadImageTask extends AsyncTask<Void, Void, Bitmap> {
        private final Context context;
        private final String imageUrl;
        ImageButton showImageButton;

        LoadImageTask(Context context, String imageUrl, ImageButton showImageButton) {
            this.context = context;
            this.imageUrl = imageUrl;
            this.showImageButton=showImageButton;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                return Picasso.get().load(imageUrl).get();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                showImageButton.setVisibility(View.VISIBLE);
                showImageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showImagePopup(context, result);
                    }
                });
            }
        }
    }
    private static void showImagePopup(Context context, Bitmap bitmap) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_image, null);
        ImageView imageView = popupView.findViewById(R.id.showCityImageView);
        imageView.setImageBitmap(bitmap);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(popupView);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private class SendIataRequestTask extends AsyncTask<String, Void, String> {
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
                    photoTargetIata= destination.getString("iata").trim();
                    englishTargetName= destination.getString("name").trim();
                    Log.d("Iata Code", "Target Iata: " + photoTargetIata);
                    String imageUrl = "https://photo.hotellook.com/static/cities/960x720/"+photoTargetIata.toUpperCase()+".jpg";
                    Log.d("imageUrl", imageUrl);
                    LoadImageTask loadImageTask = new LoadImageTask(LaunchActivity.this, imageUrl, showImageButton);
                    loadImageTask.execute();
                } catch (JSONException e) {
                    Log.e("JSON parsing error: ", e.getMessage());
                }
            } else {
            }
        }
    }
    private class LoadDataAsyncTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                JSONArray citiesArray = loadJSONFromAsset(LaunchActivity.this, "cities_sorted.json");
                List<String> autoCompleteOptions = new ArrayList<>();
                for (int i = 0; i < citiesArray.length(); i++) {
                    JSONObject city = citiesArray.getJSONObject(i);
                    String cityName = city.getString("name");
                    autoCompleteOptions.add(cityName);
                }
                return autoCompleteOptions;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<String> autoCompleteOptions) {
            departureLocationSelect = findViewById(R.id.departureLocationSelect);
            targetLocationSelect = findViewById(R.id.targetLocationSelect);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(LaunchActivity.this, R.layout.custom_dropdown_item, R.id.customTextView, autoCompleteOptions);
            departureLocationSelect.setAdapter(adapter);
            targetLocationSelect.setAdapter(adapter);

            departureLocationSelect.setOnClickListener(v -> departureLocationSelect.showDropDown());
            targetLocationSelect.setOnClickListener(v -> targetLocationSelect.showDropDown());
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);
        }
    }
    private static class TaxiAsyncTask extends AsyncTask<Void, Void, String> {
        private final double startLat;
        private final double startLon;
        private final double endLat;
        private final double endLon;
        private final Context context;
        private final String yandexTaxiUrl;
        private final String hotelName;
        private final String locationNameForTaxiOrder;
        private final WebView webView;
        TextView transferInfoTextView;
        private int selectedTripId;
        private String yandexTaxiApiKey;
        private String yandexClid;
        public TaxiAsyncTask(Context context, WebView webView, double startLat, double startLon, double endLat, double endLon, String yandexTaxiUrl, String hotelName, String locationNameForTaxiOrder, TextView transferInfoTextView, int selectedTripId, String yandexTaxiApiKey, String yandexClid) {
            this.context = context;
            this.startLat = startLat;
            this.startLon = startLon;
            this.endLat = endLat;
            this.endLon = endLon;
            this.yandexTaxiUrl=yandexTaxiUrl;
            this.hotelName=hotelName;
            this.locationNameForTaxiOrder=locationNameForTaxiOrder;
            this.webView = webView;
            this.transferInfoTextView = transferInfoTextView;
            this.selectedTripId=selectedTripId;
            this.yandexTaxiApiKey=yandexTaxiApiKey;
            this.yandexClid=yandexClid;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String apiUrl = "https://taxi-routeinfo.taxi.yandex.net/taxi_info?clid="+yandexClid+"&apikey="+yandexTaxiApiKey+"&rll="
                    + startLon + "," + startLat + "~" + endLon + "," + endLat + "&class=econom&lang=ru";
            Log.d("price request", apiUrl);
            return getExpectedPrice(apiUrl, yandexTaxiApiKey);
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d("Response", result);
                Log.d("yandex url", yandexTaxiUrl);
                webView.loadUrl(yandexTaxiUrl);
                String transferInfo = locationNameForTaxiOrder + " → " +hotelName+ ", "+result+"\nСсылка YandexGo: " + yandexTaxiUrl;
                writeTransferDataToDB(context, transferInfo, locationNameForTaxiOrder, selectedTripId);
            } else {
                Log.e("Error", "Failed to get expected price");
                Toast.makeText(context, "В этом регионе Яндекс.Такси не работает, попробуйте найти локальный трансфер.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public class ToursLaunchAdapter extends RecyclerView.Adapter<LaunchActivity.ToursLaunchAdapter.ToursViewHolder> {

        private final List<ArrayList<Object>> toursDataList;

        public ToursLaunchAdapter(List<ArrayList<Object>> toursDataList) {
            this.toursDataList = toursDataList;
        }
        public void clearData() {
            toursDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LaunchActivity.ToursLaunchAdapter.ToursViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.launch_tour_card, parent, false);
            return new LaunchActivity.ToursLaunchAdapter.ToursViewHolder(view);
        }
        private void openTourLink(String link) {
            if (link != null && !link.isEmpty()) {
                Uri uri = Uri.parse(link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        }

        @SuppressLint("Range")
        @Override
        public void onBindViewHolder(@NonNull LaunchActivity.ToursLaunchAdapter.ToursViewHolder holder, int position) {
            ArrayList<Object> toursData = toursDataList.get(position);
            String title = toursData.get(0).toString();
            String link = toursData.get(4).toString();
            String mainPhotoUrl=toursData.get(3).toString();
            String price = toursData.get(2).toString();
            String duration = toursData.get(1).toString();

            updateImage(holder.launchTourImageView, mainPhotoUrl);
            holder.launchTourName.setText(title);
            holder.launchTourDuration.setText(duration);
            holder.launchTourPrice.setText(price);

            holder.linkButton.setOnClickListener(v -> {
                openTourLink(link);
            });
        }
        @Override
        public int getItemCount() {
            return toursDataList.size();
        }
        private void updateImage(ImageView imageView, String imageUrl) {
            Picasso.get().load(imageUrl).into(imageView);
        }
        public class ToursViewHolder extends RecyclerView.ViewHolder {

            public ImageView launchTourImageView;
            public TextView launchTourName;
            public TextView launchTourPrice;
            public TextView launchTourDuration;
            public ImageButton linkButton;

            public ToursViewHolder(View itemView) {
                super(itemView);
                launchTourImageView = itemView.findViewById(R.id.launchTourImageView);
                launchTourName = itemView.findViewById(R.id.launchTourName);
                launchTourPrice = itemView.findViewById(R.id.launchTourPrice);
                launchTourDuration = itemView.findViewById(R.id.launchTourDuration);
                linkButton = itemView.findViewById(R.id.linkButton);
            }
        }
    }
    public class LandmarksLaunchAdapter extends RecyclerView.Adapter<LaunchActivity.LandmarksLaunchAdapter.LandmarksViewHolder> {

        private final List<ArrayList<Object>> landmarksDataList;

        public LandmarksLaunchAdapter(List<ArrayList<Object>> landmarksDataList) {
            this.landmarksDataList = landmarksDataList;
        }

        public void clearData() {
            landmarksDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LaunchActivity.LandmarksLaunchAdapter.LandmarksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.launch_landmark_card, parent, false);
            return new LaunchActivity.LandmarksLaunchAdapter.LandmarksViewHolder(view);
        }

        @SuppressLint("Range")
        @Override
        public void onBindViewHolder(@NonNull LaunchActivity.LandmarksLaunchAdapter.LandmarksViewHolder holder, int position) {
            ArrayList<Object> landmarkData = landmarksDataList.get(position);
            String title = landmarkData.get(0).toString();
            String mainPhotoUrl = landmarkData.get(1).toString();
            String address = landmarkData.get(2).toString();

            updateImage(holder.launchLandmarkImage, mainPhotoUrl);
            holder.launchLandmarkTitle.setText(title);
            holder.launchLandmarkAddress.setText(address);

            holder.launchLandmarkDeleteButton.setOnClickListener(v -> {
                int userId = selectedTripId;
                SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
                Cursor cursor = db.rawQuery("SELECT landmarksList FROM jjtrips1 WHERE userId=?", new String[]{String.valueOf(userId)});
                if (cursor != null && cursor.moveToFirst()) {
                    String existingLandmarksInfo = cursor.getString(cursor.getColumnIndex("landmarksList"));
                    if (existingLandmarksInfo != null && existingLandmarksInfo.contains(title)) {
                        existingLandmarksInfo = existingLandmarksInfo.replaceAll("(?m)^.*" + title + ".*\\n", "");
                        ContentValues values = new ContentValues();
                        values.put("landmarksList", existingLandmarksInfo);
                        int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
                        if (affectedRows > 0) {
                            Log.d("Data updated successfully.", "Data updated successfully.");
                        } else {
                            Log.e("Error updating data:", "No data updated.");
                        }
                    } else {
                        Log.d("Landmark not found.", "Landmark not found in landmarksList.");
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
                readAndLogDataFromSQLite(selectedTripId);
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
            public ImageView launchLandmarkImage;
            public TextView launchLandmarkTitle;
            public TextView launchLandmarkAddress;
            public Button launchLandmarkDeleteButton;

            public LandmarksViewHolder(View itemView) {
                super(itemView);
                launchLandmarkImage = itemView.findViewById(R.id.launchLandmarkImage);
                launchLandmarkTitle = itemView.findViewById(R.id.launchLandmarkTitle);
                launchLandmarkAddress = itemView.findViewById(R.id.launchLandmarkAddress);
                launchLandmarkDeleteButton = itemView.findViewById(R.id.launchLandmarkDeleteButton);
            }
        }
    }

    public class EventsLaunchAdapter extends RecyclerView.Adapter<LaunchActivity.EventsLaunchAdapter.EventsViewHolder> {

        private final List<ArrayList<Object>> eventsDataList;

        public EventsLaunchAdapter(List<ArrayList<Object>> eventsDataList) {
            this.eventsDataList = eventsDataList;
        }

        public void clearData() {
            eventsDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LaunchActivity.EventsLaunchAdapter.EventsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.launch_event_card, parent, false);
            return new LaunchActivity.EventsLaunchAdapter.EventsViewHolder(view);
        }

        @SuppressLint("Range")
        @Override
        public void onBindViewHolder(@NonNull LaunchActivity.EventsLaunchAdapter.EventsViewHolder holder, int position) {
            ArrayList<Object> eventData = eventsDataList.get(position);
            String category = eventData.get(0).toString();
            String title = eventData.get(1).toString();
            String startAt = eventData.get(2).toString();
            String address = eventData.get(3).toString();

            switch(category){
                case "concerts":
                    holder.launchEventImageView.setImageResource(R.drawable.music);
                    break;
                case "academic":
                    holder.launchEventImageView.setImageResource(R.drawable.academic);
                    break;
                case "school-holidays":
                    holder.launchEventImageView.setImageResource(R.drawable.school_holidays);
                    break;
                case "observances":
                    holder.launchEventImageView.setImageResource(R.drawable.observances);
                    break;
                case "politics":
                    holder.launchEventImageView.setImageResource(R.drawable.politics);
                    break;
                case "conferences":
                    holder.launchEventImageView.setImageResource(R.drawable.conferences);
                    break;
                case "expos":
                    holder.launchEventImageView.setImageResource(R.drawable.expos);
                    break;
                case "festivals":
                    holder.launchEventImageView.setImageResource(R.drawable.festivals);
                    break;
                case "performing-arts":
                    holder.launchEventImageView.setImageResource(R.drawable.performing_arts);
                    break;
                case "sports":
                    holder.launchEventImageView.setImageResource(R.drawable.sports);
                    break;
                case "community":
                    holder.launchEventImageView.setImageResource(R.drawable.community);
                    break;
                case "daylight-savings":
                    holder.launchEventImageView.setImageResource(R.drawable.daylight_savings);
                    break;
                case "airport-delays":
                    holder.launchEventImageView.setImageResource(R.drawable.airport_delays);
                    break;
                case "severe-weather":
                    holder.launchEventImageView.setImageResource(R.drawable.severe_weaither);
                    break;
                case "disasters":
                    holder.launchEventImageView.setImageResource(R.drawable.disasters);
                    break;
                case "terror":
                    holder.launchEventImageView.setImageResource(R.drawable.terror);
                    break;
                case "health-warnings":
                    holder.launchEventImageView.setImageResource(R.drawable.health_warnings);
                    break;
                case "public-holidays":
                    holder.launchEventImageView.setImageResource(R.drawable.public_holidays);
                    break;
                default:
                    holder.launchEventImageView.setImageResource(R.drawable.festivals);
                    break;
            }

            holder.launchEventTitle.setText(title);
            holder.launchEventAddress.setText(address);
            holder.launchEventTime.setText("Начало: "+startAt);
        }

        @Override
        public int getItemCount() {
            return eventsDataList.size();
        }

        public class EventsViewHolder extends RecyclerView.ViewHolder {
            public TextView launchEventTitle;
            public TextView launchEventAddress;
            public TextView launchEventTime;
            public ImageView launchEventImageView;

            public EventsViewHolder(View itemView) {
                super(itemView);
                launchEventTitle = itemView.findViewById(R.id.launchEventTitle);
                launchEventAddress = itemView.findViewById(R.id.launchEventAddress);
                launchEventTime = itemView.findViewById(R.id.launchEventTime);
                launchEventImageView = itemView.findViewById(R.id.launchEventImageView);
            }
        }
    }
}
