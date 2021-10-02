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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.adapter.PodcastsAdapter;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.PreferenceHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;
import org.gateshipone.odyssey.viewmodels.PodcastsViewModel;

public class PodcastsFragment extends OdysseyFragment<TrackModel> implements AdapterView.OnItemClickListener {

    private PreferenceHelper.LIBRARY_TRACK_CLICK_ACTION mClickAction;

    private String mSearchString;

    private static final String PODCASTSFRAGMENT_SAVED_INSTANCE_SEARCH_STRING = "PodcastsFragment.SearchString";

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

        mAdapter = new PodcastsAdapter(getActivity());

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = view.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) view.findViewById(R.id.empty_view_message)).setText(R.string.empty_podcasts_message);

        registerForContextMenu(mListView);

        // activate options menu in toolbar
        setHasOptionsMenu(true);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mClickAction = PreferenceHelper.getClickAction(sharedPreferences, getContext());

        // try to resume the saved search string
        if (savedInstanceState != null) {
            mSearchString = savedInstanceState.getString(PODCASTSFRAGMENT_SAVED_INSTANCE_SEARCH_STRING);
        }

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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_podcasts_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_search).setIcon(drawable);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        // Check if a search string is saved from before
        if (mSearchString != null) {
            // Expand the view
            searchView.setIconified(false);
            menu.findItem(R.id.action_search).expandActionView();
            // Set the query string
            searchView.setQuery(mSearchString, false);
            // Notify the adapter
            applyFilter(mSearchString);
        }

        searchView.setOnQueryTextListener(new PodcastsFragment.SearchTextObserver());

        super.onCreateOptionsMenu(menu, menuInflater);
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

    private class SearchTextObserver implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {

            if (query.isEmpty()) {
                mSearchString = null;
                removeFilter();
            } else {
                mSearchString = query;
                applyFilter(query);
            }

            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (newText.isEmpty()) {
                mSearchString = null;
                removeFilter();
            } else {
                mSearchString = newText;
                applyFilter(newText);
            }

            return true;
        }
    }
}
