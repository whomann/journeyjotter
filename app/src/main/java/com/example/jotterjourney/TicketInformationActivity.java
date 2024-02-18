package com.example.jotterjourney;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketInformationActivity extends AppCompatActivity {

    ImageView loadingContainer,loadingIV; TextView originOriginLocationTV, returnArrivalLocationTV, returnOriginLocationTV, originArrivalLocationTV; private String flightNumber,departureAt,targetAirportName, originAirportName,originAirport,returnAt,airlineCode,airlineName,targetAirport,duration,durationTo,durationBack,link, targetLocation, departureLocation; private int price,transfers,returnTransfers,adultsCount, selectedTripId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_information);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.accent_color_2));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        loadingContainer=findViewById(R.id.loadingContainer);
        loadingContainer.setVisibility(View.VISIBLE);
        loadingIV=findViewById(R.id.loadingIV);
        loadingIV.setVisibility(View.VISIBLE);
        Glide.with(this).load(R.drawable.loading).into(loadingIV);
        flightNumber = getIntent().getStringExtra("flightNumber");
        departureAt=getIntent().getStringExtra("departureAt");
        durationTo=getIntent().getStringExtra("durationTo");
        durationBack=getIntent().getStringExtra("durationBack");
        originAirport=getIntent().getStringExtra("originAirport");
        returnAt=getIntent().getStringExtra("returnAt");
        targetAirport=getIntent().getStringExtra("targetAirport");
        airlineCode=getIntent().getStringExtra("airlineCode");
        airlineName=getIntent().getStringExtra("airlineName");
        price=getIntent().getIntExtra("price",0);
        adultsCount=getIntent().getIntExtra("adultsCount",1);
        transfers=getIntent().getIntExtra("transfers",0);
        returnTransfers=getIntent().getIntExtra("returnTransfers",0);
        duration=getIntent().getStringExtra("duration");
        link=getIntent().getStringExtra("link");
        targetLocation= getIntent().getStringExtra("targetLocation");
        departureLocation=getIntent().getStringExtra("departureLocation");
        selectedTripId=getIntent().getIntExtra("selectedTripId",1);

        ImageView airlinesImageView=findViewById(R.id.airlinesImageView);
        ImageView airlinesReturnImageView=findViewById(R.id.airlinesReturnImageView);
        originOriginLocationTV=findViewById(R.id.originOriginLocationTV);
        returnArrivalLocationTV=findViewById(R.id.returnArrivalLocationTV);
        returnOriginLocationTV=findViewById(R.id.returnOriginLocationTV);
        originArrivalLocationTV=findViewById(R.id.originArrivalLocationTV);
        TextView returnTransfersTV=findViewById(R.id.returnTransfersTV);
        TextView originTransfersTV=findViewById(R.id.originTransfersTV);
        ImageView transfersReturnImageView=findViewById(R.id.transfersReturnImageView);
        ImageView transfersImageView=findViewById(R.id.transfersImageView);
        TextView ticketPriceTV=findViewById(R.id.ticketPriceTV);
        TextView pricePerPersonTV=findViewById(R.id.pricePerPersonTV);
        ImageButton buyTicketButton=findViewById(R.id.buyTicketButton);
        ImageButton boughtTicketIButton=findViewById(R.id.boughtTicketIButton);
        TextView returnAirlinesTextView=findViewById(R.id.returnAirlinesTextView);
        TextView airlinesTextView=findViewById(R.id.airlinesTextView);
        TextView returnDuration_TV=findViewById(R.id.returnDuration_TV);
        TextView originDuration_TV=findViewById(R.id.originDuration_TV);
        TextView originDirection_TV=findViewById(R.id.originDirection_TV);
        TextView returnDirection_TV=findViewById(R.id.returnDirection_TV);
        TextView ticketNumberTV=findViewById(R.id.ticketNumberTV);
        ImageButton backButton=findViewById(R.id.backButton);
        TextView totalDurationTV=findViewById(R.id.totalDurationTV);
        TextView originDepartureTime=findViewById(R.id.originDepartureTime);
        TextView returnDepartureTime=findViewById(R.id.returnDepartureTime);
        TextView returnArrivalTime=findViewById(R.id.returnArrivalTime);
        TextView originArrivalTime=findViewById(R.id.originArrivalTime);

        backButton.setOnClickListener(v->{
            onBackPressed();
        });

        buyTicketButton.setOnClickListener(v -> {
            openTicketLink(link);
        });

        boughtTicketIButton.setOnClickListener(v -> {
            writeToDB();
        });

        String transfersStringTo="";
        if(transfers==1){
            transfersStringTo="пересадка";
        }
        else if(transfers>1&&transfers<5){
            transfersStringTo="пересадки";
        }
        else{
            transfersStringTo="пересадок";
        }

        String transfersStringBack="";
        if(returnTransfers==1){
            transfersStringBack="пересадка";
        }
        else if(returnTransfers>1&&returnTransfers<5){
            transfersStringBack="пересадки";
        }
        else{
            transfersStringBack="пересадок";
        }
        int durationBackUnvonverted = getIntent().getIntExtra("durationBackUnvonverted",0);
        int durationToUnvonverted = getIntent().getIntExtra("durationToUnvonverted",0);
        String departureAtUnconverted = getIntent().getStringExtra("departureAtUnconverted");
        String returnAtUnconverted = getIntent().getStringExtra("returnAtUnconverted");

        String originArriveAt=calculateDates(departureAtUnconverted, durationToUnvonverted);
        String targetArriveAt=calculateDates(returnAtUnconverted, durationBackUnvonverted);

        Log.d("originArriveAt: ",originArriveAt+", departureAt: "+departureAtUnconverted+", durationTo: "+durationToUnvonverted);
        Log.d("targetArriveAt: ", targetArriveAt+", returnAt: "+returnAtUnconverted+", durationBack: "+durationBackUnvonverted);

        originArrivalTime.setText(originArriveAt);
        returnArrivalTime.setText(targetArriveAt);
        returnDepartureTime.setText(formatDateString(returnAtUnconverted));
        originDepartureTime.setText(formatDateString(departureAtUnconverted));
        totalDurationTV.setText("Общее время в пути: "+duration);
        ticketNumberTV.setText("Билет №"+flightNumber);
        returnDirection_TV.setText(targetLocation+" - "+departureLocation);
        originDirection_TV.setText(departureLocation+" - "+targetLocation);
        originDuration_TV.setText("В пути "+durationTo);
        returnDuration_TV.setText("В пути "+durationBack);
        returnTransfersTV.setText(returnTransfers+" "+transfersStringBack);
        returnAirlinesTextView.setText(airlineName+" ("+airlineCode+")");
        airlinesTextView.setText(airlineName+" ("+airlineCode+")");
        originTransfersTV.setText(transfers+" "+transfersStringTo);
        pricePerPersonTV.setText("Цена: "+String.valueOf(price)+" руб. за человека");
        ticketPriceTV.setText("Итого: "+String.valueOf(price*adultsCount)+" руб.");
        if(returnTransfers>0){
            transfersReturnImageView.setImageResource(R.drawable.dir_transfers);
        }
        else{
            transfersReturnImageView.setImageResource(R.drawable.dir_no_transfers);
        }

        if(transfers>0){
            transfersImageView.setImageResource(R.drawable.dir_transfers);
        }
        else{
            transfersImageView.setImageResource(R.drawable.dir_no_transfers);
        }

        new GetAirportInfoTask().execute();
        loadAirlineLogo(airlineCode, airlinesReturnImageView);
        loadAirlineLogo(airlineCode, airlinesImageView);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public static String calculateDates(String departureDateTime, int durationInMinutes) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        Date departureDate;
        try {
            departureDate = inputFormat.parse(departureDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
            System.err.println("Error parsing departure date and time: " + departureDateTime);
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(departureDate);
        calendar.add(Calendar.MINUTE, durationInMinutes);
        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm\nd MMM", new Locale("ru"));
        return outputFormat.format(calendar.getTime());
    }

    public static String formatDateString(String inputDateString) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        Date date;
        try {
            date = inputFormat.parse(inputDateString);
        } catch (ParseException e) {
            e.printStackTrace();
            System.err.println("Error parsing input date string: " + inputDateString);
            return null;
        }
        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm\nd MMM", new Locale("ru"));
        return outputFormat.format(date);
    }
    private void writeToDB(){
        String updatedTicketInfo = originAirport + " <-> " + targetAirport + " ("+airlineName+").\nВылет туда: "+departureAt+",\nВылет обратно: "+returnAt;
        int userId = selectedTripId;
        SQLiteDatabase db = openOrCreateDatabase("JourneyJotterDB", MODE_PRIVATE, null);

        ContentValues values = new ContentValues();
        values.put("ticketInfo", updatedTicketInfo);
        values.put("originIATA", originAirport);
        values.put("targetIATA", targetAirport);

        int affectedRows = db.update("jjtrips1", values, "userId=?", new String[]{String.valueOf(userId)});
        if (affectedRows > 0) {
            Log.d("Data updated successfully.", "Data updated successfully.");
        } else {
            Log.e("Error updating data:", "No data updated.");
        }
        db.close();
        Toast.makeText(this, "Билет куплен!", Toast.LENGTH_SHORT).show();
    }
    private void loadAirlineLogo(String airlineCode, ImageView imageView){
        String imageUrl = "https://cors.eu.org/http://pics.avs.io/200/200/" + airlineCode + ".png";
        Picasso.get().load(imageUrl).into(imageView);
    }
    private void openTicketLink(String link) {
        if (link != null && !link.isEmpty()) {
            Uri uri = Uri.parse(link);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }
    private class GetAirportInfoTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String apiUrl = "https://api.travelpayouts.com/data/ru/airports.json";
            StringBuilder response = new StringBuilder();

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();
            } catch (Exception e) {
                Log.e("AirportInfo", "Error fetching data", e);
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONArray airports = new JSONArray(result);
                for (int i = 0; i < airports.length(); i++) {
                    JSONObject airport = airports.getJSONObject(i);
                    String code = airport.optString("code");
                    String name = airport.optString("name");
                    String enName = airport.optJSONObject("name_translations").optString("en");

                    if (code.equals(originAirport)) {
                        Log.d("AirportInfo", "Airport Code: " + code);
                        originAirportName=name.isEmpty() ? enName : name;
                        Log.d("AirportInfo", "Airport Name: " + originAirportName);
                    }
                    if (code.equals(targetAirport)) {
                        Log.d("AirportInfo", "Return Airport Code: " + code);
                        targetAirportName= name.isEmpty() ? enName : name;
                        Log.d("AirportInfo", "Return Airport Name: " +targetAirportName);
                    }
                }
                originOriginLocationTV.setText(departureLocation+"\n"+originAirportName+", "+originAirport);
                originArrivalLocationTV.setText(targetLocation+"\n"+targetAirportName+", "+targetAirport);
                returnArrivalLocationTV.setText(departureLocation+"\n"+originAirportName+", "+originAirport);
                returnOriginLocationTV.setText(targetLocation+"\n"+targetAirportName+", "+targetAirport);
                loadingContainer.setVisibility(View.GONE);
                loadingIV.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("AirportInfo", "Error parsing JSON", e);
            }
        }
    }
}