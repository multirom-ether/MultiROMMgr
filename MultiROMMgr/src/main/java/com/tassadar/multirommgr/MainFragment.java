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
import android.app.Fragment;
import android.view.View;

import com.tassadar.multirommgr.installfragment.InstallFragment;
import com.tassadar.multirommgr.romlistfragment.RomListFragment;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class MainFragment extends Fragment implements OnRefreshListener {

    public static final int MAIN_FRAG_INSTALL   = 0;
    public static final int MAIN_FRAG_ROM_LIST  = 1;

    static public MainFragment newFragment(int type) {
        switch(type) {
            case MAIN_FRAG_INSTALL:
                return new InstallFragment();
            case MAIN_FRAG_ROM_LIST:
                return new RomListFragment();
            default:
                return null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            m_actListener = (MainActivityListener)activity;
        } catch(ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        m_actListener.onFragmentViewDestroyed();
    }

    protected View findViewById(int id) {
        assert m_view != null;
        return m_view.findViewById(id);
    }

    @Override
    public void onRefreshStarted(View view) {
        m_actListener.refresh();
    }

    protected void setPtrLayout(int id) {
        m_ptrLayout = (PullToRefreshLayout)findViewById(id);
        ActionBarPullToRefresh
                .from(getActivity())
                .allChildrenArePullable()
                .listener(this)
                .setup(m_ptrLayout);
    }

    public void startRefresh() {
        m_ptrLayout.setRefreshing(true);
    }

    public void refresh() { }
    public void onStatusTaskFinished(StatusAsyncTask.Result res) { }

    public void setRefreshComplete() {
        m_ptrLayout.setRefreshComplete();
    }

    protected MainActivityListener m_actListener;
    protected View m_view;
    protected PullToRefreshLayout m_ptrLayout;
}
