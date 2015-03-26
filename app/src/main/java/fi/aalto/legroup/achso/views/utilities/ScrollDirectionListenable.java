package fi.aalto.legroup.achso.views.utilities;

import android.support.annotation.Nullable;

import com.melnykov.fab.ScrollDirectionListener;

/**
 * Denotes a one-dimensionally scrollable object whose scrolling direction can be listened to.
 */
public interface ScrollDirectionListenable {

    /**
     * Sets the scroll direction listener.
     *
     * @param listener Scroll direction listener, or null to remove a previously set listener.
     */
    public void setScrollDirectionListener(@Nullable ScrollDirectionListener listener);

}
