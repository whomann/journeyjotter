package com.example.jotterjourney;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class CountryInfoPopup extends Dialog {
    private Context context;
    private String countryCode;

    public CountryInfoPopup(Context context, String countryCode) {
        super(context, R.style.CustomDialog);
        this.context = context;
        this.countryCode = countryCode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_info_popup);
        setOnShowListener(dialogInterface -> {
            int decorViewWidth = getWindow().getDecorView().getWidth();
            int dialogWidth = (int) (decorViewWidth * 0.95);
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.width = dialogWidth;
            getWindow().setAttributes(layoutParams);
        });
        ImageButton closeButton = findViewById(R.id.buttonClose);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        ImageView flagImageView=findViewById(R.id.flagImageView);
        String countryImageFileName = countryCode.toLowerCase() + ".png";
        String imagePath = "flags/" + countryImageFileName;
        try {
            InputStream inputStream = context.getAssets().open(imagePath);
            Drawable drawable = Drawable.createFromStream(inputStream, null);
            flagImageView.setImageDrawable(drawable);
        } catch (IOException e) {

            flagImageView.setImageResource(R.drawable.placeholder);
        }
        ImageView imageViewCountryInfo=findViewById(R.id.imageViewCountryInfo);
        TextView countryNameTextView = findViewById(R.id.textViewCountryName);
        TextView shortInfoTextView = findViewById(R.id.textViewShortInfo);
        TextView mainDishesTextView = findViewById(R.id.textViewCousine);
        TextView communicationStyleTextView = findViewById(R.id.textViewCommunicationStyle);
        TextView giftGivingTextView = findViewById(R.id.textViewGiftGiving);
        TextView dressCodeTextView = findViewById(R.id.textViewDressCode);
        TextView publicTransportTextView = findViewById(R.id.textViewPublicTransport);
        TextView taboosTextView = findViewById(R.id.textViewTaboos);
        TextView emergencyInfoTextView = findViewById(R.id.textViewEmergencyInfo);
        TextView currencyTextView = findViewById(R.id.textViewCurrency);
        TextView weatherTextView = findViewById(R.id.textViewWeather);
        Log.d("countryCode", countryCode);

        try {
            JSONObject json = loadJsonFromAsset();
            JSONArray countries = json.getJSONArray("countries");
            for (int i = 0; i < countries.length(); i++) {
                JSONObject country = countries.getJSONObject(i);
                if (country.getString("country_code").equals(countryCode)) {
                    String imageLink=country.getString("image");
                    countryNameTextView.setText(country.getString("country_name"));
                    shortInfoTextView.setText(country.getString("short_info"));
                    JSONObject cuisine = country.getJSONObject("cuisine");
                    mainDishesTextView.setText("• Блюда: "+cuisine.getJSONArray("main_dishes").join(", ").replace("\"", "")+".\n• Напитки: "+cuisine.getJSONArray("local_drinks").join(", ").replace("\"", "")+".\n• Десерты: "+cuisine.getJSONArray("desserts").join(", ").replace("\"", "")+".");
                    JSONObject socialNorms = country.getJSONObject("social_norms");
                    communicationStyleTextView.setText(socialNorms.getString("communication_style"));
                    giftGivingTextView.setText(socialNorms.getString("gift_giving"));
                    dressCodeTextView.setText(country.getString("dress_code"));
                    publicTransportTextView.setText(country.getString("public_transport"));
                    JSONArray taboos = country.getJSONArray("taboos");
                    taboosTextView.setText(taboos.join("\n").replace("\"", ""));
                    JSONArray languages = country.getJSONArray("language");
                    JSONObject emergencyInfo = country.getJSONObject("emergency_info");
                    emergencyInfoTextView.setText("Полиция: " + emergencyInfo.getString("полиция") + "\nСкорая помощь: " + emergencyInfo.getString("медицина") + "\nПожарная охрана: " + emergencyInfo.getString("пожарная служба"));
                    currencyTextView.setText("Язык: "+languages.join(", ").replace("\"", "")+"\nВалюта: "+country.getString("currency"));
                    weatherTextView.setText(country.getString("weather"));
                    updateImage(imageViewCountryInfo, imageLink);
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void updateImage(ImageView imageView, String imageUrl) {
        Picasso.get().load(imageUrl).into(imageView);
    }

    private JSONObject loadJsonFromAsset() {
        String json;
        try {
            InputStream is = getContext().getAssets().open("countries_culture_tips.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}