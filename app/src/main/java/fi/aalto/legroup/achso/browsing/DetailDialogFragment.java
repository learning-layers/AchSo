package fi.aalto.legroup.achso.browsing;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;

public final class DetailDialogFragment extends DialogFragment {

    private static final String ARG_ID = "id";

    private View view;
    private Toolbar toolbar;
    private Video video;

    private boolean instanceIsSaved = false;
    private final int ORG_TITLE = 0;
    private final int ORG_GENRE = 1;
    private final int ORG_QR = 2;
    private final Object[] originals = new Object[3];

    public static DetailDialogFragment newInstance(UUID video) {
        DetailDialogFragment f = new DetailDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ID, video.toString());
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        LayoutInflater inflater = LayoutInflater.from(context);
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.view = inflater.inflate(R.layout.dialog_information, null);

        UUID id = UUID.fromString(this.getArguments().getString(ARG_ID));

        try {
            this.video = App.videoRepository.getVideo(id).inflate();

            this.originals[ORG_TITLE] = video.getTitle();
            this.originals[ORG_GENRE] = video.getGenre();
            //this.originals[ORG_QR] = video.getQrCode();

            this.initToolbar(this.video);
            this.initGenre(this.video);
            this.initLocation(this.video);
        } catch (IOException e) {
            e.printStackTrace();
            return dialog;
        }


        dialog.setContentView(this.view);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        return dialog;
    }

    private void initToolbar(Video video) {
        this.toolbar = (Toolbar) this.view.findViewById(R.id.information_toolbar);

        this.view.findViewById(R.id.information_close_action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DetailDialogFragment.this.resetOriginals(DetailDialogFragment.this.video);
                DetailDialogFragment.this.dismiss();
            }
        });

        this.view.findViewById(R.id.information_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DetailDialogFragment.this.readValues();
                DetailDialogFragment.this.dismiss();
            }
        });

        ((EditText) this.view.findViewById(R.id.information_title)).setText(video.getTitle());
        //((TextView) this.view.findViewById(R.id.video_information_auhtor)).setText(video.getCreator());
    }

    private void readValues() {
        EditText titleText = (EditText) this.view.findViewById(R.id.information_title);
        String title = titleText.getText().toString();

        this.video.setTitle(title);

        if (!this.video.save()) {
            SnackbarManager.show(Snackbar.with(getActivity()).text(R.string.storage_error));
        }
    }

    private void resetOriginals(Video video) {
        video.setTitle((String) this.originals[ORG_TITLE]);
        //video.setGenre((SemanticVideo.Genre) this.originals[ORG_GENRE]);
        //video.setQrCode((String) this.originals[ORG_QR]);
    }

    private void initGenre(final Video video) {
        /*if (video.getQrCode() != null) {
            ((TextView) this.view.findViewById(R.id.video_qr_code)).setText(video.getQrCode());
        }*/

        Spinner spinner = (Spinner) this.view.findViewById(R.id.video_information_genre);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.view.getContext(),
                R.array.genres,
                R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //VideoInformationDialogFragment.this.video.setGenre(SemanticVideo.Genre.values()[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //spinner.setSelection(video.getGenre().ordinal());
    }

    private void initQR(Video video) {
        /*final ArrayList<SemanticVideo> videos = new ArrayList<SemanticVideo>();
        videos.add(this.video);
        ((ImageView) this.view.findViewById(R.id.video_information_qr_button)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        QRHelper.readQRCodeForVideos(getActivity(), videos, null);
                    }
                });

        String code = video.getQrCode();
        TextView codeView = (TextView) this.view.findViewById(R.id.video_qr_code);
        if (code != null) {
            codeView.setText(code);
        } else {
            codeView.setText("");
        }*/
    }

    private void initLocation(Video video) {
        Location location = video.getLocation();
        if (location != null) {
            Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
            try {
                List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(),
                        1);
                if (null != listAddresses && listAddresses.size() > 0) {
                    ((TextView) this.view.findViewById(R.id.video_location)).setText(listAddresses.get(
                            0).getAddressLine(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            LatLng ltlg = new LatLng(location.getLatitude(), location.getLongitude());
            GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.video_map)).getMap();
            map.getUiSettings().setZoomControlsEnabled(false);
            map.moveCamera(CameraUpdateFactory.newLatLng(ltlg));
            map.moveCamera(CameraUpdateFactory.zoomTo(15));
            map.addMarker(new MarkerOptions().position(ltlg));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.initQR(this.video);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        this.instanceIsSaved = true;
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (!this.instanceIsSaved) {
            MapFragment f = (MapFragment) this.getFragmentManager().findFragmentById(R.id.video_map);
            if (f != null) {
                this.getFragmentManager().beginTransaction().remove(f).commit();
            }
        } else {
            this.instanceIsSaved = false;
        }
    }
}
