package iswd.aarol.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import iswd.aarol.R;
import iswd.aarol.activity.MainActivity;
import iswd.aarol.model.LocationPackage;
import iswd.aarol.model.LocationPoint;
import iswd.aarol.model.PackageManager;
import iswd.aarol.model.XYZLocation;

import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.min;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class OverlayView extends View {
    private boolean ready = false;

    private Paint waitingTextPaint = null;
    private Paint circlesPaint = null;
    private boolean paintsPrepared = false;

    private Map<LocationPoint, double[]> oldPositions = null;

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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        oldPositions = null;
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

        if (oldPositions == null) {
            oldPositions = new HashMap<LocationPoint, double[]>();
        }

        MainActivity activity = (MainActivity) getContext();

        if (activity.getCamera() == null) {
            return;
        }

        final LocationPoint lastLocation = activity.getLastLocation();
        final float[] lastGravity = activity.getLastGravity();
        final float[] lastGeomagnetic = activity.getLastGeomagnetic();

        float[] transformationMatrix = new float[9];
        SensorManager.getRotationMatrix(transformationMatrix, null, lastGravity, lastGeomagnetic);

        boolean enableStabilization = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("enable_stabilization", true);

        if (activity.getLeadToLocation() == null) {
            for (LocationPackage locationPackage : activity.getEnabledPackages()) {
                circlesPaint.setColor(PackageManager.getColorOfPackage(getContext(), locationPackage.getPackageName()));
                for (LocationPoint locationPoint : locationPackage.getLocations()) {
                    double[] relativePosition = getRelativePosition(lastLocation, transformationMatrix, locationPoint);
                    if (enableStabilization) {
                        if (oldPositions.containsKey(locationPoint)) {
                            double[] oldPosition = oldPositions.get(locationPoint);
                            for (int i = 0; i < 3; i++) {
                                relativePosition[i] = relativePosition[i] * 0.2 + oldPosition[i] * 0.8;
                            }
                        }
                        oldPositions.put(locationPoint, relativePosition.clone());
                    }
                    drawLocation(activity, canvas, relativePosition);
                }
            }
        } else {
            double[] relativePosition = getRelativePosition(lastLocation, transformationMatrix, activity.getLeadToLocation());
            double angle = toDegrees(atan2(relativePosition[1], relativePosition[2]));
            drawArrow(canvas, angle);
        }
    }

    private void drawArrow(Canvas canvas, double degrees) {
        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        canvas.rotate((float) degrees);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_up);
        int halfOfBorder = min(getWidth(), getHeight()) / 4;
        canvas.drawBitmap(bitmap,
                new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                new Rect(-halfOfBorder, -halfOfBorder, halfOfBorder, halfOfBorder),
                null);

        canvas.restore();
    }

    private void drawLocation(MainActivity mainActivity, Canvas canvas, double[] relativePosition) {

        if (relativePosition[2] == 0.) {
            return;
        }

        //Check if we are facing the target
        if (relativePosition[2] > 0.) {
            return;
        }

        double xOnScreen = toDegrees(atan(relativePosition[1] / relativePosition[2])) / mainActivity.getCamera().getParameters().getHorizontalViewAngle() * mainActivity.getCameraPreview().getWidth();
        double yOnScreen = toDegrees(atan(relativePosition[0] / relativePosition[2])) / mainActivity.getCamera().getParameters().getVerticalViewAngle() * mainActivity.getCameraPreview().getHeight();

        canvas.drawCircle((float) (getWidth() / 2 + xOnScreen), (float) (getHeight() / 2 + yOnScreen), 10, circlesPaint);
    }

    private double[] getRelativePosition(LocationPoint lastLocation, float[] transformationMatrix, LocationPoint targetLocation) {
        boolean useAltitude = lastLocation.hasAltitude() && targetLocation.hasAltitude();

        XYZLocation testXYZ = targetLocation.getXYZPosition(useAltitude);

        XYZLocation translatedTestXYZ = testXYZ.rotateAlongY(-toRadians(lastLocation.getLongitude())).rotateAlongX(toRadians(lastLocation.getLatitude()));

        translatedTestXYZ.z -= lastLocation.getEarthRadius();
        if (useAltitude)
            translatedTestXYZ.z -= lastLocation.getAltitude();

        return new double[]{
                translatedTestXYZ.x * transformationMatrix[0] + translatedTestXYZ.y * transformationMatrix[3] + translatedTestXYZ.z * transformationMatrix[6],
                translatedTestXYZ.x * transformationMatrix[1] + translatedTestXYZ.y * transformationMatrix[4] + translatedTestXYZ.z * transformationMatrix[7],
                translatedTestXYZ.x * transformationMatrix[2] + translatedTestXYZ.y * transformationMatrix[5] + translatedTestXYZ.z * transformationMatrix[8]
        };
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
        circlesPaint.setStrokeWidth(2);
    }
}
