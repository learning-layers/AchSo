package fi.aalto.legroup.achso.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class TrimSeekBar extends SeekBar {

    TrimBarChangeListener listener;

    public TrimSeekBar(Context context) {
        super(context);
        init();
    }

    public TrimSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrimSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private  void init() {
        listener = new TrimBarChangeListener();
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener decoratedListener) {
        listener.setDelegate(decoratedListener);
        super.setOnSeekBarChangeListener(listener);
    }

    /**
     * An implementation of an OnSeekBarChangeListener that snaps to positions on the bar. Can also
     * decorate other listeners to provide the same functionality.
     */
    private static final class TrimBarChangeListener implements OnSeekBarChangeListener {

        private OnSeekBarChangeListener delegate;

        /**
         * Constructs a new listener.
         */
        public TrimBarChangeListener() {}

        /**
         * Decorates a listener with snapping functionality.
         * @param delegate  Listener to decorate.
         */
        public TrimBarChangeListener(OnSeekBarChangeListener delegate) {
            this.delegate = delegate;
        }

        public void setDelegate(OnSeekBarChangeListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (delegate != null) {
                delegate.onProgressChanged(seekBar, progress, fromUser);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (delegate != null) {
                delegate.onStartTrackingTouch(seekBar);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (delegate != null) {
                delegate.onStopTrackingTouch(seekBar);
            }
        }
    }
}
