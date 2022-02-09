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

package org.gateshipone.odyssey.models;

import android.util.Log;

import org.gateshipone.odyssey.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 * This class keeps a HashMap of all artists that are part of a track list (e.g. playlist)
 * and their belonging tracks with the original list position as a pair. This can be used to
 * randomize the playback of the playback equally distributed over all artists of the original
 * track list.
 */
public class TrackRandomGenerator {
    private static final String TAG = TrackRandomGenerator.class.getSimpleName();

    /**
     * Underlying data structure for artist-track buckets
     */
    private final ArrayList<List<Integer>> mData;

    /**
     * Creates an empty data structure
     */
    public TrackRandomGenerator() {
        mData = new ArrayList<>();
    }

    private final BetterPseudoRandomGenerator mRandomGenerator = new BetterPseudoRandomGenerator();

    private List<TrackModel> mOriginalList;

    private int mIntelligenceFactor;

    /**
     * Creates a list of artists and their tracks with position in the original playlist
     *
     * @param tracks List of tracks
     */
    public synchronized void fillFromList(List<TrackModel> tracks) {
        // Clear all entries
        mData.clear();

        mOriginalList = tracks;

        if (mIntelligenceFactor == 0) {
            return;
        }
        LinkedHashMap<String, List<Integer>> hashMap = new LinkedHashMap<>();

        if (tracks == null || tracks.isEmpty()) {
            // Abort for empty data structures
            return;
        }

        // Iterate over the list and add all tracks to their artist lists
        int trackNo = 0;
        for (TrackModel track : tracks) {
            String artistName = track.getTrackArtistName();
            List<Integer> list = hashMap.get(artistName);
            if (list == null) {
                // If artist is not already in HashMap add a new list for it
                list = new ArrayList<>();
                hashMap.put(artistName, list);
            }
            // Add pair of position in original playlist and track itself to artists bucket list
            list.add(trackNo);

            // Increase the track number (index) of the original playlist
            trackNo++;
        }
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Recreated buckets with: " + hashMap.size() + " artists");
        }

        mData.addAll(hashMap.values());
        Collections.shuffle(mData);
    }

    /**
     * Generates a randomized track number within the original track list, that was used for the call
     * of fillFromList. The random track number should be equally distributed over all artists.
     *
     * @return A random number of a track of the original track list
     */
    public synchronized int getRandomTrackNumber() {
        // Randomize if a more balanced (per artist) approach or a traditional approach should be used
        boolean smartRandom = mRandomGenerator.getLimitedRandomNumber(100) < mIntelligenceFactor;

        if (smartRandom) {
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "Use smart random");
            }
            if (mData.isEmpty()) {
                // Refill list from original list
                fillFromList(mOriginalList);
            }

            // First level random, get artist
            int randomArtistNumber = mRandomGenerator.getLimitedRandomNumber(mData.size());

            // Get artists bucket list to artist number
            List<Integer> artistsTracks;


            // Get the list of tracks belonging to the selected artist
            artistsTracks = mData.get(randomArtistNumber);

            // Check if an artist was found
            if (artistsTracks == null) {
                return 0;
            }

            int randomTrackNo = mRandomGenerator.getLimitedRandomNumber(artistsTracks.size());

            Integer songNumber = artistsTracks.get(randomTrackNo);

            // Remove track to prevent double plays
            artistsTracks.remove(randomTrackNo);
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "Tracks from artist left: " + artistsTracks.size());
            }

            // Check if tracks from this artist are left, otherwise remove the artist
            if (artistsTracks.isEmpty()) {
                // No tracks left from artist, remove from map
                mData.remove(randomArtistNumber);
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Artists left: " + mData.size());
                }
            }
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "Selected artist no.: " + randomArtistNumber + " with internal track no.: " + randomTrackNo + " and original track no.: " + songNumber);
            }
            // Get random track number
            return songNumber;
        } else {
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "Use traditional random");
            }
            return mRandomGenerator.getLimitedRandomNumber(mOriginalList.size());
        }
    }

    public void setEnabled(int factor) {
        if (mIntelligenceFactor == 0 && factor != 0) {
            // Redo track buckets
            fillFromList(mOriginalList);
        } else if (mIntelligenceFactor != 0 && factor == 0) {
            // Remove track buckets
            fillFromList(null);
        }
        mIntelligenceFactor = factor;
    }

    private static class BetterPseudoRandomGenerator {
        /**
         * Timeout in ns (1 second)
         */
        private static final long TIMEOUT_NS = 10000000000L;
        private final Random mJavaGenerator;

        private static final int RAND_MAX = Integer.MAX_VALUE;

        /**
         * Value after how many random numbers a reseed is done
         */
        private static final int RESEED_COUNT = 20;

        private int mNumbersGiven = 0;

        private int mInternalSeed;


        private BetterPseudoRandomGenerator() {
            mJavaGenerator = new Random();

            // Initialize internal seed
            mInternalSeed = mJavaGenerator.nextInt();


            // Do a quick check
            //testDistribution(20,20);
        }

        private int getInternalRandomNumber() {
            /*
             * Marsaglia, "Xorshift RNGs"
             */
            int newSeed = mInternalSeed;

            newSeed ^= newSeed << 13;
            newSeed ^= newSeed >> 17;
            newSeed ^= newSeed << 5;

            mNumbersGiven++;
            if (mNumbersGiven == RESEED_COUNT) {
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Reseeded PRNG");
                }
                mInternalSeed = mJavaGenerator.nextInt();
                mNumbersGiven = 0;
            } else {
                mInternalSeed = newSeed;
            }
            return Math.abs(newSeed);
        }

        int getLimitedRandomNumber(int limit) {
            if (limit == 0) {
                return 0;
            }
            int r, d = RAND_MAX / limit;
            limit *= d;
            long startTime = System.nanoTime();
            do {
                r = getInternalRandomNumber();
                if ((System.nanoTime() - startTime) > TIMEOUT_NS) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Random generation timed out");
                    }
                    // Fallback to java generator
                    return mJavaGenerator.nextInt(limit);
                }
            } while (r >= limit);
            return r / d;
        }


        private void testDistribution(int numberLimit, int runs) {
            int[] numberCount = new int[numberLimit];

            for (int i = 0; i < runs; i++) {
                numberCount[getLimitedRandomNumber(numberLimit)]++;
            }

            // Print distribution and calculate mean
            int arithmeticMean = 0;
            for (int i = 0; i < numberLimit; i++) {
                Log.v(TAG, "Number: " + i + " = " + numberCount[i]);
                arithmeticMean += numberCount[i];
            }

            arithmeticMean /= numberLimit;
            Log.v(TAG, "Mean value: " + arithmeticMean);

            int variance = 0;
            for (int i = 0; i < numberLimit; i++) {
                variance += Math.pow((numberCount[i] - arithmeticMean), 2);
            }
            Log.v(TAG, "Variance: " + variance);
            double sd = Math.sqrt(variance);
            Log.v(TAG, "Standard deviation: " + sd);
            double rsd = sd / arithmeticMean;
            Log.v(TAG, "Relative standard deviation: " + rsd + " %");

        }
    }
}
