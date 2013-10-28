package iswd.aarol;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import iswd.aarol.widget.CameraPreview;

public class MainActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.container);
        preview.addView(mPreview);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // release the camera immediately on pause event
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

}
