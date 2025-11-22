package com.shazam.fingerprint;

/**
 * Represents a single fingerprint hash with its metadata.
 * Each hash is derived from a pair of spectral peaks (anchor + target).
 */
public class FingerprintHash implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final long hash;
    private final int timeOffset; // Time offset in the original song (in frames)
    private final String songId;

    public FingerprintHash(long hash, int timeOffset, String songId) {
        this.hash = hash;
        this.timeOffset = timeOffset;
        this.songId = songId;
    }

    public long getHash() {
        return hash;
    }

    public int getTimeOffset() {
        return timeOffset;
    }

    public String getSongId() {
        return songId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FingerprintHash that = (FingerprintHash) o;
        return hash == that.hash && timeOffset == that.timeOffset && songId.equals(that.songId);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(hash);
        result = 31 * result + timeOffset;
        result = 31 * result + songId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Hash{hash=%d, offset=%d, songId=%s}", hash, timeOffset, songId);
    }

    /**
     * Creates a hash from two frequency peaks and their time delta.
     * 
     * @param freq1     Frequency of anchor point (in bins)
     * @param freq2     Frequency of target point (in bins)
     * @param timeDelta Time difference between points (in frames)
     * @return Hash value
     */
    public static long createHash(int freq1, int freq2, int timeDelta) {
        // Combine the three values into a single long hash
        // Use bit shifting to pack the values efficiently
        return ((long) freq1 << 32) | ((long) freq2 << 16) | timeDelta;
    }
}
