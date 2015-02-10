package fi.aalto.legroup.achso.support;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bugsnag.android.Bugsnag;

import fi.aalto.legroup.achso.R;

/**
 * @author István Koren
 * @author Leo Nikkilä
 */
public class AboutDialogFragment extends DialogFragment {

    private static final String ARG_APP_NAME = "ARG_APP_NAME";
    private static final String ARG_VERSION_NAME = "ARG_VERSION_NAME";

    public static AboutDialogFragment newInstance(Context context) {
        AboutDialogFragment fragment = new AboutDialogFragment();
        Bundle arguments = new Bundle();

        PackageManager manager = context.getPackageManager();
        String packageName = context.getPackageName();

        String appName = context.getString(R.string.app_name);
        arguments.putString(ARG_APP_NAME, appName);

        try {
            String versionName = manager.getPackageInfo(packageName, 0).versionName;
            arguments.putString(ARG_VERSION_NAME, versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Bugsnag.notify(e);
        }

        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        if (arguments == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        View dialogView = View.inflate(getActivity(), R.layout.dialog_about, null);

        String appName = arguments.getString(ARG_APP_NAME, "");
        String versionName = arguments.getString(ARG_VERSION_NAME, "");

        TextView versionText = (TextView) dialogView.findViewById(R.id.versionText);

        versionText.setText(versionName);

        return new MaterialDialog.Builder(getActivity())
            .title(appName)
            .customView(dialogView, true)
            .positiveText(R.string.close)
            .show();
    }

}
