package dev.wand.stacker.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Base interface for all bot commands.
 * <p>
 * All commands must implement this interface to be registered with the CommandManager.
 * The interface provides a contract for command execution and registration.
 * <p>
 * To add a new command:
 * 1. Create a new class that implements CommandInterface
 * 2. Implement getName() to return the command name
 * 3. Implement getCommandData() to define slash command structure
 * 4. Implement execute() to handle command logic
 * 5. Register the command in Bot.java's CommandManager
 */
public interface CommandInterface {

    /**
     * Get the name of the command.
     * This should match the command name in the CommandData.
     *
     * @return The command name (e.g., "tester", "bug")
     */
    String getName();

    /**
     * Get the CommandData for registering this command with Discord.
     * This defines the slash command structure, including options and subcommands.
     *
     * @return CommandData for Discord registration
     */
    CommandData getCommandData();

    /**
     * Whether this command requires the staff role to use.
     * Override and return false to make a command available to all users.
     *
     * @return true if the staff role is required (default), false to allow everyone
     */
    default boolean requiresPermission() {
        return true;
    }

    /**
     * Execute the command logic.
     * This method is called when a user invokes the command.
     * Permission checks are handled by the CommandManager before this is called.
     *
     * @param event The slash command interaction event
     */
    void execute(SlashCommandInteractionEvent event);
}
