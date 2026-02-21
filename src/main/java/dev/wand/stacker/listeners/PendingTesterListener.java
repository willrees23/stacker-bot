package dev.wand.stacker.listeners;

import dev.wand.stacker.config.Config;
import dev.wand.stacker.utils.PendingTesterStore;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PendingTesterListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PendingTesterListener.class);

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        String userId = member.getId();

        boolean pending;
        try {
            pending = PendingTesterStore.isPendingTester(userId);
        } catch (IOException e) {
            logger.error("Failed to read pending tester store for user {}", userId, e);
            return;
        }

        if (!pending) {
            return;
        }

        Guild guild = event.getGuild();
        Role role1 = guild.getRoleById(Config.ROLE_TESTER_1);
        Role role2 = guild.getRoleById(Config.ROLE_TESTER_2);

        if (role1 == null || role2 == null) {
            logger.error("Tester roles not found in guild: {}", guild.getName());
            return;
        }

        guild.addRoleToMember(member, role1).queue(
                success1 -> guild.addRoleToMember(member, role2).queue(
                        success2 -> {
                            try {
                                PendingTesterStore.removePendingTester(userId);
                            } catch (IOException e) {
                                logger.error("Failed to remove {} from pending tester store", userId, e);
                            }
                            logger.info("Assigned tester roles to pending user {} on join", member.getUser().getName());
                        },
                        error2 -> logger.error("Failed to assign second tester role to pending user {}", userId, error2)
                ),
                error1 -> logger.error("Failed to assign first tester role to pending user {}", userId, error1)
        );
    }
}
