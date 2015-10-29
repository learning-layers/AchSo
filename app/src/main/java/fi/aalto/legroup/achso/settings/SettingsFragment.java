package fi.aalto.legroup.achso.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.JsonObject;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.support.AboutDialogFragment;
import fi.aalto.legroup.achso.support.FeedbackDialogFragment;

public final class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String BUTTON_ABOUT = "BUTTON_ABOUT";
    private static final String BUTTON_FEEDBACK = "BUTTON_FEEDBACK";
    private static final String LAYERS_BOX_URL = "LAYERS_BOX_URL";
    private static final String USE_PUBLIC_LAYERS_BOX = "USE_PUBLIC_LAYERS_BOX";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference aboutButton = findPreference(BUTTON_ABOUT);
        Preference feedbackButton = findPreference(BUTTON_FEEDBACK);
        Preference layersBoxUrlField = findPreference(LAYERS_BOX_URL);
        Preference publicLayersBoxSwitch = findPreference(USE_PUBLIC_LAYERS_BOX);

        // @Hack(public only)
        ((SwitchPreference)publicLayersBoxSwitch).setChecked(true);

        aboutButton.setOnPreferenceClickListener(this);
        feedbackButton.setOnPreferenceClickListener(this);
        layersBoxUrlField.setOnPreferenceChangeListener(this);
        publicLayersBoxSwitch.setOnPreferenceChangeListener(this);
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

            case USE_PUBLIC_LAYERS_BOX:
                tempShowPublicOnlyDialog();
                return false;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        switch (key) {

            case LAYERS_BOX_URL:
                // @Hack(public only)
                tempShowPublicOnlyDialog();
                return true;

            case USE_PUBLIC_LAYERS_BOX:
                // @Hack(public only)
                tempShowPublicOnlyDialog();
                return false;
        }
        return true;
    }

    private void showPublicWarningDialog() {

        // TODO: Use this when supported

        final SwitchPreference publicLayersBoxSwitch = (SwitchPreference)findPreference(USE_PUBLIC_LAYERS_BOX);

        if (publicLayersBoxSwitch.isChecked()) {

            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.warning)
                    .content(R.string.public_layers_box_chosen_warning)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            publicLayersBoxSwitch.setChecked(false);
                        }
                    })
                    .show();
        }
    }

    // @Hack(public only)
    private void tempShowPublicOnlyDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.failed_to_change_setting)
                .content(R.string.only_public_layers_box_description)
                .positiveText(R.string.ok)
                .show();
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
