package dev.wand.stacker.commands.bug;

import dev.wand.stacker.commands.CommandInterface;
import dev.wand.stacker.commands.bug.subcommands.FixSubcommand;
import dev.wand.stacker.commands.bug.subcommands.InProgressSubcommand;
import dev.wand.stacker.commands.bug.subcommands.ResolvedSubcommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * Parent command for bug-related operations.
 * 
 * This command serves as a parent for various bug management subcommands.
 * Currently supports:
 * - /bug fix - Mark a bug as fixed and close the thread
 * - /bug in-progress - Mark a bug as in progress (keeps thread open)
 * - /bug resolved - Mark a bug as resolved and close the thread
 * 
 * To add a new subcommand:
 * 1. Create a new subcommand class in the subcommands package
 * 2. Add the subcommand data to getCommandData()
 * 3. Add a case in the execute() method to handle the subcommand
 * 
 * This modular approach makes it easy to expand with new bug-related commands.
 */
public class BugCommand implements CommandInterface {
    
    private final FixSubcommand fixSubcommand = new FixSubcommand();
    private final InProgressSubcommand inProgressSubcommand = new InProgressSubcommand();
    private final ResolvedSubcommand resolvedSubcommand = new ResolvedSubcommand();
    
    @Override
    public String getName() {
        return "bug";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("bug", "Bug management commands")
                .addSubcommands(
                        new SubcommandData("fix", "Mark a bug as fixed and close the thread"),
                        new SubcommandData("in-progress", "Mark a bug as in progress (keeps thread open)"),
                        new SubcommandData("resolved", "Mark a bug as resolved and close the thread")
                );
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommandName = event.getSubcommandName();
        
        if (subcommandName == null) {
            return;
        }
        
        // Route to the appropriate subcommand
        switch (subcommandName) {
            case "fix":
                fixSubcommand.execute(event);
                break;
            case "in-progress":
                inProgressSubcommand.execute(event);
                break;
            case "resolved":
                resolvedSubcommand.execute(event);
                break;
            default:
                // This shouldn't happen if commands are registered correctly
                break;
        }
    }
}
