package com.example.myapplication.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.opengl.Visibility;
import android.os.Handler;
import android.util.Log;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.CoordinateUtil;
import com.example.myapplication.ZoomHandler;
import com.example.myapplication.friends.Friend;
import com.example.myapplication.LayoutHandler;
import com.example.myapplication.R;
import com.example.myapplication.location_data.LocationAPI;
import com.example.myapplication.location_data.LocationData;
import com.example.myapplication.location_data.LocationDataDao;
import com.example.myapplication.location_data.LocationDatabase;
import com.example.myapplication.location_data.LocationViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {


    private LocationGetter locGetter;
    private OrientationGetter orientGetter;
    private float orientation_current;

    private String user_UUID;
    private String user_name;
    private String private_code;
    private LocationAPI api;
    private LocationDataDao dao;
    private LocationViewModel viewModel;

    private boolean gpsStatus;

    private ScheduledExecutorService executor;
    private String custom_server = "https://socialcompass.goto.ucsd.edu/";

    ZoomHandler zh;

    LayoutHandler lh = new LayoutHandler();

    Friend myFriend = new Friend(-23f, -21f, "Calvin", "myUUID");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadProfile();

        // If nothing saved, launch InputActivity ( Do we want to check UUID or name?)
        if (user_UUID.equals("UUID NOT FOUND") || private_code.equals("PRIVATE CODE NOT FOUND") || user_name.equals("USER NAME NOT FOUND")) {
            Intent intent = new Intent(this, InputActivity.class);
            startActivity(intent);
        }

        zh = new ZoomHandler(this);

        setUpDatabases();
        setUpViewModel();

        // dao.delete_all();
        // THESE TWO GUYS ARE CURRENTLY IN THE LOCAL DATABASE AND ARE BEING DISPLAYED ON THE HOME SCREEN
        LocationData data2 = new LocationData("efb71004-d7b5-4067-b3b2-59904b7cda87", "Bob", 10f, 10f, true);
        // Bob's private code: 82b5ac85-6d9b-4084-8ebd-564363dacccb
        var data = new LocationData("c4a86ce2-fed4-4f98-bb1c-b5d83c968d93", "Calvin", 55f, -100f, true);
        // Calvin's private code: 8c89b79c-a03c-4580-b785-9be388e97f66

        // This user has...
        // public code: 68591d92-f36a-4b8a-89f1-702236f92848
        // private code: bacae4bd-6a4c-48f5-b3fc-2df94cb37f24

        final Handler handler = new Handler();
        final int delay = 5000; // 1000 milliseconds == 1 second
        handler.postDelayed(new Runnable() {
            public void run() {
                GPSCheck();

                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void setUpDatabases() {
        dao = LocationDatabase.provide(this).getDao();
        api = LocationAPI.provide(custom_server);
    }

    private void setUpViewModel() {
        viewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        viewModel.getData().observe(this, this::updateCompass);
    }

    private void updateCompass(List<LocationData> friends){
        if (friends == null) { return; };

        var friend_list = (ConstraintLayout) findViewById(R.id.friend_list);
        friend_list.removeAllViews();
        for (int i = 0; i < friends.size(); i ++ ) {
            LocationData curr_friend = friends.get(i);
            var curr_loc = locGetter.getLocation();
            if (curr_loc == null) return;
            Log.d("UPDATECOMPASS", curr_friend.label);
            int MAX_RADIUS = 450; // TODO


            // TODO: Calculate the correct angle (in degrees) to use. Changes as we rotate.
            var angle = CoordinateUtil.directionBetweenPoints(curr_loc.first, curr_friend.latitude, curr_loc.second, curr_friend.longitude);

            angle -= this.orientation_current;

            // TODO: Calculate the correct radius to use. Changes we zoom in/out. Edge of the circle is at: TODO
            float dist = (float) CoordinateUtil.distanceBetweenPoints(
                    curr_loc.first,curr_friend.latitude, curr_loc.second, curr_friend.longitude);
            Log.d(curr_friend.label, "Distance: " + String.valueOf(dist));

            int radius = (int) zh.getRadius(dist); // PLACEHOLDER

            //int radius = (450/4) * 3;
            // TODO: If too far, use a dot instead of the name
            View inflatedView = LayoutInflater.from(this).inflate(R.layout.friend_tag, friend_list, false);
            TextView friend = inflatedView.findViewById(R.id.name_tag);
            ImageView img = inflatedView.findViewById(R.id.dot);
            friend.setText(curr_friend.label);


            // If too far, display a dot instead
            boolean too_far = (radius >= MAX_RADIUS);
            // too_far = true;
            if (too_far) {
                // Dot instead of text. Display on edge of outermost server.
                img.setVisibility(View.VISIBLE);
                friend.setVisibility(View.INVISIBLE);
                radius = MAX_RADIUS;

            } else {
                img.setVisibility(View.INVISIBLE);
                friend.setVisibility(View.VISIBLE);
            }

            // Currently just has all your friends spaced around the circle.
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) inflatedView.getLayoutParams();
            params.circleAngle = angle;
            params.circleRadius = radius;
            params.circleConstraint = R.id.friend_list;

            inflatedView.setLayoutParams(params);
            friend_list.addView(inflatedView);
        }
    }

    public void updateOrientation(float orientation) {
        this.orientation_current = orientation;

        TextView orientation_text = findViewById(R.id.orientation_text);

        orientation_text.setText(Float.toString(orientation));

//        RotateAnimation rotateAnimation =
//                new RotateAnimation(currentDegree ,-1 *(this.orientation_current), Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//        rotateAnimation.setDuration(250);
//        rotateAnimation.setFillAfter(true);
//
//        this.currentDegree = -1*(this.orientation_current);
//        ImageView imageView = findViewById(R.id.compassImg);
//        imageView.startAnimation(rotateAnimation);

    }

    public void updateLocation(Pair<Double, Double> loc) {
        TextView location_text = findViewById(R.id.location_text);

        //ImageView point = findViewById(R.id.point);
        //point.setX(CoordinateUtil.directionBetweenPoints(loc.first,myFriend.getLat(),loc.second,myFriend.getLong()));
        //point.setY(CoordinateUtil.directionBetweenPoints(loc.first,myFriend.getLat(),loc.second,myFriend.getLong()));

        //getResources().getDisplayMetrics().density;

        //int px = (int) (150*getResources().getDisplayMetrics().density/160);
//        float angle = CoordinateUtil.directionBetweenPoints(loc.first,myFriend.getLat(),loc.second,myFriend.getLongit());
//        point.setX(lh.x_coordinate(angle));
//        point.setY(lh.y_coordinate(angle));

        // TODO Update all friend locations too
        location_text.setText(Double.toString(loc.first) + " , " + Double.toString(loc.second));
        //point.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        executor.shutdown();
        this.orientGetter.halt();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkLocationPermissions();
        loadProfile();

        //ImageView point = findViewById(R.id.point);
        //point.setVisibility(View.INVISIBLE);

        // set up Location and Orientation services
        orientGetter = new ActualOrientation(this);
        locGetter = new ActualLocation(this);

        // Display user's UUID
        TextView uuid_view = findViewById(R.id.uuid_view);
        uuid_view.setText("Your UUID: " + user_UUID);

        GPSCheck();


//        TextView gpsOnline = findViewById(R.id.gpsStatus);
//        gpsOnline.setText(String.valueOf(gpsstatus));

        setUpLocationExecutor();

    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200);
        }
    }

    private void setUpLocationExecutor() {
        // Every 3 seconds...
        executor = Executors.newSingleThreadScheduledExecutor();
        var poller = executor.scheduleAtFixedRate(() -> {
            var temp = viewModel.getData().getValue();
            List<LocationData> friends = new ArrayList<>(temp);

            Log.e("POLLER", "BEGIN");

            // Upload your current location to the server
            var location = locGetter.getLocation();
            var to_upload = new LocationData(this.user_UUID, this.user_name, location.first, location.second, false);
            to_upload.private_code = this.private_code;
            api.put(to_upload);

            // Pull the updated location of all your friends from the server
            for (var f : friends)
            {
                var got = api.get(f.public_code);
                dao.upsert(got);
                Log.e("FRIEND: ", got.toString());
            }

        }, 9, 3, TimeUnit.SECONDS);
    }

    private void loadProfile() {
        SharedPreferences preferences = getSharedPreferences("saved_data", MODE_PRIVATE);

        this.user_name = preferences.getString("user_name", "USERNAME NOT FOUND");
        this.user_UUID = preferences.getString("user_UUID", "UUID NOT FOUND");
        this.custom_server = preferences.getString("custom_server", "https://socialcompass.goto.ucsd.edu/");
        this.private_code = preferences.getString("private_code", "PRIVATE CODE NOT FOUND");

    }

    public void onLaunchInputClicked(View view) {
        Intent intent = new Intent(this, InputActivity.class);
        startActivity(intent);
    }

    public void onAddFriendClicked(View view) {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivity(intent);
    }


    public void onZoomInClicked(View view) {
       zh.onZoomIn();
       updateCompass(viewModel.getData().getValue());
    }

    public void onZoomOutClicked(View view) {
        zh.onZoomOut();
        updateCompass(viewModel.getData().getValue());
    }
    
    public void GPSCheck(){
        gpsStatus  = locGetter.checkIfGPSOnline();
        Log.d("GPS check val", gpsStatus+ "");
        ImageView red_dot = findViewById(R.id.reddot);
        ImageView green_dot = findViewById(R.id.greendot);
        long lengthDisabled = locGetter.GPSOfflineTime();
        TextView tv = findViewById(R.id.GPSOffline);
        red_dot.setVisibility(View.INVISIBLE);
        green_dot.setVisibility(View.INVISIBLE);

        if(gpsStatus){
            red_dot.setVisibility(View.INVISIBLE);
            tv.setVisibility(View.INVISIBLE);
            //green dot, gps active
            green_dot.setVisibility(View.VISIBLE);
        }
        else{
            long lengthDisabledToSecs = lengthDisabled/1000;
            long lengthDisabledToMins = (lengthDisabledToSecs%3600)/60;
            long lengthDisabledToHours = lengthDisabledToSecs/3600;

            if(lengthDisabledToHours < 1){
                //less than an hour, show number of minutes
                tv.setText((int)Math.floor(lengthDisabledToSecs/60) + " min");
            }
            else{
                //show minutes and seconds
                tv.setText((int)Math.floor(lengthDisabledToHours) + "h"+lengthDisabledToMins +" min");
            }
            tv.setVisibility(View.VISIBLE);
            green_dot.setVisibility(View.INVISIBLE);
            red_dot.setVisibility(View.VISIBLE);
            //red dot, gps not active
        }

    }

}