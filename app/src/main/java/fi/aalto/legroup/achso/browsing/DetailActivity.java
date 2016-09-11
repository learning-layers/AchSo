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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Group;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.views.adapters.GroupsListAdapter;

public final class DetailActivity extends FragmentActivity
        implements MenuItem.OnMenuItemClickListener {

    public static final String ARG_VIDEO_ID = "ARG_VIDEO_ID";

    private Video video;

    private ListView groupsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UUID videoId = (UUID) getIntent().getSerializableExtra(ARG_VIDEO_ID);

        try {
            video = App.videoRepository.getVideo(videoId).inflate();
        } catch (IOException e) {
            e.printStackTrace();
            SnackbarManager.show(Snackbar.with(this).text(R.string.storage_error));
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

        groupsList = (ListView) findViewById(R.id.groupsList);

        groupsList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        Button toggleGroups = (Button)  findViewById(R.id.toggleGroupsList);

        toggleGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (groupsList.getVisibility() == View.VISIBLE) {
                    groupsList.setVisibility(View.GONE);
                } else {
                    groupsList.setVisibility(View.VISIBLE);
                }
            }
        });

        loadGroups();
        setListViewHeightBasedOnChildren(groupsList);
    }
    
    // Hack from https://stackoverflow.com/questions/18367522/android-list-view-inside-a-scroll-view
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ActionBar.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    public void loadGroups() {
        try {
            ArrayList<Group> groups = new ArrayList<Group>(App.videoRepository.getGroups());
            GroupsListAdapter adapter = new GroupsListAdapter(this, R.layout.partial_group_list_item, groups);
            groupsList.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        groupsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Group group = (Group) adapterView.getItemAtPosition(i);
            }
        });
    }

    public void showGroupsList() {

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
