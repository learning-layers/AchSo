package fi.aalto.legroup.achso.utilities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;

public class ProgressDialogFragment extends DialogFragment {

    protected String message = "";

    public static ProgressDialogFragment newInstance(Context context, @StringRes int messageRes) {
        return newInstance(context.getString(messageRes));
    }

    public static ProgressDialogFragment newInstance(String message) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();

        fragment.setMessage(message);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);

        return new MaterialDialog.Builder(getActivity())
                .content(message)
                .progress(true, 0)
                .build();
    }

    protected void setMessage(String message) {
        this.message = message;
    }

}
