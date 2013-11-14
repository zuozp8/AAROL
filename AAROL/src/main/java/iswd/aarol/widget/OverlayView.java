package iswd.aarol.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import iswd.aarol.LocationPoint;
import iswd.aarol.MainActivity;
import iswd.aarol.R;
import iswd.aarol.XYZLocation;

import static java.lang.Math.atan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class OverlayView extends View {
    private boolean ready = false;

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

        if (!ready)
            return;
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

        double xOnScreen = toDegrees(atan(newPosition[1] / newPosition[2])) / activity.getCamera().getParameters().getHorizontalViewAngle() * activity.getCameraPreview().getWidth();
        double yOnScreen = toDegrees(atan(newPosition[0] / newPosition[2])) / activity.getCamera().getParameters().getVerticalViewAngle() * activity.getCameraPreview().getHeight();

        TextView text = (TextView) activity.findViewById(R.id.textView);
        text.setText("X: " + newPosition[0] + "\nY: " + newPosition[1] + "\nZ: " + newPosition[2]
                + "\nPosition: " + xOnScreen + " Vertical:" + yOnScreen
        );

        if (newPosition[2] > 0.) {
            return;
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawCircle((float) (getWidth() / 2 + xOnScreen), (float) (getHeight() / 2 + yOnScreen), 10, paint);
    }

    public void setReady() {
        ready = true;
    }
}
