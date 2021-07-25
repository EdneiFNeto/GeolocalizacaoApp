package com.example.geolocationjava;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class MainActivity extends AppCompatActivity {

    private MyLocation myLocation;
    private MainActivity context = MainActivity.this;
    private MyMaterialdialog myMaterialdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myLocation = MyLocation.getInstance(this);

        if (!myLocation.isPermission(this)) {
            Log.e("myLocation", "Exibe dialog");

            try {
                myMaterialdialog = MyMaterialdialog.getInstance(this, getCallbackClick());
                myMaterialdialog.createDialiog();
                myMaterialdialog.showDialog();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.button).setOnClickListener(v -> {
            if (myLocation.isEnabledGPS()) {
                myLocation.sendLocation(MainActivity.this);
            } else {
                myLocation.enabledGPS(context, this);
            }
        });
    }

    private MyMaterialdialog.CallbackOnClick getCallbackClick() {
        return new MyMaterialdialog.CallbackOnClick() {
            @Override
            public void confirm() {
                myLocation.isAccepPermission(MainActivity.this);
            }

            @Override
            public void cancel() {
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == myLocation.REQUEST_LOCATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("grantResults", "Permission granted");
            } else {
                Log.e("grantResults", "Permission denied");
                myMaterialdialog.closeDialog();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Now gps enabled", Toast.LENGTH_SHORT).show();
            }

            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Denied gps enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myMaterialdialog.closeDialog();
    }
}

class MyMaterialdialog extends MaterialAlertDialogBuilder {

    public static MyMaterialdialog myMaterialdialog;
    public CallbackOnClick callbackClick;
    public String title;
    public String message;

    private MyMaterialdialog(Context context, CallbackOnClick callbackClick) {
        super(context);
        this.title = "Atencão";
        this.message = "Para melhorar a experiência de uso e otimizar a velocidade, gostaria de permitir o envio de dados geográficos?";
        this.callbackClick = callbackClick;
    }

    public static MyMaterialdialog getInstance(Context context, CallbackOnClick callbackClick) {
        if (myMaterialdialog == null) {
            myMaterialdialog = new MyMaterialdialog(context, callbackClick);
        }

        return myMaterialdialog;
    }

    public AlertDialog createDialiog() {
        this.setTitle(title);
        this.setMessage(message);
        this.setCancelable(false);

        this.setPositiveButton("Cofirma", (dialog, which) -> {
            dialog.dismiss();
            callbackClick.confirm();
        });

        this.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
            callbackClick.cancel();
        });

        return this.create();
    }

    public void showDialog() {
        createDialiog().show();
    }

    public void closeDialog() {
        if (myMaterialdialog != null) {
            if (createDialiog().isShowing())
                createDialiog().dismiss();
        }
    }

    interface CallbackOnClick {
        void confirm();

        void cancel();
    }
}

class MyLocation {

    public static MyLocation myLocation;
    private static FusedLocationProviderClient fusedLocationProviderClient;
    private static LocationManager locationManager;
    private static LocationRequest locationRequest;
    public int REQUEST_LOCATIONS = 100;
    private String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private MyLocation(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        initLocationRequest();
    }

    public static MyLocation getInstance(Context context) {
        if (myLocation == null) {
            myLocation = new MyLocation(context);
        }

        return myLocation;
    }

    public boolean isPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public void sendLocation(Activity activity) {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(activity, location -> {
            if (location != null) {
                Log.e("LAT", "" + location.toString());
                EventPoster.getInstance().setLongitude(location.getLongitude());
                EventPoster.getInstance().setLatitude(location.getLatitude());
            }
        });
    }

    public boolean isEnabledGPS() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void enabledGPS(Context context, Activity activity) {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(context)
                .checkLocationSettings(builder.build());

        task.addOnCompleteListener(task1 -> {
            try {
                LocationSettingsResponse result = task1.getResult(ApiException.class);
                Toast.makeText(context, "GPS is enables", Toast.LENGTH_SHORT).show();
            } catch (ApiException e) {
                e.printStackTrace();
                if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                    try {
                        resolvableApiException.startResolutionForResult(activity, 101);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        sendIntentException.printStackTrace();
                    }
                }

                if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                    Toast.makeText(context, "Setting not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static void initLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setFastestInterval(5000);
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void isAccepPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_LOCATIONS);
    }
}

class EventPoster {
    public static EventPoster eventPoster;

    public static EventPoster getInstance() {
        if (eventPoster == null) {
            eventPoster = new EventPoster();
        }

        return eventPoster;
    }

    public void setLatitude(Double latitude) {
    }

    public void setLongitude(Double longitude) {
    }
}

