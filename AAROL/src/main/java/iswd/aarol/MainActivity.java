package iswd.aarol;

import android.app.Activity;
import android.content.Context;
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
import android.widget.FrameLayout;
import android.widget.TextView;


import iswd.aarol.LocationPoint.XYZLocation;
import iswd.aarol.widget.CameraPreview;

import static java.lang.Math.*;


public class MainActivity extends Activity {

    private Camera camera = null;

    private XYZLocation lastPosition = null;
    private Double lastAltitude = null;
    private float[] lastGravity = null;
    private float[] lastGeomagnetic = null;

    private SensorEventListener magneticListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lastGeomagnetic = event.values;
            processSensors();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener gravityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lastGravity = event.values;
            processSensors();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastPosition = new LocationPoint(location).getXYZPosition();
            lastAltitude = location.hasAltitude() ? location.getAltitude() : null;
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
            case R.id.action_settings: {
                camera.startPreview();
                return true;
            }
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
        if (preview == null) {
            preview = new CameraPreview(this);
            FrameLayout container = (FrameLayout) findViewById(R.id.container);
            container.removeAllViews();
            container.addView(preview);
            TextView textOverlay = new TextView(this);
            textOverlay.setId(2141523);
            textOverlay.setText("kupa");
            container.addView(textOverlay);
        }
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

    private CameraPreview getCameraPreview() {
        return (CameraPreview) findViewById(R.id.cameraPreview);
    }

    private void processSensors() {
        if (lastGravity != null && lastGeomagnetic != null && lastPosition != null) {
            XYZLocation testPosition = LocationPoint.getKarolinChimneyLocation().getXYZPosition(lastAltitude != null);
            testPosition.x -= lastPosition.x;
            testPosition.y -= lastPosition.y;
            testPosition.z -= lastPosition.z;

            float[] R = new float[9];
            float[] I = new float[9];
            SensorManager.getRotationMatrix(R, I, lastGravity, lastGeomagnetic);

            double[] newPosition = new double[]{
                    testPosition.x * R[0] + testPosition.y * R[3] + testPosition.z * R[6],
                    testPosition.x * R[1] + testPosition.y * R[4] + testPosition.z * R[7],
                    testPosition.x * R[2] + testPosition.y * R[5] + testPosition.z * R[8]
            };

            if (newPosition[2] == 0.) {
                return;
            }

            double xOnScreen = toDegrees(atan(newPosition[1] / newPosition[2])) / camera.getParameters().getHorizontalViewAngle() * getCameraPreview().getWidth() / 2;
            double yOnScreen = toDegrees(atan(newPosition[0] / newPosition[2])) / camera.getParameters().getVerticalViewAngle() * getCameraPreview().getHeight() / 2;

            TextView text= (TextView) findViewById(2141523);
            text.setText("X: " + newPosition[0] + "\nY: " + newPosition[1] + "\nZ: " + newPosition[2]
                    + "\nPosition: " + xOnScreen + " Vertical:" + yOnScreen
            );
        }
    }
}
