package com.example.compass;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView compassImage;
    private TextView degreeText;
    private float currentDegree = 0f;
    private SensorManager sensorManager;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "CompassPrefs";
    private static final String LAST_DEGREE = "lastDegree";
    private static final int PERMISSION_REQUEST_CODE = 123;

    private Sensor accelerometer;
    private Sensor magnetometer;
    float[] lastAccelerometer = new float[3];
    float[] lastMagnetometer = new float[3];
    boolean haveSensor = false;
    boolean haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        compassImage = findViewById(R.id.compass_image);
        degreeText = findViewById(R.id.degree_text);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentDegree = preferences.getFloat(LAST_DEGREE, 0f);

        checkPermissions();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
        };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        haveSensor = sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        haveSensor2 = sensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        preferences.edit().putFloat(LAST_DEGREE, currentDegree).apply();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            lastAccelerometer = event.values.clone();
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            lastMagnetometer = event.values.clone();

        if (lastAccelerometer != null && lastMagnetometer != null) {
            SensorManager.getRotationMatrix(rMat, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);

            float azimuthInRadians = orientation[0];
            float azimuthInDegrees = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;

            RotateAnimation rotateAnimation = new RotateAnimation(
                    currentDegree,
                    -azimuthInDegrees,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);

            rotateAnimation.setDuration(250);
            rotateAnimation.setFillAfter(true);

            compassImage.startAnimation(rotateAnimation);
            currentDegree = -azimuthInDegrees;
            degreeText.setText(String.format("%.0f°", azimuthInDegrees));
            TextView directionText = findViewById(R.id.direction_text);
            directionText.setText(getDirectionText(azimuthInDegrees));
        }

    }

    private String getDirectionText(float degree) {
        if (degree >= 337.5 || degree < 22.5) return "N";
        if (degree >= 22.5 && degree < 67.5) return "NE";
        if (degree >= 67.5 && degree < 112.5) return "E";
        if (degree >= 112.5 && degree < 157.5) return "SE";
        if (degree >= 157.5 && degree < 202.5) return "S";
        if (degree >= 202.5 && degree < 247.5) return "SW";
        if (degree >= 247.5 && degree < 292.5) return "W";
        if (degree >= 292.5 && degree < 337.5) return "NW";
        return "";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            preferences.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        else if (id == R.id.action_delete_account) {
            showDeleteAccountDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String userEmail = preferences.getString("user_email", "");
                    DatabaseHelper dbHelper = new DatabaseHelper(this);

                    if (dbHelper.deleteUser(userEmail)) {
                        preferences.edit().clear().apply();
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}



