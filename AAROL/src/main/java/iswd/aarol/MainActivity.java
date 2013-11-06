package iswd.aarol;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import iswd.aarol.widget.CameraPreview;

public class MainActivity extends Activity {

    private Camera camera = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("AAROL", "OnCreate");

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

        Log.e("AAROL", "Camera: " + camera);
        Log.e("AAROL", "Preview: " + getCameraPreview());

        //prepare camera
        camera = Camera.open();

        //Search for existing preview
        CameraPreview preview = getCameraPreview();
        if (preview == null) {
            preview = new CameraPreview(this, camera);
            FrameLayout container = (FrameLayout) findViewById(R.id.container);
            Log.e("AAROL", "Container has: " + container.getChildCount());
            container.removeAllViews();
            container.addView(preview);
        } else {
            preview.setCamera(camera);
        }
    }

    private CameraPreview getCameraPreview() {
        return (CameraPreview) findViewById(R.id.cameraPreview);
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
    }

}
