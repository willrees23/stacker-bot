package dev.wand.stacker.commands;

import dev.wand.stacker.config.Config;
import dev.wand.stacker.embeds.EmbedManager;
import dev.wand.stacker.utils.ValidationUtils;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DuplicateCommand implements CommandInterface {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateCommand.class);

    @Override
    public String getName() {
        return "duplicate";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("duplicate", "Mark this thread as a duplicate")
                .addOptions(new OptionData(OptionType.CHANNEL, "thread", "The original thread this is a duplicate of", true));
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

        Channel originalChannel = event.getOption("thread").getAsChannel();
        applyDuplicateTag(event, threadChannel, originalChannel);
    }

    private void applyDuplicateTag(SlashCommandInteractionEvent event, ThreadChannel threadChannel, Channel originalChannel) {
        GuildChannel originalGuildChannel = (GuildChannel) originalChannel;
        if (!(threadChannel.getParentChannel() instanceof ForumChannel)) {
            logger.error("Parent channel is not a ForumChannel: {}", threadChannel.getParentChannel().getName());
            event.getHook().editOriginal("❌ This thread's parent channel is not a forum.").queue();
            return;
        }

        ForumChannel parentChannel = (ForumChannel) threadChannel.getParentChannel();

        ForumTag duplicateTag = parentChannel.getAvailableTags().stream()
                .filter(tag -> tag.getId().equals(Config.TAG_DUPLICATE))
                .findFirst()
                .orElse(null);

        if (duplicateTag == null) {
            logger.error("Duplicate tag not found in forum: {}", parentChannel.getName());
            event.getHook().editOriginal("❌ The Duplicate tag is not configured in this forum.").queue();
            return;
        }

        List<ForumTag> newTags = new ArrayList<>(threadChannel.getAppliedTags());
        newTags.removeIf(ValidationUtils::isStatusTag);
        newTags.add(duplicateTag);

        threadChannel.getManager().setAppliedTags(newTags).queue(
                success -> {
                    logger.info("Applied Duplicate tag to thread: {}", threadChannel.getName());
                    event.getHook().editOriginal("✅ Successfully marked this thread as a duplicate!").queue(
                            message -> {
                                String ownerId = threadChannel.getOwnerId();
                                String mention = "<@" + ownerId + ">";
                                threadChannel.sendMessage(mention)
                                        .addEmbeds(EmbedManager.createBugDuplicateEmbed(originalGuildChannel.getName(), originalGuildChannel.getJumpUrl()))
                                        .queue(
                                                publicMessage -> closeThread(threadChannel),
                                                error -> logger.error("Failed to send duplicate embed", error)
                                        );
                            },
                            error -> logger.error("Failed to send ephemeral response", error)
                    );
                },
                error -> {
                    logger.error("Failed to apply Duplicate tag to thread", error);
                    event.getHook().editOriginal("❌ Failed to apply the Duplicate tag. Please check bot permissions.").queue();
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
