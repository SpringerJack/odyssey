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

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.BookmarkModel;
import org.gateshipone.odyssey.playbackservice.storage.OdysseyDatabaseManager;
import org.gateshipone.odyssey.utils.GenericModelTaskRunner;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class BookmarkViewModel extends GenericViewModel<BookmarkModel> {

    /**
     * Flag if a header element should be inserted.
     */
    private final boolean mAddHeader;

    private BookmarkViewModel(@NonNull final Application application, final boolean addHeader) {
        super(application);

        mAddHeader = addHeader;
    }

    @Override
    void loadData() {
        new GenericModelTaskRunner<BookmarkModel>().executeAsync(new BookmarkLoaderTask(this), this::setData);
    }

    private static class BookmarkLoaderTask implements Callable<List<BookmarkModel>> {

        private final WeakReference<BookmarkViewModel> mViewModel;

        BookmarkLoaderTask(final BookmarkViewModel viewModel) {
            mViewModel = new WeakReference<>(viewModel);
        }

        @Override
        public List<BookmarkModel> call() throws Exception {
            final BookmarkViewModel model = mViewModel.get();

            if (model != null) {
                final Application application = model.getApplication();

                final List<BookmarkModel> bookmarks = new ArrayList<>();

                if (model.mAddHeader) {
                    // add a dummy bookmark for the choose bookmark dialog
                    // this bookmark represents the action to create a new bookmark in the dialog
                    bookmarks.add(new BookmarkModel(-1, application.getString(R.string.create_new_bookmark), -1));
                }
                bookmarks.addAll(OdysseyDatabaseManager.getInstance(application).getBookmarks());

                return bookmarks;
            }

            return null;
        }
    }

    public static class BookmarkViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private final Application mApplication;

        private final boolean mAddHeader;

        public BookmarkViewModelFactory(final Application application, final boolean addHeader) {
            mApplication = application;
            mAddHeader = addHeader;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new BookmarkViewModel(mApplication, mAddHeader);
        }
    }
}
