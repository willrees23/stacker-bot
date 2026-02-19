package dev.wand.stacker.listeners;

import dev.wand.stacker.config.Config;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener that automatically applies the "Pending" tag to new forum threads.
 * 
 * When a new thread is created in the Tester Log Forum, this listener:
 * 1. Validates that the thread belongs to the correct forum
 * 2. Applies the "Pending" tag automatically
 * 
 * This ensures all new bug reports start with a consistent status.
 */
public class ForumThreadListener extends ListenerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(ForumThreadListener.class);
    
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        // Check if the created channel is a thread
        if (!(event.getChannel() instanceof ThreadChannel)) {
            return;
        }
        
        ThreadChannel thread = (ThreadChannel) event.getChannel();
        
        // Check if this thread belongs to the Tester Log Forum
        if (thread.getParentChannel() == null || 
            !thread.getParentChannel().getId().equals(Config.CHANNEL_TESTER_LOG_FORUM)) {
            return;
        }
        
        // Verify the parent is a forum channel
        if (!(thread.getParentChannel() instanceof ForumChannel)) {
            return;
        }
        
        ForumChannel forumChannel = (ForumChannel) thread.getParentChannel();
        
        // Find the Pending tag
        ForumTag pendingTag = forumChannel.getAvailableTags().stream()
                .filter(tag -> tag.getId().equals(Config.TAG_PENDING))
                .findFirst()
                .orElse(null);
        
        if (pendingTag == null) {
            logger.warn("Pending tag not found in forum: {}", forumChannel.getName());
            return;
        }
        
        // Get current tags and add the Pending tag if not already present
        List<ForumTag> currentTags = new ArrayList<>(thread.getAppliedTags());
        
        // Check if a status tag is already applied
        boolean hasStatusTag = currentTags.stream()
                .anyMatch(tag -> 
                        tag.getId().equals(Config.TAG_PENDING) ||
                        tag.getId().equals(Config.TAG_IN_PROGRESS) ||
                        tag.getId().equals(Config.TAG_FIXED) ||
                        tag.getId().equals(Config.TAG_RESOLVED)
                );
        
        // Only add Pending tag if no status tag is present
        if (!hasStatusTag) {
            currentTags.add(pendingTag);
            
            // Apply the tags
            thread.getManager().setAppliedTags(currentTags).queue(
                    success -> logger.info("Automatically applied Pending tag to new thread: {}", thread.getName()),
                    error -> logger.error("Failed to apply Pending tag to new thread: {}", thread.getName(), error)
            );
        }
    }
}
