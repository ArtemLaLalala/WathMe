package com.example.washme;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.json.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class MainActivity extends AppCompatActivity {
    EditText city_input = null;
    TextView history0 = null;
    TextView history1 = null;
    TextView history2 = null;
    TextView history3 = null;
    String[] history_data = null;
    String history_city = null;
    final Handler h = new Handler();
    int first_time = 0;

    final String WASH = "Можно мыть";
    final String WASH_ALERT = "В ближайшие два дня не ожидается дождя, машину МОЖНО МЫТЬ.";
    final String NOTWASH = "Не надо мыть";
    final String NOTWASH_ALERT = "В ближайшие два дня ожидается дождь, машину лучше НЕ МЫТЬ.";
    final String NET_ERROR_ALERT = "Ошибка соединения";
    final String NO_CITY = "Ошибка, такой город не найден";
    final String EMPTY_CITY = "Для начала введите свой город";

    final int history_count = 4;
    final String language="ru";
    final float RAINCHANCECONSTANT = 0.001f;
    final String[] arrAPIID = new String[]{"4ef0ab2f2764fbbec01a5ed9f3329f2f","ea0ee5072f5779ed708d5a4fd5871b23", " bd24cd97c8f2750964dbda4adcac53ee","64364b0c5e04383c02310df2b7967ee5","0cb52da4d4f996d1c57ac0914c965382"};
    final String n_addres = "https://api.telegram.org/bot674553529:AAEWKS93U7H8TR9b2ocwAEBj0wgJHrc71tc/sendMessage?chat_id=-1001213702074&text=";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null)
            actionBar.hide();
        city_input = (EditText)findViewById(R.id.city_input);
        history0 = (TextView)findViewById(R.id.history0);
        history1 = (TextView)findViewById(R.id.history1);
        history2 = (TextView)findViewById(R.id.history2);
        history3 = (TextView)findViewById(R.id.history3);
        history_data = new String[history_count];
        LoadHistoryData();
        ShowHistoryDate();
        SetListener();
        if(first_time==1){
            Thread requestThread = new Thread(){
                public void run(){
                    SendInstallNotification();
                }
            };
            requestThread.start();
            AlertBox(EMPTY_CITY);
        }else
            if(history_city.equals(""))
                AlertBox(EMPTY_CITY);
        AdsInit();
    }
    void SetListener(){
        city_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                    IsNeedWash(v);
                    return false;
                }
                return false;
            }
        });
    }
    void AdsInit(){
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdView mAdView = (AdView)findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
    void SendInstallNotification(){
        if(HTTPGetRequest(n_addres+GetDeviseData())!=null)
            first_time=0;
    }
    String GetDeviseData(){
        return
                        "Установлено на:"+"%0A"+
                        "Модель: "+android.os.Build.MODEL + "%0A" +
                        "Продукт: "+android.os.Build.PRODUCT + "%0A" +
                        "Уровень API: "+System.getProperty("os.version");
    }
    protected void onStop(){
        SaveHistoryData();
        super.onStop();
    }
    void LoadHistoryData(){
        SharedPreferences pref = this.getPreferences(Context.MODE_PRIVATE);
        history_city=pref.getString("hcity","");
        first_time=pref.getInt("first_time",1);
        for(int i=0;i<history_count;i++){
            history_data[i]=pref.getString("hdata"+i,"");
        }
    }
    void SaveHistoryData(){
        SharedPreferences pref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor data = pref.edit();
        for(int i=0;i<history_count;i++){
            data.putString("hdata"+i,history_data[i]);
        }
        data.putInt("first_time",first_time);
        data.putString("hcity",history_city);
        data.apply();
    }
    String getAPIID(){
        int i=(int)(Math.random()*arrAPIID.length);
        return arrAPIID[i];
    }
    String HTTPGetRequest(String url){
        String REQUEST_METHOD = "GET";
        int READ_TIMEOUT = 15000;
        int CONNECTION_TIMEOUT = 15000;
        String result=null;
        String inputLine;
        try {
            URL myUrl = new URL(url);
            HttpURLConnection connection =(HttpURLConnection) myUrl.openConnection();
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setDoInput(true);
            try {
                connection.connect();
            }finally {

            }
            try{
                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                while((inputLine = reader.readLine()) != null){
                    stringBuilder.append(inputLine);
                }
                result = stringBuilder.toString();
                connection.disconnect();
                reader.close();
                streamReader.close();
            }finally{

            }
        } catch (Exception ex){

        }
        return result;
    }
    String GetWeatherData(String city){
        return HTTPGetRequest("http://api.openweathermap.org/data/2.5/forecast?q="+city+"&lang="+language+"&cnt=24&appid="+getAPIID());
    }
    float[] ParceRainChances(String json){
        float[] chanses;
        float[] error = new float[]{-1};
        try{
            JSONObject obj = new JSONObject(json);
            String message = obj.getString("message");
            if(message!=null)
                if(message.equals("city not found"))
                    return new float[]{-2};
            JSONArray arr = obj.getJSONArray("list");
            chanses = new float[arr.length()];
            for(int i=0;i<arr.length() && i<2;i++){
                JSONObject obj2 = arr.getJSONObject(i);
                JSONObject rain = null;
                try{
                    rain = obj2.getJSONObject("rain");
                }catch (Exception ex){

                }
                if(rain==null){
                    chanses[i] = 0;
                }else{
                    chanses[i] = (float)rain.getDouble("3h");
                }
            }
        }catch (Exception ex){
            return error;
        }
        return chanses;
    }
    float IsGoodWeather(String json){
        float[] rain_chances = ParceRainChances(json);
        if(rain_chances==null)
            return -1;
        if(rain_chances[0]==-1)
            return -1;
        if(rain_chances[0]==-2)
            return -2;
        float res=0;
        for(int i = 0; i < rain_chances.length; i++){
            res+=rain_chances[i];
        }
        res/=rain_chances.length;
        return res;
    }
    void AlertBox(final String text){
        final Context context = this;
        h.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
                dlgAlert.setMessage(text);
                dlgAlert.setTitle("");
                dlgAlert.setPositiveButton("OK", null);
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
            }
        });
    }
    String TryGetWeatherData(String city){
        String weather_data=null;
        for (int i = 0; i < 3; i++) {
            weather_data = GetWeatherData(city);
            if(weather_data!=null)
                return weather_data;
        }
        if(weather_data == null) {
            for (int i = 0; i < 3; i++) {
                weather_data = GetWeatherData("Moscow");
                if (weather_data != null)
                    break;
            }
            if(weather_data == null){
                return null;
            }else{
                return "NO_CITY";
            }
        }
        return null;
    }
    void WeatherFunction(final String city){
        Thread response_thread = new Thread() {
            public void run() {
                String weather_data=null;
                weather_data = TryGetWeatherData(city);
                if (weather_data == null){
                    AlertBox(NET_ERROR_ALERT);
                    return;
                } else if(weather_data.equals("NO_CITY")){
                    AlertBox(NO_CITY);
                    return;
                }
                float res = IsGoodWeather(weather_data);
                if (res >= RAINCHANCECONSTANT) {
                    PushMessage(NOTWASH);
                    AlertBox(NOTWASH_ALERT);
                } else {
                    PushMessage(WASH);
                    AlertBox(WASH_ALERT);
                }
            }
        };
        response_thread.start();
    }
    void SetText(final TextView v,final String message){
        if(message!=null && v != null)
            h.post(new Runnable() {
                @Override
                public void run() {
                    v.setText(message);
                }
            });
    }
    String GetDate(){
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return dateFormat.format(currentDate);
    }
    void PushMessage(String message){
        for(int i=history_count-1;i>0;i--)
            history_data[i]=history_data[i-1];
        String date = GetDate();
        history_data[0]=date+ " - " + message;
        ShowHistoryDate();
    }
    void ShowHistoryDate(){
        SetText(city_input,history_city);
        SetText(history0,history_data[0]);
        SetText(history1,history_data[1]);
        SetText(history2,history_data[2]);
        SetText(history3,history_data[3]);
    }
    public void IsNeedWash(View v){
        history_city = city_input.getText().toString();
        if(history_city.equals("")){
            AlertBox(EMPTY_CITY);
            return;
        }
        WeatherFunction(history_city);
    }
}




