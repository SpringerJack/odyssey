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

package org.gateshipone.odyssey.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.models.GenericModel;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.List;

public class GenericModelTaskRunner<T extends GenericModel> {
    private static final String TAG = GenericModelTaskRunner.class.getSimpleName();

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public interface DataLoadedCallback<T extends GenericModel> {
        void onDataLoaded(List<T> result);
    }

    public void executeAsync(Callable<List<T>> callable, DataLoadedCallback<T> callback) {
        mExecutor.execute(() -> {
            try {
                final List<T> result = callable.call();
                mHandler.post(() -> callback.onDataLoaded(result));
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "executeAsync failed to load data: " + e.getMessage());
                }
                mHandler.post(() -> callback.onDataLoaded(null));
            }
        });
    }
}
