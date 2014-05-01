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

package fi.aalto.legroup.achso.remote;

import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.util.xml.XmlObject;
import fi.aalto.legroup.achso.util.xml.XmlSerializableFactory;

public class RemoteAnnotationFactory implements XmlSerializableFactory {
    @Override
    public RemoteAnnotation fromXmlObject(XmlObject obj) {
        String text = null;
        float xpos = 0;
        float ypos = 0;
        long starttime = 0;
        long duration = 0;
        float scale = (float) 1.0;

        if (!obj.getName().equals("annotation")) {
            return null;
        }
        for (XmlObject o : obj.getSubObjects()) {
            if (o.getName().equals("text")) {
                text = o.getText();
            } else if (o.getName().equals("x_position")) {
                xpos = Float.parseFloat(o.getText());
            } else if (o.getName().equals("y_position")) {
                ypos = Float.parseFloat(o.getText());
            } else if (o.getName().equals("start_time")) {
                starttime = Long.parseLong(o.getText());
            } else if (o.getName().equals("duration")) {
                duration = Long.parseLong(o.getText());
            } else if (o.getName().equals("scale")) {
                scale = Float.parseFloat(o.getText());
            }
        }

        return new RemoteAnnotation(starttime, duration, text, new FloatPosition(xpos, ypos),
                scale);
    }
}
