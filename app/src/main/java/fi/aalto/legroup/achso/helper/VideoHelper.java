package fi.aalto.legroup.achso.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.activity.GenreSelectionActivity;
import fi.aalto.legroup.achso.activity.InformationActivity;
import fi.aalto.legroup.achso.activity.VideoPlayerActivity;
import fi.aalto.legroup.achso.database.LocalRawVideos;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.InformationFragment;
import fi.aalto.legroup.achso.util.App;

/**
 * Created by lassi on 3.11.14.
 */
public class VideoHelper {
    public static final int ACTIVITY_VIDEO_BY_RECORDING = 1;
    public static final int ACTIVITY_GENRE_SELECTION = 4;
    public static final int ACTIVITY_VIDEO_BY_PICKING = 5;
    private static Uri videoUri;

    public static void deleteVideos(final Activity activity, final List<SemanticVideo> videos, final ActionMode mode) {
        new AlertDialog.Builder(activity).setTitle(R.string.deletion_title)
                .setMessage(R.string.deletion_question)
                .setPositiveButton(activity.getString(R.string.delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        VideoDBHelper vdb = new VideoDBHelper(activity);
                        for (SemanticVideo sv : videos) {
                            vdb.delete(sv);
                        }
                        vdb.close();
                        videos.clear();

                        if (mode != null) {
                            mode.finish();
                        }

                    }
                })
                .setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private static long createSemanticVideo(Activity activity, Uri videoUri) {
        VideoDBHelper vdb = new VideoDBHelper(activity);
        JsonObject userInfo = App.loginManager.getUserInfo();
        String creator = null;

        if (userInfo != null && userInfo.has("preferred_username")) {
            creator = userInfo.get("preferred_username").getAsString();
        }

        // Generate a location and time based name for the video if we have a location and Geocoder
        // is available, otherwise just a time-based name.

        Location location = App.locationManager.getLastLocation();
        String locationString = null;
        String videoName;

        if (Geocoder.isPresent() && location != null) {
            Geocoder geocoder = new Geocoder(activity);

            try {
                Address address = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1).get(0);
                locationString = address.getAddressLine(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Date now = new Date();
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(activity);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(activity);
        String dateString = dateFormat.format(now);
        String timeString = timeFormat.format(now);

        if (locationString == null) {
            videoName = dateString + " " + timeString;
        } else {
            videoName = String.format("%s, %s %s", locationString, dateString, timeString);
        }

        SemanticVideo video = new SemanticVideo(videoName, videoUri,
                SemanticVideo.Genre.values()[0], creator, location);

        if (VideoDBHelper.getByUri(video.getUri()) != null) {
            Log.i("ActionbarActivity", "Video already exists, abort! Abort!");
            vdb.close();
            return -2;
        }

        if (!video.prepareThumbnails()) {
            Log.i("ActionbarActivity", "Failed to create thumbnails, abort! Abort!");
            vdb.close();
            return -1;
        }

        vdb.insert(video);
        vdb.close();

        return video.getId();
    }

    public static void showVideo(Activity activity, SemanticVideo video) {
        Intent detailIntent = new Intent(activity, VideoPlayerActivity.class);
        detailIntent.putExtra(VideoPlayerActivity.ARG_ITEM_ID, video.getId());
        activity.startActivity(detailIntent);
    }

    public static void videoByRecordingResult(Activity activity, int resultCode, Intent data) {
        activity.finishActivity(ACTIVITY_VIDEO_BY_RECORDING);

        //String videoPath = intent.getData();
        //Log.i("ActionbarActivity", "Received data: "+ intent.getData());
        //Log.i("ActionbarActivity", "Received path "+ videoPath);
        videoUri = data.getData();
        String received_path = LocalRawVideos.getRealPathFromURI(activity, videoUri);
        videoUri = Uri.parse(received_path);
        long video_id = createSemanticVideo(activity, videoUri);
        if (video_id == -1) {
            Toast.makeText(activity, activity.getString(R.string.unknown_format),
                    Toast.LENGTH_LONG).show();
        } else if (video_id == -2) {
            Toast.makeText(activity, activity.getString(R.string.video_already_exists),
                    Toast.LENGTH_LONG).show();
        } else {
            chooseGenre(activity, video_id);
        }
    }

    public static void chooseGenreResult(Activity activity, int resultCode, Intent data) {

    }

    public static void videoByChoosingFileResult(Activity activity, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            videoUri = data.getData();
            String received_path = LocalRawVideos.getRealPathFromURI(activity, videoUri);
            videoUri = Uri.parse(received_path);
            long video_id = createSemanticVideo(activity, videoUri);
            if (video_id == -1) {
                Toast.makeText(activity, activity.getString(R.string.unknown_format),
                        Toast.LENGTH_LONG).show();
            } else if (video_id == -2) {
                Toast.makeText(activity, activity.getString(R.string.video_already_exists),
                        Toast.LENGTH_LONG).show();
            } else {
                Intent i = new Intent(activity, GenreSelectionActivity.class);
                i.putExtra("videoId", video_id);
                chooseGenre(activity, video_id);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("CANCEL", "Video add canceled");
        } else {
            Log.i("ActionBarActivity", "Video add failed.");
        }
    }

    public static void chooseGenre(Activity activity, long videoId) {
        Intent i = new Intent(activity, GenreSelectionActivity.class);
        i.putExtra("videoId", videoId);
        activity.startActivityForResult(i, ACTIVITY_GENRE_SELECTION);
    }

    public static void videoByRecording(Activity activity) {
        File output_file = LocalRawVideos.getNewOutputFile();
        if (output_file != null) {
            App.locationManager.startLocationUpdates();
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            SharedPreferences.Editor e = activity.getSharedPreferences("AchSoPrefs", 0).edit();
            e.putString("videoUri", output_file.getAbsolutePath());
            e.commit();

            // Some Samsung devices seem to have serious problems with using MediaStore.EXTRA_OUTPUT.
            // The only way around it is that hack in AnViAnno, to let ACTION_VIDEO_CAPTURE to record where it wants by default and
            // then get the file and write it to correct place.

            // In this solution the problem is that some devices return null from ACTION_VIDEO_CAPTURE intent
            // where they should return the path. This is reported Android 4.3.1 bug. So let them try the MediaStore.EXTRA_OUTPUT-way


            if (App.API_VERSION >= 18) {
                videoUri = Uri.fromFile(output_file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri); //mVideoUri.toString()); // Set output location
            }

            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // High video quality
            activity.startActivityForResult(intent, ACTIVITY_VIDEO_BY_RECORDING);
        } else {
            new AlertDialog.Builder(activity).setTitle(activity.getApplicationContext().getResources().getString(R.string.storage_error)).setMessage(activity.getApplicationContext().getResources().getString(R.string.detailed_storage_error)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).create().show();
        }
    }

    public static void viewVideoInfo(Activity activity, SemanticVideo video, ActionMode actionMode) {
        Intent informationIntent = new Intent(activity, InformationActivity.class);

        informationIntent.putExtra(VideoPlayerActivity.ARG_ITEM_ID, video.getId());

        activity.startActivity(informationIntent);
        actionMode.finish();
    }

    public static void videoByChoosingFile(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/mp4");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        try {
            activity.startActivityForResult(intent, ACTIVITY_VIDEO_BY_PICKING);
        } catch (ActivityNotFoundException e) {
            Log.e("tag", "No activity can handle picking a file. Showing alternatives.");
        }
    }


}
