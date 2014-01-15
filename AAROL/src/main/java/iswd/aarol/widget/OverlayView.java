package iswd.aarol.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import iswd.aarol.R;
import iswd.aarol.activity.MainActivity;
import iswd.aarol.model.LocationPackage;
import iswd.aarol.model.LocationPoint;
import iswd.aarol.model.PackageManager;
import iswd.aarol.model.XYZLocation;

import static android.util.FloatMath.cos;
import static android.util.FloatMath.sin;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class OverlayView extends View {
    private boolean ready = false;

    private Paint waitingTextPaint = null;
    private Paint circlesPaint = null;
    private Paint detailsPaint = null;
    private Paint detailsPaintWithLink = null;
    private Paint distanceTextPaint;

    LocationPoint pointWithDetails = null;
    LocationPackage packageOfPointWithDetails = null;

    private RectF detailsBox = null;
    private Map<LocationPoint, double[]> oldPositions = null;
    private Paint detailsBackgroundPaint = null;
    private float minRelativeDistance;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        oldPositions = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

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
            //Parameters of the point, that will have it's description shown
            pointWithDetails = null;
            float minDistanceFromScreenCenter = Float.POSITIVE_INFINITY;
            float[] minLocation = null;

            for (LocationPackage locationPackage : activity.getEnabledPackages()) {
                circlesPaint.setColor(PackageManager.getColorOfPackage(getContext(), locationPackage.getPackageName()));
                for (LocationPoint locationPoint : locationPackage.getLocations()) {
                    double[] relativePosition = getRelativePosition(lastLocation, transformationMatrix, locationPoint);
                    float relativeDistance = FloatMath.sqrt((float) (relativePosition[0] * relativePosition[0]
                            + relativePosition[1] * relativePosition[1]
                            + relativePosition[2] * relativePosition[2]));

                    if (enableStabilization) {
                        if (oldPositions.containsKey(locationPoint)) {
                            double[] oldPosition = oldPositions.get(locationPoint);
                            for (int i = 0; i < 3; i++) {
                                relativePosition[i] = relativePosition[i] * 0.2 + oldPosition[i] * 0.8;
                            }
                        }
                        oldPositions.put(locationPoint, relativePosition.clone());
                    }
                    float[] drawLocation = drawLocation(activity, canvas, relativePosition);

                    if (drawLocation != null) {
                        float distanceFromScreenCenter = FloatMath.sqrt(drawLocation[0] * drawLocation[0] + drawLocation[1] * drawLocation[1]);
                        if (distanceFromScreenCenter < minDistanceFromScreenCenter) {
                            pointWithDetails = locationPoint;
                            packageOfPointWithDetails = locationPackage;
                            minDistanceFromScreenCenter = distanceFromScreenCenter;
                            minRelativeDistance = relativeDistance; // relative distance is counted before stabilisation
                            minLocation = drawLocation;
                        }
                    }
                }
            }

            if (pointWithDetails != null) {
                drawDetails(canvas, pointWithDetails, minLocation, true);
            } else {
                detailsBox = null;
            }
            ((MainActivity) getContext()).findViewById(R.id.lead_to_button).setEnabled(pointWithDetails != null);
        } else {
            double[] relativePosition = getRelativePosition(lastLocation, transformationMatrix, activity.getLeadToLocation());
            double angle = toDegrees(atan2(relativePosition[1], relativePosition[2]));
            drawArrow(canvas, angle);
            drawDetails(canvas, activity.getLeadToLocation(), new float[]{0, getHeight() / 4 - 20}, false);
        }
    }

    private void drawDetails(Canvas canvas, LocationPoint locationPoint, float[] onScreenLocation, boolean highlight) {
        if (highlight) {
            circlesPaint.setColor(Color.WHITE);
            canvas.drawCircle(getWidth() / 2 + onScreenLocation[0], getHeight() / 2 + onScreenLocation[1], 7, circlesPaint);
            circlesPaint.setColor(Color.BLACK);
            canvas.drawCircle(getWidth() / 2 + onScreenLocation[0], getHeight() / 2 + onScreenLocation[1], 3, circlesPaint);
            circlesPaint.setColor(Color.WHITE);
            canvas.drawCircle(getWidth() / 2 + onScreenLocation[0], getHeight() / 2 + onScreenLocation[1], 16, circlesPaint);
            circlesPaint.setColor(Color.BLACK);
            canvas.drawCircle(getWidth() / 2 + onScreenLocation[0], getHeight() / 2 + onScreenLocation[1], 13, circlesPaint);
        }

        Paint paintForNameText;
        if (locationPoint.getWikipediaLink() != null && !locationPoint.getWikipediaLink().isEmpty()) {
            paintForNameText = detailsPaintWithLink;
        } else {
            paintForNameText = detailsPaint;
        }

        paintForNameText.setTextSize(45);
        String description = locationPoint.getName();
        while (paintForNameText.measureText(description) > getWidth() * .7) {
            paintForNameText.setTextSize(paintForNameText.getTextSize() * 0.95f);
        }

        Rect bounds = new Rect();
        paintForNameText.getTextBounds(description, 0, description.length(), bounds);

        float textPositionX = getWidth() / 2f + onScreenLocation[0] - bounds.right / 2f;
        textPositionX = min(textPositionX, getWidth() - 20 - bounds.right);
        textPositionX = max(textPositionX, 20);

        float textPositionY = getHeight() / 2f - bounds.top + onScreenLocation[1] + 30;

        if (textPositionY > getHeight() - 50) {
            textPositionY = getHeight() / 2f + onScreenLocation[1] - 30;
        }

        detailsBox = new RectF(textPositionX - 8, textPositionY + bounds.top - 8,
                textPositionX + bounds.right + 8, textPositionY + 30 + 12);
        canvas.drawRoundRect(detailsBox, 5f, 5f, detailsBackgroundPaint);

        canvas.drawText(description, textPositionX, textPositionY, paintForNameText);
        String distanceText;
        if (minRelativeDistance == 0) {
            distanceText = "";
        } else if (minRelativeDistance < 1000) {
            distanceText = "" + (int) minRelativeDistance + " m";
        } else if (minRelativeDistance < 10000) {
            distanceText = "" + String.format("%.1f", minRelativeDistance / 1000) + " km";
        } else {
            distanceText = "" + (int) minRelativeDistance / 1000 + " km";
        }
        canvas.drawText("" + distanceText, textPositionX, textPositionY + 30, distanceTextPaint);
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

    private float[] drawLocation(MainActivity mainActivity, Canvas canvas, double[] relativePosition) {

        //Check if we are facing the target
        if (relativePosition[2] >= 0.) {
            return null;
        }

        float xAngle = (float) toDegrees(atan(relativePosition[1] / relativePosition[2]));
        float yAngle = (float) toDegrees(atan(relativePosition[0] / relativePosition[2]));
        float cameraXAngle = mainActivity.getCamera().getParameters().getHorizontalViewAngle();
        float cameraYAngle = mainActivity.getCamera().getParameters().getVerticalViewAngle();
        float xOnScreen = xAngle / cameraXAngle * mainActivity.getCameraPreview().getWidth();
        float yOnScreen = yAngle / cameraYAngle * mainActivity.getCameraPreview().getHeight();

        if (abs(xOnScreen) > getWidth() / 2 - 10 || abs(yOnScreen) > getHeight() / 2 - 10) {
            printBorderArrow(canvas, (float) toDegrees(atan2(yOnScreen, xOnScreen)));
            return null;
        } else {
            canvas.drawCircle(getWidth() / 2 + xOnScreen, getHeight() / 2 + yOnScreen, 10, circlesPaint);
        }
        return new float[]{xOnScreen, yOnScreen};
    }

    private void printBorderArrow(Canvas canvas, float angle) {
        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        canvas.rotate(angle);
        angle = abs(angle) % 180;
        if (angle > 90)
            angle = 180 - angle;
        angle = (float) toRadians(angle);
        float radiusToBorder = angle < atan(getHeight() / (float) getWidth()) ? getWidth() / 2 / cos(angle) : getHeight() / 2 / sin(angle);
        canvas.translate(radiusToBorder, 0);
        canvas.drawLines(new float[]{
                0, 0, -25, 0,
                0, 0, -15, 10,
                0, 0, -15, -10},
                circlesPaint);
        canvas.restore();
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

    private void init() {
        waitingTextPaint = new Paint();
        waitingTextPaint.setTextAlign(Paint.Align.CENTER);
        waitingTextPaint.setTextSize(30);
        waitingTextPaint.setColor(Color.RED);
        waitingTextPaint.setShadowLayer(5f, 0f, 0f, Color.WHITE);

        circlesPaint = new Paint();
        circlesPaint.setStyle(Paint.Style.STROKE);
        circlesPaint.setStrokeWidth(3);

        detailsPaint = new Paint();
        detailsPaint.setColor(Color.BLACK);

        detailsPaintWithLink = new Paint();
        detailsPaintWithLink.setColor(Color.BLUE);
        detailsPaintWithLink.setFlags(Paint.UNDERLINE_TEXT_FLAG);

        distanceTextPaint = new Paint();
        distanceTextPaint.setColor(Color.DKGRAY);
        distanceTextPaint.setTextSize(25);

        detailsBackgroundPaint = new Paint();
        detailsBackgroundPaint.setStyle(Paint.Style.FILL);
        detailsBackgroundPaint.setColor(Color.argb(200, 180, 180, 180));
        detailsBackgroundPaint.setShadowLayer(5f, 0f, 0f, Color.WHITE);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                if (detailsBox == null || !detailsBox.contains(event.getX(), event.getY())) {
                    return false;
                }
                String wikipediaLink = pointWithDetails.getWikipediaLink();
                if (wikipediaLink == null || wikipediaLink.isEmpty()) {
                    return false;
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(wikipediaLink));
                getContext().startActivity(browserIntent);
                return true;
            }
        });
    }

    public LocationPoint getPointWithDetails() {
        return pointWithDetails;
    }

    public LocationPackage getPackageOfPointWithDetails() {
        return packageOfPointWithDetails;
    }
}
