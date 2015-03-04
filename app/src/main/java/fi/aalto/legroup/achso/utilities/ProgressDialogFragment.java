package fi.aalto.legroup.achso.utilities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringRes;

public class ProgressDialogFragment extends DialogFragment {

    private static final String ARG_DIALOG_MESSAGE = "ARG_DIALOG_TITLE";

    public static ProgressDialogFragment newInstance(Context context, @StringRes int messageRes) {
        String title = context.getString(messageRes);
        return newInstance(title);
    }

    public static ProgressDialogFragment newInstance(String message) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        Bundle arguments = new Bundle();

        arguments.putString(ARG_DIALOG_MESSAGE, message);

        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getArguments().getString(ARG_DIALOG_MESSAGE, "");
        ProgressDialog dialog = new ProgressDialog(getActivity());

        dialog.setMessage(message);
        dialog.setIndeterminate(true);

        setCancelable(false);

        return dialog;
    }

}
