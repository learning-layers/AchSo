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

package fi.aalto.legroup.achso.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.annotation.Annotation;
import fi.aalto.legroup.achso.annotation.AnnotationSurfaceHandler;
import fi.aalto.legroup.achso.annotation.EditorListener;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.SemanticVideoPlayerFragment;
import fi.aalto.legroup.achso.util.App;
import fi.aalto.legroup.achso.util.FloatPosition;

/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 * <p/>
 * The way to use this class is to instantiate it programatically.
 * The MediaController will create a default set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 * <p/>
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 * <p/>
 * MediaController will hide and
 * show the buttons according to these rules:
 * <ul>
 * <li> The "previous" and "next" buttons are hidden until setPrevNextListeners()
 * has been called
 * <li> The "previous" and "next" buttons are visible but disabled if
 * setPrevNextListeners() was called with null listeners
 * <li> The "rewind" and "fastforward" buttons are shown unless requested
 * otherwise by using the MediaController(Context, boolean) constructor
 * with the boolean set to false
 * </ul>
 */
public class VideoControllerView extends FrameLayout {
    private static final String TAG = "VideoControllerView";

    public static final int PLAY_MODE = 0;
    public static final int PAUSE_MODE = 1;
    public static final int ANNOTATION_PAUSE_MODE = 2;
    public static final int NEW_ANNOTATION_MODE = 3;
    public static final int EDIT_ANNOTATION_MODE = 4;

