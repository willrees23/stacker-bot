package dev.wand.stacker.commands;

import dev.wand.stacker.embeds.EmbedManager;
import dev.wand.stacker.services.GameStats;
import dev.wand.stacker.services.LiveStatsStore;
import dev.wand.stacker.services.RobloxApiService;
import dev.wand.stacker.utils.PermissionUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * /stats — Displays live Roblox game statistics.
 *
 * Available to all users. The optional {@code admin:true} flag sends a non-ephemeral
 * embed that auto-refreshes every 2 minutes (staff only).
 *
 * All live embeds share a single poll: one API fetch per cycle updates every tracked message.
 */
public class StatsCommand implements CommandInterface {

    private static final Logger logger = LoggerFactory.getLogger(StatsCommand.class);

    static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    /**
     * All currently tracked live embeds: "channelId:messageId" → JDA instance.
     * A single shared poll task iterates this map each cycle.
     */
    static final ConcurrentHashMap<String, JDA> TRACKED = new ConcurrentHashMap<>();

    /** The single shared 2-minute poll task, started lazily when the first live embed is created. */
    private static volatile ScheduledFuture<?> pollTask = null;
    private static final Object POLL_LOCK = new Object();

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public boolean requiresPermission() {
        return false;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("stats", "View live game stats for Stacker")
                .addOption(OptionType.BOOLEAN, "admin",
                        "Send as live, auto-updating embed (staff only)", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping adminOption = event.getOption("admin");
        boolean admin = adminOption != null && adminOption.getAsBoolean();

        if (admin && !PermissionUtils.hasRequiredRole(event.getMember())) {
            event.replyEmbeds(EmbedManager.createPermissionDeniedEmbed())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(!admin).queue();

        SCHEDULER.submit(() -> {
            try {
                GameStats stats = RobloxApiService.fetchStats();
                if (admin) {
                    event.getHook().editOriginalEmbeds(EmbedManager.createLiveStatsEmbed(stats))
                            .queue(this::trackLiveMessage);
                } else {
                    event.getHook().editOriginalEmbeds(EmbedManager.createStatsEmbed(stats)).queue();
                }
            } catch (Exception e) {
                logger.error("Failed to fetch game stats", e);
                event.getHook().editOriginalEmbeds(
                        EmbedManager.createError("Stats Unavailable",
                                "Could not retrieve game stats. Please try again later.")
                ).queue();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Tracking & shared poll
    // -------------------------------------------------------------------------

    private void trackLiveMessage(Message message) {
        String channelId = message.getChannel().getId();
        String messageId = message.getId();
        addTracked(channelId, messageId, message.getJDA());
    }

    /**
     * Register an existing bot message as a live-updating embed.
     * Safe to call from any command (e.g. /stats-append).
     *
     * @param channelId The channel containing the message
     * @param messageId The message to track
     * @param jda       The JDA instance used to reach Discord
     */
    public static void addTracked(String channelId, String messageId, JDA jda) {
        String key = channelId + ":" + messageId;

        try {
            LiveStatsStore.add(channelId, messageId);
        } catch (Exception e) {
            logger.error("Failed to persist live stats entry {}", key, e);
        }

        TRACKED.put(key, jda);
        ensurePollRunning();
        logger.info("Tracking live stats embed {} ({} total)", key, TRACKED.size());
    }

    /**
     * Check whether a message is already in the live-tracking list.
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @return {@code true} if the message is currently tracked
     */
    public static boolean isTracked(String channelId, String messageId) {
        return TRACKED.containsKey(channelId + ":" + messageId);
    }

    /** Start the shared poll task if it isn't already running. */
    private static void ensurePollRunning() {
        synchronized (POLL_LOCK) {
            if (pollTask == null || pollTask.isCancelled() || pollTask.isDone()) {
                pollTask = SCHEDULER.scheduleAtFixedRate(
                        StatsCommand::runSharedPoll,
                        2, 2, TimeUnit.MINUTES
                );
                logger.info("Started shared live stats poll task");
            }
        }
    }

    /**
     * Fetch stats once, then push the update to every tracked live embed.
     * Called every 2 minutes by the single shared poll task.
     * Before fetching, all tracked embeds are updated to a loading state so
     * users can see that a refresh is in progress.
     */
    private static void runSharedPoll() {
        if (TRACKED.isEmpty()) return;

        // Snapshot keys to avoid concurrent-modification issues during iteration
        Set<String> snapshot = Set.copyOf(TRACKED.keySet());

        // Show loading state on all tracked embeds while the API request is in-flight
        for (String key : snapshot) {
            String[] parts = key.split(":", 2);
            String channelId = parts[0];
            String messageId = parts[1];
            JDA jda = TRACKED.get(key);
            if (jda == null) continue;

            MessageChannel channel = (MessageChannel) jda.getChannelById(MessageChannel.class, channelId);
            if (channel == null) {
                logger.info("Live stats message {} removed from poll list (channel no longer found)", key);
                removeTracked(key, channelId, messageId);
                continue;
            }

            channel.editMessageEmbedsById(messageId, EmbedManager.createLoadingStatsEmbed())
                    .queue(
                            success -> logger.debug("Showed loading state for live stats embed {}", key),
                            error -> logger.warn("Could not show loading state for embed {}: {}", key, error.getMessage())
                    );
        }

        GameStats stats;
        try {
            stats = RobloxApiService.fetchStats();
        } catch (Exception e) {
            logger.error("Shared poll: failed to fetch game stats", e);
            return;
        }

        // Re-snapshot in case the map changed while the API call was in-flight
        for (String key : Set.copyOf(TRACKED.keySet())) {
            String[] parts = key.split(":", 2);
            String channelId = parts[0];
            String messageId = parts[1];
            JDA jda = TRACKED.get(key);
            if (jda == null) continue;

            MessageChannel channel = (MessageChannel) jda.getChannelById(MessageChannel.class, channelId);
            if (channel == null) {
                logger.info("Live stats message {} removed from poll list (channel no longer found)", key);
                removeTracked(key, channelId, messageId);
                continue;
            }

            channel.editMessageEmbedsById(messageId, EmbedManager.createLiveStatsEmbed(stats))
                    .queue(
                            success -> logger.debug("Updated live stats embed {}", key),
                            error -> {
                                logger.info("Live stats message {} removed from poll list ({})",
                                        key, error.getMessage());
                                removeTracked(key, channelId, messageId);
                            }
                    );
        }
    }

    private static void removeTracked(String key, String channelId, String messageId) {
        TRACKED.remove(key);
        try {
            LiveStatsStore.remove(channelId, messageId);
        } catch (Exception e) {
            logger.error("Failed to remove live stats entry {} from store", key, e);
        }
    }

    /**
     * Resume any live polls persisted in {@code live_stats.txt}.
     * Call this once after the bot is ready.
     */
    public static void resumeLivePolls(JDA jda) {
        Set<String[]> entries;
        try {
            entries = LiveStatsStore.readAll();
        } catch (Exception e) {
            logger.error("Failed to read live_stats.txt on startup", e);
            return;
        }

        for (String[] pair : entries) {
            String channelId = pair[0];
            String messageId = pair[1];
            String key = channelId + ":" + messageId;

            MessageChannel channel;
            try {
                channel = (MessageChannel) jda.getChannelById(MessageChannel.class, channelId);
            } catch (Exception e) {
                channel = null;
            }

            if (channel == null) {
                logger.warn("Live stats channel {} not found on resume; removing entry", channelId);
                try {
                    LiveStatsStore.remove(channelId, messageId);
                } catch (Exception ex) {
                    logger.error("Failed to remove stale entry {}", key, ex);
                }
                continue;
            }

            TRACKED.put(key, jda);
            logger.info("Resumed tracking live stats embed {}", key);
        }

        if (!TRACKED.isEmpty()) {
            ensurePollRunning();
            logger.info("Resumed shared poll for {} live embed(s)", TRACKED.size());
        }
    }
}
