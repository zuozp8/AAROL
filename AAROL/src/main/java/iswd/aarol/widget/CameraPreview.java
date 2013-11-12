package iswd.aarol.widget;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreview extends SurfaceView {
    private Camera cameraInstance = null;
    private boolean surfaceCreated = false;


    public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public CameraPreview(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        SurfaceHolder holder = getHolder();
        assert holder != null;
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceCreated = true;
                manageCameraPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                manageCameraPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceCreated = false;
                // Take care of releasing the Camera preview in your activity.
            }
        });
    }

    public void setCamera(Camera camera) {
        cameraInstance = camera;
        manageCameraPreview();
    }

    private void manageCameraPreview() {
        if (cameraInstance != null && surfaceCreated) {

            try {
                cameraInstance.stopPreview();
            } catch (Exception e) {
            }
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                cameraInstance.setPreviewDisplay(getHolder());
                cameraInstance.startPreview();
            } catch (IOException e) {
                Log.e("CameraPreview", "Error setting camera preview: " + e.getMessage());
            }
        }
    }
}
