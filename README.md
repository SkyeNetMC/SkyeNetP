# SkyeNetP Plugin

## Features


## Commands

### `/fly`
- Toggles flight mode for the player.
- Permission: `skyenetp.fly`

### `/datapacks`
- Lists all active datapacks on the server.
- Permission: `skyenetp.datapacks`

### `/datapacklist`
- Enables `/datapack list` but stopping /datapack enable/disable from being used. - used on our SMP server
- Permission: `skyenetp.datapacklist`



### `/chatfilter reload`
- Reloads the chat filter configuration files (`wordlist.yml` and `regex.yml`).
- Permission: `skyenetp.chatfilter.reload`


<details>
<summary>### Chat Filter Info & Configuration</summary>

### Chat Filter Module
The Chat Filter Module provides advanced chat filtering capabilities for your Minecraft server. It includes the following features:

1. **Dynamic Wordlist Filtering**:
   - Filters chat messages based on a configurable list of blocked words.
   - The blocked words are dynamically loaded from `wordlist.yml`.
   - Debug logs are available to verify the `blockedWords` list and message content.

2. **Regex-Based Filtering**:
   - Filters chat messages using configurable regex patterns.
   - Regex patterns are dynamically loaded and compiled from `regex.yml`.
   - Debug logs are available to verify regex patterns and their matches.

3. **Bypass Permissions**:
   - Players can bypass wordlist and regex filtering if they have the appropriate permissions.
   - Bypass permissions are dynamically fetched from the configuration files (`wordlist.yml` and `regex.yml`).
   - Default permissions:
     - Wordlist Bypass: `skyenetp.wordlist.bypass`
     - Regex Bypass: `skyenetp.regex.bypass`

4. **Configuration Options**:
   - Enable or disable wordlist and regex filtering via the main configuration file.
   - Customize blocked words and regex patterns in their respective configuration files.

5. **Debugging and Logging**:
   - Extensive debug logs to assist in verifying the functionality of wordlist and regex filtering.

6. **Dynamic Reloading**:
   - Configuration files can be reloaded dynamically without restarting the server (feature implementation pending).

## Configuration Files

### `wordlist.yml`
- Contains the list of blocked words.
- Example:
  ```yaml
  blocked-words:
    - badword1
    - badword2
  bypass-permission: skyenetp.wordlist.bypass
  ```

### `regex.yml`
- Contains the list of regex patterns for filtering chat messages.
- Example:
  ```yaml
  regex-patterns:
    - ".*badregex.*"
    - "^forbidden.*"
  bypass-permission: skyenetp.regex.bypass
  ```



</details>

## Permissions

- `skyenetp.wordlist.bypass`: Allows bypassing wordlist filtering.
- `skyenetp.regex.bypass`: Allows bypassing regex filtering.
- `skyenetp.chatfilter.reload`: Allows reloading the chat filter configuration.

## Debugging

Enable debug mode in the main configuration file to view detailed logs for chat filtering operations.