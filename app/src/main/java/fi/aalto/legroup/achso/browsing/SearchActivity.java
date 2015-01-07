package fi.aalto.legroup.achso.browsing;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.app.App;
import fi.aalto.legroup.achso.entities.Annotation;
import fi.aalto.legroup.achso.entities.Video;
import fi.aalto.legroup.achso.entities.VideoInfo;

public class SearchActivity extends ActionBarActivity {
    private BrowserFragment videos;
    private MenuItem item;
    private SearchView view;
    private ArrayList<UUID> matches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.search_toolbar);
        this.setSupportActionBar(toolbar);

        this.getSupportActionBar().setHomeButtonEnabled(true);
    }


    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        this.videos = (BrowserFragment) fragment;

        this.handleIntent(this.getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY).toLowerCase();

            this.queryVideos(query);
        }
    }

    private void queryVideos(String query) {
        List<UUID> ids;

        try {
            ids = App.videoInfoRepository.getAll();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (UUID id : ids) {
            if (this.isMatch(id, query)) {
                this.matches.add(id);
            }
        }

        this.videos.setVideos(this.matches);
    }

    private boolean isMatch(UUID id, String query) {
        try {
            VideoInfo videoInfo = App.videoInfoRepository.get(id);

            if (query.equals(videoInfo.getTag())) {
                return true;
            }

            if (videoInfo.getTitle().toLowerCase().contains(query)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Video video = App.videoRepository.get(id);

            for (Annotation annotation : video.getAnnotations()) {
                if (annotation.getText().toLowerCase().contains(query)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_search, menu);
        this.item = menu.findItem(R.id.action_search);

        SearchManager manager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
        this.view = (SearchView) menu.findItem(R.id.action_search).getActionView();
        this.item = menu.findItem(R.id.action_search);
        this.view.setSearchableInfo(manager.getSearchableInfo(this.getComponentName()));

        String query = this.getIntent().getStringExtra(SearchManager.QUERY);
        this.view.setQuery(query, false);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
