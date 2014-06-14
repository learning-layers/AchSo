
Version 0.9.10
- Opened a public intent "fi.aalto.legroup.achso.action.RECORD" that can be used to start Ach so! video recording from other apps.
- Further adjusted uses-feature -info to allow more devices that ought to see Ach so! in Play store, but were excluded because irrelevant oddities in cameras.

Version 0.9.9
- Fixed implicit requirements to be less strict: there should be 1000+ more compatible devices.
- Fix for failure to import videos from device in KitKat.
- Modifications to server API:
    Added:
    - get_unique_id
    - add_community
    - edit_community
    - join_community
    - leave_community
    - get_communities
    - add_video_to_community
    - remove_video_from_community
    - get_annotations
    - get_videos
    - get_videos_in_community

    Renamed:
    - upload_achso_data -> upload_video_metadata
    - update_achso_data -> update_video_metadata
    - upload_achso_annotation -> upload_annotation
    - update_achso_annotation -> update_annotation
    Implemented most of those to https://github.com/LeGroup/AchSoServer
- Unified behavior of remote and local semanticvideos
- Implemented upload process with ClViTra2 and Aalto AchSoServer: First ask key, assign key for
video in device, send metadata to AchSoServer, send video to ClViTra2, start polling for finished
video transcoding, get urls for transcoded video and thumbnail, patch metadata in AchSoServer to
have video and thumbnail urls.
- Added pending requests-preference to continue video finalizing process if it is still unfinished
when the app shuts down.




Version 0.9.8
- Improvements on stability when showing and editing annotations that are near to each other, in time or in placement.
- Vector-based annotation marker, scaling is prettier.
- Draws a hairline to connect selected annotation and related editing buttons.
- When pausing on annotation, pause can be skipped by tapping on screen, or lengthened by keeping the screen pressed.

Version 0.9.7
-Fixed a bug with inaccurate video seek that caused annotations to get stuck in a loop [#LL-200].
-Added a big red start recording -button for tablet interface.[#LL-209]

Version 0.9.6
-Fixed strange bug when Nexus 7 crashes after QR-code reading.

Version 0.9.5
-Annotations can be scaled
-Annotations can be added with long press
-Annotation timeline is more precise
-Adding videos from device to Ach so!
-Networking support, mostly finished but disabled for this release
-Improved browsing of videos
-Improved QR adding/searching workflow
- Many, many minor fixes and improvements


... earlier version texts are dispersed around our documents.


Fixes for version 0.8.1:
- Fixed Samsung GT-I9000 4.3.1 video recording returning null after record and crashing. From 4.3.1 >, device better support MediaStore.EXTRA_OUTPUT -argument.
- Fixed critical bug that prevents installing in many devices.
- Fixed video path to store in MOVIES not in PICTURES

Fixes for version 0.8:
- Added 'keep' button when editing annotations to give an easier choice when wanting to save changes, but not wanting to edit annotation text.
- Annotation marker now maintains its correct position when bottom row buttons slide to view.
- Show 'Offline' text on menubar instead of Login / Logout when network is not available
- Can be set to login automatically on start, this is saved in SharedPreferences.
- Playback pauses when jumping with timeline, because when playing and jumping to annotation marker, it would start 3 second pause, which feels confusing.
- More visual feedback for 3 second pause if player buttons are visible - Annotation button changes to small progress bar.
- Better looking add annotation -button.
- Renamed classes, some methods and most layouts to better describe their purpose in Ach So!
- Revised layout system to use less layout file versions for different screens and instead use <include> tags to provide those elements that need to adjust for different screens.
- Simplified video playback layout methods to use same principle in each case and detect if all vertical space is used as a hint to hide/show buttons. This should be more general solution to layouts.
- Provided logging to file in device. Log can be found in /Android/data/fi.aalto.legroup.achso/files/achso.log
- Updated Finnish translation
- Fixed misread x-coordinate for touch events on portrait mode videos in landscape view.
- Fixed hiding borders in some small landscape views where the video almost fills the vertical space, but not enough to trigger border hiding.
- Fixed subtitle position in small landscape views (still needs to improve on few devices).
- Fixed missing templates for small but hi-res devices. (Sony xpiria)
- Fixed issue with Samsung GT-I9195 not returning video path.
- Fixed blocker issue with Samsung GT-P5110 and GT-P3110 not saving their recordings. Removed reliance of EXTRA_OUTPUT in recording intent and 1. did the workaround used in AnViAnno + delete temp recording afterwards. 2. Did another workaround based on finding the real path name and moving the file instead of rewriting, since that is very slow with larger videos.
- Added notification if QR code is a link and made the link clickable.