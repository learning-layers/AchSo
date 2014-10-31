package fi.aalto.legroup.achso.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.adapter.VideoListTabAdapter;
import fi.aalto.legroup.achso.database.SemanticVideo;

public class MainActivity extends FragmentActivity {

    VideoListTabAdapter tabAdapter;
    ViewPager tab;
    ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.tabAdapter = new VideoListTabAdapter(this, this.getSupportFragmentManager());
        this.tab = (ViewPager) this.findViewById(R.id.pager);
        this.tab.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar = getActionBar();
                actionBar.setSelectedNavigationItem(position);
            }
        });
        this.tab.setAdapter(this.tabAdapter);
        this.actionBar = this.getActionBar();
        this.actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }

        };

        this.actionBar.addTab(this.actionBar.newTab().setText(getString(R.string.my_videos)).setTabListener(tabListener));
        for(SemanticVideo.Genre genre : SemanticVideo.Genre.values()) {
            this.actionBar.addTab(this.actionBar.newTab().setText(SemanticVideo.genreStrings.get(genre)).setTabListener(tabListener));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
