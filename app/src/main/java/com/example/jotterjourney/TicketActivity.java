package com.example.jotterjourney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class TicketActivity extends AppCompatActivity {
    ImageView imageView15; private String aviasalesApiKey; private int selectedTripId; ProgressBar progressBar; CheckBox noTransfersCheckBox; RecyclerView recyclerView; Spinner sortBySpinner; private SQLiteDatabase db; private String ticketInfo,sortBy, departureLocation,targetLocation,returnDate,departureDate,targetLocationCode, departureLocationCode; private int adultsCount; ArrayList<ArrayList<Object>> ticketsList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);
        Properties properties = new Properties();
        try (InputStream input = getResources().getAssets().open("secrets.properties")) {
            properties.load(input);
            aviasalesApiKey = properties.getProperty("hotellookApiKey");
        } catch (IOException e) {
            e.printStackTrace();
        }
        selectedTripId = getIntent().getIntExtra("selectedTripId", 1);
        getWindow().setStatusBarColor(Color.parseColor("#6f4d6e"));
        recyclerView=findViewById(R.id.recyclerViewGuides);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);
        readAndLogDataFromSQLite(selectedTripId);
        sortBySpinner=findViewById(R.id.ticketSortBy);
        imageView15=findViewById(R.id.imageView15);
        imageView15.setVisibility(View.VISIBLE);
        noTransfersCheckBox=findViewById(R.id.noTransfersCheckBox);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        progressBar=findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.INVISIBLE);
        findViewById(R.id.searchButtonTicket).setOnClickListener(v->{
            imageView15.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            TicketActivity.TicketAdapter ticketAdapter = new TicketActivity.TicketAdapter(new ArrayList<>());
            recyclerView.setAdapter(ticketAdapter);
            ticketAdapter.clearData();
            String requestUrl = "https://www.travelpayouts.com/widgets_suggest_params?q=Из%20" +
                    departureLocation + "%20в%20" + targetLocation;
            Log.d("loc request", requestUrl);

            new SendRequestTask().execute(requestUrl);
        });
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
            sortBy = sortBySpinner.getSelectedItem().toString();
            if(sortBy.equals("по цене")){
                sortBy="price";
            }
            else{
                sortBy="route";
            }
            if (response != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    Log.d("Location response",response);
                    JSONObject origin = jsonResponse.getJSONObject("origin");
                    JSONObject destination = jsonResponse.getJSONObject("destination");

                    String departureIata = origin.getString("iata");
                    String targetIata = destination.getString("iata");

                    departureLocationCode = departureIata;
                    targetLocationCode = targetIata;

                    Log.d("Iata Codes", "Departure Iata: " + departureIata);
                    Log.d("Iata Codes", "Target Iata: " + targetIata);
                    new FlightInfoAsyncTask().execute(departureLocationCode, targetLocationCode, departureDate, returnDate, sortBy);

                } catch (JSONException e) {
                    Log.e("JSON parsing error: ", e.getMessage());
                }
            } else {
                Log.e("No data available", "Error retrieving data");
                Toast.makeText(TicketActivity.this, "Локация не найдена.", Toast.LENGTH_SHORT).show();
            }
        }
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

                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    private class FlightInfoAsyncTask extends AsyncTask<String, Void, ArrayList<ArrayList<Object>>> {
        @Override
        protected ArrayList<ArrayList<Object>> doInBackground(String... params) {
            String departureLocationCode = params[0];
            String targetLocationCode = params[1];
            String departureDate = params[2];
            String returnDate = params[3];
            try {
                String apiUrl = "https://api.travelpayouts.com/aviasales/v3/prices_for_dates";
                String fullUrl = apiUrl + "?origin=" + departureLocationCode +
                        "&destination=" + targetLocationCode +
                        "&departure_at=" + departureDate +
                        "&return_at=" + returnDate +
                        "&unique=false&sorting=" + sortBy + "&direct=false&cy=rub&limit=1000&page=1&one_way=false&token="+aviasalesApiKey;
                Log.d("Request: ", fullUrl);

                URL url = new URL(fullUrl);
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

                return parseFlightData(responseStringBuilder.toString());
            } catch (IOException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            }
        }

        private ArrayList<ArrayList<Object>> parseFlightData(String jsonResponse) {
            try {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray dataArray = jsonObject.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject flight = dataArray.getJSONObject(i);

                    int transfers =  flight.getInt("transfers");
                    int returnTransfers =  flight.getInt("return_transfers");
                    if (noTransfersCheckBox.isChecked() && (transfers > 0 || returnTransfers > 0)) {
                        continue;
                    }
                    else {
                        String flightNumber = flight.getString("flight_number");
                        String link = flight.getString("link");
                        int price = flight.getInt("price");
                        String destinationAirport = flight.getString("destination_airport");
                        String originAirport = flight.getString("origin_airport");
                        String departureAt = flight.getString("departure_at");
                        String airline = flight.getString("airline");
                        String returnAt = flight.getString("return_at");
                        int duration = flight.getInt("duration");
                        int durationTo = flight.getInt("duration_to");
                        int durationBack = flight.getInt("duration_back");

                        ArrayList<Object> flightInfo = new ArrayList<>();
                        flightInfo.add(flightNumber);
                        flightInfo.add(departureAt);
                        flightInfo.add(originAirport);
                        flightInfo.add(returnAt);
                        flightInfo.add(destinationAirport);
                        flightInfo.add(airline);
                        flightInfo.add(price);
                        flightInfo.add(duration);
                        flightInfo.add(link);
                        flightInfo.add(durationTo);
                        flightInfo.add(durationBack);
                        flightInfo.add(transfers);
                        flightInfo.add(returnTransfers);

                        ticketsList.add(flightInfo);
                    }
                }

                return ticketsList;
            } catch (JSONException e) {
                Log.e("JSON parsing error: ", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<ArrayList<Object>> ticketsList) {
            if (ticketsList != null) {
                RecyclerView recyclerView = findViewById(R.id.recyclerViewGuides);
                imageView15.setVisibility(View.GONE);
                TicketAdapter ticketAdapter = new TicketAdapter(ticketsList);
                recyclerView.setAdapter(ticketAdapter);
                progressBar.setVisibility(View.GONE);
                Log.d("Response: ", String.valueOf(ticketsList));
                if(ticketsList.isEmpty()){
                    Toast.makeText(TicketActivity.this, "Билеты не найдены.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    imageView15.setVisibility(View.VISIBLE);
                }
            } else {
                Log.e("No data available", "Error retrieving data");
                Toast.makeText(TicketActivity.this, "Билеты не найдены.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                imageView15.setVisibility(View.VISIBLE);
            }
        }
    }

    public class TicketAdapter extends RecyclerView.Adapter<TicketActivity.TicketAdapter.TicketViewHolder> {

        private List<ArrayList<Object>> ticketDataList;


        public TicketAdapter(List<ArrayList<Object>> ticketDataList) {
            this.ticketDataList = ticketDataList;
        }
        public void clearData() {
            ticketDataList.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TicketActivity.TicketAdapter.TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ticket_card, parent, false);
            return new TicketActivity.TicketAdapter.TicketViewHolder(view);
        }
        public String convertTimestamp(String timestamp) {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM, HH:mm", new Locale("ru", "RU"));

            try {
                Date date = inputFormat.parse(timestamp);
                return outputFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
                return "";
            }
        }
        public String formatDuration(int durationInMinutes) {
            int days = durationInMinutes / (24 * 60);
            int hours = (durationInMinutes % (24 * 60)) / 60;
            int minutes = durationInMinutes % 60;

            String formattedDuration;

            if (days > 0 && hours > 0 && minutes > 0) {
                formattedDuration = String.format("%d д %d ч %02d мин", days, hours, minutes);
            } else if (days > 0 && hours > 0) {
                formattedDuration = String.format("%d д %d ч", days, hours);
            } else if (days > 0 && minutes > 0) {
                formattedDuration = String.format("%d д %02d мин", days, minutes);
            } else if (hours > 0 && minutes > 0) {
                formattedDuration = String.format("%d ч %02d мин", hours, minutes);
            } else if (days > 0) {
                formattedDuration = String.format("%d д", days);
            } else if (hours > 0) {
                formattedDuration = String.format("%d ч", hours);
            } else {
                formattedDuration = String.format("%d мин", minutes);
            }

            return formattedDuration;
        }

        public String getAirlineNameByCode(String airlineCode) {
            try {
                String jsonString = loadJsonFromAsset("airlines.json");
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject airlineObject = jsonArray.getJSONObject(i);
                    if (airlineObject.has("code") && airlineObject.getString("code").equals(airlineCode)) {
                        if (airlineObject.has("name")) {
                            return airlineObject.getString("name");
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String loadJsonFromAsset(String filename) {
            String json;
            try {
                InputStream is = getAssets().open(filename);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return json;
        }

        @Override
        public void onBindViewHolder(@NonNull TicketActivity.TicketAdapter.TicketViewHolder holder, int position) {
            ArrayList<Object> ticketData = ticketDataList.get(position);
            String flightNumber = ticketData.get(0).toString();
            String departureAtUnconverted = ticketData.get(1).toString();
            String departureAt = convertTimestamp(departureAtUnconverted);
            String originAirport = ticketData.get(2).toString();
            String returnAtUnconverted = ticketData.get(3).toString();
            String returnAt = convertTimestamp(returnAtUnconverted);
            String destinationAirport = ticketData.get(4).toString();
            String airline = ticketData.get(5).toString();
            int price = (int) ticketData.get(6)*adultsCount;
            int durationUnconverted = (int) ticketData.get(7);
            String duration = formatDuration(durationUnconverted);
            String airlineName=getAirlineNameByCode(airline);

            holder.ticketNumberTV.setText("Билет №"+flightNumber);
            holder.airlinesTV.setText(airlineName);
            holder.priceTV.setText(String.valueOf(price)+" \u20BD \n");
            holder.timeInTravelTV.setText(duration);
            holder.departureIataTV.setText(originAirport);
            holder.targetIataTV2.setText(destinationAirport);
            holder.departureDateTV.setText(departureAt);
            holder.returnDateTV.setText(returnAt);
            holder.adultsTV.setText("за "+String.valueOf(adultsCount)+" человека");
        }
        @Override
        public int getItemCount() {
            return ticketDataList.size();
        }

        public class TicketViewHolder extends RecyclerView.ViewHolder {

            public TextView ticketNumberTV;
            public TextView airlinesTV;
            public TextView priceTV;
            public TextView timeInTravelTV;
            public TextView departureIataTV;
            public TextView targetIataTV2;
            public TextView departureDateTV;
            public TextView returnDateTV;
            public TextView adultsTV;
            public Button buyTicketButton;
            ImageView backgroundImage;
            public Button boughtTicketButton;

            public TicketViewHolder(View itemView) {
                super(itemView);
                ticketNumberTV = itemView.findViewById(R.id.trainNumberTV);
                airlinesTV = itemView.findViewById(R.id.airlinesTV);
                priceTV = itemView.findViewById(R.id.trainPriceTV);
                timeInTravelTV = itemView.findViewById(R.id.trainTimeInTravelTV);
                departureIataTV = itemView.findViewById(R.id.departureIataTV);
                targetIataTV2 = itemView.findViewById(R.id.trainDirectionTV);
                departureDateTV = itemView.findViewById(R.id.trainDepartureDateTV);
                returnDateTV = itemView.findViewById(R.id.trainArrivalDateTV);
                adultsTV = itemView.findViewById(R.id.trainAdultsTV);
                buyTicketButton = itemView.findViewById(R.id.trainBuyTicketButton);
                boughtTicketButton = itemView.findViewById(R.id.trainBoughtTicketButton);
                backgroundImage=itemView.findViewById(R.id.backgroundImage);

                itemView.setOnClickListener(view -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ArrayList<Object> clickedTicketData = ticketDataList.get(position);
                        Intent intent = new Intent(itemView.getContext(), TicketInformationActivity.class);
                        intent.putExtra("flightNumber", clickedTicketData.get(0).toString().trim());
                        String departureAtUnconverted = clickedTicketData.get(1).toString();
                        intent.putExtra("departureAt", convertTimestamp(departureAtUnconverted).trim());
                        intent.putExtra("durationTo", formatDuration((int) clickedTicketData.get(9)));
                        intent.putExtra("durationBack", formatDuration((int) clickedTicketData.get(10)));
                        intent.putExtra("originAirport", clickedTicketData.get(2).toString());
                        String returnAtUnconverted = clickedTicketData.get(3).toString();
                        intent.putExtra("returnAt", convertTimestamp(returnAtUnconverted).trim());
                        intent.putExtra("targetAirport", clickedTicketData.get(4).toString().trim());
                        String airline=clickedTicketData.get(5).toString().trim();
                        intent.putExtra("airlineCode", airline);
                        intent.putExtra("airlineName", getAirlineNameByCode(airline).trim());
                        intent.putExtra("price", (int) clickedTicketData.get(6));
                        intent.putExtra("adultsCount", adultsCount);
                        int durationUnconverted = (int) clickedTicketData.get(7);
                        intent.putExtra("duration", formatDuration(durationUnconverted));
                        intent.putExtra("link", "https://www.aviasales.ru"+clickedTicketData.get(8).toString().trim());
                        intent.putExtra("transfers", (int) clickedTicketData.get(11));
                        intent.putExtra("returnTransfers", (int) clickedTicketData.get(12));
                        intent.putExtra("departureLocation", departureLocation);
                        intent.putExtra("targetLocation", targetLocation);
                        intent.putExtra("selectedTripId",selectedTripId);
                        intent.putExtra("returnAtUnconverted", clickedTicketData.get(3).toString());
                        intent.putExtra("departureAtUnconverted", clickedTicketData.get(1).toString());
                        intent.putExtra("durationToUnvonverted", (int) clickedTicketData.get(9));
                        intent.putExtra("durationBackUnvonverted", (int) clickedTicketData.get(10));

                        itemView.getContext().startActivity(intent);
                    }
                });
            }
        }
    }

}