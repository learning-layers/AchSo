package fi.aalto.legroup.achso.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.helper.SettingsHelper;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by lassi on 17.11.14.
 */
public class FeedbackDialogFragment extends DialogFragment {


    private View view;
    private final int[] fields = {R.id.feedback_body, R.id.feedback_summary, R.id.feedback_user, R.id.feedback_email};
    private final String[] names = {"message", "subject", "name", "email"};

    public static FeedbackDialogFragment newInstance(String name, String email) {
        FeedbackDialogFragment f = new FeedbackDialogFragment();

        Bundle args = new Bundle();
        args.putString("email", email);
        args.putString("name", name);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

        builder.setTitle(this.getString(R.string.feedback));
        builder.setPositiveButton(this.getString(R.string.feedback_send), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                FeedbackDialogFragment.this.sendFeedback();
            }
        });

        builder.setNegativeButton(this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                FeedbackDialogFragment.this.dismiss();
            }
        });


        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        this.view = inflater.inflate(R.layout.fragment_feedback, null);

        String email = this.getArguments().getString("email");
        ((TextView) this.view.findViewById(R.id.feedback_email)).setText(email);

        String name = this.getArguments().getString("name");
        ((TextView) this.view.findViewById(R.id.feedback_user)).setText(name);

        builder.setView(this.view);

        return builder.create();
    }

    private HashMap<String, String> readValues() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < fields.length; i++) {
            map.put(names[i], ((TextView) this.view.findViewById(fields[i])).getText().toString());
        }

        map.put("ip", "10.0.0.0");
        map.put("source", "Android");
        return map;
    }

    protected void sendFeedback() {
        final Context context = this.getActivity();

        SettingsHelper.sendFeedback(this.getActivity(), this.readValues(), new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                FeedbackDialogFragment.this.dismiss();
                SettingsHelper.showFeedbackSentDialog(context);
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }
}
