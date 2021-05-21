package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    static final ArrayList<String> locations = new ArrayList<>();
    Boolean music;
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String PREFS_LOC = "locations";
    public static final String PREFS_MUS = "usic";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MyLocationListener.SetUpLocationListener(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //restore preferences
        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
        music = settings.getBoolean("music", true);
        locations.addAll(settings.getStringSet(PREFS_LOC, new HashSet(locations)));
        Collections.sort(locations, (b, a) -> a.compareTo(b));

    }

    @Override
    protected void onStart() {
        super.onStart();
        this.setTitle(MyLocationListener.getLocation());
        freshListView();
    }

    private void freshListView(){
        ListView positions = findViewById(R.id.coordinatesList);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, locations.toArray());
        // используем адаптер данных
        positions.setAdapter(adapter);
    }

    public void onMyButtonClick(View view) {
        Log.println(Log.INFO, PREFS_LOC.toUpperCase(), MyLocationListener.imHere.toString());
        String text = locations.size() + ": " + MyLocationListener.getLocation();
        // выводим сообщение
        this.setTitle(text);
//        Toast.makeText(this,text,Toast.LENGTH_SHORT).show();
        // определяем строковый массив
        locations.add(0, text);
        freshListView();

        //save music setup to system
        music = !music;

        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFS_MUS, music);

        editor.putStringSet(PREFS_LOC, new HashSet(locations));
        editor.apply();

    }

}