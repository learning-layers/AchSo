# About authentication process in Ach so! with i5 OpenId Connect -server

## Overview

[OpenId Connect](http://openid.net/connect/) is a new combination of authentication protocol and identity information that can be used for web services to use single **identity** (single login) for multiple services that need not to know about each other. The actual authentication protocol is OAuth2, a popular and proven protocal, but OpenId Connect is a suggestion of using it in a certain way to better share the identity of authenticated user between services. My understanding is that the identity here means the metadata associated with user: real name, email etc.

In our case we are testing using [i5 OpenId Connect -server](http://137.226.58.15:9085/openid-connect-server-webapp/) (i5OIDC from now on) as an authentication server for Layers services. This entails that all services and clients follow certain steps for identifying their users and giving them access to resources. 

The first, and ongoing, problem is to differentiate between services and clients in i5OIDC authentication activities. There is a triangle, where services communicate with i5OIDC, clients communicate with i5OIDC and clients communicate with services. Documentation tends to focus on how services communicate with i5OIDC, and in some cases ([OpenID Connect Basic Client Implementer's Guide](http://openid.net/specs/openid-connect-basic-1_0.html), [MitreID's Open Id Connect Diagrams](https://github.com/mitreid-connect/OpenID-Connect-Java-Spring-Server/blob/master/docs/OpenID_Connect_Diagrams.pdf)) the documentation calls the services using the authentication server as clients, which has caused quite a lot of confusion. Latest addition (7.7.) in [ClViTra openidconnect documentation](https://github.com/learning-layers/Cloud-Video-Transcoder/blob/master/openidconnect.md) presents a case where clients communicate only to services, not making direct contact to i5OIDC. This is easiest approach for web apps and recommended way to go. This document wanders into more complicated approaches, that may be useful if the simple approach leads to unwanted behaviours, like background processes needing to display login pages.

Next I try to tell the whole story of what happens with authentication: what  client, service and i5OIDC each are doing. I haven't seen such yet.  

## Web app case

In a typical web application, the *client* is user's browser doing javascript -powered http-requests to *service*, which is a backend that constructs the responses to requests. In most of the web apps these are strongly coupled together: one of the tasks of the server is to generate the running clients into visitors' web browsers. In these normal cases the authentication goes like this:

* Service creates an anonymous web client for user: web client doesn't yet have any authentication information it can use.
* Web client asks service to authenticate it, to recognize it as something that can ask for specific controlled information. Specifically it asks the service to give it a *token*, an identifying secret piece of text, that it can use in future calls to service to tell who is calling. 
* Service starts doing this by asking i5OIDC to authenticate the given client and sends i5OIDC the address where the i5OIDC should call back when it has authenticated the client. Remember it was the client who originally asked to authenticate itself. Client's request is not responded, but it is redirected  for i5OIDC to respond.  
* i5OIDC responds by showing login screen(s) for client, or if the original request is coming from already recognized user in i5OIDC, by calling to the address that the service gave to notify the success. When i5OIDC calls to the return address at the service, this call is done as communication between services, invisible to user, but also the responsibility to respond to client is implicitly moved back to service. 
* When the service receives the success notification (a call to specific url that the service is watching), the notification includes a short `code` that the service cannot understand or connect to any client. It just knows that some user has successfully authenticated to the i5OIDC, and now that unknown user needs to get some robust means to identify itself also for the service. The service takes the short code for use in the next communication with i5OIDC, so that at i5OIDC can know which client this next communication is about.
* The service reacts to success notification and received code by formulating a token request for i5OIDC. This is basically saying 'give the user who sent that previous authentication (identified by code) a token (longer piece of code) that it can keep using to access my private data'. The request also includes some pre-defined information about what service is making the request , what kind of information the token should include etc.
* The i5OIDC responds to service by sending the requested token as `access token` to service. The response includes the token and some additional information, like if the token expires at some point.
* The service finally responds to the client who originally requested for token by forwarding the token to client. Even as the client is the closest part to the actual user in this process, this whole authentication discussion may remain invisible for the user. If the i5OIDC already knows that this browser is logged in and it has permission to give this information to the service, there is nothing to show for user about the debacle. The client silently stores the token for future use.     

Now the client has this magical token. To be able to use it, the client has to include the token in calls to the service, e.g. if we call url '.../showMyVideos' the call has a Header where it reads 'Authorization=Bearer xxyyzz' where xxyyzz is a long random-looking string, the previously received `access_token`. In everyday browsing, the browser makes sure that the authorization is kept the same when browsing around the domain (not sure!), for ajax -calls it needs to be added by client for each call.

The service should decide what to do with the token it receives with each request from client. It should, at least when first receiving a token, to validate it from i5OIDC -- to check if this is a real token and to what user it corresponds to. If there is no valid token, the service can deny to show the requested page. The validation is done by sending the token to i5OIDC. The way how example web service ClViTra does it is by asking user information about the token, and if it receives something it is valid and if not, then it isn't. Once validated, the server can try to remember that this token matches with this user. 

## Case 'Ach so!'

The difference between service and client is murkier in Ach so!. Basically Ach so! is a client that tries to use different services, video upload service, video metadata service etc. In web app case all of the communication to i5OIDC went through service that the client was trying to use, and the service makes the orders for the tokens from i5OIDC. The problem now is that I don't know if the same token can work in several services, or if we should do the token negotiation a) once directly between Ach so! and i5OIDC, omitting the service from between, b) once with one service, and then use the token across other services or c) once for each service. I have for now implemented (a), by having 'Ach so!' client fake being a 'clvitra' service and doing the negotiation with i5OIDC by itself and then using the access_token it received to access actual clvitra resources. For stateless service this shouldn't be a problem. :) 

The choice to do (a) was based on older version of [ClViTra openidconnect documentation](https://github.com/learning-layers/Cloud-Video-Transcoder/blob/master/openidconnect.md) (see section old documentation) which focuses on explaining how service authenticates to i5OIDC, so I implemented quite a lot of service into our client. The new version from 7.7. adds more about how clients should use the ClViTra2-service to obtain authentication, and this points that the clients are expected to do (b) or (c) from previous paragraph. Choice (b) would have been the easiest to implement, (c) is not much more complicated, but inelegant.  

For Android app like Ach so! the i5OIDC authentication has the slight problem when client interaction may begin as requests that may not need any user interaction, but they may end up as going to login screens or permission grant pages. Starting a web view and loading a web page (e.g. http://137.226.58.15:9085/openid-connect-server-webapp/login ) breaks the experience of solid, contained app. Also Android has its own account management system, where login information and possible authentication tokens can be stored in devices settings, where they are presented nicely and can be managed in way that is uniform across accounts. To be able to use this and avoid login screens, the Ach so! authentication process does some own tricks:

* When user logins (in future, if enabled, automatically when the app starts), the account information is get from device's AccountManager, if such exists.

* If account doesn't exist Ach so! presents a login screen, which uses Android UI elements, but duplicates the fields of i5OIDC login screen. Login information is sent to the form receiver at i5OIDC. There is also a registration link, that turns the page to duplicate i5OIDC's registration page. The results are sent to i5OIDC. If the registration/login was success, new account is created to AccountManager.  

* If account exists, it is used to fill the login info at i5OIDC automatically, without user doing anything.

* If there are several suitable accounts, user chooses one.
 
* In all cases Registration/login pages in i5OIDC have JSESSIONID cookies that are used later to recognize that the browser is the same that logged in. This cookie is stored.

Now the result of this dance is that the user is logged in to i5OIDC server before we try to ask the access token. This makes sure that the request for access token doesn't redirect to login screens and goes smoothly. Except that there is one case: In first time use the login process may need to ask if the user is ok with the specific service using the authentication. This is displayed in WebView, as it would be wrong to circumvent this.

Also of note of Android apps is that the library used in ClViTra and examples,
 [nimbus.oauth2](http://connect2id.com/products/nimbus-oauth-openid-connect-sdk) doesn't work in Android (it assumes one often-used module to be higher version than android's module of same name.), but the library still manages to bring so many classes and abstraction levels on top of the actual requests it makes, so that it is difficult to see what there should be happening. 

In Ach so!, the authentication ping-pong is implemented without OAuth2 or OpenId Connect -specific libraries, by building the requests with standard blocks. (Remember that we are implementing the service stuff, where we ought to be implementing only the client stuff, which is well explained in new version of [ClViTra openidconnect documentation](https://github.com/learning-layers/Cloud-Video-Transcoder/blob/master/openidconnect.md) ) The form of actual requests is now simple to explain -- simpler than with Nimbus-libraries, as they don't require knowing the OAuth2-jargon beforehand. 

The next sections are about explaining what the authentication steps require on low level http post- and get- requests for service, which should help anyone who wants or needs to implement the authentication ping-pong without support libraries or with e.g. jQuery or plain javascript.

Both versions can be found from [src/fi/aalto/legroup/achso/state/i5OpenIdConnectLoginState.java](src/fi/aalto/legroup/achso/state/i5OpenIdConnectLoginState.java).

## Version 1: Authentication steps for services and clients pretending to be services 

This is based on the steps from the 'old version' part of [ClViTra openidconnect.md](https://github.com/learning-layers/Cloud-Video-Transcoder/blob/master/openidconnect.md) and some misunderstandings about who is client and from whose perspective.



### Step 0 -- Login or register to i5OIDC

Create a http-client and make it go to login page, and log in with correct credentials. Now the http-client has stored a cookie that ensures that if during the operation http-client ends up in i5OIDC, it can provide the session cookie to prove login status. Without the JSESSIONID -cookie the login doesn't stick.    

### Step 1 -- Announcing our intentions for i5OIDC

#### Request:
First we want to tell i5OIDC that we would like to be recognized as an client with identity and access to certain service. This is done with a GET -request, with following values:

```
Type: GET
URL: http://137.226.58.15:9085/openid-connect-server-webapp/authorize
Headers: "Content-type: application/x-www-form-urlencoded"
Params: 
    "response_type":"code" // always this
    "client_id":"clvitra" // we are trying to get into clvitra
    "redirect_uri":"http://137.226.58.27:9080/ClViTra_2.0/FileUpload.html" 
        // address where the response code (6 character string required for next step) is sent. The idea here is that if this whole operation was called by a web service, the web service may listen to this address, and when i5OIDC sends a request there, it can continue with the code there. In our case, you can put here just about anything that is safe, since we are explicitly not following the redirection, just taking note of the location where the redirection would go and parsing the code from the url. 
    "scope":"openid+email+profile" // what userinfo we finally would like to have
    "state":"324fca49" // random verification string -- this is a safety feature, the response should have the same string. 
    "nonce":"76ace126" // don't know what this is, random string here too. 
``` 

Example code from [i5OpenIdConnectLoginState](src/fi/aalto/legroup/achso/state/i5OpenIdConnectLoginState.java):

```java
String url = "http://137.226.58.15:9085/openid-connect-server-webapp/authorize";
String redirect_uri = "http://137.226.58.27:9080/ClViTra_2.0/FileUpload.html";
String client_id = "clvitra";
Bundle result = new Bundle();
List<NameValuePair> pairs = new ArrayList<NameValuePair>();
String state = UUID.randomUUID().toString().substring(0,8);
String nonce = UUID.randomUUID().toString().substring(0,8);
pairs.add(new BasicNameValuePair("response_type", "code"));
pairs.add(new BasicNameValuePair("client_id", client_id));
pairs.add(new BasicNameValuePair("redirect_uri", redirect_uri));
pairs.add(new BasicNameValuePair("scope", "openid+email+profile"));
pairs.add(new BasicNameValuePair("state", state));
pairs.add(new BasicNameValuePair("nonce", nonce));

String paramString = URLEncodedUtils.format(pairs, "utf-8");
if (!url.endsWith("?")) {
    url += "?";
}
url += paramString;

HttpResponse response;
HttpGet get = new HttpGet(url);
// next lines are about disabling redirection, we don't follow 304:s.
HttpParams params = new BasicHttpParams();
HttpClientParams.setRedirecting(params, false);
get.setParams(params);
get.setHeader("Content-type", "application/x-www-form-urlencoded");
try {
    response = http.execute(get);
} catch (IOException e) {
    e.printStackTrace();
}
```

#### Response:

What happens next is simple for web services, they are listening to redirect_uri and they get a request from i5OIDC aiming to that uri, with two parameters `code`and `state`. 

`redirect_uri?code=abuT2a&state=324fca49`

There are three ways to use this redirection: 1) Have your web service listen to redirect_uri, and when it gets called, get the code. 2) disable automatic redirection and when you get a state 304 (redirection) as a response from previous call, you can get the redirect_uri from Header:Location, and from there the parameters `code` and `state`.  3) Have javascript observing where the request it is being directed to, take the parameter from url (not sure if it can be made to work).

State needs to match the state that was sent with request. Code is a short string that is needed in next step.  

### Step 2 -- Asking for access_token

#### Request:
Now we get the actual access_token, as the i5OIDC knows that there is a logged in user and it knows that there is a service that wants to be responsive for this user. The access_token request is POST request:

```
Type: POST
URL: http://137.226.58.15:9085/openid-connect-server-webapp/token
Headers: 
    "Content-type: application/x-www-form-urlencoded"
    "Authorization: Basic 324398example2423498329847827737dfs78775a5s56d" 
    // this is base64-encoded authentication for this specific service. Authentication is form 'id:secret', for clvitra it is 'clvitra:clvitra'

Content: 
    "grant_type":"authorization_code" // always this
    "code":"abuT2a" // code received from previous step
    "redirect_uri":"http://137.226.58.27:9080/ClViTra_2.0/FileUpload.html" 
        // Same as with step 1, either have an address you can listen to, or read the response. This time it is easier, as the data is in the response content, not in its address.
``` 

Example code from [i5OpenIdConnectLoginState](src/fi/aalto/legroup/achso/state/i5OpenIdConnectLoginState.java):
```java
String tokenEndpoint = "http://137.226.58.15:9085/openid-connect-server-webapp/token";
String target = "http://137.226.58.27:9080/ClViTra_2.0/FileUpload.html";
String client_id = "clvitra";
String auth = "Basic " + Base64.encodeToString((client_id + ":" + client_id).getBytes(), Base64.NO_WRAP);
HttpPost post = new HttpPost(tokenEndpoint);
post.setHeader("Content-type", "application/x-www-form-urlencoded");
post.setHeader("Authorization", auth);
List<NameValuePair> pairs = new ArrayList<NameValuePair>();
pairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
pairs.add(new BasicNameValuePair("code", code));
pairs.add(new BasicNameValuePair("redirect_uri", target));
try {
    post.setEntity(new UrlEncodedFormEntity(pairs));
} catch (UnsupportedEncodingException e) {
    e.printStackTrace();
}
HttpResponse response;
try {
    response = http.execute(post);
    InputStream content = response.getEntity().getContent();

    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
    String json_string = "";
    String s;
    while ((s = buffer.readLine()) != null) {
        json_string += s;
    }
    response.getEntity().consumeContent();
    if (response.getStatusLine().getStatusCode() == 200) {
        // this is what we want
        return json_string;
    } else {
        return "";
    }
} catch (IOException e) {
    e.printStackTrace();
}
```


#### Response:
JSON string with `access_token`, `identity_token` and possibly other values.
-- or error messages

Remember to store the access token! It should have life longer than this one session.

### Step 3 -- Asking for identity information

#### Request:

```
Type: GET
URL: http://137.226.58.15:9085/openid-connect-server-webapp/userinfo
Headers: 
    "Content-type: application/x-www-form-urlencoded"
    "Authorization: Bearer 324398427423498329847827737dfs78775a5s56d..." 
    // string after Bearer is access_token received from previous step. 

``` 

Example code from [i5OpenIdConnectLoginState](src/fi/aalto/legroup/achso/state/i5OpenIdConnectLoginState.java):
```java
String json_string = "";
String url = "http://137.226.58.15:9085/openid-connect-server-webapp/userinfo";
HttpResponse response;
HttpGet get = new HttpGet(url);
get.setHeader("Content-type", "application/x-www-form-urlencoded");
get.setHeader("Authorization", "Bearer " + mAuthToken);
try {
    response = http.execute(get);
    InputStream content = response.getEntity().getContent();

    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
    String s;
    while ((s = buffer.readLine()) != null) {
        json_string += s;
    }
    response.getEntity().consumeContent();
} catch (IOException e) {
    e.printStackTrace();
}
```
#### Response:
JSON string with userinfo: `preferred_username` (= login name), `name` (= full name), `email`.


## Version 2: Authentication steps for clients who rely on services:

As you can see, these are very much simpler than when communicating directly to i5OAIDC! If these are enough!

### Step 1 -- Asking for authorization from ClViTra2:

This step in fact is an exception to the division of labour where client talks to service and service talks to i5OIDC. Here client talks to service, and service gives an address in i5OIDC, and then client talks to i5OIDC. Version 1 above just makes the address to i5OIDC by itself and calls it. 

Request 1a:
```
Type: GET
URL: http://137.226.58.27:9080/ClViTra_2.0/rest/openIDauth

```

Response 1a:
```
"http://137.226.58.15:9085/openid-connect-server-webapp/authorize?response_type=code&client_id=clvitra&redirect_uri=http%3A%2F%2F137.226.58.27%3A9080%2FClViTra_2.0%2FFileUpload.html&scope=openid+email+profile&state=K_dIjaqMHT0I2w0b_IKOUlpNdD6e6YZ4V7I77qJcUxU&nonce=x63Gjx7jqbzFFTCFe4Ujd2E_Ud0TEy08T1ERe_cGaAs"
```
It is the url we need to go next.

Request 1b:
```
Type: GET
URL: http://137.226.58.15:9085/openid-connect-server-webapp/authorize?response_type=code&client_id=clvitra&redirect_uri=http%3A%2F%2F137.226.58.27%3A9080%2FClViTra_2.0%2FFileUpload.html&scope=openid+email+profile&state=K_dIjaqMHT0I2w0b_IKOUlpNdD6e6YZ4V7I77qJcUxU&nonce=x63Gjx7jqbzFFTCFe4Ujd2E_Ud0TEy08T1ERe_cGaAs
```

Response 1b:
(after possible login screens and permission to authenticate to service)
```
"4935ti" - string containing the `code`
```
### Step 2 -- Asking for auth token from ClViTra2

Request:
```
Type: GET
URL: http://137.226.58.27:9080/ClViTra_2.0/rest/getAuthToken
Headers: "Code", $code (where $code is `code` from previous step.)
```

Response:
```
dsfdklak9987f56a55656s656d6saf65d6... -- authToken
```

### Step 3 -- Verifying auth token from ClViTra2 and getting username

Request:
```
Type: GET
URL: http://137.226.58.27:9080/ClViTra_2.0/rest/verifyAccessToken
Headers: "AccessToken", $authToken (where $authToken is result of previous step.)
```

Response:
```
jukka -- username for holder of this (accepted) token.
```

## Current situation and open questions

It works, it can be implemented with various level of effort to all kinds of client/service combinations. Open questions:

* Is access_token for one service usable for other services, or should the authentication process be repeated (partly) for all services Ach so! wants to use?
* What is the future role for userinfo provided by i5OIDC? Services easily have overlapping user information, and certainly SSS will have lots of user information. Should we ignore userinfo and get richer data from SSS? Now, or at some point?
* Will there be web ui for changing userinfo? 
* Will there be API for logging in or registering new accounts? Now I'm just hacking to forms, which is brittle solution.

