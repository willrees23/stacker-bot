package dev.wand.stacker.commands.tester;

import dev.wand.stacker.commands.CommandInterface;
import dev.wand.stacker.config.Config;
import dev.wand.stacker.embeds.EmbedManager;
import dev.wand.stacker.utils.PendingTesterStore;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to assign tester roles to a user.
 * 
 * Usage: /tester <user>
 * 
 * This command assigns both tester roles (ROLE_TESTER_1 and ROLE_TESTER_2) 
 * to the specified user.
 * 
 * Requirements:
 * - User must have the required role (checked by CommandManager)
 * - Target user must be a member of the guild
 * - Bot must have permission to manage roles
 */
public class TesterCommand implements CommandInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(TesterCommand.class);
    
    @Override
    public String getName() {
        return "tester";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("tester", "Assign tester roles to a user")
                .addOption(OptionType.USER, "user", "The user to assign tester roles to", true);
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer the reply since role assignment might take a moment
        event.deferReply().queue();
        
        User targetUser = event.getOption("user").getAsUser();
        Guild guild = event.getGuild();
        
        if (guild == null) {
            event.getHook().editOriginalEmbeds(EmbedManager.createError(
                    "Error",
                    "This command can only be used in a server."
            )).queue();
            return;
        }
        
        // Get the member object for the target user
        guild.retrieveMember(targetUser).queue(
                targetMember -> assignTesterRoles(event, guild, targetMember),
                error -> {
                    logger.info("User {} not in server; adding to pending tester store", targetUser.getName());
                    try {
                        PendingTesterStore.addPendingTester(targetUser.getId());
                    } catch (java.io.IOException e) {
                        logger.error("Failed to add {} to pending tester store", targetUser.getId(), e);
                    }
                    event.getHook().editOriginalEmbeds(
                            EmbedManager.createTesterPendingEmbed(targetUser.getName())
                    ).queue();
                }
        );
    }
    
    /**
     * Assign both tester roles to the target member.
     * 
     * @param event The command event
     * @param guild The guild where the command was executed
     * @param targetMember The member to assign roles to
     */
    private void assignTesterRoles(SlashCommandInteractionEvent event, Guild guild, Member targetMember) {
        Role role1 = guild.getRoleById(Config.ROLE_TESTER_1);
        Role role2 = guild.getRoleById(Config.ROLE_TESTER_2);
        
        if (role1 == null || role2 == null) {
            logger.error("Tester roles not found in guild: {}", guild.getName());
            event.getHook().editOriginalEmbeds(EmbedManager.createError(
                    "Error",
                    "Tester roles are not configured properly in this server."
            )).queue();
            return;
        }
        
        // Add both roles
        guild.addRoleToMember(targetMember, role1).queue(
                success1 -> guild.addRoleToMember(targetMember, role2).queue(
                        success2 -> {
                            logger.info("Successfully assigned tester roles to: {}", targetMember.getUser().getName());
                            event.getHook().editOriginalEmbeds(
                                    EmbedManager.createTesterRolesAssignedEmbed(targetMember.getUser().getName())
                            ).queue();
                        },
                        error2 -> {
                            logger.error("Failed to assign second tester role", error2);
                            event.getHook().editOriginalEmbeds(EmbedManager.createError(
                                    "Error",
                                    "Failed to assign all tester roles. Please check bot permissions."
                            )).queue();
                        }
                ),
                error1 -> {
                    logger.error("Failed to assign first tester role", error1);
                    event.getHook().editOriginalEmbeds(EmbedManager.createError(
                            "Error",
                            "Failed to assign tester roles. Please check bot permissions."
                    )).queue();
                }
        );
    }
}
