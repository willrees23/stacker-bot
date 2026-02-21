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

public class InProgressCommand implements CommandInterface {

    private static final Logger logger = LoggerFactory.getLogger(InProgressCommand.class);

    @Override
    public String getName() {
        return "in-progress";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("in-progress", "Mark a bug as in progress (keeps thread open)");
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
        applyInProgressTag(event, threadChannel);
    }

    private void applyInProgressTag(SlashCommandInteractionEvent event, ThreadChannel threadChannel) {
        if (!(threadChannel.getParentChannel() instanceof ForumChannel)) {
            logger.error("Parent channel is not a ForumChannel: {}", threadChannel.getParentChannel().getName());
            event.getHook().editOriginal("❌ This thread's parent channel is not a forum.").queue();
            return;
        }

        ForumChannel parentChannel = (ForumChannel) threadChannel.getParentChannel();

        ForumTag inProgressTag = parentChannel.getAvailableTags().stream()
                .filter(tag -> tag.getId().equals(Config.TAG_IN_PROGRESS))
                .findFirst()
                .orElse(null);

        if (inProgressTag == null) {
            logger.error("In Progress tag not found in forum: {}", parentChannel.getName());
            event.getHook().editOriginal("❌ The In Progress tag is not configured in this forum.").queue();
            return;
        }

        List<ForumTag> newTags = new ArrayList<>(threadChannel.getAppliedTags());
        newTags.removeIf(ValidationUtils::isStatusTag);
        newTags.add(inProgressTag);

        threadChannel.getManager().setAppliedTags(newTags).queue(
                success -> {
                    logger.info("Applied In Progress tag to thread: {}", threadChannel.getName());
                    event.getHook().editOriginal("✅ Successfully marked this bug as in progress!").queue(
                            message -> threadChannel.sendMessageEmbeds(EmbedManager.createBugInProgressEmbed()).queue(
                                    publicMessage -> logger.info("Sent in progress embed to thread: {}", threadChannel.getName()),
                                    error -> logger.error("Failed to send bug in progress embed", error)
                            ),
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
