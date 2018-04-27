package edu.illinois.cs.cs125.busapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "busapp:Main";
    static final int REQUEST_LOCATION = 1;
    private String dlat;
    private String dlon;
    private String olat;
    private String olon;
    private String station;
    private static RequestQueue requestQueue;
    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestQueue = Volley.newRequestQueue(MainActivity.this);

        final Button startSearch = findViewById(R.id.get_route);
        startSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                getLocation();

                EditText address = (EditText) findViewById(R.id.input_route);
                String b = address.getText().toString();
                getDestination(b);
            }
        });

        final Button walking = findViewById(R.id.get_dir);
        walking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                getMap();
            }
        });
    }

    void getMap() {
        String a = station.replaceAll(" ","+");
        Log.d(TAG, a);
        Uri gmmIntentUri = Uri.parse("google.navigation:q="+a+"&mode=w");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }
    void getDestination (final String add) {
        String a = add.replaceAll(" ","+");
        try {

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    "https://maps.googleapis.com/maps/api/geocode/json?key=" + Keys.GOOGLE_API_KEY + "&address=" + a,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {
                            try {
                                JSONArray test = (JSONArray) response.get("results");
                                JSONObject test2 = test.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                                dlat = test2.getString("lat");
                                dlon = test2.getString("lng");

                                getRoute();

                            } catch (JSONException ignored) { }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError error) {
                    Log.e(TAG, error.toString());
                }
            });
            requestQueue.add(jsonObjectRequest);

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)  != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation
                    (LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                olat = Double.toString(lat);
                olon = Double.toString(lon);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_LOCATION:
                getLocation();
                break;
        }
    }
    void getRoute() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        String test = sdf.format(cal.getTime());
        Log.e(TAG, test);
        try {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    "https://developer.cumtd.com/api/v2.2/json/getplannedtripsbylatlon?key=" + Keys.MTD_API_KEY + "&origin_lat=" + olat + "&origin_lon=" + olon + "&destination_lat=" + dlat + "&destination_lon=" + dlon + "&time=" + test,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {
                            String result = "";
                            try {
                                JSONArray it = response.getJSONArray("itineraries");
                                JSONObject itineraries = it.getJSONObject(0);
                                String start = itineraries.getString("start_time");
                                String end = itineraries.getString("end_time");
                                String travel = itineraries.getString("travel_time");
                                JSONArray legs = itineraries.getJSONArray("legs");
                                JSONObject se = legs.getJSONObject(1);
                                JSONArray services = se.getJSONArray("services");
                                for (int i = 0; i < services.length(); i++) {
                                    result += services.getJSONObject(i).getJSONObject("route").getString("route_id");
                                    result += "\n";
                                    station = services.getJSONObject(i).getJSONObject("begin").getString("name") + "     ";
                                    result += station;
                                    result += services.getJSONObject(i).getJSONObject("begin").getString("time").substring(11,16);
                                    result += "\n";
                                    result += services.getJSONObject(i).getJSONObject("end").getString("name") + "     ";
                                    result += services.getJSONObject(i).getJSONObject("end").getString("time").substring(11,16);
                                    result += "\n\n";
                                }

                            } catch (Exception e) { e.printStackTrace(); }
                            TextView View1 = findViewById(R.id.output_route);
                            View1.setText(result);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError error) {
                    Log.e(TAG, error.toString());
                }
            });
            requestQueue.add(jsonObjectRequest);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
