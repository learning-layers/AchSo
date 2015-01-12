package fi.aalto.legroup.achso.browsing;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Video;

public class DetailActivity extends FragmentActivity implements MenuItem.OnMenuItemClickListener {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    private Video video;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UUID videoId = (UUID) getIntent().getSerializableExtra(ARG_VIDEO_ID);

        try {
            video = App.videoRepository.get(videoId);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_information);
        ActionBar bar = getActionBar();

        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        Location location = video.getLocation();

        if (location != null) {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

            GoogleMap map = mapFragment.getMap();

            map.addCircle(new CircleOptions()
                    .center(position)
                    .radius(location.getAccuracy())
                    .strokeWidth(3.0f)
                    .strokeColor(Color.WHITE)
                    .fillColor(Color.parseColor("#80ffffff")));

            map.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14.5f));

            findViewById(R.id.unknownLocationText).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Using the Google Maps API via Google Play Services requires displaying its licence
     * information. We'll show the information in an AlertDialog triggered by a MenuItem.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(R.string.about_maps).setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        String licenceInfo = GooglePlayServicesUtil
                .getOpenSourceSoftwareLicenseInfo(DetailActivity.this);

        // Using a WebView is a lot faster than showing the text as the dialog's message
        WebView webView = new WebView(DetailActivity.this);
        webView.loadDataWithBaseURL(null, licenceInfo, "text/plain", null, null);

        new AlertDialog.Builder(DetailActivity.this)
                .setTitle(R.string.about_maps)
                .setView(webView)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();

        return true;
    }

    public Video getVideo() {
        return video;
    }

}
