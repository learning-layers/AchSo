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

package fi.aalto.legroup.achso.fragment;

import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.annotation.AnnotationSurfaceHandler;
import fi.aalto.legroup.achso.annotation.EditorListener;
import fi.aalto.legroup.achso.annotation.SubtitleManager;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.remote.RemoteResultCache;
import fi.aalto.legroup.achso.remote.RemoteSemanticVideo;
import fi.aalto.legroup.achso.util.Dialog;
import fi.aalto.legroup.achso.util.FloatPosition;
import fi.aalto.legroup.achso.view.AnnotatedSeekBar;
import fi.aalto.legroup.achso.view.VideoControllerView;

import static fi.aalto.legroup.achso.util.App.appendLog;




public class SemanticVideoPlayerFragment extends Fragment implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener, VideoControllerView.MediaPlayerControl, View.OnTouchListener,
        MediaPlayer.OnCompletionListener, OnErrorListener, OnVideoSizeChangedListener, EditorListener,
        MediaPlayer.OnBufferingUpdateListener, VideoControllerView.VideoControllerShowHideListener {

    private static final int NORMAL_MODE = 0;
    private static final int NEW_ANNOTATION_MODE = 1;
    private static final int EDIT_ANNOTATION_MODE = 2;
    private static final int POLL_RATE_MILLISECONDS = 100;
    private static final int IS_PINCHING = 1;
    private static final int WAS_PINCHING = 2;

    private SurfaceView mAnnotationSurface;
    private SurfaceView mVideoSurface;
    private RelativeLayout mVideoSurfaceContainer;
    private MediaPlayer mMediaPlayer;
    private VideoControllerView mController;
    private AnnotationSurfaceHandler mAnnotationSurfaceHandler;
    private LinearLayout mTitleArea;
    private TextView mSubtitleTextView;
    private ProgressBar mBufferProgress;
    private int mBufferPercentage = 0;
    private AnnotationUpdateHandler mUpdateHandler = null;
    private long mVideoId;
    private long mLastPos = 0;
    private Handler mPauseHandler;
    private SavedState mSavedState;
    private int mPinching = 0;
    private boolean mEditableAnnotations;
    private boolean mVideoTakesAllVerticalSpace = false;
    private ScaleGestureDetector mScaleGestureDetector;

    public SemanticVideoPlayerFragment() {
        SubtitleManager.clearSubtitles();
        mEditableAnnotations = true;
    }
    @Override
    public boolean videoFillsVerticalSpace() {
        return mVideoTakesAllVerticalSpace;
    }

    @Override
    public void videoControllerShow() {
        Log.i("SemanticVideoPlayerFragment", "Show video controller");
        ActionBar bar = getActivity().getActionBar();
        RelativeLayout.LayoutParams host_lp = (RelativeLayout.LayoutParams) mVideoSurfaceContainer.getLayoutParams();
        if (mTitleArea != null && bar != null) {
            bar.show();
            mTitleArea.bringToFront();
            mTitleArea.measure(0, 0);
            //TranslateAnimation slide = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) bar.getHeight());
            //slide.setInterpolator(getActivity(), android.R.anim.decelerate_interpolator);
            //slide.setDuration(200);
            //slide.setFillAfter(true);
            //mTitleArea.startAnimation(slide);
            host_lp.setMargins(0, 0, 0, 0); //bar.getHeight()
        }
    }

    @Override
    public void videoControllerHide() {
        Log.i("SemanticVideoPlayerFragment", "Hide video controller");
        ActionBar bar = getActivity().getActionBar();
        LinearLayout l = (LinearLayout) getActivity().findViewById(R.id.video_title_area);
        RelativeLayout.LayoutParams host_lp = (RelativeLayout.LayoutParams) mVideoSurfaceContainer.getLayoutParams();
        if (l != null && bar != null && host_lp != null) {
            bar.hide();
            l.bringToFront();
            l.measure(0, 0);
            host_lp.setMargins(0, 0, 40, 0); //bar.getHeight()


            //TranslateAnimation slide = new TranslateAnimation(0.0f, 0.0f, (float) getActivity().getActionBar()
            //        .getHeight(), 0.0f);
            //slide.setInterpolator(getActivity(), android.R.anim.accelerate_interpolator);
            //slide.setDuration(175);
            //slide.setFillAfter(true);
            //slide.setFillBefore(true);
            //l.startAnimation(slide);
        }
    }

    //public long getVideoId() {
    //    return mVideoId;
    //}

    @Override
    public void newAnnotation(FloatPosition position) {
        Annotation ann = mAnnotationSurfaceHandler.addAnnotation(mMediaPlayer.getCurrentPosition(), position);
        mController.setCurrentAnnotation(ann);
        mAnnotationSurfaceHandler.select(ann);
        mAnnotationSurfaceHandler.draw();
        appendLog(String.format("Added new annotation %s to video %d at time %d", ann.toString(), mVideoId, mMediaPlayer.getCurrentPosition()));
    }

    @Override
    public void addTextToAnnotation(final Annotation a) {
        Dialog.getTextSetterDialog(getActivity(), a, a.getText(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAnnotationSurfaceHandler.select(null);
                mAnnotationSurfaceHandler.draw(a);
            }
        }, a).show();
    }

    @Override
    public void drawAnnotations() {
        mAnnotationSurfaceHandler.draw();
    }

    @Override
    public void deleteAnnotation(final Annotation a) {
        mAnnotationSurfaceHandler.removeAnnotation(a);
        mAnnotationSurfaceHandler.draw();
        appendLog(String.format("Deleted annotation %s.", a.toString()));
    }

    @Override
    public void revertAnnotationChanges(final Annotation a) {
        a.revertToRemembered();
        mAnnotationSurfaceHandler.draw();
        appendLog(String.format("Canceled annotation change for %s.", a.toString()));
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mBufferPercentage = percent;
        if (mBufferProgress != null) {
            if (mMediaPlayer.isPlaying()) {
                mBufferProgress.setVisibility(View.GONE);
            } else if (mBufferPercentage != 100) {
                mBufferProgress.setVisibility(View.VISIBLE);
            }
        }
    }

    public void onProgressChanged(long pos) {
        final List<Annotation> annotationsToShow = new ArrayList<Annotation>();
        for (final Annotation a : mAnnotationSurfaceHandler.getAnnotations()) {
            final long aTime = a.getStartTime();
            if (pos >= aTime && aTime > mLastPos && a.isAlive()) {
                if (mMediaPlayer.isPlaying()) {
                    annotationsToShow.add(a);
                }
            } else {
                a.setVisible(false);
            }
        }
        if (!annotationsToShow.isEmpty()) {
            mController.setAnnotationPausedMode(true);
            mAnnotationSurfaceHandler.showMultiple(annotationsToShow);
            pause();
            mPauseHandler = new Handler();
            mPauseHandler.postDelayed(new Runnable() {
                private int counter = 0;

                @Override
                public void run() {
                    counter += POLL_RATE_MILLISECONDS;
                    if (counter < Annotation.ANNOTATION_SHOW_DURATION_MILLISECONDS && mController.isPausedForShowingAnnotation()) {
                        mController
                                .setAnnotationPausedProgress((int) ((counter * 100) / Annotation.ANNOTATION_SHOW_DURATION_MILLISECONDS));
                        mPauseHandler.postDelayed(this, POLL_RATE_MILLISECONDS);
                    } else {
                        for (Annotation a : annotationsToShow) {
                            a.setVisible(false);
                        }
                        if (mMediaPlayer != null) { // Fragment may be closed
                            // during the wait
                            mMediaPlayer.start();
                            mAnnotationSurfaceHandler.select(null);
                            mAnnotationSurfaceHandler.draw();
                            mController.setAnnotationPausedMode(false);
                            if (mController.isShowing()) {
                                mController.show();
                            }
                        }
                    }
                }
            }, POLL_RATE_MILLISECONDS);
        }
        mLastPos = pos;
        mAnnotationSurfaceHandler.draw();
    }

    public void setEditableAnnotations(boolean editableAnnotations) {
        mEditableAnnotations = editableAnnotations;
    }

    public void onPause() {
        super.onPause();
        mAnnotationSurfaceHandler.stopRectangleAnimation();
        if (mMediaPlayer != null) {
            SharedPreferences.Editor e = getActivity().getSharedPreferences("AchSoPrefs", 0).edit();
            e.putBoolean("stateSaved", true);
            e.putBoolean("annotationModeAvailable", mController.isAnnotationModeAvailable());
            if (mController.annotationModeIsEdit()) {
                e.putInt("annotationMode", EDIT_ANNOTATION_MODE);
            } else if (mController.isAnnotationModeEnabled()) {
                e.putInt("annotationMode", NEW_ANNOTATION_MODE);
            } else {
                e.putInt("annotationMode", NORMAL_MODE);
            }
            if (mController.getCurrentAnnotation() != null) {
                e.putLong("annotationId", mController.getCurrentAnnotation().getId());
            } else {
                e.putLong("annotationId", -1);
            }
            e.putLong("videoId", mVideoId);
            e.putBoolean("playing", mMediaPlayer.isPlaying());
            e.putInt("position", mMediaPlayer.getCurrentPosition());
            e.commit();

            mMediaPlayer.release();
            mController.removeMessages(); // Prevent mController from using
            // released mMediaPlayer
            mMediaPlayer = null;
        }
    }

    public void onStop() {
        super.onStop();
        if (mMediaPlayer != null) {
            mController.removeMessages(); // Prevent mController from using
            // released mMediaPlayer
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void createMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnPreparedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("SemanticVideoPlayerFragment", "resuming video player - reorganizing surfaces if necessary");

        Bundle args = getArguments();
        mSavedState = new SavedState();
        if (!mSavedState.ok)
            mSavedState = null;
        if (mBufferProgress == null) {
            mBufferProgress = (ProgressBar) getView().findViewById(R.id.video_buffer_progress);
        }
        if (args != null && mController == null && mMediaPlayer == null) {
            mVideoId = args.getLong(VideoViewerFragment.ARG_ITEM_ID);
            boolean cached_video = (mVideoId == -1);            
            mVideoSurface = (SurfaceView) getView().findViewById(R.id.video_surface);
            mAnnotationSurface = (SurfaceView) getView().findViewById(R.id.annotation_surface);
            mAnnotationSurface.setZOrderOnTop(true);
            mSubtitleTextView = (TextView) getView().findViewById(R.id.subtitle);
            SurfaceHolder annotation_holder = mAnnotationSurface.getHolder();
            if (annotation_holder != null)
                annotation_holder.setFormat(PixelFormat.TRANSPARENT);
            mAnnotationSurfaceHandler = new AnnotationSurfaceHandler(getActivity(), mAnnotationSurface, mVideoId);
            if (cached_video) {
                RemoteSemanticVideo rsv = RemoteResultCache.getSelectedVideo();
                mAnnotationSurfaceHandler.setAnnotations(rsv.getAnnotations(getActivity()));
            } else if (mSavedState != null) {
                VideoDBHelper dbh = new VideoDBHelper(getActivity());
                mAnnotationSurfaceHandler.setAnnotations(dbh.getAnnotations(mSavedState.videoId));
                dbh.close();
            }
            mVideoSurfaceContainer = (RelativeLayout) getView().findViewById(R.id.video_surface_container);
            SurfaceHolder video_holder = mVideoSurface.getHolder();
            if (video_holder != null) {
                video_holder.addCallback(this);
            }
            createMediaPlayer();
            mUpdateHandler = new AnnotationUpdateHandler(this, mMediaPlayer);

            SemanticVideo semantic_video;
            if (cached_video) {
                semantic_video = RemoteResultCache.getSelectedVideo();
            } else {
                semantic_video = VideoDBHelper.getById(mVideoId);
            }
            mController = new VideoControllerView(getActivity(), this, mEditableAnnotations);
            mController.setVideoControllerShowHideListener(this);
            if (mSavedState != null) {
                mController.setAnnotationModeAvailable(mSavedState.annotationModeAvailable);
            }

            setVideo(semantic_video.getUri());
        } else if (mMediaPlayer == null) {
            createMediaPlayer();
            SemanticVideo semantic_video = VideoDBHelper.getById(mVideoId);
            setVideo(semantic_video.getUri());
        }
        if (mBufferProgress != null && mVideoId != -1) {
            mBufferProgress.setVisibility(View.GONE);
        }
        mTitleArea = (LinearLayout) getActivity().findViewById(R.id.video_title_area);
    }

    public void recalculateSurfaceSize() {
        Log.i("SemanticVideoPlayerFragment", "Recalculating surface size");
        int available_width = mVideoSurface.getWidth();
        int available_height = mVideoSurface.getHeight();
        float ratio = ((float) mMediaPlayer.getVideoWidth()) / mMediaPlayer.getVideoHeight();
        assert(mVideoSurface != null);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mVideoSurface.getLayoutParams();
        RelativeLayout.LayoutParams host_lp = (RelativeLayout.LayoutParams) mVideoSurfaceContainer.getLayoutParams();
        assert(lp != null);
        Log.i("SemanticVideoPlayerFragment", String.format("mMediaPlayer: %d, %d mVideoSurface(lp): %d, %d .mVideoSurface(wh): %d, %d  Ratio: %f", mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight(), lp.width, lp.height, mVideoSurface.getWidth(), mVideoSurface.getHeight(), ratio));
        if (available_height > (int) (available_width / ratio) + (int) (available_height * 0.1)) {
            lp.width = available_width;
            lp.height = (int) (available_width / ratio);
            mVideoTakesAllVerticalSpace = false;
        } else {
            lp.width = (int) (available_height * ratio);
            lp.height = available_height;
            mVideoTakesAllVerticalSpace = true;
            ActionBar bar = getActivity().getActionBar();
            if (bar != null) {
                if (bar.isShowing()) {
                    host_lp.setMargins(0, 0, 0, 0); //bar.getHeight()
                } else {
                    host_lp.setMargins(0, 0, 40, 0); //bar.getHeight()
                }
            };
            mVideoSurfaceContainer.setLayoutParams(host_lp);


        }

        mVideoSurface.setLayoutParams(lp);
        mAnnotationSurface.setLayoutParams(lp);
        Log.i("SemanticVideoPlayerFragment", String.format("recalculated video/annotation surface size, w:%d h:%d", lp.width, lp.height));
    }

    public boolean videoIsPortrait() {
        return (mMediaPlayer.getVideoHeight() > mMediaPlayer.getVideoWidth());
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup container, Bundle savedInstanceState) {
        View v = i.inflate(R.layout.activity_video_player, container, false);
        assert(v != null);
        mBufferProgress = (ProgressBar) v.findViewById(R.id.video_buffer_progress);
        v.setOnTouchListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(getActivity(),
                new simpleOnScaleGestureListener());
        mTitleArea = (LinearLayout) getActivity().findViewById(R.id.video_title_area);
        return v;
    }

    public void setVideo(Uri uri) {
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(getActivity(), uri);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mMediaPlayer == null)
            return;
        mMediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("SemanticVideoPlayerFragment", String.format("surfaceChanged called -- format: %d width: %d height %d ", format, width, height));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i("SemanticVideoPlayerFragment", "prepared video with MediaPlayer "+mp.toString());
        recalculateSurfaceSize();
        mController.setMediaPlayer(this);
        ViewGroup controllerView = (FrameLayout) getView().findViewById(R.id.video_controller_container);
        mController.setAnchorView(controllerView);
        if (mSavedState != null)
            seekTo(mSavedState.position, false);
            // Do not draw annotations this time (because of possible orientation change)
        SubtitleManager.setSubtitleTextView(mSubtitleTextView);
        mController.show();
        if (mSavedState == null || mSavedState.playing) {
            start();
        }
        mTitleArea.bringToFront();
        if (mSavedState != null) {
            Annotation a = mAnnotationSurfaceHandler.getAnnotation(mSavedState.annotationId);
            switch (mSavedState.annotationMode) {
                case NEW_ANNOTATION_MODE:
                    mController.enableNewAnnotationModeForAnnotation(a);
                    break;
                case EDIT_ANNOTATION_MODE:
                    mController.enableAnnotationEditModeForAnnotation(a);
                    break;
            }
        }
        mUpdateHandler.sendEmptyMessage(0);

        AnnotatedSeekBar asb = (AnnotatedSeekBar) mController.getProgressBar();
        asb.setAnnotationSurfaceHandler(mAnnotationSurfaceHandler);

        SubtitleManager.updateVisibleSubtitles();

        // This message queue hack is needed, because for some unknown reason
        // mAnnotationSurface
        // is not yet resized at this point (even if it is resized in
        // calculateSurfaceSize()).
        // Adding the drawing of annotations to message queue will run the
        // drawing operations after mAnnotationSurface is correctly resized.
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mAnnotationSurfaceHandler.draw();
            }
        });
    }

    @Override
    public void start() {
        if (mMediaPlayer == null)
            return;
        mAnnotationSurfaceHandler.stopRectangleAnimation();

        mMediaPlayer.start();
        mAnnotationSurfaceHandler.select(null);
        mAnnotationSurfaceHandler.draw();
        mController.updatePausePlay();
        mController.hide();
        //ViewGroup area = (ViewGroup) rootView.findViewById(R.id.video_title_area);
        //ViewGroup area_host = (ViewGroup) area.getParent();
        //area_host.removeView(area);
        //area_host.addView(area);

    }

    @Override
    public void pause() {
        if (mMediaPlayer == null)
            return;
        mMediaPlayer.pause();
        mController.updatePausePlay();
    }

    @Override
    public int getDuration() {
        if (mMediaPlayer == null)
            return -1;
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        if (mMediaPlayer == null)
            return -1;
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        if (mController.isPausedForShowingAnnotation()) {
            mController.setAnnotationPausedMode(false);
        }
        seekTo(pos, true);
    }

    // When recovering from orientation change, redrawing annotations would
    // place them incorrectly. This method can be used to prevent annotation
    // layer from drawing.
    private void seekTo(int pos, boolean redrawAnnotations) {
        if (mMediaPlayer == null)
            return;

        mMediaPlayer.seekTo(pos);
        if (redrawAnnotations) {
            mAnnotationSurfaceHandler.draw();
        }
    }

    @Override
    public boolean isPlaying() {
        return (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    @Override
    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void toggleFullScreen() {
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //Log.i("SemanticVideoPlayerFragment", "TouchEvent x: " + event.getX() + " y: " + event.getY());
        //Log.i("SemanticVideoPlayerFragment", "Video size w: " + mVideoSurface.getWidth() + " h: " + mVideoSurface.getHeight());
        //Log.i("SemanticVideoPlayerFragment", "view size w: " + getView().getWidth() + " h: " + getView().getHeight());
        // getView().getHeight()/2 returns the center of the big surfaceview
        // mVideoSurface.getHeight()/2 returns the center of the video shown
        // These two are subtracted from event.getY() to get the actual position
        // on the (possibly) scaled video
        mScaleGestureDetector.onTouchEvent(event);
        if (mPinching != NORMAL_MODE) {
            if (mPinching == WAS_PINCHING && event.getAction() == MotionEvent.ACTION_UP) {
                mPinching = NORMAL_MODE;
                mController.show();
            }
            return true;
        }

        float width_diff = ((getView().getWidth() / 2) - (mVideoSurface.getWidth() / 2));
        float height_diff = ((getView().getHeight() / 2) - (mVideoSurface.getHeight() / 2));
        float x = (event.getX() - width_diff) / mVideoSurface.getWidth();
        float y = (event.getY() - height_diff) / mVideoSurface.getHeight();
        //Log.e("SemanticVideoPlayerFragment", String.format("x:%f y:%f , event x: %f event y: %f, width_diff: %f height_diff: %f ", x, y, event.getX(), event.getY(), width_diff, height_diff)  );
        FloatPosition position = new FloatPosition(x, y);
        if (isPlaying() && canPause()) {
            mController.doPauseResume();
            mController.show();
        }
        if (mController.isAnnotationModeEnabled()) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mController.hide();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                mController.show();
            } else {
                mController.getCurrentAnnotation().setPosition(position);
            }
            mAnnotationSurfaceHandler.draw();
        } else {
            Annotation a = mAnnotationSurfaceHandler.getAnnotation(position);
            if (a != null && !isPlaying() && !mController.isPausedForShowingAnnotation()) {
                mAnnotationSurfaceHandler.stopRectangleAnimation();
                mController.enableAnnotationEditModeForAnnotation(a);
                mController.hide();
            } else {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        //_longPressDetector.postDelayed(_longPressed,
                        //        ANNOTATION_ACTIVATION_DELAY);
                        mAnnotationSurfaceHandler.startRectangleAnimation
                                (position, mController);
                        //mAnnotationSurfaceHandler.drawIncoming();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mAnnotationSurfaceHandler.moveRectangleAnimation(position);
                        break;
                    case MotionEvent.ACTION_UP:
                        mAnnotationSurfaceHandler.stopRectangleAnimation();
                        break;
                }
                return true;
            }
        }
        return true;
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        mController.updatePausePlay();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
    }

    private static class AnnotationUpdateHandler extends Handler {
        private final WeakReference<SemanticVideoPlayerFragment> mFragment;
        private final WeakReference<MediaPlayer> mMediaPlayer;

        AnnotationUpdateHandler(SemanticVideoPlayerFragment fragment, MediaPlayer player) {
            mFragment = new WeakReference<SemanticVideoPlayerFragment>(fragment);
            mMediaPlayer = new WeakReference<MediaPlayer>(player);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaPlayer mp = mMediaPlayer.get();
            SemanticVideoPlayerFragment pf = mFragment.get();
            if (mp == null || pf == null) {
                return;
            }
            try {
                if (mp.isPlaying()) {
                    pf.onProgressChanged(mp.getCurrentPosition());
                }
                sendEmptyMessageDelayed(0, POLL_RATE_MILLISECONDS);
            } catch (IllegalStateException e) {
                mFragment.clear();
                mMediaPlayer.clear();
            }
        }
    }

    private class SavedState {
        public boolean ok;
        public boolean playing;
        public int position;
        public boolean annotationModeAvailable;
        public int annotationMode;
        public long annotationId;
        public long videoId;
        public SavedState() {
            SharedPreferences prefs = getActivity().getSharedPreferences("AchSoPrefs", 0);
            this.ok = prefs.getBoolean("stateSaved", false);
            this.annotationModeAvailable = prefs.getBoolean("annotationModeAvailable", false);
            this.playing = prefs.getBoolean("playing", false);
            this.position = prefs.getInt("position", 0);
            this.annotationMode = prefs.getInt("annotationMode", -1);
            this.annotationId = prefs.getLong("annotationId", -1);
            this.videoId = prefs.getLong("videoId", -1);
        }
    }

    public class simpleOnScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mController.isAnnotationModeEnabled()) {
                Annotation a = mController.getCurrentAnnotation();
                a.setScaleFactor(a.getScaleFactor() * detector.getScaleFactor());
                mAnnotationSurfaceHandler.draw();
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mAnnotationSurfaceHandler.stopRectangleAnimation();
            mPinching = IS_PINCHING;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mController.isAnnotationModeEnabled()) {
                //mController.getCurrentAnnotation().
            }
            mPinching = WAS_PINCHING;
        }


    }
}
