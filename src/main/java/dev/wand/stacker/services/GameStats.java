package dev.wand.stacker.services;

import java.time.Instant;

/**
 * Immutable data holder for fetched Roblox game statistics.
 */
public class GameStats {
    public final long playersOnline;
    public final int serverCount;
    public final long visits;
    public final long upVotes;
    public final long favourites;
    public final Instant retrievedAt;

    public GameStats(long playersOnline, int serverCount, long visits,
                     long upVotes, long favourites, Instant retrievedAt) {
        this.playersOnline = playersOnline;
        this.serverCount = serverCount;
        this.visits = visits;
        this.upVotes = upVotes;
        this.favourites = favourites;
        this.retrievedAt = retrievedAt;
    }
}
