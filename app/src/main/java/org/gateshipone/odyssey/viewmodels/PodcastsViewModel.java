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

package org.gateshipone.odyssey.viewmodels;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;

import java.lang.ref.WeakReference;
import java.util.List;

public class PodcastsViewModel extends GenericViewModel<TrackModel> {

    private PodcastsViewModel(@NonNull final Application application) {
        super(application);
    }

    void loadData() {
        new PodcastsLoaderTask(this).execute();
    }

    private static class PodcastsLoaderTask extends AsyncTask<Void, Void, List<TrackModel>> {

        private final WeakReference<PodcastsViewModel> mViewModel;

        PodcastsLoaderTask(final PodcastsViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        protected List<TrackModel> doInBackground(Void... voids) {
            final PodcastsViewModel model = mViewModel.get();

            if (model != null) {
                final Application application = model.getApplication();

                return MusicLibraryHelper.getAllPodcasts(application);
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<TrackModel> result) {
            final PodcastsViewModel model = mViewModel.get();

            if (model != null) {
                model.setData(result);
            }
        }
    }

    public static class PodcastsViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        public PodcastsViewModelFactory(final Application application) {
            mApplication = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new PodcastsViewModel(mApplication);
        }
    }
}
