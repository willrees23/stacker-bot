package dev.wand.stacker;

import dev.wand.stacker.commands.CommandInterface;
import dev.wand.stacker.commands.CommandManager;
import dev.wand.stacker.commands.bug.BugCommand;
import dev.wand.stacker.commands.tester.TesterCommand;
import dev.wand.stacker.config.Config;
import dev.wand.stacker.listeners.ForumThreadListener;
import dev.wand.stacker.listeners.PendingTesterListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main bot class - Entry point for the Stacker Discord Bot.
 * 
 * This bot uses JDA (Java Discord API) to interact with Discord.
 * It implements a modular command system with proper permission checking
 * and a centralized embed management system.
 * 
 * Architecture:
 * - Commands are registered with CommandManager
 * - All commands require the configured role (automatic check)
 * - Embeds are managed centrally through EmbedManager
 * - Configuration is stored in Config class
 * - Event listeners handle automatic actions (e.g., auto-tagging new threads)
 * 
 * To add a new command:
 * 1. Create a class that implements CommandInterface in the commands package
 * 2. Implement the required methods (getName, getCommandData, execute)
 * 3. Register it in the setupCommands() method
 * 4. The command will automatically have permission checks applied
 * 
 * Environment Variables Required:
 * - DISCORD_BOT_TOKEN: Your Discord bot token
 */
public class Bot {
    
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private static JDA jda;
    
    public static void main(String[] args) {
        try {
            logger.info("Starting Stacker Bot...");
            
            // Get bot token from environment variable
            String token = Config.getBotToken();
            
            // Create CommandManager
            CommandManager commandManager = new CommandManager();
            
            // Create event listeners
            ForumThreadListener forumThreadListener = new ForumThreadListener();
            PendingTesterListener pendingTesterListener = new PendingTesterListener();

            // Build JDA instance
            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .setActivity(Activity.watching("for bugs"))
                    .addEventListeners(commandManager, forumThreadListener, pendingTesterListener)
                    .build();
            
            // Wait for JDA to be ready
            jda.awaitReady();
            logger.info("Bot is ready!");
            
            // Register commands with the CommandManager
            setupCommands(commandManager);
            
            // Register commands with Discord
            registerCommandsWithDiscord(commandManager);
            
            logger.info("All commands registered successfully!");
            
        } catch (Exception e) {
            logger.error("Failed to start bot", e);
            System.exit(1);
        }
    }
    
    /**
     * Register all commands with the CommandManager.
     * Add new commands here to make them available.
     * 
     * @param commandManager The command manager instance
     */
    private static void setupCommands(CommandManager commandManager) {
        // Register the tester command
        commandManager.registerCommand(new TesterCommand());
        
        // Register the bug command (with subcommands)
        commandManager.registerCommand(new BugCommand());
        
        // Add more commands here as needed
        // commandManager.registerCommand(new YourNewCommand());
    }
    
    /**
     * Register all commands with Discord.
     * This updates the slash commands available in Discord.
     * Commands are registered to the specific guild for immediate availability.
     * 
     * @param commandManager The command manager with registered commands
     */
    private static void registerCommandsWithDiscord(CommandManager commandManager) {
        // Register commands to the specific guild for immediate availability
        // Guild commands are available immediately (no 1-hour wait like global commands)
        var guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) {
            logger.error("Guild with ID {} not found. Bot may not be a member of this guild.", Config.GUILD_ID);
            return;
        }
        
        guild.updateCommands()
                .addCommands(
                        commandManager.getCommands().values().stream()
                                .map(CommandInterface::getCommandData)
                                .toList()
                )
                .queue(
                        success -> logger.info("Commands registered with guild {}", Config.GUILD_ID),
                        error -> logger.error("Failed to register commands with guild", error)
                );
    }
    
    /**
     * Get the JDA instance.
     * Useful for accessing the JDA API from other classes.
     * 
     * @return The JDA instance
     */
    public static JDA getJDA() {
        return jda;
    }
}
