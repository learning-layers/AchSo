/**
 * Copyright 2013 Chair for Information Systems and Databases, RWTH Aachen University
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

import android.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.Date;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import fi.aalto.legroup.achso.adapter.BrowsePagerAdapter;
import i5.las.httpConnector.client.AccessDeniedException;
import i5.las.httpConnector.client.AuthenticationFailedException;
import i5.las.httpConnector.client.Client;
import i5.las.httpConnector.client.ConnectorClientException;
import i5.las.httpConnector.client.NotFoundException;
import i5.las.httpConnector.client.ServerErrorException;
import i5.las.httpConnector.client.TimeoutException;
import i5.las.httpConnector.client.UnableToConnectException;

// TODO: Auto-generated Javadoc

/**
 * The Class LasConnection.
 */
public class LasConnection implements Connection {

    public static final String CONNECTION_PROBLEM = "An error accoured when connectiong to web please check your internet conenction settings";
    public static final String AUTHENTICATION_PROBLEM = "wrong username or password, please try again";
    public static final String UNDEFINED_PROBLEM = "an exception occured, please try again";
    private static LasConnection con = null;
    // FIELDS
    // -------------------------------------------------------------------
    private Client client;
    private String errorCode;
    private boolean connected;
    private String basedir;
    private String sessionId;

    /**
     * password , used for initial testing purposes TODO replace with openId,
     * OAuth server/client communication
     */
    private String pass;

    /**
     * Creates a new instance of this class with empty values.
     */
    public LasConnection() {
        this.errorCode = new String();
        this.connected = false;

    }

    // CONSTRUCTORS
    // -------------------------------------------------------------

    /**
     * Gets the connection.
     *
     * @return the connection
     */
    public static LasConnection getConnection() {
        if (con == null) {
            con = new LasConnection();
        }

        return con;
    }

    // GETTERS
    // ------------------------------------------------------------------

    /**
     * Checks wheter a LAS connection is active method
     * {@code continueConnection()}.
     *
     * @return the status of the LAS connection.
     */
    public boolean isConnected() {
        try {
            // TODO add behavior
        } catch (NullPointerException e) {
            System.err.println(e.getMessage());
            Log.e("LasConnection", e.getMessage());
            return false;
        }

        return this.connected;
    }

    /**
     * Determins the error code.
     *
     * @return the error code or an empty string.
     */
    public String getErrorCode() {
        String err = this.errorCode;

        return err;
    }

    /**
     * Sets the error code.
     *
     * @param err the new error code
     */
    public void setErrorCode(String err) {
        this.errorCode = err;
    }

    /**
     * Returns the connector client.
     *
     * @return the client.
     */
    public Client getClient() {
        return client;
    }

    // METHOD
    // -------------------------------------------------------------------

    /**
     * Gets the basedir.
     *
     * @return the basedir
     */
    public String getbasedir() {
        return basedir;
    }

