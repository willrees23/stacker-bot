package dev.wand.stacker.embeds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;

/**
 * Centralized manager for all bot embeds.
 * This class provides methods to create consistent, well-formatted embeds
 * for various bot responses and messages.
 * 
 * All embeds are defined here to ensure consistency and easy customization.
 * To add a new embed type, simply add a new static method that returns a MessageEmbed.
 */
public class EmbedManager {
    
    // Color scheme for different embed types
    private static final Color COLOR_SUCCESS = new Color(87, 242, 135); // Green
    private static final Color COLOR_ERROR = new Color(237, 66, 69); // Red
    private static final Color COLOR_INFO = new Color(88, 101, 242); // Discord Blurple
    private static final Color COLOR_WARNING = new Color(254, 231, 92); // Yellow
    
    /**
     * Create a success embed with a custom message.
     * 
     * @param title The embed title
     * @param description The embed description
     * @return A success-styled MessageEmbed
     */
    public static MessageEmbed createSuccess(String title, String description) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(COLOR_SUCCESS)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create an error embed with a custom message.
     * 
     * @param title The embed title
     * @param description The embed description
     * @return An error-styled MessageEmbed
     */
    public static MessageEmbed createError(String title, String description) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(COLOR_ERROR)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create an info embed with a custom message.
     * 
     * @param title The embed title
     * @param description The embed description
     * @return An info-styled MessageEmbed
     */
    public static MessageEmbed createInfo(String title, String description) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(COLOR_INFO)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create the embed for when a bug is marked as fixed.
     * This is sent when the /bug fix command is used.
     * 
     * @return The bug fixed embed
     */
    public static MessageEmbed createBugFixedEmbed() {
        return new EmbedBuilder()
                .setTitle("🔧 Bug Fixed")
                .setDescription("This bug has been marked as fixed and the thread is now closed.\n\n" +
                        "Thank you for your report!")
                .setColor(COLOR_SUCCESS)
                .setTimestamp(Instant.now())
                .setFooter("Stacker Bot", null)
                .build();
    }
    
    /**
     * Create the embed for when a bug is marked as in progress.
     * This is sent when the /bug in-progress command is used.
     * 
     * @return The bug in progress embed
     */
    public static MessageEmbed createBugInProgressEmbed() {
        return new EmbedBuilder()
                .setTitle("🔄 Bug In Progress")
                .setDescription("This bug is now being worked on. The thread will remain open for updates.\n\n" +
                        "Thank you for your patience!")
                .setColor(COLOR_WARNING)
                .setTimestamp(Instant.now())
                .setFooter("Stacker Bot", null)
                .build();
    }
    
    /**
     * Create the embed for when a bug is marked as resolved.
     * This is sent when the /bug resolved command is used.
     * 
     * @return The bug resolved embed
     */
    public static MessageEmbed createBugResolvedEmbed() {
        return new EmbedBuilder()
                .setTitle("✅ Bug Resolved")
                .setDescription("This bug has been resolved and the thread is now closed.\n\n" +
                        "Thank you for your report!")
                .setColor(COLOR_SUCCESS)
                .setTimestamp(Instant.now())
                .setFooter("Stacker Bot", null)
                .build();
    }
    
    /**
     * Create the embed for permission denied errors.
     * 
     * @return The permission denied embed
     */
    public static MessageEmbed createPermissionDeniedEmbed() {
        return new EmbedBuilder()
                .setTitle("❌ Permission Denied")
                .setDescription("You don't have the required role to use this command.")
                .setColor(COLOR_ERROR)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create the embed for when a command is used in the wrong context.
     * 
     * @param requirement The requirement that was not met
     * @return The invalid context embed
     */
    public static MessageEmbed createInvalidContextEmbed(String requirement) {
        return new EmbedBuilder()
                .setTitle("❌ Invalid Context")
                .setDescription("This command can only be used " + requirement + ".")
                .setColor(COLOR_ERROR)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create the embed for successful tester role assignment.
     * 
     * @param username The username of the user who received the roles
     * @return The roles assigned embed
     */
    public static MessageEmbed createTesterRolesAssignedEmbed(String username) {
        return new EmbedBuilder()
                .setTitle("✅ Tester Roles Assigned")
                .setDescription("Successfully assigned tester roles to **" + username + "**.")
                .setColor(COLOR_SUCCESS)
                .setTimestamp(Instant.now())
                .build();
    }
    
    private EmbedManager() {
        // Utility class, prevent instantiation
    }
}
