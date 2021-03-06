package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    DBHelper dbHelper;
    SQLiteDatabase db;

    static final ArrayList<String> locations = new ArrayList<>();
    static final ArrayList<LatLng> locationPoints = new ArrayList<>();
    int points = 1000;
    int scale = 1000000;
    long start = 0;
    long count = 0;
//    int[][] matrix = new int[points][points];
//    int[] neighbors = new int[points];

    Boolean music;
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String PREFS_LOC = "locations";
    public static final String PREFS_MUS = "music";

    final String LOG_TAG = "myLogs";
    GoogleMap mMap;
    int n = 0;
    SupportMapFragment mapFragment;
    LatLng base;

    static Window window;
    static Timer timer;

    // ?????????? ?????? ???????????????? ????????, ?????? ?????????? ?????????????????????? ?????? ???????????? ?????????????? (???????????? ?????? ??????????????):
    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            // ???????????????????? ???????????????????? ?? ?????????????????? ???????? count:
            runOnUiThread(() -> {
                window.setTitle(new Date().toString());
                String text = MyLocationListener.getLocation();
                locations.add(text);
                LatLng p = getPoint(text);
                locationPoints.add(p);
                saveDBRecord(new Date().getTime(),p.latitude,p.longitude);
//                System.out.println((new Date().getTime()-start)/1000+" "+locationPoints.size()+" "+text);
                freshListView();
                if ((++count % 10) == 0) refreshMap();
            });
        }
    }

    @Override
    protected void onStop() {
        // ?????????????????? ?????????????????????? ?? ????
        dbHelper.close();
        super.onStop();
//        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MyLocationListener.SetUpLocationListener(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = new Date().getTime();

        dbHelper = new DBHelper(this);
        // ???????????????????????? ?? ????
        db = dbHelper.getWritableDatabase();
        clearDBData();

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
//        points = locations.size(); System.out.println(points);
//        System.out.println(locations);
        for (String s : locations) locationPoints.add(getPoint(s));

        Location location = MyLocationListener.imHere;
        base = new LatLng(location.getLatitude(), location.getLongitude());

        Log.i(LOG_TAG, base.toString());

        points = locationPoints.size();

        Collections.sort(locationPoints, this::compare);

//        locationPoints.remove(0);

//        int i = 0;
//        for(LatLng p:locationPoints) Log.i(LOG_TAG,++i+": "+ p);
//        System.out.println();
//        fillMatrix();
//        System.out.println();

        // ?????????????????? ???????????? MyTimerTask, ???????????????? ?????????????? ?????????? ????????:
        window = this.getWindow();
        timer = new Timer();
        timer.schedule(new MyTimerTask(), 10000, 10000);
    }

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

    int compare(LatLng b, LatLng a) {
        return (int) (((a.latitude == b.latitude) ? a.longitude - b.longitude : a.latitude - b.latitude) * scale);
//        return (int) (((a.longitude == b.longitude) ?a.latitude-b.latitude:a.longitude-b.longitude)*scale);
//        return (int) (Math.abs(a.longitude-b.longitude) + Math.abs(a.latitude-b.latitude)*1000000);
    }

    MarkerOptions setMarker(int bits) {
        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.alpha(0.1F);
        Bitmap bitmap = Bitmap.createBitmap(bits, bits, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < bits; x++)
            for (int y = 0; y < bits; y++) bitmap.setPixel(x, y, Color.BLACK);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
//        markerOptions.icon( BitmapDescriptorFactory.fromAsset("pixel.bmp") );
//                Bitmap.createBitmap(1,1,new Bitmap.Config()));
        return markerOptions;
    }

    void refreshMap() {
        Location location = MyLocationListener.imHere;
        // Add a marker in Sydney and move the camera
//       LatLng me = new LatLng(56.628335, 47.876477);
        LatLng me = new LatLng(location.getLatitude(), location.getLongitude());

        if (count == 0) {
            //        PolylineOptions line = new PolylineOptions();
            MarkerOptions markerOptions = setMarker(10);

            //        line.width(4f).color(R.color.indigo_900);
            //        LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
            for (LatLng point : locationPoints) {
                //            if(point!=null) line.add(point);
                if (point != null) {
                    markerOptions.position(point);
                    mMap.addMarker(markerOptions);
                }
            }
        }

//            latLngBuilder.include(point);
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

//        mMap.addPolyline(line);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(me));
//        1: World
//        5: Landmass/continent
//        10: City
//        15: Streets
//        20: Buildings
//        if((count%10)==0)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14.0f));
        mMap.addMarker(new MarkerOptions().position(me).title("It's Me"));
//        mMap.setMaxZoomPreference(mMap.getMaxZoomLevel());
//        getLocation();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        refreshMap();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.setTitle(MyLocationListener.getLocation());
        freshListView();
    }

    private void freshListView() {
        ListView positions = findViewById(R.id.coordinatesList);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, locations.toArray());
        // ???????????????????? ?????????????? ????????????
        positions.setAdapter(adapter);
    }

    public void onMyButtonClick(View view) {
        // 100+locations.size() + ": " +
        Log.println(Log.INFO, PREFS_LOC.toUpperCase(), MyLocationListener.imHere.toString());
        String text = MyLocationListener.getLocation();
        // ?????????????? ??????????????????
        this.setTitle(text);
        locations.add(text);
        Toast.makeText(this, locations.size() + ":" + text, Toast.LENGTH_SHORT).show();
        // ???????????????????? ?????????????????? ????????????
        freshListView();

        //save music setup to system
        music = !music;

        SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFS_MUS, music);

        editor.putStringSet(PREFS_LOC, new HashSet(locations));
        editor.apply();
        save();

        LatLng p = getPoint(text);
        saveDBRecord(new Date().getTime(),p.latitude,p.longitude);
        getDBData();
    }

    void save() {
        File file = new File(Environment.getExternalStorageDirectory(), "/points.txt");
        Log.i(LOG_TAG, file.toString());
        FileOutputStream fos = null; // save
        try {
            fos = new FileOutputStream(file, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
//        String data = "TEST DATA\r\n";
//            fos.write(data.getBytes());
            for (LatLng pt : locationPoints)
                fos.write((pt.toString() + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    long saveDBRecord(long time,double latitude,double longitude) {
        if(getDBRecord(latitude,longitude)) return 0;
        // ?????????????? ???????????? ?????? ????????????
        ContentValues cv = new ContentValues();
//        cv.put("name", "name");
//        cv.put("email", "email");
        cv.put("datetime", time);
        cv.put("latitude", latitude);
        cv.put("longitude", longitude);
        // ?????????????????? ???????????? ?? ???????????????? ???? ID
        long rowID = db.insert("mytable", null, cv);
//        Log.d(LOG_TAG, "row inserted, ID = " + rowID);
        return rowID;
    }

    void clearDBData() {
        Log.d(LOG_TAG, "--- Clear mytable: ---");
        // ?????????????? ?????? ????????????
        int clearCount = db.delete("mytable", null, null);
        Log.d(LOG_TAG, "deleted rows count = " + clearCount);
    }

    boolean getDBRecord(double latitude,double longitude){
        Cursor c = db.rawQuery("SELECT id,datetime FROM mytable WHERE "
                +"latitude = "+latitude+" AND " +"longitude = "+longitude, null);
        if (c.moveToFirst()){
            do {
                // Passing values
                long id = c.getLong(0);
                long time = c.getLong(1);
                // Do something Here with values
            } while(c.moveToNext());
            return true;
        }
        c.close();
        return false;
    }

    void getDBData() {
        // ???????????? ???????????? ???????? ???????????? ???? ?????????????? mytable, ???????????????? Cursor
        Cursor c = db.query("mytable", null, null, null, null, null, null);
        // ???????????? ?????????????? ?????????????? ???? ???????????? ???????????? ??????????????
        // ???????? ?? ?????????????? ?????? ??????????, ???????????????? false
        if (c.moveToFirst()) {
            // ???????????????????? ???????????? ???????????????? ???? ?????????? ?? ??????????????
            int idColIndex = c.getColumnIndex("id");
            int datetimeColIndex = c.getColumnIndex("datetime");
            int latitudeColIndex = c.getColumnIndex("latitude");
            int longitudeColIndex = c.getColumnIndex("longitude");
            int nameColIndex = c.getColumnIndex("name");
            int emailColIndex = c.getColumnIndex("email");
            do {
                // ???????????????? ???????????????? ???? ?????????????? ???????????????? ?? ?????????? ?????? ?? ??????
                Log.d(LOG_TAG,
                    "ID = " + c.getInt(idColIndex)
                        +", time = " + c.getString(datetimeColIndex)
                        +", latitude = " + c.getString(latitudeColIndex)
                        +", longitude = " + c.getString(longitudeColIndex)
//                        ", name = " + c.getString(nameColIndex) +
//                        ", email = " + c.getString(emailColIndex)
                );
                // ?????????????? ???? ?????????????????? ????????????
                // ?? ???????? ?????????????????? ?????? (?????????????? - ??????????????????), ???? false - ?????????????? ???? ??????????
            } while (c.moveToNext());
        } else
            Log.d(LOG_TAG, "0 rows");
        c.close();
    }
}