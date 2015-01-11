package fi.aalto.legroup.achso.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.service.OsTicketService;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by lassi on 17.11.14.
 */
public class FeedbackDialogFragment extends DialogFragment implements Callback<String> {

    public final static String ARG_EMAIL = "email";
    public final static String ARG_NAME = "name";

    private final static int[] FIELDS = {R.id.feedback_body, R.id.feedback_summary, R.id.feedback_user, R.id.feedback_email};
    private final static String[] NAMES = {"message", "subject", "name", "email"};

    private Context context;
    private View view;

    public static FeedbackDialogFragment newInstance(String name, String email) {
        FeedbackDialogFragment f = new FeedbackDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_NAME, name);
        f.setArguments(args);

        return f;
    }

    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.context = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);

        builder.setTitle(this.getString(R.string.feedback));

        builder.setPositiveButton(this.getString(R.string.feedback_send), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FeedbackDialogFragment.this.sendFeedback();
            }
        });

        builder.setNegativeButton(this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FeedbackDialogFragment.this.dismiss();
            }
        });

        // Don't close the dialog if the user taps the background
        this.setCancelable(false);

        LayoutInflater inflater = LayoutInflater.from(this.context);

        // Building dialogs with views is one of the rare cases where null as the root is valid.
        this.view = inflater.inflate(R.layout.fragment_feedback, null);

        String email = this.getArguments().getString(ARG_EMAIL);
        ((TextView) this.view.findViewById(R.id.feedback_email)).setText(email);

        String name = this.getArguments().getString(ARG_NAME);
        ((TextView) this.view.findViewById(R.id.feedback_user)).setText(name);

        builder.setView(this.view);

        return builder.create();
    }

    private void sendFeedback() {
        String endPoint = getString(R.string.feedbackServerUrl);
        String apiKey = getString(R.string.feedbackServerKey);

        new RestAdapter.Builder()
                .setEndpoint(endPoint)
                .build()
                .create(OsTicketService.class)
                .sendFeedback(apiKey, buildBody(), this);
    }

    /**
     * Builds the request body.
     */
    private Map<String, String> buildBody() {
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < FIELDS.length; i++) {
            map.put(NAMES[i], ((TextView) this.view.findViewById(FIELDS[i])).getText().toString());
        }

        // Some IP address needs to be sent, doesn't have to be real.
        map.put("ip", "10.0.0.0");
        map.put("source", "Android");

        return map;
    }

    @Override
    public void success(String s, Response response) {
        dismiss();

        String message = this.context.getString(R.string.feedback_sent);

        Toast.makeText(this.context, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void failure(RetrofitError error) {
        String message = this.context.getString(R.string.feedback_error);

        Toast.makeText(this.context, message, Toast.LENGTH_LONG).show();
    }

}
