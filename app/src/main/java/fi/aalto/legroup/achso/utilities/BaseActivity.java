package fi.aalto.legroup.achso.utilities;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.EventListener;

import javax.annotation.Nullable;

import fi.aalto.legroup.achso.R;

/**
 * An activity that handles toolbars and snackbars nicely.
 */
public abstract class BaseActivity extends ActionBarActivity implements EventListener {

    @Nullable
    private Toolbar toolbar;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Set a toolbar automatically if found.
        View toolbarView = findViewById(R.id.toolbar);

        if (toolbarView instanceof Toolbar) {
            toolbar = (Toolbar) toolbarView;
            setSupportActionBar(toolbar);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Call EventListener#onShow again if the activity has resumed and a snackbar is visible.
        Snackbar snackbar = SnackbarManager.getCurrentSnackbar();

        if (snackbar != null && snackbar.isShowing()) {
            onShow(snackbar);
        }
    }

    /**
     * Returns the current toolbar, or null if there's none.
     */
    @Nullable
    protected Toolbar getToolbar() {
        return toolbar;
    }

    /**
     * Creates and shows a new snackbar that reports events to this activity.
     *
     * @param resId Resource ID for the message shown on the bar.
     */
    protected void showSnackbar(@StringRes int resId) {
        showSnackbar(getString(resId));
    }

    /**
     * Creates and shows a new snackbar that reports events to this activity.
     *
     * @param message Message shown on the bar.
     */
    protected void showSnackbar(String message) {
        SnackbarManager.show(createSnackbar(message));
    }

    /**
     * Creates a new snackbar that reports events to this activity.
     *
     * @param resId Resource ID for the message shown on the bar.
     *
     * @return The created snackbar.
     */
    protected Snackbar createSnackbar(@StringRes int resId) {
        return createSnackbar(getString(resId));
    }

    /**
     * Creates a new snackbar that reports events to this activity.
     *
     * @param message Message shown on the bar.
     *
     * @return The created snackbar.
     */
    protected Snackbar createSnackbar(String message) {
        return Snackbar.with(this).eventListener(this).text(message);
    }

    /*
     * Snackbar listeners are implemented for convenience.
     */

    @Override
    public void onShow(Snackbar snackbar) {
        // Do nothing
    }

    @Override
    public void onShowByReplace(Snackbar snackbar) {
        // Do nothing
    }

    @Override
    public void onShown(Snackbar snackbar) {
        // Do nothing
    }

    @Override
    public void onDismiss(Snackbar snackbar) {
        // Do nothing
    }

    @Override
    public void onDismissByReplace(Snackbar snackbar) {
        // Do nothing
    }

    @Override
    public void onDismissed(Snackbar snackbar) {
        // Do nothing
    }

}
