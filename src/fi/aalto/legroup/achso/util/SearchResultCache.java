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

package fi.aalto.legroup.achso.util;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.legroup.achso.database.SemanticVideo;

public class SearchResultCache {
    static ArrayList<SemanticVideo> lastSearch;

    static {
        lastSearch = null;
    }

    public static List<SemanticVideo> getLastSearch() {
        return lastSearch;
    }

    public static void clearLastSearch() {
        lastSearch = null;
    }
}
