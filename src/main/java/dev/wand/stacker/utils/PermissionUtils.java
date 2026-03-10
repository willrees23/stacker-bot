package dev.wand.stacker.utils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

/**
 * Utility class for permission checking.
 * Provides methods to validate user permissions.
 */
public class PermissionUtils {

    private PermissionUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Check if a member has Administrator permission to use bot commands.
     *
     * @param member The guild member to check
     * @return true if the member has Administrator permission, false otherwise
     */
    public static boolean hasRequiredRole(Member member) {
        if (member == null) {
            return false;
        }

        return member.hasPermission(Permission.ADMINISTRATOR);
    }
}
