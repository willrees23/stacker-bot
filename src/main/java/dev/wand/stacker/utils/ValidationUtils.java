package dev.wand.stacker.utils;

import dev.wand.stacker.config.Config;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

/**
 * Utility class for validating command context and inputs.
 * Provides methods to check if commands are used in the correct context.
 */
public class ValidationUtils {
    
    /**
     * Check if a channel is a thread in the Tester Log Forum.
     * 
     * @param channel The channel to check
     * @return true if the channel is a thread in the Tester Log Forum, false otherwise
     */
    public static boolean isThreadInTesterLogForum(Channel channel) {
        if (!(channel instanceof ThreadChannel)) {
            return false;
        }
        
        ThreadChannel threadChannel = (ThreadChannel) channel;
        String parentChannelId = threadChannel.getParentChannel().getId();
        
        return parentChannelId.equals(Config.CHANNEL_TESTER_LOG_FORUM);
    }
    
    private ValidationUtils() {
        // Utility class, prevent instantiation
    }
}
