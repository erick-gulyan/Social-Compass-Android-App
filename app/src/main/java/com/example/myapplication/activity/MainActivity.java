package com.example.myapplication.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.CoordinateUtil;
import com.example.myapplication.friends.Friend;
import com.example.myapplication.LayoutHandler;
import com.example.myapplication.R;
import com.example.myapplication.friends.FriendAdapter;
import com.example.myapplication.friends.FriendDatabase;
import com.example.myapplication.friends.FriendListDao;
import com.example.myapplication.location_data.LocationAdapter;
import com.example.myapplication.location_data.LocationData;
import com.example.myapplication.location_data.LocationDataDao;
import com.example.myapplication.location_data.LocationDatabase;
import com.example.myapplication.location_data.LocationViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public RecyclerView recyclerView;

    private LocationGetter locGetter;
    private OrientationGetter orientGetter;
    private float orientation_current;
    private float set_orientation;
    private String user_UUID;
    float currentDegree = 0.0f;



    LayoutHandler lh = new LayoutHandler();

    Friend myFriend = new Friend(55f, -100f, "Calvin", "myUUID");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // LAB CODE
        LocationDataDao locationDataDao = LocationDatabase.provide(this).getDao();
//
//        List<LocationData> friends = locationDataDao.getAll().getValue();


        // THESE TWO GUYS ARE CURRENTLY IN THE LOCAL DATABASE AND ARE BEING DISPLAYED ON THE HOME SCREEN
        LocationData data2 = new LocationData("abc", "Bob", 10f, 10f, true);
        var data = new LocationData("myUUID", "Calvin", 55f, -100f, false);
        locationDataDao.upsert(data);
//        // locationDataDao.upsert(data);
//
//        var exists = locationDataDao.exists("abc");
//        var cde = locationDataDao.exists("cde");
//
//        var got = locationDataDao.get("abc");
//
//        LocationAdapter adapter = new LocationAdapter();
//        adapter.setHasStableIds(true);
//        adapter.setLocationData(friends);
//
//        recyclerView = findViewById(R.id.friend_list);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // TODO do I want a custom cicrular layout manager...
//        recyclerView.setAdapter(adapter);
        // TODO do I need to remove scrolling?


        // SHAREDNOTES CODE
        var viewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        var adapter = new LocationAdapter();
        adapter.setHasStableIds(true);
        viewModel.getData().observe(this, adapter::setLocationData);



        recyclerView = findViewById(R.id.friend_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadSetOrientation();
        loadProfile();

        // If nothing saved, launch InputActivity ( Do we want to check UUID or name?)
        if (user_UUID.equals("")) {
            Intent intent = new Intent(this, InputActivity.class);
            startActivity(intent);
        }

    }

    public void updateOrientation(float orientation) {
        this.orientation_current = orientation;

        TextView orientation_text = findViewById(R.id.orientation_text);

        orientation_text.setText(Float.toString(orientation));
        updateParentRelDirection();

        RotateAnimation rotateAnimation =
                new RotateAnimation(currentDegree ,-1 *(this.orientation_current), Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(250);
        rotateAnimation.setFillAfter(true);

        this.currentDegree = -1*(this.orientation_current);
        ImageView imageView = findViewById(R.id.compassImg);
        imageView.startAnimation(rotateAnimation);


    }

    // TODO adapt this for friends instead
    public void updateParentRelDirection() {

    }

    public void updateLocation(Pair<Double, Double> loc) {
        TextView location_text = findViewById(R.id.location_text);

        ImageView point = findViewById(R.id.point);
        //point.setX(CoordinateUtil.directionBetweenPoints(loc.first,myFriend.getLat(),loc.second,myFriend.getLong()));
        //point.setY(CoordinateUtil.directionBetweenPoints(loc.first,myFriend.getLat(),loc.second,myFriend.getLong()));

        //getResources().getDisplayMetrics().density;

        //int px = (int) (150*getResources().getDisplayMetrics().density/160);
        float angle = CoordinateUtil.directionBetweenPoints(loc.first,myFriend.getLat(),loc.second,myFriend.getLongit());
        point.setX(lh.x_coordinate(angle));
        point.setY(lh.y_coordinate(angle));

        // TODO Update all friend locations too
        location_text.setText(Double.toString(loc.first) + " , " + Double.toString(loc.second));
        updateParentRelDirection();
        point.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // this.orientGetter.halt();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ask for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200);
        }

        loadProfile();
        ImageView point = findViewById(R.id.point);
        point.setVisibility(View.INVISIBLE);
        // When returning from InputActivity, check for mocked orientation - SHOULDN'T BE MOCKED
//        loadSetOrientation();
//
//        if (this.set_orientation == 360) {
//            orientGetter = new ActualOrientation(this);
//        }
//        else {
//            orientGetter = new SetOrientation(this, set_orientation);
//        }
        orientGetter = new ActualOrientation(this);
        locGetter = new ActualLocation(this);

        // Display user's UUID
        TextView uuid_view = findViewById(R.id.uuid_view);
        uuid_view.setText("Your UUID: " + user_UUID);


    }

    private void loadProfile() {
        SharedPreferences preferences = getSharedPreferences("saved_data", MODE_PRIVATE);

        this.user_UUID = preferences.getString("user_UUID", "");

    }
    private void loadSetOrientation() {
        SharedPreferences preferences = getSharedPreferences("saved_data", MODE_PRIVATE);

        this.set_orientation = preferences.getFloat("set_orientation", 360);

    }

    public void onLaunchInputClicked(View view) {
        Intent intent = new Intent(this, InputActivity.class);
        startActivity(intent);
    }

}