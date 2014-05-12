/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
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

import fi.aalto.legroup.achso.adapter.BrowsePagerAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;

public class i5Connection implements Connection {

    private static Connection con;
    private String errorCode;
    private boolean connected;

    /**
     * Constructor
     */
    public i5Connection() {
        this.errorCode = new String();
        this.connected = false;

    }


    /**
     * Method to get video data that can be later expanded to a list of SemanticVideos
     * When this gets called we should already be in asynchronous thread.
     * @param query_type -- query types are defined at BrowsePagerAdapter,
     *                   but the actual implementation for formulating the query in such way that
     *                   the server can understand it is done here.
     *
     * @param query -- if there are keywords to search, locations etc. give them here. (we can
     *              change this to List<String> if necessary.)
     * @return
     */
    @Override
    public List<SemanticVideo> getVideos(int query_type, String query) {
        List<SemanticVideo> res = new ArrayList<SemanticVideo>();
        // prepare authentication arguments for http GET

        // prepare url to call and its search arguments
        switch (query_type) {
            case BrowsePagerAdapter.SEARCH:
                break;
            case BrowsePagerAdapter.QR_SEARCH:
                break;
            case BrowsePagerAdapter.LATEST:
                break;
            case BrowsePagerAdapter.MY_VIDEOS:
                break;
            case BrowsePagerAdapter.RECOMMENDED:
                break;
            case BrowsePagerAdapter.BROWSE_BY_GENRE:
                break;
            case BrowsePagerAdapter.NEARBY:
                break;
        }
        // start building http GET


        // execute GET


        // handle results

        return res;
    }
}
