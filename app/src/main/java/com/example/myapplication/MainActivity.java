package com.example.myapplication;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Timer;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    static final ArrayList<String> locations = new ArrayList<>();
    Boolean music;
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String PREFS_LOC = "locations";
    public static final String PREFS_MUS = "music";

    final String LOG_TAG = "myLogs";
    GoogleMap mMap;
//    Timer myTimer = new Timer();
    int n=0;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MyLocationListener.SetUpLocationListener(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        //restore preferences
        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
        music = settings.getBoolean("music", true);
        locations.addAll(settings.getStringSet(PREFS_LOC, new HashSet(locations)));
        Collections.sort(locations, (b, a) -> a.compareTo(b));

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Location location = MyLocationListener.imHere;
        // Add a marker in Sydney and move the camera
//       LatLng me = new LatLng(56.628335, 47.876477);
        LatLng me = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(me));
//        1: World
//        5: Landmass/continent
//        10: City
//        15: Streets
//        20: Buildings
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 17.0f));
        mMap.addMarker(new MarkerOptions().position(me).title("It's Me"));
//        mMap.setMaxZoomPreference(mMap.getMaxZoomLevel());
//        getLocation();
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
        String text =
//                100+locations.size() + ": " +
                MyLocationListener.getLocation();
        // выводим сообщение
        this.setTitle(text);
        Toast.makeText(this,text, Toast.LENGTH_SHORT).show();
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