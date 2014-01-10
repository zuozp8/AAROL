package iswd.aarol.model;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class AbstractFromXMLFactory {
    protected static String NS = ""; //XML namespace

    protected static String readTag(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, name);
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, NS, name);
        return text;
    }

    protected static XmlPullParser initializeParser(InputStream content, String rootElementName) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(content, null);
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, NS, rootElementName);
        return parser;
    }
}
