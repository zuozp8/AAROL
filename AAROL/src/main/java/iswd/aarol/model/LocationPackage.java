package iswd.aarol.model;

import java.util.ArrayList;
import java.util.List;

public class LocationPackage {

    private String packageName = "";
    private String creatorName = "";
    private String description = "";

    private List<LocationPoint> locations = new ArrayList<LocationPoint>();

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<LocationPoint> getLocations() {
        return locations;
    }

    public void addLocation(LocationPoint locationPoint) {
        locations.add(locationPoint);
    }
}
