package fi.aalto.legroup.achso.authentication;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.auth.openidconnect.IdTokenResponse;
import com.google.api.client.auth.openidconnect.IdTokenVerifier;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import fi.aalto.legroup.achso.app.App;

/**
 * A layer of syntactic sugar around the google-oauth-java-client library to simplify using OpenID
 * Access on Android.
 *
 * Currently this helper class is fairly limited. It's suitable for our use case and pretty much
 * nothing else.
 */
public class OIDCUtils {

    /**
     * Generates an URL to the Authorization Endpoint. The user can then authenticate themselves,
     * authorise this app and obtain an Authorization Token.
     */
    public static String newAuthorizationUrl(String authorizationServerUrl, String tokenServerUrl,
                                             String redirectUrl, String clientId,
                                             String clientSecret, String[] scopes) {

        List<String> scopesList = Arrays.asList(scopes);

        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                new NetHttpTransport(),
                new GsonFactory(),
                new GenericUrl(tokenServerUrl),
                new BasicAuthentication(clientId, clientSecret),
                clientId,
                authorizationServerUrl
        ).build();

        // Generate the URL that we'll modify a bit to comply with the spec
        AuthorizationCodeRequestUrl authUrl = flow.newAuthorizationUrl();

        // The flow builder sets `response_type` and `client_id` parameters. The OpenID spec
        // requires the `scope` and `redirect_uri` parameters as well.
        authUrl.setScopes(scopesList);
        authUrl.setRedirectUri(redirectUrl);

        // If the list of scopes includes the special `offline_access` scope that enables issuing
        // of Refresh Tokens, we need to ask for consent by including this parameter.
        if (scopesList.contains("offline_access")) {
            authUrl.set("prompt", "consent");
        }

        // Tell the server to ask for login details again. This ensures that in case of multiple
        // accounts, the user won't accidentally authorise the wrong one.
        authUrl.set("prompt", "login");

        // An optional request parameter that asks the server to provide a touch-enabled interface.
        // Who knows, maybe the server is nice enough to make some changes.
        authUrl.set("display", "touch");

        return authUrl.toString();
    }

    /**
     * Exchanges an Authorization Token for an ID Token, Access Token and Refresh Token.
     *
     * Generally ID tokens are valid for a longer period of time than access tokens, since they're
     * meant for authentication. The way I (maybe) understand it, access tokens should be used for
     * short-term access to protected resources.
     *
     * Needs to be run on a separate thread.
     */
    public static IdTokenResponse requestTokens(String authorizationServerUrl,
                                                String tokenServerUrl, String redirectUrl,
                                                String clientId, String clientSecret,
                                                String authToken) throws IOException {

        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                new NetHttpTransport(),
                new GsonFactory(),
                new GenericUrl(tokenServerUrl),
                new BasicAuthentication(clientId, clientSecret),
                clientId,
                authorizationServerUrl
        ).build();

        TokenRequest request = flow.newTokenRequest(authToken);

        // Again, we need to set the `redirect_uri` parameter. This time a dedicated method
        // setRedirectUri() doesn't exist for some reason.
        request.set("redirect_uri", redirectUrl);

        IdTokenResponse response = IdTokenResponse.execute(request);
        String idToken = response.getIdToken();

        if (isValidIdToken(clientId, idToken)) {
            return response;
        } else {
            throw new IOException("Invalid ID token returned: " + idToken);
        }
    }

    /**
     * Exchanges a Refresh Token for a new set of tokens.
     *
     * Note that the Token Server may require you to use the `offline_access` scope to receive
     * Refresh Tokens.
     */
    public static IdTokenResponse refreshTokens(String tokenServerUrl, String clientId,
                                                String clientSecret, String[] scopes,
                                                String refreshToken) throws IOException {

        List<String> scopesList = Arrays.asList(scopes);

        RefreshTokenRequest request = new RefreshTokenRequest(
                new NetHttpTransport(),
                new GsonFactory(),
                new GenericUrl(tokenServerUrl),
                refreshToken
        );

        request.setClientAuthentication(new BasicAuthentication(clientId, clientSecret));
        request.setScopes(scopesList);

        return IdTokenResponse.execute(request);
    }

    /**
     * Verifies an ID Token.
     * TODO: Look into verifying the token issuer as well?
     */
    public static boolean isValidIdToken(String clientId, String tokenString) throws IOException {
        List<String> audiences = Arrays.asList(clientId);
        IdTokenVerifier verifier = new IdTokenVerifier.Builder().setAudience(audiences).build();

        IdToken idToken = IdToken.parse(new GsonFactory(), tokenString);

        return verifier.verify(idToken);
    }

    /**
     * Gets user information from the UserInfo endpoint.
     */
    public static JsonObject getUserInfo(String userInfoUrl, String idToken) throws IOException {
        Request request = new Request.Builder()
                .url(userInfoUrl)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + idToken)
                .get()
                .build();

        Response response = App.httpClient.newCall(request).execute();

        if (response.isSuccessful()) {
            String jsonString = response.body().string();
            return new JsonParser().parse(jsonString).getAsJsonObject();
        } else {
            throw new IOException("Could not get user info: " + response.code() + " " +
                    response.message());
        }
    }

}
