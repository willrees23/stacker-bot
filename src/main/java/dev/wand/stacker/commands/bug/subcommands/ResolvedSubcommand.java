package dev.wand.stacker.commands.bug.subcommands;

import dev.wand.stacker.config.Config;
import dev.wand.stacker.embeds.EmbedManager;
import dev.wand.stacker.utils.ValidationUtils;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand to mark a bug as resolved.
 * 
 * Usage: /bug resolved
 * 
 * This command:
 * 1. Validates that it's used in a thread within the Tester Log Forum
 * 2. Applies the "Resolved" tag to the thread
 * 3. Sends a confirmation embed
 * 4. Closes the thread
 * 
 * Requirements:
 * - Must be used in a thread
 * - Thread must belong to the Tester Log Forum channel
 * - User must have the required role (checked by CommandManager)
 */
public class ResolvedSubcommand {
    
    private static final Logger logger = LoggerFactory.getLogger(ResolvedSubcommand.class);
    
    /**
     * Execute the resolved subcommand.
     * 
     * @param event The slash command interaction event
     */
    public void execute(SlashCommandInteractionEvent event) {
        Channel channel = event.getChannel();
        
        // Validate that this is a thread in the Tester Log Forum
        if (!ValidationUtils.isThreadInTesterLogForum(channel)) {
            event.replyEmbeds(EmbedManager.createInvalidContextEmbed(
                    "in a thread within the Tester Log Forum"
            )).setEphemeral(true).queue();
            return;
        }
        
        ThreadChannel threadChannel = (ThreadChannel) channel;
        
        // Defer the reply ephemerally since we're doing multiple operations
        event.deferReply(true).queue();
        
        // Apply the Resolved tag
        applyResolvedTag(event, threadChannel);
    }
    
    /**
     * Apply the Resolved tag to the thread and close it.
     * 
     * @param event The command event
     * @param threadChannel The thread to mark as resolved
     */
    private void applyResolvedTag(SlashCommandInteractionEvent event, ThreadChannel threadChannel) {
        // Verify the parent channel is a ForumChannel
        if (!(threadChannel.getParentChannel() instanceof ForumChannel)) {
            logger.error("Parent channel is not a ForumChannel: {}", threadChannel.getParentChannel().getName());
            event.getHook().editOriginal("❌ This thread's parent channel is not a forum.").queue();
            return;
        }
        
        ForumChannel parentChannel = (ForumChannel) threadChannel.getParentChannel();
        
        // Get the Resolved tag from the forum
        ForumTag resolvedTag = parentChannel.getAvailableTags().stream()
                .filter(tag -> tag.getId().equals(Config.TAG_RESOLVED))
                .findFirst()
                .orElse(null);
        
        if (resolvedTag == null) {
            logger.error("Resolved tag not found in forum: {}", parentChannel.getName());
            event.getHook().editOriginal("❌ The Resolved tag is not configured in this forum.").queue();
            return;
        }
        
        // Get current tags and add the Resolved tag
        List<ForumTag> newTags = new ArrayList<>(threadChannel.getAppliedTags());
        
        // Remove any existing status tags and add the Resolved tag
        newTags.removeIf(tag -> 
                tag.getId().equals(Config.TAG_FIXED) ||
                tag.getId().equals(Config.TAG_IN_PROGRESS) ||
                tag.getId().equals(Config.TAG_PENDING) ||
                tag.getId().equals(Config.TAG_RESOLVED)
        );
        newTags.add(resolvedTag);
        
        // Apply the tags
        threadChannel.getManager().setAppliedTags(newTags).queue(
                success -> {
                    logger.info("Applied Resolved tag to thread: {}", threadChannel.getName());
                    // Send ephemeral success message to the user
                    event.getHook().editOriginal("✅ Successfully marked this bug as resolved!").queue(
                            message -> {
                                // Send the public embed into the channel
                                threadChannel.sendMessageEmbeds(EmbedManager.createBugResolvedEmbed()).queue(
                                        publicMessage -> closeThread(threadChannel),
                                        error -> logger.error("Failed to send bug resolved embed", error)
                                );
                            },
                            error -> logger.error("Failed to send ephemeral response", error)
                    );
                },
                error -> {
                    logger.error("Failed to apply Resolved tag to thread", error);
                    event.getHook().editOriginal("❌ Failed to apply the Resolved tag. Please check bot permissions.").queue();
                }
        );
    }
    
    /**
     * Close the thread after marking it as resolved.
     * 
     * @param threadChannel The thread to close
     */
    private void closeThread(ThreadChannel threadChannel) {
        threadChannel.getManager().setArchived(true).queue(
                success -> logger.info("Closed thread: {}", threadChannel.getName()),
                error -> logger.error("Failed to close thread: {}", threadChannel.getName(), error)
        );
    }
}
