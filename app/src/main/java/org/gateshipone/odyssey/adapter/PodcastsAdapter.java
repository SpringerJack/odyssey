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

package org.gateshipone.odyssey.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceManager;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artwork.ArtworkManager;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.viewitems.ListViewItem;

public class PodcastsAdapter extends GenericSectionAdapter<TrackModel> {
    private static final String TAG = PodcastsAdapter.class.getSimpleName();

    private final Context mContext;

    private ArtworkManager mArtworkManager;

    private int mListItemHeight;

    private boolean mHideArtwork;

    public PodcastsAdapter(final Context context) {
        super();

        mContext = context;
        mListItemHeight = (int) context.getResources().getDimension(R.dimen.material_list_item_height);

        mArtworkManager = ArtworkManager.getInstance(context);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mHideArtwork = sharedPreferences.getBoolean(context.getString(R.string.pref_hide_artwork_key), context.getResources().getBoolean(R.bool.pref_hide_artwork_default));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TrackModel podcast = getItem(position);

        ListViewItem listViewItem;

        if (convertView != null) {
            listViewItem = (ListViewItem) convertView;
        } else {
            listViewItem = ListViewItem.createPodcastItem(mContext, this);
        }

        listViewItem.setPodcast(podcast);

        if (!mHideArtwork) {
            // This will prepare the view for fetching the image from the internet if not already saved in local database.
            listViewItem.prepareArtworkFetching(mArtworkManager, podcast);

            // Check if the scroll speed currently is already 0, then start the image task right away.
            if (mScrollSpeed == 0) {
                listViewItem.setImageDimension(mListItemHeight, mListItemHeight);
                listViewItem.startCoverImageTask();
            }
        }
        return listViewItem;
    }
}
