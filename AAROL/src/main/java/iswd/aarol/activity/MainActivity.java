package iswd.aarol.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import iswd.aarol.R;
import iswd.aarol.model.LocationPackage;
import iswd.aarol.model.LocationPackageFactory;
import iswd.aarol.model.LocationPoint;
import iswd.aarol.model.PackageManager;
import iswd.aarol.widget.CameraPreview;
import iswd.aarol.widget.OverlayView;


public class MainActivity extends Activity {

    public static final String LEAD_TO_PACKAGE_NAME = "leadTo";
    public static final String LEAD_TO_LOCATION_ID = "leadToId";

    private Camera camera = null;

    private LocationPoint lastLocation = null;

    private List<LocationPackage> enabledPackages = null;
    private LocationPoint leadToLocation = null;
    private float[] lastGravity = null;
    private float[] lastGeomagnetic = null;

    private SensorEventListener magneticListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lastGeomagnetic = event.values.clone();
            processSensors();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            int textId;
            switch (accuracy) {
                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                    textId = R.string.compass_low;
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                    textId = R.string.compass_medium;
                    break;
                default:
                    textId = R.string.compass_high;
            }
            Toast toast = Toast.makeText(getApplicationContext(), textId, Toast.LENGTH_LONG);
            toast.show();
        }
    };

    private SensorEventListener gravityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lastGravity = event.values.clone();
            processSensors();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = new LocationPoint(location);
            processSensors();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_packages: {
                Intent aboutIntent = new Intent(this, PackagesActivity.class);
                startActivity(aboutIntent);
                return true;
            }
            case R.id.action_settings: {
                Intent aboutIntent = new Intent(this, SettingsActivity.class);
                startActivity(aboutIntent);
                return true;
            }
            case R.id.action_about: {
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();

        startSensors();

        loadPackages();

        manageLeadToMode();
    }

    private void loadPackages() {
        enabledPackages = new ArrayList<LocationPackage>();
        for (String packageName : PackageManager.getEnabledPackageList(this)) {
            try {
                enabledPackages.add(LocationPackageFactory.create(this, packageName));
            } catch (Exception e) {
                e.printStackTrace();
                Toast toast = Toast.makeText(getApplicationContext(), R.string.loading_package_error, Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    private void startCamera() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    camera = Camera.open();
                } catch (RuntimeException e) {
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    //Search for existing preview
                    CameraPreview preview = getCameraPreview();
                    preview.setCamera(camera);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.no_camera, Toast.LENGTH_LONG);
                    toast.show();
                }
            }

            @Override
            protected void onCancelled() {
                stopCamera();
            }
        }.execute();
    }

    private void startSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(magneticListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(gravityListener, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (IllegalArgumentException e) {
            Toast toast = Toast.makeText(this, R.string.no_gps, Toast.LENGTH_LONG);
            toast.show();
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // release the camera immediately on pause event
        stopCamera();
        stopSensors();
    }

    private void stopCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
            getCameraPreview().setCamera(null);
        }
    }

    private void stopSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(magneticListener);
        sensorManager.unregisterListener(gravityListener);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
    }

    public CameraPreview getCameraPreview() {
        return (CameraPreview) findViewById(R.id.cameraPreview);
    }

    public LocationPoint getLastLocation() {
        return lastLocation;
    }

    public float[] getLastGravity() {
        return lastGravity;
    }

    public float[] getLastGeomagnetic() {
        return lastGeomagnetic;
    }

    public Camera getCamera() {
        return camera;
    }

    private void processSensors() {
        if (lastGravity != null && lastGeomagnetic != null && lastLocation != null) {
            OverlayView overlayView = (OverlayView) findViewById(R.id.overlayView);
            overlayView.setReady();
            overlayView.invalidate();
        }
    }

    private void manageLeadToMode() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String leadToPackageName = sharedPref.getString(MainActivity.LEAD_TO_PACKAGE_NAME, null);
        leadToLocation = null;
        if (leadToPackageName != null && PackageManager.isEnabled(this, leadToPackageName)) {
            int leadToLocationId = sharedPref.getInt(MainActivity.LEAD_TO_LOCATION_ID, 0);
            for (LocationPackage locationPackage : enabledPackages) {
                if (locationPackage.getPackageName().equals(leadToPackageName)) {
                    try {
                        leadToLocation = locationPackage.getLocations().get(leadToLocationId);
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                }
            }

        }
        findViewById(R.id.cancel_lead_to_button).setVisibility(leadToLocation != null ? View.VISIBLE : View.GONE);
    }

    public void cancelLeadTo(View view) {
        SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(this).edit();
        sharedPref.remove(LEAD_TO_PACKAGE_NAME);
        sharedPref.remove(LEAD_TO_LOCATION_ID);
        sharedPref.commit();

        manageLeadToMode();
    }

    public List<LocationPackage> getEnabledPackages() {
        return enabledPackages;
    }

    public LocationPoint getLeadToLocation() {
        return leadToLocation;
    }
}
