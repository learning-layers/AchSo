package fi.aalto.legroup.achso.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.google.gson.JsonObject;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.support.AboutDialogFragment;
import fi.aalto.legroup.achso.support.FeedbackDialogFragment;

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String BUTTON_ABOUT = "BUTTON_ABOUT";
    private static final String BUTTON_FEEDBACK = "BUTTON_FEEDBACK";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference aboutButton = findPreference(BUTTON_ABOUT);
        Preference feedbackButton = findPreference(BUTTON_FEEDBACK);

        aboutButton.setOnPreferenceClickListener(this);
        feedbackButton.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case BUTTON_ABOUT:
                showAboutDialog();
                return true;

            case BUTTON_FEEDBACK:
                showFeedbackDialog();
                return true;
        }

        return false;
    }

    private void showAboutDialog() {
        AboutDialogFragment.newInstance(getActivity()).show(getFragmentManager(), "AboutDialog");
    }

    private void showFeedbackDialog() {
        String name = "";
        String email = "";

        JsonObject userInfo = App.loginManager.getUserInfo();

        if (userInfo != null) {
            name = userInfo.get("name").getAsString();
            email = userInfo.get("email").getAsString();
        }

        FeedbackDialogFragment.newInstance(name, email)
                .show(getFragmentManager(), "FeedbackDialog");
    }

}
