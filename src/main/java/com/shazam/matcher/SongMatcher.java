package com.shazam.matcher;

import com.shazam.config.Constants;
import com.shazam.database.FingerprintDatabase;
import com.shazam.fingerprint.FingerprintHash;
import com.shazam.model.Song;

import java.util.*;

/**
 * Matches query audio against the fingerprint database.
 * Uses time-consistency scoring to find the best match.
 */
public class SongMatcher {

    private final FingerprintDatabase database;

    public SongMatcher(FingerprintDatabase database) {
        this.database = database;
    }

    /**
     * Represents a match result with confidence score.
     */
    public static class MatchResult implements Comparable<MatchResult> {
        private final Song song;
        private final double confidence;
        private final int matchCount;

        public MatchResult(Song song, double confidence, int matchCount) {
            this.song = song;
            this.confidence = confidence;
            this.matchCount = matchCount;
        }

        public Song getSong() {
            return song;
        }

        public double getConfidence() {
            return confidence;
        }

        public int getMatchCount() {
            return matchCount;
        }

        @Override
        public int compareTo(MatchResult other) {
            // Sort by confidence descending
            return Double.compare(other.confidence, this.confidence);
        }

        @Override
        public String toString() {
            return String.format("%s (Confidence: %.2f%%, Matches: %d)",
                    song, confidence * 100, matchCount);
        }
    }

    /**
     * Finds the best matching songs for the query fingerprints.
     * 
     * @param queryHashes Fingerprint hashes from query audio
     * @return List of match results sorted by confidence
     */
    public List<MatchResult> findMatches(List<FingerprintHash> queryHashes) {
        if (queryHashes.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Query database for matching hashes
        Map<String, List<FingerprintHash>> matchesBySong = database.query(queryHashes);

        if (matchesBySong.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: Score each candidate song using time-consistency
        List<MatchResult> results = new ArrayList<>();

        for (Map.Entry<String, List<FingerprintHash>> entry : matchesBySong.entrySet()) {
            String songId = entry.getKey();
            List<FingerprintHash> matches = entry.getValue();

            // Skip if too few matches
            if (matches.size() < Constants.MIN_MATCHING_HASHES) {
                continue;
            }

            // Calculate time-consistency score
            double confidence = calculateTimeConsistencyScore(matches, queryHashes);

            if (confidence >= Constants.MIN_CONFIDENCE_THRESHOLD) {
                Song song = database.getSong(songId);
                results.add(new MatchResult(song, confidence, matches.size()));
            }
        }

        // Step 3: Sort by confidence
        Collections.sort(results);

        return results;
    }

    /**
     * Calculates a confidence score based on time-consistency of matches.
     * Matches should align temporally (same time offset between query and song).
     */
    private double calculateTimeConsistencyScore(List<FingerprintHash> dbMatches,
            List<FingerprintHash> queryHashes) {
        // Create a map of query hash values to their time offsets
        Map<Long, Integer> queryTimeMap = new HashMap<>();
        for (FingerprintHash qh : queryHashes) {
            queryTimeMap.put(qh.getHash(), qh.getTimeOffset());
        }

        // Calculate time deltas (difference between db time and query time)
        Map<Integer, Integer> timeDeltaHistogram = new HashMap<>();

        for (FingerprintHash dbMatch : dbMatches) {
            Integer queryTime = queryTimeMap.get(dbMatch.getHash());
            if (queryTime != null) {
                int timeDelta = dbMatch.getTimeOffset() - queryTime;
                timeDeltaHistogram.put(timeDelta, timeDeltaHistogram.getOrDefault(timeDelta, 0) + 1);
            }
        }

        if (timeDeltaHistogram.isEmpty()) {
            return 0.0;
        }

        // Find the most common time delta (peak in histogram)
        int maxCount = 0;
        int bestTimeDelta = 0;

        for (Map.Entry<Integer, Integer> entry : timeDeltaHistogram.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                bestTimeDelta = entry.getKey();
            }
        }

        // Count matches that are consistent with the best time delta
        int consistentMatches = 0;
        for (Map.Entry<Integer, Integer> entry : timeDeltaHistogram.entrySet()) {
            int delta = entry.getKey();
            if (Math.abs(delta - bestTimeDelta) <= Constants.TIME_DELTA_TOLERANCE) {
                consistentMatches += entry.getValue();
            }
        }

        // Confidence is the ratio of consistent matches to total query hashes
        double confidence = (double) consistentMatches / queryHashes.size();

        return Math.min(1.0, confidence);
    }

    /**
     * Gets the top N matches.
     */
    public List<MatchResult> getTopMatches(List<FingerprintHash> queryHashes, int topN) {
        List<MatchResult> allMatches = findMatches(queryHashes);
        return allMatches.subList(0, Math.min(topN, allMatches.size()));
    }
}
