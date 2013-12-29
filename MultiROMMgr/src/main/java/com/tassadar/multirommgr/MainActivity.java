/*
 * This file is part of MultiROM Manager.
 *
 * MultiROM Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiROM Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiROM Manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.tassadar.multirommgr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.app.FragmentManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tassadar.multirommgr.installfragment.UbuntuManifestAsyncTask;

public class MainActivity extends Activity implements StatusAsyncTask.StatusAsyncTaskListener, MainActivityListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This activity is using different background color, which would cause overdraw
        // of the whole area, so disable the default background
        getWindow().setBackgroundDrawable(null);

        Utils.installHttpCache(this);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        m_curFragment = -1;

        m_fragmentTitles = getResources().getStringArray(R.array.main_fragment_titles);
        m_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        m_drawerList = (ListView) findViewById(R.id.left_drawer);

        m_fragments = new MainFragment[2];
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction t = fragmentManager.beginTransaction();
        for(int i = 0; i < m_fragments.length; ++i) {
            m_fragments[i] = (MainFragment)fragmentManager.findFragmentByTag(m_fragmentTitles[i]);
            if(m_fragments[i] == null) {
                m_fragments[i] = MainFragment.newFragment(i);
                t.add(R.id.content_frame, m_fragments[i], m_fragmentTitles[i]);
            }
            t.hide(m_fragments[i]);
        }
        t.commit();

        // Set the adapter for the list view
        m_drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, m_fragmentTitles));
        // Set the list's click listener
        m_drawerList.setOnItemClickListener(new DrawerItemClickListener());

        m_drawerTitle = getText(R.string.app_name);

        m_drawerToggle = new ActionBarDrawerToggle(
                this, m_drawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(m_title);
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(m_drawerTitle);
            }
        };
        m_drawerLayout.setDrawerListener(m_drawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if(savedInstanceState != null) {
            selectItem(savedInstanceState.getInt("curFragment", 0));
        } else {
            selectItem(0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Utils.flushHttpCache();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curFragment", m_curFragment);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        m_refreshItem = menu.findItem(R.id.action_refresh);
        if(!StatusAsyncTask.instance().isComplete())
            m_refreshItem.setEnabled(false);
        return true;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        if(position < 0 || position >= m_fragments.length) {
            Log.e("MultiROMMgr", "Invalid fragment index " + position);
            return;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction t = fragmentManager.beginTransaction();

        if(m_curFragment != -1)
            t.hide(m_fragments[m_curFragment]);
        t.show(m_fragments[position]);
        t.commit();

        m_curFragment = position;

        // Highlight the selected item, update the title, and close the drawer
        m_drawerList.setItemChecked(position, true);
        setTitle(m_fragmentTitles[position]);
        m_drawerLayout.closeDrawer(m_drawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        m_title = title;
        getActionBar().setTitle(m_title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        m_drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        m_drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem it) {
        if (m_drawerToggle.onOptionsItemSelected(it))
            return true;

        switch(it.getItemId()) {
            case R.id.action_refresh:
                refresh();
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_reboot:
            {
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle(R.string.reboot)
                        .setCancelable(true)
                        .setNegativeButton(R.string.cancel, null)
                        .setItems(R.array.reboot_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0: Utils.reboot(""); break;
                                    case 1: Utils.reboot("recovery"); break;
                                    case 2: Utils.reboot("bootloader"); break;
                                }
                            }
                        })
                        .create().show();
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public void startRefresh() {
        if(m_refreshItem != null)
            m_refreshItem.setEnabled(false);

        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].startRefresh();

        StatusAsyncTask.instance().setListener(this);
        StatusAsyncTask.instance().execute();
    }

    @Override
    public void refresh() {
        StatusAsyncTask.destroy();
        UbuntuManifestAsyncTask.destroy();

        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].refresh();

        startRefresh();
    }

    @Override
    public void setRefreshComplete() {
        if(m_refreshItem != null)
            m_refreshItem.setEnabled(true);
        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].setRefreshComplete();
    }

    @Override
    public void onFragmentViewCreated() {
        if(++m_fragmentViewsCreated == m_fragments.length) {
            Intent i = getIntent();
            if(i == null || !i.getBooleanExtra("force_refresh", false)) {
                startRefresh();
            } else {
                i.removeExtra("force_refresh");
                refresh();
            }
        }
    }

    @Override
    public void onFragmentViewDestroyed() {
        --m_fragmentViewsCreated;
    }

    @Override
    public void onStatusTaskFinished(StatusAsyncTask.Result res) {
        for(int i = 0; i < m_fragments.length; ++i)
            m_fragments[i].onStatusTaskFinished(res);
    }

    private DrawerLayout m_drawerLayout;
    private ListView m_drawerList;
    private String[] m_fragmentTitles;
    private MainFragment[] m_fragments;
    private int m_curFragment;
    private CharSequence m_title;
    private ActionBarDrawerToggle m_drawerToggle;
    private CharSequence m_drawerTitle;
    private MenuItem m_refreshItem;
    private int m_fragmentViewsCreated;
}
