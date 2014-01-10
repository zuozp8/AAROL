package iswd.aarol.model;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class LocationPackageFactory extends AbstractFromXMLFactory {


    public static LocationPackage create(Context context, String name) throws IOException, XmlPullParserException {
        InputStream content = context.openFileInput(PackageManager.getFileNameOfPackage(name));
        return createFromXML(content);
    }

    protected static LocationPackage createFromXML(InputStream content) throws IOException, XmlPullParserException {
        XmlPullParser parser = initializeParser(content, "package");

        LocationPackage result = new LocationPackage();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("packageName")) {
                result.setPackageName(readTag(parser, name));
            } else if (name.equals("creatorName")) {
                result.setCreatorName(readTag(parser, name));
            } else if (name.equals("description")) {
                result.setDescription(readTag(parser, name));
            } else if (name.equals("location")) {
                result.addLocation(readEntry(parser));
            } else {
                throw new XmlPullParserException("Unexpected tag: " + name);
            }
        }
        return result;
    }

    private static LocationPoint readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, "location");
        double latitude = 0., longitude = 0., altitude = 0.;
        String locationName = null, wikipediaLink = null;
        boolean hasAltitude = false;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            try {
                String name = parser.getName();
                if (name.equals("name")) {
                    locationName = readTag(parser, name);
                } else if (name.equals("longitude")) {
                    longitude = Double.parseDouble(readTag(parser, name));
                } else if (name.equals("latitude")) {
                    latitude = Double.parseDouble(readTag(parser, name));
                } else if (name.equals("altitude")) {
                    altitude = Double.parseDouble(readTag(parser, name));
                    hasAltitude = true;
                } else if (name.equals("wikipediaLink")) {
                    wikipediaLink = readTag(parser, name);
                } else {
                    throw new XmlPullParserException("Unexpected tag: " + name);
                }
            } catch (NumberFormatException e) {
                throw new XmlPullParserException("Number parsing error");
            }
        }
        LocationPoint result;
        if (hasAltitude) {
            result = new LocationPoint(latitude, longitude, altitude);
        } else {
            result = new LocationPoint(latitude, longitude);
        }
        if (locationName != null) {
            result.setName(locationName);
        }
        if (wikipediaLink != null) {
            result.setWikipediaLink(wikipediaLink);
        }
        return result;
    }
}
