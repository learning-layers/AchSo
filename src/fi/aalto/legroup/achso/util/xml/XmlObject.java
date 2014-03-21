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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class XmlObject {
    private String mName;
    private String mText;
    private LinkedHashMap<String, String> mAttributes;
    private List<XmlObject> mSubObjects;

    public XmlObject(String name) {
        mName = name;
        mText = null;
        mAttributes = new LinkedHashMap<String, String>();
        mSubObjects = new ArrayList<XmlObject>();
    }

    public XmlObject addAttribute(String name, String value) {
        mAttributes.put(name, value);
        return this;
    }

    public XmlObject addSubObject(String key, String value) {
        mSubObjects.add(new XmlObject(key).setText(value));
        return this;
    }

    public XmlObject addSubObject(XmlObject o) {
        mSubObjects.add(o);
        return this;
    }

    public String getName() {
        return mName;
    }

    public String getText() {
        return mText;
    }

    public XmlObject setText(String text) {
        mText = text;
        return this;
    }

    public LinkedHashMap<String, String> getAttributes() {
        return mAttributes;
    }

    public List<XmlObject> getSubObjects() {
        return mSubObjects;
    }
}
