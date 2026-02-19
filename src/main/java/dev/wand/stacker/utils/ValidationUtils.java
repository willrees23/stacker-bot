package dev.wand.stacker.utils;

import dev.wand.stacker.config.Config;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;

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
    
    /**
     * Check if a forum tag is a status tag (Fixed, In Progress, Pending, or Resolved).
     * 
     * @param tag The forum tag to check
     * @return true if the tag is a status tag, false otherwise
     */
    public static boolean isStatusTag(ForumTag tag) {
        String tagId = tag.getId();
        return tagId.equals(Config.TAG_FIXED) ||
               tagId.equals(Config.TAG_IN_PROGRESS) ||
               tagId.equals(Config.TAG_PENDING) ||
               tagId.equals(Config.TAG_RESOLVED);
    }
    
    private ValidationUtils() {
        // Utility class, prevent instantiation
    }
}