    /**
     * Invokes the specified LAS service method.
     *
     * @param service the service.
     * @param method  the method.
     * @param params  the parameter list.
     * @return the object returned by the service method.
     */
    public Object invoke(String service, String method, Object... params) {
        final String disconnected = "ERR4"; //$NON-NLS-1$
        final String accessDenied = "ERR6"; //$NON-NLS-1$
        final String unknownError = "ERR7"; //$NON-NLS-1$
        final String serverError = "ERR8"; //$NON-NLS-1$
        final String unkownMethod = "ERR9"; //$NON-NLS-1$

        this.errorCode = new String();

        if (client == null) {
            Log.e("LasConnection", "Missing client.");
            return null;
        }

        try {
            Date before = new Date();
            Object res = client.invoke(service, method, params);
            Date after = new Date();

            String s = (after.getTime() - before.getTime()) + "ms: " + service
                    + "." + method;
            Log.i("LasConnection", s);

            return res;
        } catch (AccessDeniedException e) {
            this.errorCode = accessDenied;
            e.printStackTrace();
        } catch (AuthenticationFailedException e) {
            this.errorCode = accessDenied;
            e.printStackTrace();
        } catch (UnableToConnectException e) {
            this.errorCode = disconnected;
            this.connected = false;
            e.printStackTrace();
        } catch (ServerErrorException e) {
            this.errorCode = serverError;
            this.connected = false;
            e.printStackTrace();
        } catch (TimeoutException e) {
            this.errorCode = serverError;
            this.connected = false;
            e.printStackTrace();
        } catch (NotFoundException e) {
            this.errorCode = unkownMethod;
            e.printStackTrace();
        } catch (ConnectorClientException e) {
            this.errorCode = serverError;
            this.connected = false;
            e.printStackTrace();
        } catch (Exception e) {
            this.errorCode = unknownError;
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Checks if for a given media, a media description (metadata) exists
     *
     * @param params the params
     * @return true, if successful
     */
    public boolean existsMediaDescription(Object[] params) {

        boolean resp = false;
        try {

            resp = (Boolean) this.invoke("mpeg7_multimediacontent_service",
                    "existsMediaDescription", params);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (resp) {
            Log.i("LasConnection", "Media uid " + params[0] + " already exists");
            return true;
        } else {
            return false;
        }

    }

    /**
     * Gets the URLs of existing media from the SeViAnno 2.0 Database Each URL
     * is of the form
     * "http://tosini.informatik.rwth-aachen.de:8134/videos/construction.mp4"
     *
     * @return Array containing the video URLs as a String
     */
    public String[] getMediaURLs() {
        String[] urls = null;
        try {

            this.instantiateDBContext();

            urls = (String[]) this.invoke("mpeg7_multimediacontent_service",
                    "getMediaURLs");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return urls;

    }

    /**
     * Gets the xml containing existing media from the SeViAnno 2.0 Database The
     * result contains elements of the form <videos> <video> <title>X</title>
     * <creator>uploader</creator> <video_url>http://...mp4</video_url>
     * <created_at>2011-11-11T10:20:57:728F1000+01:00</created_at> <thumb_image
     * uploader="uploader">http://...d0.jpg</thumb_image> </video> ... </videos>
     *
     * @return XML document representation of videos content
     */
    public String getVideoInformations() {
        String xmlString = null;
        try {

            this.instantiateDBContext();

            // returns a String with the XML containing all the videos and the
            // corresponding data for each video
            // TODO add semantic annotations and genre in the returned XML
            // representation

            // These come from i5.atlas.las.service.mpeg7.multimediacontent
            // MPEG7MultimediaContentService.getVideoInformations
            // What should we invoke to get
            // i5.atlas.las.service.videoinformation.Videoinformation
            //
            xmlString = (String) this.invoke("mpeg7_multimediacontent_service",
                    "getVideoInformations");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return xmlString;
    }

    /**
     * Converts a String into an XML Document TODO use the XmlObject here
     *
     */
    private Document getXMLDocument(String xmlRepresentation) {
        // convert the result into an XML Document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document document = null;
        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(
                    xmlRepresentation)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;

    }

    /**
     * Instantiates the database context
     *
     * @return true, if successful
     */
    public Boolean instantiateDBContext() {
        boolean isInstantiated = false;
        ResourceBundle b;
        b = ResourceBundle.getBundle("achsoLASsettings");
        try {
            String appCode = b.getString("context_appCode"); //$NON-NLS-1$
            String constraints = b.getString("context_constraints"); //$NON-NLS-1$

            Object[] params = {appCode, constraints};

            this.invoke("xmldbxs-context-service", "instantiateContext", params);
            isInstantiated = true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return isInstantiated;

    }

    /**
     * Establishes a connection with the LAS server for the specified user. If
     * the connection cannot be established an error code will be returned:
     * <ul>
     * <li>{@code ERR15} - the port number could not be parse.</li>
     * <li>{@code -1} - Client could not connect to LAS.</li>
     * <li>{@code -2} - User could not be authenticated.</li>
     * <li>{@code -3} - Any other error occurred when connecting.</li>
     * </ul>
     *
     * @param username the username
     * @param password the user's password.
     * @return the session ID or an error code as specified above.
     */
    public String connect(String username, String password) {
        final String configFileErr = "ERR15"; //$NON-NLS-1$
        ResourceBundle b;
        b = ResourceBundle.getBundle("achsoLASsettings"); //$NON-NLS-1$

        String lasHostname;
        int lasPort;
        long timeOut;
        try {
            lasHostname = b.getString("las_host"); //$NON-NLS-1$
            lasPort = Integer.parseInt(b.getString("las_port")); //$NON-NLS-1$
            timeOut = Long.parseLong(b.getString("las_timeout"));

        } catch (Exception e) {
            return configFileErr;
        }

        Log.i("LasConnection", "Connecting LAS Host: " + lasHostname + " port: " + lasPort);

        // userService.setCurrentUser(new User());

        client = new Client(lasHostname, lasPort, timeOut * 1000, username,
                password);
        client.getMobileContext().setApplicationCode("AchSo");

        Log.i("LasConnection", "CLIENT TIMEOUT: " + client.getTimeoutMs());
        try {
            // connect to LAS server
            client.connect();
            this.pass = password;
            this.setSessionId(client.getSessionId());
            // parameters used for determining the database connection to be
            // used to store the metadata
            // below, we use the same DB tables as SeViAnno2.0 version
            String appCode = b.getString("context_appCode"); //$NON-NLS-1$
            String constraints = b.getString("context_constraints"); //$NON-NLS-1$

            Object[] params = {appCode, constraints};

            this.invoke("xmldbxs-context-service", "instantiateContext", params);
            this.connected = true;

        } catch (UnableToConnectException e) {
            e.printStackTrace();
            this.connected = false;
            return CONNECTION_PROBLEM;
        } catch (AuthenticationFailedException e) {
            e.printStackTrace();
            this.connected = false;
            return AUTHENTICATION_PROBLEM;
        } catch (Exception e) {
            e.printStackTrace();
            this.connected = false;
            return UNDEFINED_PROBLEM;
        }

        return client.getSessionId();
    }

    /**
     * Disconnect.
     *
     * @return true, if successful
     */
    public boolean disconnect() {
        try {
            client.disconnect();
            this.connected = false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPass() {
        return pass;
    }


    public String getVideos(int query_type, String query) {
        String xml = "";
        // prepare authentication arguments for http GET
        con = getConnection();
        if (con.client == null) {

        }

        // prepare url to call and its search arguments
        switch (query_type) {
            case BrowsePagerAdapter.SEARCH:
                xml = getVideoInformations();
                break;
            case BrowsePagerAdapter.QR_SEARCH:
                xml = getVideoInformations();
                break;
            case BrowsePagerAdapter.LATEST:
                xml = getVideoInformations();
                break;
            case BrowsePagerAdapter.MY_VIDEOS:
                xml = getVideoInformations();
                break;
            case BrowsePagerAdapter.RECOMMENDED:
                xml = getVideoInformations();
                break;
            case BrowsePagerAdapter.BROWSE_BY_GENRE:
                xml = getVideoInformations();
                break;
            case BrowsePagerAdapter.NEARBY:
                xml = getVideoInformations();
                break;
        }
        // start building http GET


        // execute GET


        // handle results

        return xml;

    }
}
