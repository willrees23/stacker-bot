package dev.wand.stacker.config;

/**
 * Central configuration class containing all Discord IDs used by the bot.
 * This class stores tag IDs, channel IDs, and role IDs as constants.
 */
public class Config {
    
    // Tag IDs for forum posts
    public static final String TAG_FIXED = "1473409315749498960";
    public static final String TAG_IN_PROGRESS = "1473409358732722459";
    public static final String TAG_PENDING = "1473409882085396622";
    public static final String TAG_BUG = "1473409378974564535";
    public static final String TAG_RESOLVED = "1473827786471768307";
    public static final String TAG_FEATURE = "1473409710819639297";
    public static final String TAG_FEEDBACK = "1473409393151180901";
    
    // Channel IDs
    public static final String CHANNEL_TESTER_LOG_FORUM = "1473013973334102251";
    
    // Guild ID
    public static final String GUILD_ID = "1473013379902996542";
    
    // Role IDs
    public static final String ROLE_REQUIRED = "1473026921158676480";
    public static final String ROLE_TESTER_1 = "1473013562371997879";
    public static final String ROLE_TESTER_2 = "1473013593405653115";
    
    // Bot token (should be set via environment variable)
    public static String getBotToken() {
        String token = System.getenv("DISCORD_BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("DISCORD_BOT_TOKEN environment variable is not set");
        }
        return token;
    }
    
    private Config() {
        // Utility class, prevent instantiation
    }
}
