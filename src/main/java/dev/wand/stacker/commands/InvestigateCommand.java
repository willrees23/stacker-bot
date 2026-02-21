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

public class InvestigateCommand implements CommandInterface {

    private static final Logger logger = LoggerFactory.getLogger(InvestigateCommand.class);

    @Override
    public String getName() {
        return "investigate";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("investigate", "Mark a bug as being investigated (keeps thread open)");
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
        applyInvestigatingTag(event, threadChannel);
    }

    private void applyInvestigatingTag(SlashCommandInteractionEvent event, ThreadChannel threadChannel) {
        if (!(threadChannel.getParentChannel() instanceof ForumChannel)) {
            logger.error("Parent channel is not a ForumChannel: {}", threadChannel.getParentChannel().getName());
            event.getHook().editOriginal("❌ This thread's parent channel is not a forum.").queue();
            return;
        }

        ForumChannel parentChannel = (ForumChannel) threadChannel.getParentChannel();

        ForumTag investigatingTag = parentChannel.getAvailableTags().stream()
                .filter(tag -> tag.getId().equals(Config.TAG_INVESTIGATING))
                .findFirst()
                .orElse(null);

        if (investigatingTag == null) {
            logger.error("Investigating tag not found in forum: {}", parentChannel.getName());
            event.getHook().editOriginal("❌ The Investigating tag is not configured in this forum.").queue();
            return;
        }

        List<ForumTag> newTags = new ArrayList<>(threadChannel.getAppliedTags());
        newTags.removeIf(ValidationUtils::isStatusTag);
        newTags.add(investigatingTag);

        threadChannel.getManager().setAppliedTags(newTags).queue(
                success -> {
                    logger.info("Applied Investigating tag to thread: {}", threadChannel.getName());
                    event.getHook().editOriginal("✅ Successfully marked this bug as being investigated!").queue(
                            message -> threadChannel.sendMessageEmbeds(EmbedManager.createBugInvestigatingEmbed()).queue(
                                    publicMessage -> logger.info("Sent investigating embed to thread: {}", threadChannel.getName()),
                                    error -> logger.error("Failed to send bug investigating embed", error)
                            ),
                            error -> logger.error("Failed to send ephemeral response", error)
                    );
                },
                error -> {
                    logger.error("Failed to apply Investigating tag to thread", error);
                    event.getHook().editOriginal("❌ Failed to apply the Investigating tag. Please check bot permissions.").queue();
                }
        );
    }
}
