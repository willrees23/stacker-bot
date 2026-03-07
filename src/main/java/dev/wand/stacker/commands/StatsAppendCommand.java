package dev.wand.stacker.commands;

import dev.wand.stacker.embeds.EmbedManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * /stats-append — Adds an existing bot message to the live-updating embed list.
 *
 * <p>Accepts a Discord message link (e.g. {@code https://discord.com/channels/GID/CID/MID}).
 * If the target message was not sent by this bot, the command fails ephemerally.
 * Requires the staff role to use.</p>
 */
public class StatsAppendCommand implements CommandInterface {

    private static final Logger logger = LoggerFactory.getLogger(StatsAppendCommand.class);

    /**
     * Matches both discord.com and canary/ptb variants. Captures guildId, channelId, messageId.
     */
    private static final Pattern MESSAGE_LINK_PATTERN = Pattern.compile(
            "https://(?:(?:canary|ptb)\\.)?discord(?:app)?\\.com/channels/(\\d+)/(\\d+)/(\\d+)"
    );

    @Override
    public String getName() {
        return "stats-append";
    }

    @Override
    public boolean requiresPermission() {
        return true;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("stats-append",
                        "Add an existing bot message to the live-updating stats embed list")
                .addOption(OptionType.STRING, "messagelink",
                        "The Discord message link to the target embed", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping linkOption = event.getOption("messagelink");
        if (linkOption == null) {
            event.replyEmbeds(EmbedManager.createError("Missing Argument",
                            "Please provide a message link."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String link = linkOption.getAsString().trim();
        Matcher matcher = MESSAGE_LINK_PATTERN.matcher(link);
        if (!matcher.find()) {
            event.replyEmbeds(EmbedManager.createError("Invalid Link",
                            "The provided link is not a valid Discord message link.\n" +
                                    "Expected format: `https://discord.com/channels/GUILD/CHANNEL/MESSAGE`"))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String channelId = matcher.group(2);
        String messageId = matcher.group(3);

        // Check if already tracked
        if (StatsCommand.isTracked(channelId, messageId)) {
            event.replyEmbeds(EmbedManager.createError("Already Tracked",
                            "That message is already in the live-updating embed list."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        MessageChannel channel = (MessageChannel) event.getJDA()
                .getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            event.replyEmbeds(EmbedManager.createError("Channel Not Found",
                            "The bot cannot access the channel from that link."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Defer ephemerally while we fetch the message
        event.deferReply(true).queue();

        channel.retrieveMessageById(messageId).queue(
                message -> handleMessage(event, message),
                error -> {
                    logger.warn("stats-append: failed to retrieve message {}/{}: {}",
                            channelId, messageId, error.getMessage());
                    event.getHook().editOriginalEmbeds(EmbedManager.createError("Message Not Found",
                                    "Could not retrieve that message. Make sure the link is correct " +
                                            "and the bot has access to the channel."))
                            .queue();
                }
        );
    }

    private void handleMessage(SlashCommandInteractionEvent event, Message message) {
        // Only allow messages authored by this bot
        if (!message.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            event.getHook().editOriginalEmbeds(EmbedManager.createError("Not a Bot Message",
                            "That message was not sent by this bot. Only messages sent by the bot " +
                                    "can be added to the live-updating embed list."))
                    .queue();
            return;
        }

        String channelId = message.getChannel().getId();
        String messageId = message.getId();

        StatsCommand.addTracked(channelId, messageId, event.getJDA());

        logger.info("stats-append: added {}/{} to live stats tracking by {}",
                channelId, messageId,
                event.getUser().getName());

        event.getHook().editOriginalEmbeds(EmbedManager.createSuccess("Embed Added",
                        "The message has been added to the live-updating stats embed list.\n" +
                                "It will be refreshed on the next poll cycle."))
                .queue();
    }
}
