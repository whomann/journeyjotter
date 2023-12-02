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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrainTicketActivity extends AppCompatActivity {

    private int selectedTripId;TextView arrowTextView; Boolean isReturnBought = false; Button buyReturnTicket; RecyclerView recyclerView; ArrayList<ArrayList<Object>> trainsList = new ArrayList<>(); Spinner departureSpinner, targetSpinner; ImageButton searchTrainTicketButton;  private SQLiteDatabase db; private String returnDate, departureDate, targetLocation, departureLocation, departureLocationCode, targetLocationCode, ticketInfo; private int adultsCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_ticket);
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        recyclerView=findViewById(R.id.recyclerViewTicket);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        arrowTextView=findViewById(R.id.arrowTextView);
        arrowTextView.setText("\u276F");
        buyReturnTicket=findViewById(R.id.buyReturnTicket);
        buyReturnTicket.setVisibility(View.GONE);

        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        readAndLogDataFromSQLite(selectedTripId);

        searchTrainTicketButton=findViewById(R.id.searchTrainTicketButton);
        searchTrainTicketButton.setEnabled(false);
        departureSpinner = findViewById(R.id.departureStationSelect);
        targetSpinner = findViewById(R.id.targetStationSelect);
        buyReturnTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                arrowTextView.setText("\u276E");
                trainsList.clear();
                TrainTicketAdapter ticketAdapter = new TrainTicketAdapter(new ArrayList<>());
                recyclerView.setAdapter(ticketAdapter);
                ticketAdapter.clearData();

                String targetOption = targetSpinner.getSelectedItem().toString();
                String targetStationId = extractStationId(targetOption);
                String departureOption = departureSpinner.getSelectedItem().toString();
                String departureStationId = extractStationId(departureOption);

                String callbackName = "handleResponse" + System.currentTimeMillis();
                String apiUrl = "https://cors.eu.org/https://suggest.travelpayouts.com/search?service=tutu_trains&term=" + targetStationId + "&term2=" + departureStationId + "&callback=" + callbackName;
                Log.d("tutu url", apiUrl);
                isReturnBought = true;
                new SendTrainRequestTask().execute(apiUrl);
            }
        });
        searchTrainTicketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                arrowTextView.setText("\u276F");
                isReturnBought = false;
                trainsList.clear();
                String targetOption = targetSpinner.getSelectedItem().toString();
                String targetStationId = extractStationId(targetOption);
                String departureOption = departureSpinner.getSelectedItem().toString();
                String departureStationId = extractStationId(departureOption);
                String callbackName = "handleResponse" + System.currentTimeMillis();
                String apiUrl = "https://cors.eu.org/https://suggest.travelpayouts.com/search?service=tutu_trains&term=" + departureStationId + "&term2=" + targetStationId + "&callback=" + callbackName;
                Log.d("tutu url", apiUrl);
                new SendTrainRequestTask().execute(apiUrl);
            }
        });
    }
    private String extractStationId(String selectedOption) {
        int startIndex = selectedOption.indexOf("(") + 1;
        int endIndex = selectedOption.indexOf(")");
        return selectedOption.substring(startIndex, endIndex);
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
                    targetLocation = cursor.getString(cursor.getColumnIndex("targetLocation"));
                    departureLocation = cursor.getString(cursor.getColumnIndex("departureLocation"));
                    ticketInfo = cursor.getString(cursor.getColumnIndex("ticketInfo"));

                    Log.d("SQLite Data", "User ID: " + userID);
                    Log.d("SQLite Data", "Departure Date: " + departureDate);
                    Log.d("SQLite Data", "Return Date: " + returnDate);
                    Log.d("SQLite Data", "Adults Count: " + adultsCount);
                    Log.d("SQLite Data", "Departure location: "+departureLocation);
                    Log.d("SQLite Data", "Target Location: " + targetLocation);
                    Log.d("SQLite Data", "Ticket info: " + ticketInfo);

                    String requestUrl = "https://www.travelpayouts.com/widgets_suggest_params?q=Из%20" +
                            departureLocation + "%20в%20" + targetLocation;
                    Log.d("loc request", requestUrl);
                    new SendRequestTask().execute(requestUrl);

                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
    private class SendTrainRequestTask extends AsyncTask<String, Void, String> {
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
            String formattedDepartureDate ="";
            String formattedReturnDate ="";
            if (response != null) {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date departureDateParsed = inputFormat.parse(departureDate);
                    Date returnDateParsed = inputFormat.parse(returnDate);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    formattedDepartureDate = outputFormat.format(departureDateParsed);
                    formattedReturnDate = outputFormat.format(returnDateParsed);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                int startIndex = response.indexOf('{');
                int endIndex = response.lastIndexOf('}');
                if (startIndex != -1 && endIndex != -1) {
                    String jsonResponseString = response.substring(startIndex, endIndex + 1);
                    try {
                        JSONObject jsonResponse = new JSONObject(jsonResponseString);
                        Log.d("Location response", jsonResponseString);
                        JSONArray trips = jsonResponse.getJSONArray("trips");
                        ArrayList<ArrayList<Object>> trainsList = new ArrayList<>();
                        for (int i = 0; i < trips.length(); i++) {
                            JSONObject trip = trips.getJSONObject(i);
                            String departureTime = trip.getString("departureTime");
                            int timeInSeconds = trip.getInt("travelTimeInSeconds");
                            String name = trip.getString("name");
                            String trainNumber = trip.getString("trainNumber");
                            String numberForUrl = trip.getString("numberForUrl");
                            String departureStation = trip.getString("departureStation");
                            String arrivalStation = trip.getString("arrivalStation");
                            String formattedTravelTime = formatTimeInSeconds(timeInSeconds);
                            String link, arriveAt, departureAt;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            if (isReturnBought) {
                                departureAt = returnDate + " " + departureTime;
                                link = "https://www.tutu.ru/poezda/wizard/seats/?departure_st=" + departureStation + "&arrival_st=" + arrivalStation + "&dep_st=" + departureStation + "&arr_st=" + arrivalStation + "&tn=" + numberForUrl + "&date=" + formattedReturnDate;
                            } else {
                                departureAt = departureDate + " " + departureTime;
                                link = "https://www.tutu.ru/poezda/wizard/seats/?departure_st=" + departureStation + "&arrival_st=" + arrivalStation + "&dep_st=" + departureStation + "&arr_st=" + arrivalStation + "&tn=" + numberForUrl + "&date=" + formattedDepartureDate;
                            }
                            Date departureDateTime = dateFormat.parse(departureAt);
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(departureDateTime);
                            calendar.add(Calendar.SECOND, timeInSeconds);
                            Date arriveDateTime = calendar.getTime();
                            arriveAt = dateFormat.format(arriveDateTime);
                            ArrayList<Object> trainInfo = new ArrayList<>();

                            trainInfo.add(departureAt);
                            trainInfo.add(arriveAt);
                            trainInfo.add(formattedTravelTime);
                            trainInfo.add(name);
                            trainInfo.add(trainNumber);
                            trainInfo.add(departureStation);
                            trainInfo.add(arrivalStation);
                            trainInfo.add(link);

                            ArrayList<String> pricesList = new ArrayList<>();
                            JSONArray categories = trip.getJSONArray("categories");
                            for (int j = 0; j < categories.length(); j++) {
                                JSONObject category = categories.getJSONObject(j);
                                String priceName = category.getString("type");
                                int price = category.getInt("price");

                                String priceString = priceName + ": " + price;
                                pricesList.add(priceString);
                            }
                            trainInfo.add(pricesList);
                            trainsList.add(trainInfo);
                            RecyclerView recyclerView = findViewById(R.id.recyclerViewTicket);
                            TrainTicketAdapter ticketAdapter = new TrainTicketAdapter(trainsList);
                            recyclerView.setAdapter(ticketAdapter);
                        }
                        Log.d("train list", String.valueOf(trainsList));
                    } catch (JSONException e) {
                        Log.e("JSON parsing error: ", e.getMessage());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Toast.makeText(TrainTicketActivity.this, "Билеты не найдены, попробуйте другие станции.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("No data available", "Error retrieving data");
                Toast.makeText(TrainTicketActivity.this, "Билеты не найдены, попробуйте другие станции.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private String formatTimeInSeconds(long timeInSeconds) {
        long days = timeInSeconds / (24 * 3600);
        long hours = (timeInSeconds % (24 * 3600)) / 3600;
        long minutes = (timeInSeconds % 3600) / 60;

        StringBuilder formattedTime = new StringBuilder();

        if (days > 0) {
            formattedTime.append(days).append(" д. ");
        }

        if (hours > 0) {
            formattedTime.append(hours).append(" ч. ");
        }

        if (minutes > 0) {
            formattedTime.append(minutes).append(" мин.");
        }

        return formattedTime.toString();
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
                    Log.d("Location response", response);
                    JSONObject origin = jsonResponse.getJSONObject("origin");
                    JSONObject destination = jsonResponse.getJSONObject("destination");

                    departureLocationCode = origin.getString("iata");
                    targetLocationCode = destination.getString("iata");

                    Log.d("Iata Codes", "Departure Iata: " + departureLocationCode);
                    Log.d("Iata Codes", "Target Iata: " + targetLocationCode);

                    departureSpinner.setAdapter(null);
                    targetSpinner.setAdapter(null);
                    String stationsData = loadJSONFromAsset("stations_test.json");

                    if (stationsData != null) {
                        JSONArray stationsArray = new JSONArray(stationsData);
                        ArrayAdapter<String> departureAdapter = new ArrayAdapter<>(TrainTicketActivity.this, android.R.layout.simple_spinner_item);
                        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(TrainTicketActivity.this, android.R.layout.simple_spinner_item);
                        departureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        for (int i = 0; i < stationsArray.length(); i++) {
                            JSONObject station = stationsArray.getJSONObject(i);
                            String iataCode = station.getString("iata_code").trim().toUpperCase();
                            String option = station.getString("station_name") + " (" + station.getString("station_id") + ")" +
                                    " [" + iataCode + "]";
                            if (iataCode.equals(departureLocationCode)) {
                                departureAdapter.add(option);
                            }
                            if (iataCode.equals(targetLocationCode)) {
                                targetAdapter.add(option);
                            }
                        }
                        departureSpinner.setAdapter(departureAdapter);
                        targetSpinner.setAdapter(targetAdapter);
                        searchTrainTicketButton.setEnabled(true);
                    }
                } catch (JSONException e) {
                    Log.e("JSON parsing error: ", e.getMessage());
                }
            } else {
                Log.e("No data available", "Error retrieving data");
            }
        }
        private String loadJSONFromAsset(String fileName) {
            String json = null;
            try {
                InputStream is = getAssets().open(fileName);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer, "UTF-8");

            } catch (IOException e) {
                Log.e("Error reading JSON", e.getMessage());
            }
            return json;
        }
    }
    public class TrainTicketAdapter extends RecyclerView.Adapter<TrainTicketActivity.TrainTicketAdapter.TrainTicketViewHolder> {

        private List<ArrayList<Object>> ticketDataList;

        public TrainTicketAdapter(List<ArrayList<Object>> ticketDataList) {
            this.ticketDataList = ticketDataList;
        }
        public void clearData() {
            ticketDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TrainTicketAdapter.TrainTicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.train_ticket_card, parent, false);
            return new TrainTicketActivity.TrainTicketAdapter.TrainTicketViewHolder(view);
        }
        private void openTrainTicketLink(String link) {
            if (link != null && !link.isEmpty()) {
                Uri uri = Uri.parse(link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        }

        @SuppressLint("Range")
        @Override
        public void onBindViewHolder(@NonNull TrainTicketActivity.TrainTicketAdapter.TrainTicketViewHolder holder, int position) {
            ArrayList<Object> ticketData = ticketDataList.get(position);
            String departureAt = ticketData.get(0).toString();
            String arriveAt = ticketData.get(1).toString();
            String duration = ticketData.get(2).toString();
            String trainName = ticketData.get(3).toString();
            String trainNumber = ticketData.get(4).toString();
            String departureStation = ticketData.get(5).toString();
            String arrivalStation = ticketData.get(6).toString();
            String link = ticketData.get(7).toString();
            String pricesString=ticketData.get(8).toString();
            String formattedPrices = convertPricesString(pricesString);
            String departureStationName = getStationName(departureStation);
            String arrivalStationName = getStationName(arrivalStation);
            String formattedDepartureAt = formatDate(departureAt).toLowerCase();
            String formattedArriveAt = formatDate(arriveAt).toLowerCase();

            String adultsString;
            if(adultsCount>1){
                adultsString = "за "+adultsCount+" человека:";
            }
            else{
                adultsString="";
            }

            holder.ticketNumberTV.setText(trainNumber+" "+trainName);
            holder.priceTV.setText(formattedPrices);
            holder.timeInTravelTV.setText(duration);
            holder.directionTV.setText(departureStationName+"   ➔   "+arrivalStationName);
            holder.departureDateTV.setText(formattedDepartureAt);
            holder.arrivalDateTV.setText(formattedArriveAt);
            holder.adultsTV.setText(adultsString);

            holder.buyTrainTicketButton.setOnClickListener(v -> {
                openTrainTicketLink(link);
            });
            holder.boughtTrainTicketButton.setOnClickListener(v -> {
                String charForTicket="";
                if(isReturnBought){
                    charForTicket = " обратно";
                }
                else{
                    charForTicket = " туда";
                }
                String updatedTrainTicketInfo = "Билет"+charForTicket+": "+trainNumber+"\n"+departureStationName+" - "+arrivalStationName+" ("+formattedDepartureAt +")";
                int userId = selectedTripId;
                SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
                ContentValues values = new ContentValues();

                if(isReturnBought){
                    Cursor cursor = db.rawQuery("SELECT ticketInfo FROM jjtrips1 WHERE userId=?", new String[]{String.valueOf(userId)});
                    if (cursor != null && cursor.moveToFirst()) {
                        String existingTicketInfo = cursor.getString(cursor.getColumnIndex("ticketInfo"));
                        updatedTrainTicketInfo = existingTicketInfo + "\n\n" + updatedTrainTicketInfo;
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    buyReturnTicket.setVisibility(View.GONE);
                }
                else {
                    buyReturnTicket.setVisibility(View.VISIBLE);
                    values.put("originIATA", departureStationName);
                    values.put("targetIATA", arrivalStationName);
                }
                values.put("ticketInfo", updatedTrainTicketInfo);
                int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
                if (affectedRows > 0) {
                    Log.d("Data updated successfully.", "Data updated successfully.");
                } else {
                    Log.e("Error updating data:", "No data updated.");
                }
                db.close();
            });
        }
        private String formatDate(String dateTimeString) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("ru", "RU"));
                Date date = inputFormat.parse(dateTimeString);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, HH:mm", new Locale("ru", "RU"));
                return outputFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
                return "Invalid Date";
            }
        }
        private String getStationName(String stationId) {
            try {
                InputStream is = getAssets().open("stations_test.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String jsonString = new String(buffer, StandardCharsets.UTF_8);
                JSONArray stationsArray = new JSONArray(jsonString);
                for (int i = 0; i < stationsArray.length(); i++) {
                    JSONObject station = stationsArray.getJSONObject(i);
                    String stationIdFromJson = station.getString("station_id");
                    if (stationIdFromJson.equals(stationId)) {
                        return station.getString("station_name");
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return "Station Not Found";
        }
        @Override
        public int getItemCount() {
            return ticketDataList.size();
        }
        private String convertPricesString(String pricesString) {
            String[] priceEntries = pricesString.replace("[", "").replace("]", "").split(",");
            List<String> formattedPrices = new ArrayList<>();

            for (String entry : priceEntries) {
                String[] parts = entry.trim().split(":");
                if (parts.length == 2) {
                    String priceName = parts[0].trim();
                    switch (priceName) {
                        case "plazcard":
                            priceName = "Плацкарт";
                            break;
                        case "coupe":
                            priceName = "Купе";
                            break;
                        case "lux":
                            priceName = "Люкс";
                            break;
                        case "sedentary":
                            priceName = "Сидячий";
                            break;
                        case "soft":
                            priceName = "СВ";
                            break;
                    }
                    String priceValue = parts[1].trim();
                    int priceInt= Integer.parseInt(priceValue);
                    priceInt=priceInt*adultsCount;
                    String formattedPrice = String.format("%s: %s ₽", priceName, priceInt);
                    formattedPrices.add(formattedPrice);
                }
            }
            return String.join("\n", formattedPrices);
        }

        public class TrainTicketViewHolder extends RecyclerView.ViewHolder {

            public TextView ticketNumberTV;
            public TextView priceTV;
            public TextView timeInTravelTV;
            public TextView directionTV;
            public TextView arrivalDateTV;
            public TextView departureDateTV;
            public TextView adultsTV;
            public Button buyTrainTicketButton;
            public Button boughtTrainTicketButton;

            public TrainTicketViewHolder(View itemView) {
                super(itemView);
                ticketNumberTV = itemView.findViewById(R.id.trainNumberTV);
                priceTV = itemView.findViewById(R.id.trainPriceTV);
                timeInTravelTV = itemView.findViewById(R.id.trainTimeInTravelTV);
                directionTV = itemView.findViewById(R.id.trainDirectionTV);
                arrivalDateTV = itemView.findViewById(R.id.trainArrivalDateTV);
                departureDateTV = itemView.findViewById(R.id.trainDepartureDateTV);
                adultsTV = itemView.findViewById(R.id.trainAdultsTV);
                buyTrainTicketButton = itemView.findViewById(R.id.trainBuyTicketButton);
                boughtTrainTicketButton = itemView.findViewById(R.id.trainBoughtTicketButton);
            }
        }
    }
}