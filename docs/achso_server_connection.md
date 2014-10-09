

- [Ach so! server connection](#h1)
    - [Implementations](#implementations)
        - [ClViTra](#clvitra)
        - [ClViTra2.0](#clvitra20)
        - [AchSoServer](#achsoserver)
        - [MPEG7MultimediaContentService](#mpeg7multimediacontentservice)
        - [VideoInformation -service](#videoinformation-service)
    - [Uploading and updating](#uploading_and_updating)
    - [Calls to VideoStorage](#calls_to_videostorage)
        - [upload_video](#upload_video)
        - [get_processing_state](#get_processing_state)
    - [Calls to AchSoService](#calls_to_achsoservice)
        - [get_unique_id](#get_unique_id)
        - [upload_video_metadata](#upload_video_metadata)
        - [update_video_metadata](#update_video_metadata)
        - [upload_annotation](#upload_annotation)
        - [update_annotation](#update_annotation)
    - [Annotation listings](#Annotation_listings)
        - [get_annotations](#get_annotations)
    - [Video listings](#Video_listings)
        - [get_videos](#get_videos)
    - [Communities](#communities) - moved to SSS
        - [add_community](#add_community)
        - [edit_community](#edit_community)
        - [join_community](#join_community)
        - [leave_community](#leave_community)
        - [get_communities](#get_communities)
        - [add_video_to_community](#add_video_to_community)
        - [remove_video_from_community](#remove_video_from_community)
        - [get_videos_in_community](#get_videos_in_community)

<a name="h1"></a>

Ach so! server connection
=========================

This is an API description of connections from Ach so! -Android client to servers. This document has two purposes: to collect Ach so! developers' suggestions what server APIs should provide and for server backend developers to document what exactly their API is providing and how to call its features.

If you have a server API that is doing something close to what is described here, please modify the description here to describe your API. If you have something that you are going to implement, but is not implemented yet, mention it here.

<a name="implementations"></a>

Implementations
---------------

Now here things get confusing.


<a name="clvitra"></a>

### ClViTra
**CLViTra** is an existing implementation of idea of doing video transcoding in cloud. It is one possible way of implementing the tasks required from VideoStorage, but it is now deprecated and redone as ClViTra2.0 with more straightforward API. The first Ach So! server connections and AnViAnno used this as a receiving end for video uploads, so the code has some legacy cruft from that.

- [ClViTra](http://merian.informatik.rwth-aachen.de:5080/ClViTra/)
- No more actively developed?
- Use ClViTra2.0 instead!

<a name="clvitra20"></a>

### ClViTra2.0
For VideoStorage, the actively developed implementation is **ClViTra2.0**.

- [ClViTra2.0 in Github](https://github.com/learning-layers/Cloud-Video-Transcoder)
- Instead of documentation, it is better to just see what services are implemented in [services -directory](https://github.com/learning-layers/Cloud-Video-Transcoder/tree/master/ClViTra_2.0/src/main/java/de/dbis/services)
- If you are updating ClViTra2.0 API, please update relevant parts in this document or notify @jpurma.

<a name="achsoserver"></a>

### AchSoServer

This document describes need for two services, VideoStorage and AchSoService. **AchSoServer** is a small lightweight implementation of AchSoService, for purpose of development and testing, but not for real secure use. It is implemented with Node.js and hosted in Aalto. It is 'cutting edge' for Ach so!: we need some service where to test if the planned interactions work as expected where we can modify the interactions easily. Then the API here is modified to reflect the improvements and the solid Tethys services should follow.

- [AchSoServer in Github](https://github.com/learning-layers/AchSoServer)
- [API implementation, all of the code is in this file.](https://github.com/learning-layers/AchSoServer/blob/master/server.js)
- running test server: [http://achso.aalto.fi/server/api/](http://achso.aalto.fi/server/api/)

The idea is to use AchSoServer until the proper metadata services in Tethys catch up with API coverage. AchSoServer is not safe or durable for long-term solution, unless time and expenses are invested for making it so.

<a name="mpeg7multimediacontentservice"></a>

### MPEG7MultimediaContentService

*MPEG7MultimediaContentService* is a service used by Sevianno2.2 in Tethys/AtlasLAS environment. It is used to browse and show videos. In Sevianno it is called through lasAjaxClient-javascript library (http://dbis.rwth-aachen.de/gadgets/lib/las/lasAjaxClient.js). For Java clients it is available as `http-connector-client.jar` and then imported as `i5.las.httpConnector.client`. The .jar can be found from AchSo/libs, but the java source for that I don't have.

- [JavaDoc for MPEG7MultimediaContentService](http://www-i5.informatik.rwth-aachen.de/~atlas/module_build_3/JavaDoc//atlas_las_mpeg7-services/HEAD/javadoc/i5/atlas/las/service/mpeg7/multimediacontent/MPEG7MultimediaContentService.html) (latest I found)

- [Sevianno in Github](https://github.com/learning-layers/sevianno), [Sevianno demo site](http://role-sandbox.eu/spaces/sevianno2.2) for observing server calls.

- [lasAjaxClient.js](http://dbis.rwth-aachen.de/gadgets/lib/las/lasAjaxClient.js) -- javascript implementation for lasConnection, which is all the setup necessary for calling (old style?) LAS services.
- [/libs/http-connector-client.jar](/libs/http-connector-client.jar) Java implementation for lasConnection. Source I don't have, but [here is JavaDoc](
http://www-i5.informatik.rwth-aachen.de/~atlas/module_build_3/JavaDoc//atlas_las_http-connector/release_0_4-mobsos/javadoc/i5/las/httpConnector/client/Client.html)
- Use through Java http-connector-client:

```java
import i5.las.httpConnector.client.Client;

client = new Client(lasHostname, lasPort, timeOutms, username, password);
client.connect();
Object[] params = {videoUrls};
Object res = client.invoke("mpeg7_multimediacontent_service","getMediaCreationTitles", params);
```

<a name="videoinformation-service"></a>

### VideoInformation -service

*VideoInformation* -service is another service to provide some metadata about videos uploaded to Sevianno/Tethys/AtlasLAS. It is supposed to be especially for Ach so! and it was actively developed by Kevin Jahns in January-February 2014.

- JavaDoc API [http://dbis.rwth-aachen.de/~jahns/javadocs/videoinformation/i5/atlas/las/service/videoinformation/Videoinformation.html](http://dbis.rwth-aachen.de/~jahns/javadocs/videoinformation/i5/atlas/las/service/videoinformation/Videoinformation.html)
- Use through LasConnection:

```java
import i5.las.httpConnector.client.Client;

client = new Client(lasHostname, lasPort, timeOutms, username, password);
client.connect();
Object[] params = {username};
Object res = client.invoke("videoinformation","getVideoInformationFilterByCreator", params);
```

<a name="uploading_and_updating"></a>

Uploading and updating
----------------------

I have assumed 2 services to handle Ach so! videos. One is VideoStorage service (e.g. ClViTra2.0) and the other is the service that understands Ach so! metadata, AchSoService.

The workflow for uploading is following:

* Prepare necessary authentication means for both services, AchSoService and VideoStorage.
* Ask AchSoService for unique id that can be used to recognize both the video file in VideoStorage and metadata in AchSoService  (`GET:$achsoservice_api/get_unique_id`).
* Send video metadata (including `unique_id`) to AchSoService, with `video_uri` and `thumbnail_uri` empty.(`POST:$achsoservice_api/upload_video_metadata`)
* If Success: Send video file to video upload service (use key as filename). (`POST:$videostorage_api/upload_video`)
* If Success: Start polling upload service (`GET:$videostorage_api/get_processing_state`) with key
	* write to AchSo's SharedPreferences that polling of video $key is active. – If app is shut down and resumed, we know to resume polling.
* When poll replies with `'state' == 'finished'`, update video metadata (`POST:$achsoservice_api/upload_video_metadata`) with `video_uri` and `thumbnail_uri`.
	* remove polling instruction from AchSo prefs. OR
	* When replies `'state' == 'encoding error'` or `'not found'`, stop polling & remove polling instruction from AchSo prefs. Alert user.

The workflow for modifying video metadata is following:

* Prepare necessary authentication means for both services, AchSoService and VideoStorage.
* If online: call upload_video_metadata with video key and changed fields (JSON object), if this returns `Ok`, we are done.
* If offline:
	* add changes (as a JSON string) to pending changes in AchSo prefs.
	* When back online, send changes with `POST:$achsoservice_api/upload_video_metadata`. (Ach so! needs to do some cleaning to remove redundant changes before sending.)
	* Note that this is vulnerable to conflicts, or older changes rewriting newer changes. We can see if it is worth working around with timestamps for modifications or such.)

The workflow for modifying existing annotation is following:

* Prepare necessary authentication means for both services, AchSoService and VideoStorage.
* If online:
	* call ´update_annotation´ with annotation key and changed fields.
	* If Success: done
* If offline:
	* add changes (JSON string) to pending changes in AchSo prefs.
	* When back online, send changes with `POST:$achsoservice_api/update_annotation`. (Ach so! needs to do some cleaning to remove redundant changes before sending.)

The workflow for adding annotation (for uploaded video) is following:
    - (prepare authentication means for ach so data service)
    - If online:
        - call upload_annotation with annotation data.
        - If Success: done
    - If offline:
        - add annotation (json string) to pending changes in AchSo prefs. When back online, send changes with `POST:$achsoservice_api/upload_annotation`.


<a name="calls_to_videostorage"></a>

## Calls to VideoStorage

<a name="upload_video"></a>

### upload_video

This is used to send video for encoding/conversion

*Request:* POST

*URL:*

	http://$videostorage_api/upload
    eg.
    http://$server/ClViTra_2.0/rest/upload


*What is sent:*

"File" as "multipart/form-data". File has filename, which is form of `$key.$suffix`, where `$suffix` is mp4,avi,mpg... This filename (without suffix) will be used when querying the state of the processing, so remember it.

*Headers:* authentication token?

*What is received:*

    200: Success
    404: URL not found
    401: Unauthorized
    500: Server Errors

*Example:*

```java
HttpPost httppost = new HttpPost("http://merian.informatik.rwth-aachen.de:5080/ClViTra_2.0/rest/upload");
httppost.setHeader("X-auth-token", "04ef789a010c6f252a9f572347cac345");
MultipartEntityBuilder multipart_entity = MultipartEntityBuilder.create();
multipart_entity.addPart("File", new FileBody(video_file));
httppost.setEntity(multipart_entity);
...
httpclient.execute(httppost);
...
```

<a name="get_processing_state"></a>

### get_processing_state
This is used to check if the video $key has finished its encoding and to receive the uri to use when playing it.

*URL:*

    http://$videostorage_api/videoDetail/$user/$videoId

    "user": Sender of the video,
    "videoId":String, which is the unique filename that was sent when uploading video, .

*What is sent:* Arguments are included in the path

*Headers:* ?

*What is received:*

    200: (Success) "{'Video_Name':'Name', 'Video_URL':$video_uri, 'Thumbnail_URL':$thumbnail_uri,'Status':'Transcoded'}"
    -OR-
    "{'state':'processing', 'thumbnail':$thumbnail_uri'}
    -OR-
    "{'status':'Not Found'}

    404: URL not found.
    401: Unauthorized.
    500: Server Error.

*Example:*

    http://merian.informatik.rwth-aachen.de:5080/ClViTra_2.0/rest/videoDetail/testUser/ea901369-3ae0-42c6-aece-41151e474472

    >"{'Video_Name':'Sample.mp4', 'Video_URL':'http://tosini.informatik.rwth-aachen.de:8134/videos/ea901369-3ae0-42c6-aece-41151e474472.mp4', 'Thumbnail_URL':'http://tosini.informatik.rwth-aachen.de:8134/thumbnails/ea901369-3ae0-42c6-aece-41151e474472.jpg','Status':'Transcoded'}"


    http://merian.informatik.rwth-aachen.de:5080/ClViTra_2.0/rest/videoDetail/testUser/foobar

    >"{'Status':'Not Found'}"

<a name="calls_to_achsoservice"></a>

## Calls to AchSoService

<a name="get_unique_id"></a>

### get_unique_id
Returns a new unique id. Semantic video can safely use this as an identifier and this identifier is used as a key to retrieve video metadata and annotations, and to retrieve the state of the video encoding process.

*Request:* GET

*URL:*

    http://$achsoservice_api/get_unique_id

*What is sent:* nothing

*Headers:* authentication token?

*Returns:*

    200: unique id string, e.g: "e0c3da80-e4cc-11e3-890e-5f6eb36c1ac9"

### upload_video_metadata[upload_video_metadata]
This is used to upload annotations and metadata for video. This is done before sending the actual video file, but the video uri and thumb uri have placeholders instead of actual values. When upload_video is completed, Ach so! polls server with get_processing_state until it receives 'finished' state as response, and then does update_metadata with new uri values.

Ach saves to its local preferences when unfinished video processing polling is activated, so that it can continue and finalize the video metadata next time Ach so is launched, in case that the Ach so is shut down while the processing in server side is still going on.

Communities is a list of community uids/keys. Usually adding and removing video to/from community is done through community api methods, but it is here for convenience, so that video can be created and shared with one call.

*Request:* POST.

*URL:*

    http://$achsoservice_api/upload_video_metadata

*What is sent:* "data" as UTF-8 StringEntity.
"data" is json-representation of SemanticVideo:

    SemanticVideo = json_object({
        title: String,
        creator: String,
        qr_code: String,
        created_at: Number,
        video_uri: String,
        genre: String,
        key: String,
        latitude: Number,
        longitude: Number,
        accuracy: Number,
        duration: Number,
        thumb_uri: String,
        communities: String[],
        annotations: Annotation[]
        })
    where each Annotation = {
        creator: String,
        start_time: Number,
        position_x: Number,
        position_y: Number,
        text: String,
        scale: Number,
        key: String
    }

*Headers:*

    authentication token?
    "Content-type":"application/json"
    "Accept":"application/json"

In server side annotations should be saved as individual objects that have a reference to semantic video they belong to. It is easier to avoid conflicts when updating annotations. Annotation keys are created by server.

*What is received:*

    200: Success.
    404: URL not found.
    401: Unauthorized.
    500: Server Error.

*Example:*

    json = "{
        'title': '1st video of Friday',
        'creator': 'Jukka',
        'qr_code': 'http://learning-layers.eu/',
        'created_at': 1399550703652,
        'video_uri': '',
        'genre': 'Good work',
        'key': 'ea901369-3ae0-42c6-aece-41151e474472',
        'latitude': null,
        'longitude': null,
        'accuracy': null,
        'duration': 324993,
        'thumb_uri': '',
        'communities': ['public'],
        'annotations': [
            {
            'creator': 'Jukka',
            'start_time': 12003,
            'position_x': 0.33,
            'position_y': 0.7,
            'scale': 1.2,
            'text': 'I made a scratch here.',
            'key': 'A2f-ea901369-3ae0-42c6-aece-41151e474472'
            },
            {
            'creator': 'Jukka',
            'start_time': 22003,
            'position_x': 0.63,
            'position_y': 0.12,
            'scale': 1.0,
            'text': 'Good seam.',
            'key': 'A36-ea901369-3ae0-42c6-aece-41151e474472'
            }
        ]
    }";
    HttpPost httppost = new HttpPost("http://merian.informatik.rwth-aachen.de:5080/AchSoServer/rest/upload_video_metadata");
    httppost.setHeader("X-auth-token", "04ef789a010c6f252a9f572347cac345");
    httppost.setHeader("Content-type", "application/json");
    httppost.setHeader("Accept":"application/json");
    StringEntity se = new StringEntity(json, "UTF-8");
    httppost.setEntity(se);
    ...
    httpclient.execute(httppost);
    ...

<a name="update_video_metadata"></a>

### update_video_metadata
Update some of the fields of the video -- those that are included in the sent json object (excluding 'key', it cannot be changed, it is used to find what video to change.). It is otherwise similar to upload_metadata. Annotations are not updated through this, they use
update_annotation or upload_annotation, with a video_key to link them to this video.

*Request:* POST.

*URL:*

    http://$achsoservice_api/update_video_metadata
*What is sent*: field "data" where content is UTF-8 encoded JSON.
    data is partial json-representation of SemanticVideo:

    {
        key: String, **rest are optional: **
        title: String,
        creator: String,
        qr_code: String,
        created_at: Number,
        video_uri: String,
        genre: String,
        key: String,
        latitude: Number,
        longitude: Number,
        accuracy: Number,
        duration: Number,
        thumb_uri: String
    }

*Headers*:

    authentication token?
    "Content-type":"application/json"
    "Accept":"application/json"


*What is received:*

    200: Success.
    404: URL not found.
    401: Unauthorized.
    500: Server Error.

*example:*

```java
    json = "{
        'key': 'ea901369-3ae0-42c6-aece-41151e474472',
        'video_uri': 'http://tosini.informatik.rwth-aachen.de:8134/videos/ea901369-3ae0-42c6-aece-41151e474472.mp4',
        'thumb_uri': 'http://tosini.informatik.rwth-aachen.de:8134/thumbnails/ea901369-3ae0-42c6-aece-41151e474472.jpg',
    }";
    HttpPost httppost = new HttpPost("http://merian.informatik.rwth-aachen.de:5080/AchSoServer/rest/upload_video_metadata");
    httppost.setHeader("X-auth-token", "04ef789a010c6f252a9f572347cac345");
    httppost.setHeader("Content-type", "application/json");
    httppost.setHeader("Accept":"application/json");
    StringEntity se = new StringEntity(json, "UTF-8");
    httppost.setEntity(se);
    ...
    httpclient.execute(httppost);
    ...
```

<a name="upload_annotation"></a>

### upload_annotation
This is used to upload new annotation for existing video.

*Request:* POST.

*URL:*

    http://$achsoservice_api/upload_annotation

*What is sent:* "data" as UTF-8 StringEntity.
data is a json-representation of Annotation:

    Annotation = {
        creator: String,
        start_time: Number,
        position_x: Number,
        position_y: Number,
        text: String,
        scale: Number,
        key: String,
        video_key: String,
    }

*Headers:*

    "X-auth-token":$token
    "Content-type":"application/json"
    "Accept":"application/json"

*What is received:*

    200: Success.
    404: URL not found.
    401: Unauthorized.
    500: Server Error.

*Example:*

```java
    json = "{
            'creator': 'Jukka',
            'start_time': 12003,
            'position_x': 0.33,
            'position_y': 0.7,
            'scale': 1.2,
            'text': 'I made a scratch here.',
            'key': 'A2f-ea901369-3ae0-42c6-aece-41151e474472',
            'video_key': 'ea901369-3ae0-42c6-aece-41151e474472'
    }";
    HttpPost httppost = new HttpPost("http://merian.informatik.rwth-aachen.de:5080/AchSoServer/rest/upload_annotation");
    httppost.setHeader("X-auth-token", "04ef789a010c6f252a9f572347cac345");
    httppost.setHeader("Content-type", "application/json");
    httppost.setHeader("Accept":"application/json");
    StringEntity se = new StringEntity(json, "UTF-8");
    httppost.setEntity(se);
    ...
    httpclient.execute(httppost);
    ...
```

<a name="update_annotation"></a>

### update_annotation
This is used to update attributes of existing annotation. Attributes are given as JSON object, and it needs only to include those fields that have changed.

*Request:* POST.

*URL:*

    http://$achsoservice_api/update_annotation

*What is sent:* "data" as UTF-8 StringEntity.
    "data" is a json-representation of Annotation:

    Annotation = {
        key: String, *** Rest are optional: ***
        creator: String,
        start_time: Number,
        position_x: Number,
        position_y: Number,
        text: String,
        scale: Number,
    }

*Headers:*

    "X-auth-token":$token
    "Content-type":"application/json"
    "Accept":"application/json"

*What is received:*

    200: Success.
    404: URL not found.
    401: Unauthorized.
    500: Server Error.

*Example:*

```java
    json = "{
            'key': 'A2f-ea901369-3ae0-42c6-aece-41151e474472',
            'position_x': 0.6,
            'position_y': 0.2
    }";

    HttpPost httppost = new HttpPost("http://merian.informatik.rwth-aachen.de:5080/AchSoServer/rest/upload_annotation");
    httppost.setHeader("X-auth-token", "04ef789a010c6f252a9f572347cac345");
    httppost.setHeader("Content-type", "application/json");
    httppost.setHeader("Accept":"application/json");
    StringEntity se = new StringEntity(json, "UTF-8");
    httppost.setEntity(se);
    ...
    httpclient.execute(httppost);
    ...
```

<a name="annotation_listings"></a>

## Annotation listings

<a name="get_annotations"></a>

### get_annotations
List of annotations for given video_key

*Request:* GET

*URL:*

    http://$achsoservice_api/get_annotations

*What is sent:*

    video_key:String

*Headers:*

    "X-auth-token":$token
*What is received:*
    JSON list of annotation objects, not ordered in any meaningful way

<a name="video_listings"></a>

## Video listings

<a name="get_videos"></a>

### get_videos
General interface for receiving list of videos that fill given conditions.
For searches, searches should also look into annotations and return videos that include matching annotations.
Even if community is empty, results should be filtered by those communities that the user can see.

*Request:* GET

*URL:*

    http://$achsoservice_api/get_videos
*What is sent:*

    keywords may include subset of following:
    search:"searchstring":String,
    by_user:"username":String,
    community: "community_key":String,
    genre: "genre_id":String,
    batch_size: 30(default):Number,
    result_page: 1(default):Number,
    sort_by:"date"(default)|"most_views",
    sort_order:(default)descending|ascending
    name:"community_name":String,
    listed:true|false|1|0:Boolean,
    open:true|false|1|0:Boolean,
    moderators:[user_id,]<String>
*Headers:*

    "X-auth-token":$token
    "Accept":"application/json"

*What is received:*

    JSON list of semantic video objects


<a name="communities"></a>

## Communities -moved to SSS
These are the groups/channels that are used to deduce video visibility and editing rights. The responsibility for this information has been moved to Social Semantic Service, so ignore this section. It has to be done in SSS in a way that all tools can use the same API.

<a name="add_community"></a>

### add_community
Create a community that has access to videos

*Request:* POST.

*URL:*

    http://$achsoservice_api/add_community
*What is sent:*

    JSON object, where:
    name:"name":String,
    listed:true|false|1|0:Boolean,
    open:true|false|1|0:Boolean,
    moderators:[user_id,]:<String>
*Headers:*

    authentication token?

*What is received:*
    unique id of community, or empty if it already exists

<a name="edit_community"></a>

### edit_community
Change community settings, only by moderator

*Request:* POST.

*URL:*

    http://$achsoservice_api/edit_community
*What is sent:*

    JSON object, where key is mandatory and others are optional:
    key:"stringid":String,
    name:"community_name":String,
    listed:true|false|1|0:Boolean,
    open:true|false|1|0:Boolean,
    moderators:[user_id,]<String>
*Headers:*

    authentication token?

*What is received:*

    "Ok", "Not allowed" or "Not found"

<a name="join_community"></a>

### join_community
Join user xx to a community. If closed community, this is done by authenticated user who is a moderator. Username for authenticated user is retrievable from X-auth-token?

*Request:* POST.

*URL:*

    http://$achsoservice_api/join_community
*What is sent:*

    name:"community_uid":String,
    user_id:"xx":String
*Headers:*

    authentication token?

*What is received:*

    "Ok", "Not allowed" or "Not found"

<a name="leave_community"></a>

### leave_community
Leave from a community. User xx is removed from community. There should be checks that the X-auth-token points to same user xx or to a moderator?

*Request:* POST.

*URL:*

    http://$achsoservice_api/leave_community
*What is sent:*

    name:"community_uid":String,
    user_id:"xx":String
*Headers:*

    authentication token?

*What is received:*

    "Ok", "Not allowed" or "Not found"

<a name="get_communities"></a>

### get_communities
Return a list of listable communities (no arguments), or list of provided user_id:s communities. List is a json object

*Request:* GET

*URL:*

    http://$achsoservice_api/get_communities
*What is sent:*

    user_id:"xx":String (optional)
*Headers:*

    authentication token?
    "Accept":"application/json"

*What is received:*

    [{name:String, key:String, open:Boolean, listed:Boolean, is_member:Boolean, is_moderator:Boolean},... ]

notice that if user_id is not given, is_member and is_moderator can default to false.

<a name="add_video_to_community"></a>

### add_video_to_community
Add a video to belong for a community. It is then visible for community members Requires video_key and community_id as arguments.

*Request:* POST.

*URL:*

    http://$achsoservice_api/add_video_to_community
*What is sent:*

    name:"community_id":String,
    video_key:"xx":String
*Headers:*

    authentication token?

*What is received:*

    "Ok", "Not allowed" or "Not found"

<a name="remove_video_from_community"></a>

### remove_video_from_community
Video is no longer listed as belonging to a community. Requires video_key and community_id as arguments.

*Request:* POST.

*URL:*

    http://$achsoservice_api/remove_video_from_community
*What is sent:*

    name:"community_id":String,
    video_key:"xx":String
*Headers:*

    authentication token?

*What is received:*

    "Ok", "Not allowed" or "Not found"

<a name="get_videos_in_community"></a>

### get_videos_in_community
Return metadata of videos in community. User must be a member in community to get listing

*Request:* GET

*URL:*

    http://$achsoservice_api/get_videos_in_community
*What is sent:*

    community_id:"xx":String,
*Headers:*

    authentication token?
    "Accept":"application/json"
*What is received:*
    JSON list of semantic video objects


