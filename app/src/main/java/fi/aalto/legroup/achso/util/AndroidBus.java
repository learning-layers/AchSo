package fi.aalto.legroup.achso.util;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

/**
 * Ensures that all events are posted on the main thread.
 *
 * https://github.com/square/otto/issues/38
 *
 * @author Jake Wharton
 * @author pommedeterresautee
 */
public final class AndroidBus extends Bus {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AndroidBus.super.post(event);
                }
            });
        }
    }

}
