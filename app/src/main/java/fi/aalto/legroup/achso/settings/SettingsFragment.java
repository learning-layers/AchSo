package fi.aalto.legroup.achso.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.support.AboutDialogFragment;

/**
 * @author Leo Nikkilä
 */
public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String BUTTON_ABOUT = "BUTTON_ABOUT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        findPreference(BUTTON_ABOUT).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case BUTTON_ABOUT:
                showAboutDialog();
                return true;
        }

        return false;
    }

    private void showAboutDialog() {
        AboutDialogFragment.newInstance(getActivity()).show(getFragmentManager(), "AboutDialog");
    }

}
