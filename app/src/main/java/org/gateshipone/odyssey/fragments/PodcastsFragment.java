/*
 * Copyright (C) 2021 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.adapter.TracksAdapter;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.PreferenceHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;
import org.gateshipone.odyssey.viewmodels.PodcastsViewModel;

public class PodcastsFragment extends OdysseyFragment<TrackModel> implements AdapterView.OnItemClickListener {

    private PreferenceHelper.LIBRARY_TRACK_CLICK_ACTION mClickAction;

    public static PodcastsFragment newInstance() {
        return new PodcastsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_refresh, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get listview
        mListView = view.findViewById(R.id.list_refresh_listview);

        // get swipe layout
        mSwipeRefreshLayout = view.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshContent);

        mAdapter = new TracksAdapter(getActivity());

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = view.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) view.findViewById(R.id.empty_view_message)).setText(R.string.empty_podcasts_message);

        registerForContextMenu(mListView);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mClickAction = PreferenceHelper.getClickAction(sharedPreferences, getContext());

        // setup observer for the live data
        getViewModel().getData().observe(getViewLifecycleOwner(), this::onDataReady);
    }


    /**
     * Called when the fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            // TODO use resource for title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_podcasts), false, true, false);
        }
    }

    @Override
    GenericViewModel<TrackModel> getViewModel() {
        return new ViewModelProvider(this, new PodcastsViewModel.PodcastsViewModelFactory(getActivity().getApplication())).get(PodcastsViewModel.class);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (mClickAction) {
            case ACTION_ADD_SONG:
                enqueuePodcast(position, false);
                break;
            case ACTION_PLAY_SONG:
                playPodcast(position, false);
                break;
            case ACTION_PLAY_SONG_NEXT:
                enqueuePodcast(position, true);
                break;
            case ACTION_CLEAR_AND_PLAY:
                playPodcast(position, true);
                break;
        }
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_podcasts_fragment, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        final int itemId = item.getItemId();

        if (itemId == R.id.fragment_podcasts_action_enqueue) {
            enqueuePodcast(info.position, false);
            return true;
        } else if (itemId == R.id.fragment_podcasts_action_enqueueasnext) {
            enqueuePodcast(info.position, true);
            return true;
        } else if (itemId == R.id.fragment_podcasts_action_play) {
            playPodcast(info.position, false);
            return true;
        }

        return super.onContextItemSelected(item);
    }


    private void playPodcast(final int position, final boolean clearPlaylist) {
        final TrackModel podcast = mAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().playTrack(podcast, clearPlaylist);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void enqueuePodcast(int position, boolean asNext) {
        final TrackModel track = mAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().enqueueTrack(track, asNext);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
