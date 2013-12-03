package iswd.aarol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import iswd.aarol.widget.CameraPreview;
import iswd.aarol.widget.OverlayView;


public class MainActivity extends Activity {

    private Camera camera = null;

    private LocationPoint lastLocation = null;
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

        Log.e("AAROL", "OnCreate");

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
            case R.id.action_packages:
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //prepare camera
        camera = Camera.open();

        //Search for existing preview
        CameraPreview preview = getCameraPreview();
        preview.setCamera(camera);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(magneticListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(gravityListener, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // release the camera immediately on pause event
        if (camera != null) {
            camera.release();
            camera = null;
            getCameraPreview().setCamera(null);
        }
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
}
