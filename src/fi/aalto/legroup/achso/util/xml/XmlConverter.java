/**
 * Copyright 2013 Aalto university, see AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.util.xml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;

public class XmlConverter {

    public static String bitmapToBase64(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 40, stream);
        byte[] b = stream.toByteArray();
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }

    public static Bitmap base64ToBitmap(String base64) {
        byte[] b = Base64.decode(base64, 0);
        return BitmapFactory.decodeByteArray(b, 0, b.length);
    }

    private static void addTagsFromObject(XmlSerializer s, String ns, XmlObject o) throws IOException {
        s.startTag(ns, o.getName());
        for (String attrKey : o.getAttributes().keySet()) {
            s.attribute(ns, attrKey, o.getAttributes().get(attrKey));
        }
        if (o.getText() != null) {
            s.text(o.getText());
        }
        for (XmlObject xo : o.getSubObjects()) {
            addTagsFromObject(s, ns, xo);
        }
        s.endTag(ns, o.getName());
    }

    public static byte[] toXML(Context context, XmlObject obj) {
        XmlSerializer s = Xml.newSerializer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String ns = null; // no namespace
        try {
            s.setOutput(out, "UTF-8");
            s.startDocument("UTF-8", null);
            addTagsFromObject(s, ns, obj);
            s.endDocument();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] toXML(Context context, XmlSerializable obj) {
        return toXML(context, obj.getXmlObject(context));
    }

    public static XmlObject fromXml(String xml) {
        LinkedList<XmlObject> hierarchy = new LinkedList<XmlObject>();
        XmlObject ret = null;
        XmlObject current = null;
        try {
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
            xpp.setInput(new StringReader(xml));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (ret == null) {
                            ret = new XmlObject(xpp.getName());
                            current = ret;
                            hierarchy.add(ret);
                        } else {
                            current = new XmlObject(xpp.getName());
                            hierarchy.getLast().addSubObject(current);
                            hierarchy.add(current);
                        }
                        for (int i = 0; i < xpp.getAttributeCount(); ++i) {
                            current.addAttribute(xpp.getAttributeName(i), xpp.getAttributeValue(i));
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        hierarchy.removeLast();
                        if (hierarchy.size() > 0) {
                            current = hierarchy.getLast();
                        }
                        break;
                    case XmlPullParser.TEXT:
                        current.setText(xpp.getText());
                        break;
                }
                eventType = xpp.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
