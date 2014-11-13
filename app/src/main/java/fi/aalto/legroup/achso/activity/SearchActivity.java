package fi.aalto.legroup.achso.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import java.util.List;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.VideoDBHelper;
import fi.aalto.legroup.achso.fragment.VideoBrowserFragment;

public class SearchActivity extends FragmentActivity {
    private VideoBrowserFragment videos;
    private MenuItem item;
    private SearchView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        this.setContentView(R.layout.activity_search);
    }


    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        this.videos = (VideoBrowserFragment) fragment;

        this.handleIntent(this.getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            List<SemanticVideo> videos = VideoDBHelper.queryVideoCacheByString(query);
            this.videos.setVideos(videos);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.menu_search, menu);
        this.item = menu.findItem(R.id.action_search);
        this.view = (SearchView) this.item.getActionView();

        this.view.setIconifiedByDefault(false);
        SearchManager manager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
        this.view.setSearchableInfo(manager.getSearchableInfo(this.getComponentName()));
        String query = this.getIntent().getStringExtra(SearchManager.QUERY);
        this.view.setQuery(query, false);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
