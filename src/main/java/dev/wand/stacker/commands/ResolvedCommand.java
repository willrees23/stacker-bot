package dev.wand.stacker.commands;

import dev.wand.stacker.config.Config;
import dev.wand.stacker.embeds.EmbedManager;
import dev.wand.stacker.utils.ValidationUtils;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ResolvedCommand implements CommandInterface {

    private static final Logger logger = LoggerFactory.getLogger(ResolvedCommand.class);

    @Override
    public String getName() {
        return "resolved";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("resolved", "Mark a bug as resolved and close the thread");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Channel channel = event.getChannel();

        if (!ValidationUtils.isThreadInTesterLogForum(channel)) {
            event.replyEmbeds(EmbedManager.createInvalidContextEmbed(
                    "in a thread within the Tester Log Forum"
            )).setEphemeral(true).queue();
            return;
        }

        ThreadChannel threadChannel = (ThreadChannel) channel;
        event.deferReply(true).queue();
        applyResolvedTag(event, threadChannel);
    }

    private void applyResolvedTag(SlashCommandInteractionEvent event, ThreadChannel threadChannel) {
        if (!(threadChannel.getParentChannel() instanceof ForumChannel)) {
            logger.error("Parent channel is not a ForumChannel: {}", threadChannel.getParentChannel().getName());
            event.getHook().editOriginal("❌ This thread's parent channel is not a forum.").queue();
            return;
        }

        ForumChannel parentChannel = (ForumChannel) threadChannel.getParentChannel();

        ForumTag resolvedTag = parentChannel.getAvailableTags().stream()
                .filter(tag -> tag.getId().equals(Config.TAG_RESOLVED))
                .findFirst()
                .orElse(null);

        if (resolvedTag == null) {
            logger.error("Resolved tag not found in forum: {}", parentChannel.getName());
            event.getHook().editOriginal("❌ The Resolved tag is not configured in this forum.").queue();
            return;
        }

        List<ForumTag> newTags = new ArrayList<>(threadChannel.getAppliedTags());
        newTags.removeIf(ValidationUtils::isStatusTag);
        newTags.add(resolvedTag);

        threadChannel.getManager().setAppliedTags(newTags).queue(
                success -> {
                    logger.info("Applied Resolved tag to thread: {}", threadChannel.getName());
                    event.getHook().editOriginal("✅ Successfully marked this bug as resolved!").queue(
                            message -> threadChannel.sendMessageEmbeds(EmbedManager.createBugResolvedEmbed()).queue(
                                    publicMessage -> closeThread(threadChannel),
                                    error -> logger.error("Failed to send bug resolved embed", error)
                            ),
                            error -> logger.error("Failed to send ephemeral response", error)
                    );
                },
                error -> {
                    logger.error("Failed to apply Resolved tag to thread", error);
                    event.getHook().editOriginal("❌ Failed to apply the Resolved tag. Please check bot permissions.").queue();
                }
        );
    }

    private void closeThread(ThreadChannel threadChannel) {
        threadChannel.getManager().setArchived(true).queue(
                success -> logger.info("Closed thread: {}", threadChannel.getName()),
                error -> logger.error("Failed to close thread: {}", threadChannel.getName(), error)
        );
    }
}
