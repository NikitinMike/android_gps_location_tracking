package com.example.myapplication;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class util {

    int scale = 1000000;
    int points = 1000;
    static final ArrayList<LatLng> locationPoints = new ArrayList<>();
    LatLng base;

    void fillMatrix(){
//        Log.i(LOG_TAG,":"+points);
        for (int i=0;i<points;i++) {
//            matrix[i] = getNeighbors(i);
//            Log.i(LOG_TAG,matrix[i]+" ");
        }
    }

    int[] getNeighbors(int i){
        int[] neighbors = new int[points];
        for (int j=0;j<points;j++){
            LatLng a = locationPoints.get(i);
            LatLng b = locationPoints.get(j);
            neighbors[j] = (int) ((Math.abs(b.latitude-a.latitude)+Math.abs(b.longitude-a.longitude))*scale);
        }
//        String log="";
//        for (int neighbor : neighbors)
//            log+=neighbor+", ";
//        Log.i(LOG_TAG,log+", ");
        return neighbors;
    }

    int compare2(LatLng a,LatLng b){
        double a1 = a.longitude-base.longitude;
        double a2 = a.latitude-base.latitude;
        double b1 = b.longitude-base.longitude;
        double b2 = b.latitude-base.latitude;
        return (int) ((Math.sqrt(a1*a1+a2*a2)-Math.sqrt(b1*b1+b2*b2))*scale);
    }

}
