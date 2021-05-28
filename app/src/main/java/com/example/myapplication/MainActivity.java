package com.example.myapplication;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    static final ArrayList<String> locations = new ArrayList<>();
    int points = locations.size();
    int[] neighbors = new int[points];

    Boolean music;
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String PREFS_LOC = "locations";
    public static final String PREFS_MUS = "music";

    final String LOG_TAG = "myLogs";
    GoogleMap mMap;
    int n=0;
    SupportMapFragment mapFragment;

    Window window;
    Timer timer;
    // Метод для описания того, что будет происходить при работе таймера (задача для таймера):
    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            // Отображаем информацию в текстовом поле count:
            runOnUiThread(() -> {
//                    window.setTitle(new Date().toString());
                String text = MyLocationListener.getLocation();
//                100+locations.size() + ": " +
                locations.add(text);
                freshListView();
                System.out.println(new Date()+" "+text);
            });
        }
    }

    void fillMatrix(){
        int[][] matrix = new int[points][points];
        for (int i=0;i<points;i++){
            System.out.println();
            matrix[i] = getNeighbors(i);
        }
    }

    int[] getNeighbors(int i){
        int[] neighbors = new int[points];
        for (int j=0;j<points;j++){
            LatLng a = getPoint(locations.get(i));
            LatLng b = getPoint(locations.get(j));
            neighbors[j] = (int) ((Math.abs(b.latitude-a.latitude)+Math.abs(b.longitude-a.longitude))*1000000);
        }
        for (int neighbor : neighbors) System.out.print(neighbor+", ");
        return neighbors;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MyLocationListener.SetUpLocationListener(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        //restore preferences
        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
        music = settings.getBoolean("music", true);
        locations.addAll(settings.getStringSet(PREFS_LOC, new HashSet(locations)));
        Collections.sort(locations, (a, b) -> compare(getPoint(a),getPoint(b)));
        points = locations.size();
        System.out.println(points);
        System.out.println(locations);
        fillMatrix();
//        neighbors = getNeighbors(0);
//        Arrays.sort(neighbors);
//        Arrays.sort(locations,compareN);
//        Collections.sort(neighbors, (a, b) -> compareN(a,b));
        System.out.println();

        // выполняем задачу MyTimerTask, описание которой будет ниже:
        window = this.getWindow();
        timer = new Timer();
        timer.schedule(new MyTimerTask(), 10000, 10000);
    }

//    private <T> int compare(T a, T b) {
//        return Math.abs(b-a);
//    }

    LatLng getPoint(String o) {
        final Pattern pattern = Pattern.compile("(\\d+,\\d+);(\\d+,\\d+)");
        Matcher matcher = pattern.matcher(o);
        if (matcher.find()) {
            return new LatLng(
                    Float.parseFloat(Objects.requireNonNull(matcher.group(1)).replace(",", ".")),
                    Float.parseFloat(Objects.requireNonNull(matcher.group(2)).replace(",", "."))
            );
        }
        return null;
    }

    int compare(LatLng b,LatLng a){
        return (int) (((a.latitude == b.latitude) ?a.longitude-b.longitude :a.latitude-b.latitude)*1000000);
//        return (int) (Math.abs(a.longitude-b.longitude) + Math.abs(a.latitude-b.latitude)*1000000);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Location location = MyLocationListener.imHere;
        // Add a marker in Sydney and move the camera
//       LatLng me = new LatLng(56.628335, 47.876477);
        LatLng me = new LatLng(location.getLatitude(),location.getLongitude());

        PolylineOptions line = new PolylineOptions();
//        line.width(4f).color(R.color.indigo_900);
        LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
        for (String o : locations) {
            LatLng point = getPoint(o);
            if(point!=null) line.add(point);
//                latLngBuilder.include(point);
//            if (i == 0) {
//                MarkerOptions startMarkerOptions = new MarkerOptions()
//                        .position(mPoints.get(i))
//                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_a));
//                mMap.addMarker(startMarkerOptions);
//            } else if (i == mPoints.size() - 1) {
//                MarkerOptions endMarkerOptions = new MarkerOptions()
//                        .position(mPoints.get(i))
//                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_b));
//                mMap.addMarker(endMarkerOptions);
//            }
        }
        mMap.addPolyline(line);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(me));
//        1: World
//        5: Landmass/continent
//        10: City
//        15: Streets
//        20: Buildings
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14.0f));
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
//                100+locations.size() + ": " +
        String text = MyLocationListener.getLocation();
        // выводим сообщение
        this.setTitle(text);
        Toast.makeText(this,text, Toast.LENGTH_SHORT).show();
        // определяем строковый массив
        locations.add(text);
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