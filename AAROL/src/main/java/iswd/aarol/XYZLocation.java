package iswd.aarol;

import static java.lang.Math.*;

public class XYZLocation {
    public double x, y, z;

    public XYZLocation(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @param angle in radians
     */
    public XYZLocation rotateAlongY(double angle) {
        return new XYZLocation(
                x * cos(angle) + z * sin(angle),
                y,
                z * cos(angle) - x * sin(angle)
        );
    }

    /**
     * @param angle in radians
     */
    public XYZLocation rotateAlongX(double angle) {
        return new XYZLocation(
                x,
                y * cos(angle) - z * sin(angle),
                z * cos(angle) + y * sin(angle)
        );
    }
}
