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
 * Subcommand to mark a bug as in progress.
 * 
 * Usage: /bug in-progress
 * 
 * This command:
 * 1. Validates that it's used in a thread within the Tester Log Forum
 * 2. Applies the "In Progress" tag to the thread
 * 3. Sends a confirmation embed
 * 4. Does NOT close the thread (keeps it open for updates)
 * 
 * Requirements:
 * - Must be used in a thread
 * - Thread must belong to the Tester Log Forum channel
 * - User must have the required role (checked by CommandManager)
 */
public class InProgressSubcommand {
    
    private static final Logger logger = LoggerFactory.getLogger(InProgressSubcommand.class);
    
    /**
     * Execute the in-progress subcommand.
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
        
        // Apply the In Progress tag
        applyInProgressTag(event, threadChannel);
    }
    
    /**
     * Apply the In Progress tag to the thread (without closing it).
     * 
     * @param event The command event
     * @param threadChannel The thread to mark as in progress
     */
    private void applyInProgressTag(SlashCommandInteractionEvent event, ThreadChannel threadChannel) {
        // Verify the parent channel is a ForumChannel
        if (!(threadChannel.getParentChannel() instanceof ForumChannel)) {
            logger.error("Parent channel is not a ForumChannel: {}", threadChannel.getParentChannel().getName());
            event.getHook().editOriginal("❌ This thread's parent channel is not a forum.").queue();
            return;
        }
        
        ForumChannel parentChannel = (ForumChannel) threadChannel.getParentChannel();
        
        // Get the In Progress tag from the forum
        ForumTag inProgressTag = parentChannel.getAvailableTags().stream()
                .filter(tag -> tag.getId().equals(Config.TAG_IN_PROGRESS))
                .findFirst()
                .orElse(null);
        
        if (inProgressTag == null) {
            logger.error("In Progress tag not found in forum: {}", parentChannel.getName());
            event.getHook().editOriginal("❌ The In Progress tag is not configured in this forum.").queue();
            return;
        }
        
        // Get current tags and add the In Progress tag
        List<ForumTag> newTags = new ArrayList<>(threadChannel.getAppliedTags());
        
        // Remove any existing status tags and add the In Progress tag
        newTags.removeIf(tag -> 
                tag.getId().equals(Config.TAG_FIXED) ||
                tag.getId().equals(Config.TAG_IN_PROGRESS) ||
                tag.getId().equals(Config.TAG_PENDING) ||
                tag.getId().equals(Config.TAG_RESOLVED)
        );
        newTags.add(inProgressTag);
        
        // Apply the tags
        threadChannel.getManager().setAppliedTags(newTags).queue(
                success -> {
                    logger.info("Applied In Progress tag to thread: {}", threadChannel.getName());
                    // Send ephemeral success message to the user
                    event.getHook().editOriginal("✅ Successfully marked this bug as in progress!").queue(
                            message -> {
                                // Send the public embed into the channel
                                threadChannel.sendMessageEmbeds(EmbedManager.createBugInProgressEmbed()).queue(
                                        publicMessage -> logger.info("Sent in progress embed to thread: {}", threadChannel.getName()),
                                        error -> logger.error("Failed to send bug in progress embed", error)
                                );
                            },
                            error -> logger.error("Failed to send ephemeral response", error)
                    );
                },
                error -> {
                    logger.error("Failed to apply In Progress tag to thread", error);
                    event.getHook().editOriginal("❌ Failed to apply the In Progress tag. Please check bot permissions.").queue();
                }
        );
    }
}
