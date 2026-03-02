package dev.wand.stacker.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wand.stacker.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Fetches live game statistics from the Roblox API.
 *
 * <ul>
 *   <li>Game details + vote counts — via rotunnel.com (proxies the Roblox API)</li>
 *   <li>Server count — directly from games.roblox.com using the place ID</li>
 * </ul>
 */
public class RobloxApiService {

    private static final Logger logger = LoggerFactory.getLogger(RobloxApiService.class);
    private static final String ROTUNNEL_BASE = "https://games.rotunnel.com/v1";
    private static final String ROBLOX_BASE   = "https://games.roblox.com/v1";
    private static final int    MAX_SERVER_PAGES = 10;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetch current game stats for the configured universe.
     *
     * @return A populated {@link GameStats} object
     * @throws IOException if any API call fails or returns an unexpected response
     */
    public static GameStats fetchStats() throws IOException {
        String universeId = Config.ROBLOX_UNIVERSE_ID;

        // --- 1. Game details (players, visits, favourites, rootPlaceId) ---
        JsonObject gameData = getFirst(fetch(ROTUNNEL_BASE + "/games?universeIds=" + universeId), "data");
        long playersOnline = gameData.get("playing").getAsLong();
        long visits        = gameData.get("visits").getAsLong();
        long favourites    = gameData.get("favoritedCount").getAsLong();
        String placeId     = gameData.get("rootPlaceId").getAsString();

        // --- 2. Vote counts ---
        JsonObject votesData = fetch(ROTUNNEL_BASE + "/games/" + universeId + "/votes");
        long upVotes = votesData.get("upVotes").getAsLong();

        // --- 3. Server count (paginated via games.roblox.com) ---
        int serverCount = countServers(placeId);

        logger.info("Fetched game stats: players={}, servers={}, visits={}, upVotes={}, favourites={}",
                playersOnline, serverCount, visits, upVotes, favourites);

        return new GameStats(playersOnline, serverCount, visits, upVotes, favourites, Instant.now());
    }

    private static int countServers(String placeId) throws IOException {
        int total = 0;
        String cursor = null;
        for (int page = 0; page < MAX_SERVER_PAGES; page++) {
            String url = ROBLOX_BASE + "/games/" + placeId + "/servers/Public?sortOrder=Asc&limit=100";
            if (cursor != null) {
                url += "&cursor=" + cursor;
            }
            JsonObject response = fetch(url);
            if (response.has("data")) {
                total += response.getAsJsonArray("data").size();
            }
            if (response.has("nextPageCursor") && !response.get("nextPageCursor").isJsonNull()) {
                cursor = response.get("nextPageCursor").getAsString();
            } else {
                break;
            }
        }
        return total;
    }

    private static JsonObject fetch(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " from " + url);
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted: " + url, e);
        }
    }

    /** Extract the first element of a named JSON array. */
    private static JsonObject getFirst(JsonObject root, String arrayKey) throws IOException {
        if (!root.has(arrayKey) || root.getAsJsonArray(arrayKey).isEmpty()) {
            throw new IOException("Empty '" + arrayKey + "' in response");
        }
        return root.getAsJsonArray(arrayKey).get(0).getAsJsonObject();
    }

    private RobloxApiService() {}
}
