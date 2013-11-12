package iswd.aarol;

import android.location.Location;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

public class LocationPoint {

    private final static double WGS84_A = 6378137.; //WGS84 major axis, radius of earth on equator
    private final static double WGS84_B = 6356752.314245; //WGS84 semi-major axis, radius of earth on poles

    private double latitude, longitude, altitude;
    private boolean hasAltitude = false;

    public class XYZLocation {
        double x, y, z;
        public XYZLocation(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public LocationPoint(Location l) {
        latitude = l.getLatitude();
        longitude = l.getLongitude();
        altitude = l.getAltitude();
        hasAltitude = l.hasAltitude();
    }

    public LocationPoint(double latitude, double longitude, double altitude) {
        this(latitude, longitude);
        this.altitude = altitude;
        hasAltitude = true;
    }

    public LocationPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        altitude = 0;
        hasAltitude = false;
    }

    public static LocationPoint getKarolinChimneyLocation() {
        return new LocationPoint(52.436411, 16.988583, 83);
    }

    public XYZLocation getXYZPosition() {
        return getXYZPosition(true);
    }

    public XYZLocation getXYZPosition(boolean useAltitude) {
        double latitude = toRadians(this.latitude);
        double longitude = toRadians(this.longitude);

        double radius = WGS84_A * WGS84_B / sqrt(
                pow(WGS84_B * cos(latitude), 2) +
                        pow(WGS84_A * sin(latitude), 2)
        );
        if (useAltitude) {
            radius += this.altitude;
        }

        double y = radius * sin(latitude);
        double z = radius * cos(latitude) * cos(longitude);
        double x = radius * cos(latitude) * sin(longitude);

        return new XYZLocation(x, y, z);
    }
}
