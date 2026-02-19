package dev.wand.stacker.commands;

import dev.wand.stacker.embeds.EmbedManager;
import dev.wand.stacker.utils.PermissionUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Central command manager that handles command registration and routing.
 * 
 * This class:
 * - Registers all commands with the bot
 * - Routes incoming slash command events to the appropriate command handler
 * - Performs permission checks before executing commands
 * - Provides centralized error handling
 * 
 * To add a new command:
 * 1. Create a class that implements CommandInterface
 * 2. Call registerCommand() with an instance of your command class
 * 3. The command will automatically be available and permission-checked
 */
public class CommandManager extends ListenerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    private final Map<String, CommandInterface> commands = new HashMap<>();
    
    /**
     * Register a command with the manager.
     * The command will be added to the internal registry and made available for execution.
     * 
     * @param command The command to register
     */
    public void registerCommand(CommandInterface command) {
        commands.put(command.getName().toLowerCase(), command);
        logger.info("Registered command: {}", command.getName());
    }
    
    /**
     * Get all registered commands.
     * Used for registering commands with Discord.
     * 
     * @return Map of command name to command instance
     */
    public Map<String, CommandInterface> getCommands() {
        return commands;
    }
    
    /**
     * Handle incoming slash command events.
     * This method:
     * 1. Finds the appropriate command handler
     * 2. Checks if the user has the required role
     * 3. Executes the command or sends an error message
     * 
     * @param event The slash command interaction event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName().toLowerCase();
        CommandInterface command = commands.get(commandName);
        
        if (command == null) {
            logger.warn("Unknown command: {}", commandName);
            return;
        }
        
        // Check permissions
        Member member = event.getMember();
        if (!PermissionUtils.hasRequiredRole(member)) {
            event.replyEmbeds(EmbedManager.createPermissionDeniedEmbed())
                    .setEphemeral(true)
                    .queue();
            logger.info("User {} attempted to use command {} without required role", 
                    member != null ? member.getUser().getName() : "Unknown", 
                    commandName);
            return;
        }
        
        // Execute the command
        try {
            logger.info("Executing command: {} by user: {}", 
                    commandName, 
                    member != null ? member.getUser().getName() : "Unknown");
            command.execute(event);
        } catch (Exception e) {
            logger.error("Error executing command: {}", commandName, e);
            event.replyEmbeds(EmbedManager.createError(
                    "Error",
                    "An error occurred while executing the command."
            )).setEphemeral(true).queue();
        }
    }
}
