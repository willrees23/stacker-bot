package dev.wand.stacker.utils;

import dev.wand.stacker.config.Config;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

/**
 * Utility class for permission checking.
 * Provides methods to validate user permissions based on roles.
 */
public class PermissionUtils {
    
    /**
     * Check if a member has the required role to use bot commands.
     * 
     * @param member The guild member to check
     * @return true if the member has the required role, false otherwise
     */
    public static boolean hasRequiredRole(Member member) {
        if (member == null) {
            return false;
        }
        
        for (Role role : member.getRoles()) {
            if (role.getId().equals(Config.ROLE_REQUIRED)) {
                return true;
            }
        }
        
        return false;
    }
    
    private PermissionUtils() {
        // Utility class, prevent instantiation
    }
}
