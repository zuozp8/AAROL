package iswd.aarol.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import iswd.aarol.R;
import iswd.aarol.activity.MainActivity;
import iswd.aarol.model.LocationPoint;
import iswd.aarol.model.XYZLocation;

import static java.lang.Math.atan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class OverlayView extends View {
    private boolean ready = false;

    private Paint waitingTextPaint = null;
    private Paint circlesPaint = null;
    private boolean paintsPrepared = false;

    public OverlayView(Context context) {
        super(context);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!paintsPrepared) {
            preparePaints();
        }

        if (!ready) {
            String message = getResources().getString(R.string.waiting_for_signals);
            canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, waitingTextPaint);
            return;
        }
        ready = false;

        MainActivity activity = (MainActivity) getContext();

        final LocationPoint lastLocation = activity.getLastLocation();
        final float[] lastGravity = activity.getLastGravity();
        final float[] lastGeomagnetic = activity.getLastGeomagnetic();

        LocationPoint testLocation = LocationPoint.getKarolinChimneyLocation();

        boolean useAltitude = lastLocation.hasAltitude() && testLocation.hasAltitude();

        XYZLocation testXYZ = testLocation.getXYZPosition(useAltitude);

        XYZLocation translatedTestXYZ = testXYZ.rotateAlongY(-toRadians(lastLocation.getLongitude())).rotateAlongX(toRadians(lastLocation.getLatitude()));

        translatedTestXYZ.z -= lastLocation.getEarthRadius();

        float[] transformationMatrix = new float[9];
        SensorManager.getRotationMatrix(transformationMatrix, null, lastGravity, lastGeomagnetic);

        double[] newPosition = new double[]{
                translatedTestXYZ.x * transformationMatrix[0] + translatedTestXYZ.y * transformationMatrix[3] + translatedTestXYZ.z * transformationMatrix[6],
                translatedTestXYZ.x * transformationMatrix[1] + translatedTestXYZ.y * transformationMatrix[4] + translatedTestXYZ.z * transformationMatrix[7],
                translatedTestXYZ.x * transformationMatrix[2] + translatedTestXYZ.y * transformationMatrix[5] + translatedTestXYZ.z * transformationMatrix[8]
        };

        if (newPosition[2] == 0.) {
            return;
        }

        if (activity.getCamera() == null) {
            return;
        }

        double xOnScreen = toDegrees(atan(newPosition[1] / newPosition[2])) / activity.getCamera().getParameters().getHorizontalViewAngle() * activity.getCameraPreview().getWidth();
        double yOnScreen = toDegrees(atan(newPosition[0] / newPosition[2])) / activity.getCamera().getParameters().getVerticalViewAngle() * activity.getCameraPreview().getHeight();

        if (newPosition[2] > 0.) {
            return;
        }

        canvas.drawCircle((float) (getWidth() / 2 + xOnScreen), (float) (getHeight() / 2 + yOnScreen), 10, circlesPaint);
    }

    public void setReady() {
        ready = true;
    }

    private void preparePaints() {
        waitingTextPaint = new Paint();
        waitingTextPaint.setTextAlign(Paint.Align.CENTER);
        waitingTextPaint.setTextSize(30);
        waitingTextPaint.setColor(Color.RED);
        waitingTextPaint.setShadowLayer(5f, 0f, 0f, Color.WHITE);

        circlesPaint = new Paint();
        circlesPaint.setStyle(Paint.Style.STROKE);
        circlesPaint.setColor(Color.RED);
        circlesPaint.setStrokeWidth(2);
    }
}
