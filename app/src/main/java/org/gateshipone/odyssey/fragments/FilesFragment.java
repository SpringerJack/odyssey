/*
 * Copyright (C) 2020 Team Gateship-One
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.adapter.FilesAdapter;
import org.gateshipone.odyssey.dialogs.ChooseStorageVolumeDialog;
import org.gateshipone.odyssey.listener.OnDirectorySelectedListener;
import org.gateshipone.odyssey.listener.OnPlaylistSelectedListener;
import org.gateshipone.odyssey.mediascanner.MediaScannerService;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.models.PlaylistModel;
import org.gateshipone.odyssey.utils.PreferenceHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;
import org.gateshipone.odyssey.viewmodels.FileViewModel;
import org.gateshipone.odyssey.viewmodels.GenericViewModel;

import java.util.List;

public class FilesFragment extends OdysseyFragment<FileModel> implements AdapterView.OnItemClickListener {

    /**
     * Listener to open a child directory
     */
    private OnDirectorySelectedListener mOnDirectorySelectedCallback;

    private OnPlaylistSelectedListener mOnPlaylistSelectedCallback;

    /**
     * the current directory that is displayed by the fragment
     */
    private FileModel mCurrentDirectory;
    /**
     * flag if the current directory is a root directory
     */
    private boolean mIsRootDirectory = false;

    /**
     * Saved search string when user rotates devices
     */
    private String mSearchString;

    /**
     * key values for arguments of the fragment
     */
    private static final String ARG_DIRECTORYPATH = "directory_path";

    private static final String ARG_ISROOTDIRECTORY = "is_root_directory";

    /**
     * Constant for state saving
     */
    private static final String FILESFRAGMENT_SAVED_INSTANCE_SEARCH_STRING = "FilesFragment.SearchString";

    /**
     * Action to execute when the user selects an item in the list
     */
    private PreferenceHelper.LIBRARY_TRACK_CLICK_ACTION mClickAction;

    public static FilesFragment newInstance(@NonNull final String directoryPath, final boolean isRootDirectory) {
        final Bundle args = new Bundle();
        args.putString(ARG_DIRECTORYPATH, directoryPath);
        args.putBoolean(ARG_ISROOTDIRECTORY, isRootDirectory);

        final FilesFragment fragment = new FilesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        mAdapter = new FilesAdapter(getActivity());

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        // get empty view
        mEmptyView = view.findViewById(R.id.empty_view);

        // set empty view message
        ((TextView) view.findViewById(R.id.empty_view_message)).setText(R.string.empty_directory_message);

        // register listview for a context menu
        registerForContextMenu(mListView);

        // activate options menu in toolbar
        setHasOptionsMenu(true);

        // get the current directory
        Bundle args = getArguments();

        if (args != null) {
            String directoryPath = args.getString(ARG_DIRECTORYPATH);
            mIsRootDirectory = args.getBoolean(ARG_ISROOTDIRECTORY);

            if (directoryPath != null) {
                mCurrentDirectory = new FileModel(directoryPath);
            }
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mClickAction = PreferenceHelper.getClickAction(sharedPreferences, getContext());

        // try to resume the saved search string
        if (savedInstanceState != null) {
            mSearchString = savedInstanceState.getString(FILESFRAGMENT_SAVED_INSTANCE_SEARCH_STRING);
        }

        // setup observer for the live data
        getViewModel().getData().observe(getViewLifecycleOwner(), this::onDataReady);
    }

    @Override
    GenericViewModel<FileModel> getViewModel() {
        return new ViewModelProvider(this, new FileViewModel.FileViewModelFactory(getActivity().getApplication(), mCurrentDirectory)).get(FileViewModel.class);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // save the already typed search string (or null if nothing is entered)
        outState.putString(FILESFRAGMENT_SAVED_INSTANCE_SEARCH_STRING, mSearchString);
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mOnDirectorySelectedCallback = (OnDirectorySelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnDirectorySelectedListener");
        }

        try {
            mOnPlaylistSelectedCallback = (OnPlaylistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPlaylistFileSelectedListener");
        }
    }

    /**
     * Called when the fragment resumes.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(mCurrentDirectory.getName(), false, mIsRootDirectory, false);
        }
    }

    /**
     * Called when the observed {@link androidx.lifecycle.LiveData} is changed.
     * <p>
     * This method will update the related adapter and the {@link androidx.swiperefreshlayout.widget.SwipeRefreshLayout} if present.
     *
     * @param model The data observed by the {@link androidx.lifecycle.LiveData}.
     */
    @Override
    protected void onDataReady(List<FileModel> model) {
        super.onDataReady(model);

        if (mToolbarAndFABCallback != null) {
            // set up play button
            if (mAdapter.isEmpty()) {
                mToolbarAndFABCallback.setupFAB(null);
            } else {
                mToolbarAndFABCallback.setupFAB(v -> playCurrentFolderAndSubFolders());
            }
        }
    }

    /**
     * Callback when an item in the ListView was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileModel selectedFile = mAdapter.getItem(position);

        if (selectedFile.isDirectory()) {
            // file is directory open new fragment
            mOnDirectorySelectedCallback.onDirectorySelected(selectedFile.getPath(), false);
        } else if (selectedFile.isPlaylist()) {
            mOnPlaylistSelectedCallback.onPlaylistSelected(new PlaylistModel(selectedFile.getNameWithoutExtension(), selectedFile.getPath()));
        } else {
            switch (mClickAction) {
                case ACTION_ADD_SONG:
                    enqueueFile(position, false);
                    break;
                case ACTION_PLAY_SONG:
                    playFile(position, false);
                    break;
                case ACTION_PLAY_SONG_NEXT:
                    enqueueFile(position, true);
                    break;
                case ACTION_CLEAR_AND_PLAY:
                    if (mSearchString != null) {
                        playFile(position, true);
                    } else {
                        playFolder(position);
                    }
                    break;
            }
        }
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        FileModel currentFile = mAdapter.getItem(info.position);

        if (currentFile.isFile()) {
            // show context menu for files
            inflater.inflate(R.menu.context_menu_files_files_fragment, menu);

            // hide enqueue as next for playlist files
            menu.findItem(R.id.fragment_files_action_enqueueasnext).setVisible(!currentFile.isPlaylist());
        } else {
            // show context menu for directories
            inflater.inflate(R.menu.context_menu_directories_files_fragment, menu);
        }
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        final int itemId = item.getItemId();

        if (itemId == R.id.fragment_files_action_add_folder) {
            enqueueFolderAndSubFolders(info.position);
            return true;
        } else if (itemId == R.id.fragment_files_action_play_folder) {
            playFolderAndSubFolders(info.position);
            return true;
        } else if (itemId == R.id.fragment_files_action_add_file) {
            enqueueFile(info.position, false);
            return true;
        } else if (itemId == R.id.fragment_files_action_enqueueasnext) {
            enqueueFile(info.position, true);
            return true;
        } else if (itemId == R.id.fragment_files_action_play_file) {
            playFile(info.position, false);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_files_fragment, menu);

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

        searchView.setOnQueryTextListener(new SearchTextObserver());

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.action_add_directory) {
            enqueueCurrentFolderAndSubFolders();
            return true;
        } else if (itemId == R.id.action_switch_storage_volume) {
            ChooseStorageVolumeDialog chooseDialog = new ChooseStorageVolumeDialog();
            chooseDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChooseVolumeDialog");
            return true;
        } else if (itemId == R.id.action_set_default_directory) {
            SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            sharedPrefEditor.putString(getString(R.string.pref_file_browser_root_dir_key), mCurrentDirectory.getPath());
            sharedPrefEditor.apply();
            return true;
        } else if (itemId == R.id.action_start_mediascanner) {
            startMediaScanning();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isRootDirectory() {
        return mIsRootDirectory;
    }

    public FileModel getCurrentDirectory() {
        return mCurrentDirectory;
    }

    /**
     * Call the PBS to play the selected file.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected file
     */
    private void playFile(final int position, final boolean clearPlaylist) {

        FileModel currentFile = mAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().playFile(currentFile.getPath(), clearPlaylist);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the selected file.
     *
     * @param position the position of the selected file
     * @param asNext   flag if the file should be enqueued as next
     */
    private void enqueueFile(int position, boolean asNext) {

        FileModel currentFile = mAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().enqueueFile(currentFile.getPath(), asNext);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play all music files from the selected folder (excluding subfolders) and starts with the selected file.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected file in the adapter
     */
    private void playFolder(int position) {
        try {
            // compute position
            int index = position - mCurrentDirectory.getNumberOfSubFolders();
            ((GenericActivity) getActivity()).getPlaybackService().playDirectory(mCurrentDirectory.getPath(), index);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play all music files from the selected folder and his children.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected folder
     */
    private void playFolderAndSubFolders(int position) {

        FileModel currentFolder = mAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().playDirectoryAndSubDirectories(currentFolder.getPath(), null);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue all music files from the selected folder and his children.
     *
     * @param position the position of the selected folder
     */
    private void enqueueFolderAndSubFolders(int position) {

        FileModel currentFolder = mAdapter.getItem(position);

        try {
            ((GenericActivity) getActivity()).getPlaybackService().enqueueDirectoryAndSubDirectories(currentFolder.getPath(), null);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play all music in the current folder and his children.
     * A previous playlist will be cleared.
     */
    private void playCurrentFolderAndSubFolders() {

        try {
            ((GenericActivity) getActivity()).getPlaybackService().playDirectoryAndSubDirectories(mCurrentDirectory.getPath(), mSearchString);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Start the media scan service for the current directory
     */
    private void startMediaScanning() {
        Intent serviceIntent = new Intent(getActivity(), MediaScannerService.class);
        serviceIntent.setAction(MediaScannerService.ACTION_START_MEDIASCANNING);

        serviceIntent.putExtra(MediaScannerService.BUNDLE_KEY_DIRECTORY, mCurrentDirectory.getPath());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(serviceIntent);
        } else {
            getActivity().startService(serviceIntent);
        }
    }

    /**
     * Call the PBS to enqueue all music in the current folder and his children.
     */
    private void enqueueCurrentFolderAndSubFolders() {

        try {
            ((GenericActivity) getActivity()).getPlaybackService().enqueueDirectoryAndSubDirectories(mCurrentDirectory.getPath(), mSearchString);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Observer class to apply a filter
     */
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
