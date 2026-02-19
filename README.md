# Stacker Discord Bot

A modular Discord bot built with JDA (Java Discord API) for managing tester roles and bug reports.

## Features

- **Modular Command System**: Easy-to-extend architecture with support for commands and subcommands
- **Permission Management**: Automatic role-based permission checking for all commands
- **Centralized Embeds**: All bot responses use consistent, customizable embeds
- **Bug Management**: Mark bugs as fixed and automatically close threads
- **Tester Role Assignment**: Easily assign tester roles to users

## Architecture

The bot follows object-oriented design principles with clear separation of concerns:

```
src/main/java/dev/wand/stacker/
├── Bot.java                    # Main entry point
├── config/
│   └── Config.java            # Centralized configuration (IDs, tokens)
├── commands/
│   ├── CommandInterface.java  # Base interface for all commands
│   ├── CommandManager.java    # Command registration and routing
│   ├── tester/
│   │   └── TesterCommand.java # /tester command
│   └── bug/
│       ├── BugCommand.java    # /bug parent command
│       └── subcommands/
│           └── FixSubcommand.java  # /bug fix subcommand
├── embeds/
│   └── EmbedManager.java      # Centralized embed creation
└── utils/
    ├── PermissionUtils.java   # Permission checking utilities
    └── ValidationUtils.java   # Context validation utilities
```

## Commands

### `/tester <user>`
Assigns both tester roles to the specified user.

**Requirements:**
- User must have the required role
- Target user must be in the server

**Usage:**
```
/tester @username
```

### `/bug fix`
Marks a bug report as fixed and closes the thread.

**Requirements:**
- User must have the required role
- Must be used in a thread within the Tester Log Forum
- Applies the "Fixed" tag and archives the thread

**Usage:**
```
/bug fix
```

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- A Discord bot token from the [Discord Developer Portal](https://discord.com/developers/applications)

### Configuration

1. **Create a Discord Bot:**
   - Go to the [Discord Developer Portal](https://discord.com/developers/applications)
   - Create a new application
   - Go to the "Bot" section and create a bot
   - Copy the bot token
   - Enable the following Privileged Gateway Intents:
     - Server Members Intent
     - Message Content Intent

2. **Set Environment Variable:**
   ```bash
   export DISCORD_BOT_TOKEN="your-bot-token-here"
   ```

   Or on Windows:
   ```cmd
   set DISCORD_BOT_TOKEN=your-bot-token-here
   ```

3. **Update Configuration (if needed):**
   - Edit `src/main/java/dev/wand/stacker/config/Config.java`
   - Update the IDs for roles, channels, and tags to match your Discord server

4. **Invite the Bot to Your Server:**
   - In the Discord Developer Portal, go to OAuth2 > URL Generator
   - Select scopes: `bot`, `applications.commands`
   - Select permissions: `Manage Roles`, `Manage Threads`, `Send Messages`, `Embed Links`
   - Use the generated URL to invite the bot

### Building

```bash
mvn clean package
```

This creates an executable JAR file in the `target/` directory.

### Running

```bash
java -jar target/stacker-bot-1.0-SNAPSHOT.jar
```

Or with Maven:

```bash
mvn clean compile exec:java -Dexec.mainClass="dev.wand.stacker.Bot"
```

## Adding New Commands

The bot is designed to make adding new commands easy:

### 1. Create a Command Class

Create a new class that implements `CommandInterface`:

```java
package dev.wand.stacker.commands.yourcommand;

import dev.wand.stacker.commands.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class YourCommand implements CommandInterface {
    
    @Override
    public String getName() {
        return "yourcommand";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("yourcommand", "Description of your command");
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Your command logic here
        event.reply("Hello!").queue();
    }
}
```

### 2. Register the Command

In `Bot.java`, add your command to the `setupCommands()` method:

```java
private static void setupCommands(CommandManager commandManager) {
    commandManager.registerCommand(new TesterCommand());
    commandManager.registerCommand(new BugCommand());
    commandManager.registerCommand(new YourCommand()); // Add this line
}
```

That's it! The command will automatically:
- Have permission checking applied (required role)
- Be registered with Discord
- Be available to users

### Adding Subcommands

To add a subcommand to an existing command (like `/bug fix`):

1. Create a subcommand class in the appropriate `subcommands/` package
2. Add the subcommand data to the parent command's `getCommandData()`
3. Add routing logic in the parent command's `execute()` method

See `BugCommand.java` and `FixSubcommand.java` for an example.

## Dependencies

- **JDA (Java Discord API)** 5.1.2 - Discord bot framework
- **SLF4J** 2.0.9 - Logging facade
- **Logback** 1.4.11 - Logging implementation

All dependencies are managed through Maven and specified in `pom.xml`.

## Permission System

All commands automatically require users to have the configured role (ID: `1473026921158676480`). This is enforced by the `CommandManager` before any command execution.

To customize permissions:
- Update `Config.ROLE_REQUIRED` with your role ID
- Modify `PermissionUtils.hasRequiredRole()` for custom permission logic

## Logging

The bot uses SLF4J with Logback for logging. Logs include:
- Command execution
- Permission checks
- Errors and exceptions
- Bot lifecycle events

## Contributing

When contributing new features:
1. Follow the existing package structure
2. Implement the appropriate interfaces
3. Add comprehensive JavaDoc comments
4. Use the centralized `EmbedManager` for all user-facing messages
5. Handle errors gracefully with user-friendly messages

## License

This project is provided as-is for the Stacker community.

## Support

For issues or questions, please contact the development team.