    private static final int sDefaultTimeout = 3000;
    private final AnnotationSurfaceHandler mAnnotationSurfaceHandler;
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };
    private View.OnClickListener mFullscreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            doToggleFullscreen();
            show(sDefaultTimeout);
        }
    };
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private final int mInfinity = 3600000;
    private final int mAnimationLengthInMs = 100;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private MediaPlayerControl mPlayer;
    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos -= 1000; // milliseconds
            mPlayer.seekTo(pos, SemanticVideoPlayerFragment.DO_NOTHING);
            updateProgress();
            mAnnotationSurfaceHandler.showAnnotationsAt(pos);
            mAnnotationSurfaceHandler.draw();

            show(sDefaultTimeout);
        }
    };
    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos += 1000; // milliseconds
            mPlayer.seekTo(pos, SemanticVideoPlayerFragment.DO_NOTHING);
            updateProgress();
            mAnnotationSurfaceHandler.showAnnotationsAt(pos);
            mAnnotationSurfaceHandler.draw();

            show(sDefaultTimeout);
        }
    };
    private Context mContext;
    private ViewGroup mAnchor;
    private View mRoot;
    private ProgressBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mSuperSeekListener.onStartTrackingTouch(bar);
            //Log.i("VIdeoControllerView", "Started tracking touch");
            // moving to annotation markers while play is on creates a confusing situation
            // as 3 second pause starts. So we always toggle pause on when we jump on timeline.
            if (mPlayer.isPlaying() && mPlayer.canPause()) {
                doPauseResume();
                show();
            }
            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and

            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            mSuperSeekListener.onProgressChanged(bar, progress, fromuser);

            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            //Log.i("VideoControllerView", "OnProgressChanged called: " + progress);
            //mPlayer.seekTo(progress);
            AnnotatedSeekBar asb = (AnnotatedSeekBar) mProgress;
            if (!asb.suggests_position) {
                mPlayer.seekTo(progress, SemanticVideoPlayerFragment.DO_NOTHING);
            }
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime(progress));

        }

        public void onStopTrackingTouch(SeekBar bar) {
            mSuperSeekListener.onStopTrackingTouch(bar);

            mDragging = false;
            updateProgress();
            //Log.i("VideoControllerView", "Stopped tracking touch");
            //show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };
    private boolean mUseFastForward;
    private boolean mFromXml;
    private ImageButton mPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private Button mAnnotationButton;
    private Button mEditTextButton;
    private Button mKeepButton;
    private Button mCancelButton;
    private Button mDeleteButton;
    private TextView mCreatorText;
    public int controllerMode;
    private OnClickListener mAnnotationButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            if (controllerMode == ANNOTATION_PAUSE_MODE) {
                setControllerMode(PAUSE_MODE);
            } else if (!mPlayer.isPlaying()) addNewAnnotation(null);
        }
    };
    private Handler mHandler = new MessageHandler(this);
    private ProgressBar mAnnotationPausedProgress;
    private EditorListener mEditorListener;
    private Annotation mCurrentAnnotation;
    private OnClickListener mEditTextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mEditorListener.addTextToAnnotation(mCurrentAnnotation);
            saveCurrentAnnotation();
            setCurrentAnnotation(null);
            setControllerMode(PAUSE_MODE);
        }
    };
    private OnClickListener mDeleteListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mEditorListener.deleteAnnotation(mCurrentAnnotation);
            setControllerMode(PAUSE_MODE);
        }
    };
    private boolean mHideAnimationRunning = false;
    private boolean mShowAnimationRunning = false;
    private SemanticVideo.Genre mVideoGenre;
    private boolean mAttached = false;
    private boolean mCurrentAnnotationIsNew = false;
    private OnClickListener mCancelListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            if (mCurrentAnnotation != null) {
                if (controllerMode == NEW_ANNOTATION_MODE)
                    mEditorListener.deleteAnnotation(mCurrentAnnotation);
                else mEditorListener.revertAnnotationChanges(mCurrentAnnotation);
            }
            setControllerMode(PAUSE_MODE);
            setCurrentAnnotation(null);
        }
    };
    private boolean mCanAnnotate = true;
    private VideoControllerShowHideListener mControllerListener = null;
    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener mSuperSeekListener; // This is the seek listener that is defined in AnnotatedSeekBar.java
    private OnClickListener mKeepListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            saveCurrentAnnotation();
            setCurrentAnnotation(null);
            setControllerMode(PAUSE_MODE);
        }
    };

    public void saveCurrentAnnotation() {
        Annotation a = getCurrentAnnotation();
        VideoDBHelper vdb = new VideoDBHelper(mContext);
        vdb.update(a);
        vdb.close();
    }

    public VideoControllerView(Context context, boolean useFastForward, EditorListener listener,
                               boolean annotationModeAvailable,
                               AnnotationSurfaceHandler annotationSurfaceHandler) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
        mEditorListener = listener;
        mCanAnnotate = annotationModeAvailable;
        mAnnotationSurfaceHandler = annotationSurfaceHandler;
    }

    public void setVideoControllerShowHideListener(VideoControllerShowHideListener l) {
        mControllerListener = l;
    }

    public boolean isAnnotationModeAvailable() {
        return mCanAnnotate;
    }

    public void setAnnotationModeAvailable(boolean b) {
        mCanAnnotate = b;
    }

    public ProgressBar getProgressBar() {
        return mProgress;
    }

    public Annotation getCurrentAnnotation() {
        return mCurrentAnnotation;
    }

    public void setCurrentAnnotation(Annotation a) {
        if (a == null && mCurrentAnnotation == null) {
            return;
        }
        mAnnotationSurfaceHandler.select(a);
        mAnnotationSurfaceHandler.draw();
        mCurrentAnnotation = a;
        if (a != null) {
            a.rememberState();
        }

        if (a != null) {
            mCreatorText.setText(a.getCreator());

            JsonObject userInfo = App.loginManager.getUserInfo();
            String creator = null;

            boolean isCurrent = false;
            if (userInfo != null && userInfo.has("preferred_username")) {
                creator = userInfo.get("preferred_username").getAsString();
                if (creator.equals(a.getCreator())) {
                    isCurrent = true;
                }
            } else {
                isCurrent = true;
            }

            if (isCurrent) {
                mDeleteButton.setEnabled(true);
                mEditTextButton.setEnabled(true);
                mKeepButton.setEnabled(true);
            } else {
                mDeleteButton.setEnabled(false);
                mEditTextButton.setEnabled(false);
                mKeepButton.setEnabled(false);
            }
        }

        mCurrentAnnotationIsNew = false;
    }

    public boolean screenIsPortrait() {
        Configuration conf = getResources().getConfiguration();
        return (conf != null && conf.orientation == Configuration.ORIENTATION_PORTRAIT);
    }


    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        mAnchor = view;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        Log.i("VideoControllerView", "Setting anchor view -- removing and adding control view");
        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mAnnotationPausedProgress = (ProgressBar) v.findViewById(R.id.annotation_delay_progress);
        mAnnotationPausedProgress.setVisibility(View.GONE);

        mFfwdButton = (ImageButton) v.findViewById(R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            if (!mFromXml) {
                mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        mRewButton = (ImageButton) v.findViewById(R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            if (!mFromXml) {
                mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        mAnnotationButton = (Button) v.findViewById(R.id.add_annotation_button);
        if (mAnnotationButton != null) {
            if (mCanAnnotate) {
                //Drawable d=getResources().getDrawable(R.drawable.ic_square);
                //if(d!=null) {
                //    d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
                //    mAnnotationButton.setCompoundDrawables(null, null, null, d);
                //}
                mAnnotationButton.setOnClickListener(mAnnotationButtonListener);
            } else mAnnotationButton.setVisibility(View.GONE);
        }

        mEditTextButton = (Button) v.findViewById(R.id.edit_annotation_button);
        if (mEditTextButton != null) {
            mEditTextButton.setVisibility(View.GONE);
            mEditTextButton.setOnClickListener(mEditTextListener);
        }
        mKeepButton = (Button) v.findViewById(R.id.keep_annotation_button);
        if (mKeepButton != null) {
            mKeepButton.setVisibility(View.GONE);
            mKeepButton.setOnClickListener(mKeepListener);
        }
        mCancelButton = (Button) v.findViewById(R.id.cancel_annotation_button);
        if (mCancelButton != null) {
            mCancelButton.setOnClickListener(mCancelListener);
            mCancelButton.setVisibility(View.GONE);
        }
        mDeleteButton = (Button) v.findViewById(R.id.delete_annotation_button);
        if (mDeleteButton != null) {
            mDeleteButton.setOnClickListener(mDeleteListener);
            mDeleteButton.setVisibility(View.GONE);
        }

        mCreatorText = (TextView) v.findViewById(R.id.annotation_creator_text);
        if (mCreatorText != null) {
            mCreatorText.setVisibility(View.GONE);
        }

        mProgress = (ProgressBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof AnnotatedSeekBar) {
                AnnotatedSeekBar seeker = (AnnotatedSeekBar) mProgress;
                mSuperSeekListener = seeker.seekBarChangeListener;
                seeker.setOnSeekBarChangeListener(mSeekListener);
                seeker.setController(this);
            }
            mProgress.setMax(1000); // this will be set to video length
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        if (mPlayer == null) {
            return;
        }

        try {
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !mPlayer.canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !mPlayer.canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     *                the controller until hide() is called.
     */
    public void show(int timeout) {
        if (mShowing || mShowAnimationRunning) {
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
            return;
        }

        if (mPlayer == null) return;
        if (mHideAnimationRunning) {
            mHideAnimationRunning = false;
            this.getAnimation().cancel();
            this.clearAnimation();
        }

        // Always show controls if the video is stopped
        if (!mPlayer.isPlaying()) timeout = mInfinity;

        if (!mShowing && mAnchor != null) {
            updateProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButtons();

            if (!mAttached) {
                Log.i("VideoControllerView", "Attaching VideoControllerView back to anchorView.");
                FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM
                );

                mAnchor.addView(this, tlp);
                mAttached = true;
            }

            if (mControllerListener != null && mControllerListener.videoFillsVerticalSpace()) {
                LinearLayout lo = (LinearLayout) findViewById(R.id.media_controllers);
                lo.measure(0, 0);
                TranslateAnimation slide = new TranslateAnimation(0.0f, 0.0f, lo.getMeasuredHeight(), 0.0f);
                slide.setDuration(mAnimationLengthInMs);
                slide.setFillAfter(true);
                slide.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mShowAnimationRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (mShowAnimationRunning) {
                            mShowAnimationRunning = false;
                            mEditorListener.drawAnnotations();
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });


                this.startAnimation(slide);
                mControllerListener.videoControllerShow();
            }
            mShowing = true;
        }

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.

        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        //Message msg = mHandler.obtainMessage(FADE_OUT);
        //if (timeout != 0) {
        //    mHandler.removeMessages(FADE_OUT);
        //    mHandler.sendMessageDelayed(msg, timeout);
        //}
    }

    public boolean isShowing() {
        return mShowing;
    }

    //public boolean isAnnotationModeEnabled() {
    //    return mAnnotationModeEnabled;
    //}


    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mHideAnimationRunning || !mShowing) {
            return;
        }
        if (mAnchor == null) {
            return;
        }

        if (mShowAnimationRunning) {
            mShowAnimationRunning = false;
            this.getAnimation().cancel();
            this.clearAnimation();
        }
        if (mControllerListener != null && mControllerListener.videoFillsVerticalSpace()) {
            // Animate the hiding of the controller
            LinearLayout lo = (LinearLayout) findViewById(R.id.media_controllers);
            lo.measure(0, 0);
            TranslateAnimation slide = new TranslateAnimation(0.0f, 0.0f, 0.0f, lo.getMeasuredHeight());
            slide.setDuration(mAnimationLengthInMs);

            slide.setFillAfter(true);
            slide.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mHideAnimationRunning = true;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mHideAnimationRunning) {
                        mShowing = false;
                        mHideAnimationRunning = false;
                        mHandler.removeMessages(SHOW_PROGRESS);
                        mEditorListener.drawAnnotations();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            this.startAnimation(slide);
            mControllerListener.videoControllerHide();
        } else {
            Log.i("VideoControllerView", "Ignore hide.");
        }
    }

    public void removeMessages() {
        mHandler.removeMessages(SHOW_PROGRESS);
        mHandler.removeMessages(FADE_OUT);
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int updateProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                mProgress.setProgress(position);
            }
            float percent = mPlayer.getBufferPercentage() / 100f;
            int buffer_position = (int) (percent * duration);
            mProgress.setSecondaryProgress(buffer_position);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // everything should be done in SemanticVideoPlayerFragment.
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mPlayer == null) {
            return true;
        }

        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                setControllerMode(PLAY_MODE);
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                setControllerMode(PAUSE_MODE);
                show(mInfinity);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }


    public void doPauseResume() {
        if (mPlayer == null) {
            return;
        }
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            setControllerMode(PAUSE_MODE);
        } else {
            mPlayer.start();
            setControllerMode(PLAY_MODE);
        }
        mEditorListener.drawAnnotations();
    }

    private void doToggleFullscreen() {
        if (mPlayer == null) {
            return;
        }

        mPlayer.toggleFullScreen();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(VideoControllerView.class.getName());
    }

    @SuppressLint("NewApi")
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(VideoControllerView.class.getName());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }


    public void fakeNewAnnotationModeForAnnotation(Annotation a) {
        // Fake new annotation mode for existing annotation -- this happens if screen is restored
        // (because of rotation) during the creation of new annotation.
        setCurrentAnnotation(a);
        setControllerMode(NEW_ANNOTATION_MODE);
        mCurrentAnnotationIsNew = true;
    }

    //public boolean annotationModeIsEdit() {
    //    return mAnnotationModeEnabled && !mCurrentAnnotationIsNew;
    //}

    public void setControllerMode(int mode) {
        switch (mode) {
            case PLAY_MODE:
                setCurrentAnnotation(null);
                mAnnotationPausedProgress.setVisibility(View.GONE);
                mRewButton.setVisibility(View.VISIBLE);
                mPauseButton.setImageResource(R.drawable.pause);
                mPauseButton.setVisibility(View.VISIBLE);
                mFfwdButton.setVisibility(View.VISIBLE);
                if (mCanAnnotate) {
                    mAnnotationButton.setVisibility(View.VISIBLE);
                }
                mProgress.setVisibility(View.VISIBLE);
                mCurrentTime.setVisibility(View.VISIBLE);
                mEndTime.setVisibility(View.VISIBLE);

                mEditTextButton.setVisibility(View.GONE);
                mKeepButton.setVisibility(View.GONE);
                mCancelButton.setVisibility(View.GONE);
                mDeleteButton.setVisibility(View.GONE);
                mCreatorText.setVisibility(View.GONE);
                break;
            case PAUSE_MODE:
                setCurrentAnnotation(null);
                mAnnotationPausedProgress.setVisibility(View.GONE);
                mRewButton.setVisibility(View.VISIBLE);
                mPauseButton.setImageResource(R.drawable.play);
                mPauseButton.setVisibility(View.VISIBLE);
                mFfwdButton.setVisibility(View.VISIBLE);
                if (mCanAnnotate) {
                    mAnnotationButton.setVisibility(View.VISIBLE);
                }
                mProgress.setVisibility(View.VISIBLE);
                mCurrentTime.setVisibility(View.VISIBLE);
                mEndTime.setVisibility(View.VISIBLE);

                mEditTextButton.setVisibility(View.GONE);
                mKeepButton.setVisibility(View.GONE);
                mCancelButton.setVisibility(View.GONE);
                mDeleteButton.setVisibility(View.GONE);
                mCreatorText.setVisibility(View.GONE);
                break;
            case ANNOTATION_PAUSE_MODE:
                mAnnotationPausedProgress.setVisibility(View.VISIBLE);
                mAnnotationButton.setVisibility(View.GONE);
                break;
            case NEW_ANNOTATION_MODE:
                mAnnotationPausedProgress.setVisibility(View.GONE);
                mAnnotationButton.setVisibility(View.GONE);
                mRewButton.setVisibility(View.GONE);
                mPauseButton.setVisibility(View.GONE);
                mFfwdButton.setVisibility(View.GONE);
                mAnnotationButton.setVisibility(View.GONE);
                mProgress.setVisibility(View.GONE);
                mCurrentTime.setVisibility(View.GONE);
                mEndTime.setVisibility(View.GONE);
                mDeleteButton.setVisibility(View.GONE);
                mCreatorText.setVisibility(View.GONE);
                mEditTextButton.setVisibility(View.VISIBLE);
                mKeepButton.setVisibility(View.VISIBLE);
                mCancelButton.setVisibility(View.VISIBLE);
                break;
            case EDIT_ANNOTATION_MODE:
                if (getCurrentAnnotation() == null) {
                    Log.e("VideoControllerView", "Moved to EDIT_ANNOTATION_MODE without " +
                            "annotation selected.");
                }
                mAnnotationPausedProgress.setVisibility(View.GONE);
                mAnnotationButton.setVisibility(View.GONE);
                mRewButton.setVisibility(View.GONE);
                mPauseButton.setVisibility(View.GONE);
                mFfwdButton.setVisibility(View.GONE);
                mAnnotationButton.setVisibility(View.GONE);
                mProgress.setVisibility(View.GONE);
                mCurrentTime.setVisibility(View.GONE);
                mEndTime.setVisibility(View.GONE);
                mDeleteButton.setVisibility(View.VISIBLE);
                mCreatorText.setVisibility(View.VISIBLE);
                mEditTextButton.setVisibility(View.VISIBLE);
                mKeepButton.setVisibility(View.VISIBLE);
                mCancelButton.setVisibility(View.VISIBLE);
                break;
        }
        controllerMode = mode;
    }

    public void setAnnotationPausedProgress(int c) {
        mAnnotationPausedProgress.setProgress(c);
    }

    public boolean isPausedForShowingAnnotation() {
        return (controllerMode == ANNOTATION_PAUSE_MODE);
    }

    public void addAnnotationToPlace(FloatPosition place) {
        if (mPlayer.isPlaying()) {
            return;
        }
        addNewAnnotation(place);
    }


    public void addNewAnnotation(FloatPosition place) {
        setControllerMode(NEW_ANNOTATION_MODE);

        if (place == null) {
            place = new FloatPosition((float) 0.5, (float) 0.5);
        }
        mEditorListener.newAnnotation(place);
        mCurrentAnnotationIsNew = true;
    }


    public int getDuration() {
        return mPlayer.getDuration();
    }

    public void playerSeekTo(int position) {
        mPlayer.seekTo(position, SemanticVideoPlayerFragment.DO_NOTHING);
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));
    }

    public int getControllerTop() {
        LinearLayout lo = (LinearLayout) findViewById(R.id.media_controllers);
        if (lo != null) {
            return getRootView().getMeasuredHeight() - lo.getMeasuredHeight();
        } else {
            Log.e("VideoControllerView", "media_controllers doesn't exist!");
            return 0;
        }
    }

    public boolean isEditingAnnotations() {
        return (controllerMode == NEW_ANNOTATION_MODE || controllerMode == EDIT_ANNOTATION_MODE);
    }

    public interface VideoControllerShowHideListener {
        public void videoControllerShow();

        public void videoControllerHide();

        public boolean videoFillsVerticalSpace();
    }

    public interface MediaPlayerControl {
        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos, int do_after_seek);

        boolean isPlaying();

        int getBufferPercentage();

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        boolean isFullScreen();

        void toggleFullScreen();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<VideoControllerView> mView;

        MessageHandler(VideoControllerView view) {
            mView = new WeakReference<VideoControllerView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoControllerView view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    try {
                        view.hide();
                    } catch (IllegalStateException e) {/*Delayed message may be handled after the player has been released*/}
                    break;
                case SHOW_PROGRESS:
                    try {
                        pos = view.updateProgress();
                        if (!view.mDragging && view.mShowing && view.mPlayer.isPlaying()) {
                            msg = obtainMessage(SHOW_PROGRESS);
                            sendMessageDelayed(msg, 1000 - (pos % 1000));
                        }
                    } catch (IllegalStateException e) {/*Delayed message may be handled after the player has been released*/}
                    break;
            }
        }
    }
}
