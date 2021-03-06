package iswd.aarol.model;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class RepositoryFactory extends AbstractFromXMLFactory {

    public static Repository createFromXML(InputStream content) throws IOException, XmlPullParserException {
        XmlPullParser parser = initializeParser(content, "packageList");

        Repository result = new Repository();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            result.add(readEntry(parser));
        }
        return result;
    }

    private static LocationPackageSnippet readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, "package");
        LocationPackageSnippet result = new LocationPackageSnippet();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("packageName")) {
                result.name = readTag(parser, name);
            } else if (name.equals("description")) {
                result.description = readTag(parser, name);
            } else if (name.equals("creatorName")) {
                result.creatorName = readTag(parser, name);
            } else {
                throw new XmlPullParserException("Unexpected tag: " + name);
            }
        }
        return result;
    }


}
