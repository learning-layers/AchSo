package fi.aalto.legroup.achso.util;


import fi.aalto.legroup.achso.adapter.BrowsePagerAdapter;

public class i5Connection {

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

    public static String getVideos(int query_type, String query) {
        String xml = "";
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

        return xml;
    }
}
