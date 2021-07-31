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

package org.gateshipone.odyssey.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.utils.GenericModelTaskRunner;
import org.gateshipone.odyssey.utils.PermissionHelper;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Callable;

public class FileViewModel extends GenericViewModel<FileModel> {

    /**
     * The parent directory.
     */
    private final FileModel mCurrentDirectory;

    private FileViewModel(@NonNull final Application application, final FileModel directory) {
        super(application);

        mCurrentDirectory = directory;
    }

    @Override
    void loadData() {
        new GenericModelTaskRunner<FileModel>().executeAsync(new FileLoaderTask(this), this::setData);
    }

    private static class FileLoaderTask implements Callable<List<FileModel>> {

        private final WeakReference<FileViewModel> mViewModel;

        FileLoaderTask(final FileViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        public List<FileModel> call() throws Exception {
            final FileViewModel model = mViewModel.get();

            if (model != null) {
                return PermissionHelper.getFilesForDirectory(model.getApplication(), model.mCurrentDirectory);
            }

            return null;
        }
    }

    public static class FileViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        private final FileModel mCurrentDirectory;

        public FileViewModelFactory(final Application application, final FileModel directory) {
            mApplication = application;
            mCurrentDirectory = directory;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new FileViewModel(mApplication, mCurrentDirectory);
        }
    }
}
