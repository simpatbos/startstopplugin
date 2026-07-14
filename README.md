# StartStop Plugin

A Velocity proxy plugin that automatically starts and stops your Minecraft server hosted on AWS EC2 based on player activity.

## Features

*   **Automatic Server Startup:** When the first player attempts to join, the plugin can trigger the startup of your EC2 instance.
*   **Automatic Server Shutdown:** If no players are online for a configurable duration, the plugin will automatically initiate the shutdown of your EC2 instance.
*   **Dynamic Server Status:** Displays real-time server status (Running, Starting, Stopped, Stopping, Unknown) in the server list (MOTD).
*   **Custom Kick Screens:** Provides informative kick messages to players based on the server's current state (e.g., "Server is starting up," "Server is stopping").
*   **Configurable AWS Endpoints:** Uses configurable URLs to interact with AWS Lambda functions (or similar HTTP endpoints) that manage your EC2 instance.

## How it Works

The plugin integrates with your Velocity proxy and listens for player events:
1.  **Player Join:** If a player joins and the server is not running or starting, it triggers a startup command via a configured AWS endpoint. If a shutdown timer was active, it gets cancelled.
2.  **Player Disconnect:** If the last player disconnects, a shutdown timer starts. If no one joins before the timer expires, a shutdown command is sent via a configured AWS endpoint.
3.  **Server List Ping (MOTD):** When players refresh their server list, the plugin queries the EC2 instance status via a configured AWS endpoint and updates the server's Message Of The Day (MOTD) to reflect the current state.
4.  **Kicked from Server:** If a player is kicked (e.g., due to the server not being available), a custom message is displayed based on the known server status.

## Configuration

The plugin uses a `config.toml` file for its settings. Upon first run, a default `config.toml` will be generated in your Velocity proxy's `plugins/StartStop` directory. You **must** edit this file with your specific AWS endpoint URLs.

Example `config.toml`:

```toml
start-url="<your-aws-lambda-start-url>"
shutdown-url="<your-aws-lambda-shutdown-url>"
status-url="<your-aws-lambda-status-url>"
timeout=300 # Time in seconds before the server shuts down after the last player leaves. (e.g., 300 seconds = 5 minutes)
```

**Note:** You need to set up AWS Lambda functions (or other HTTP endpoints) that can receive a GET request and trigger the appropriate EC2 actions (start, stop, get status). The responses from these endpoints should conform to the expected JSON structure (status code and EC2 server status).

## Building from Source

This project uses Maven.

1.  Clone the repository:
    ```bash
    git clone https://github.com/your-repo/startstopplugin.git
    cd startstopplugin
    ```
2.  Build the plugin:
    ```bash
    mvn clean package
    ```
    This will produce a JAR file in the `target/` directory.

## Installation

1.  Place the generated `.jar` file into the `plugins` folder of your Velocity proxy server.
2.  Start your Velocity server. A `config.toml` file will be generated in `plugins/StartStop/`.
3.  Edit the `config.toml` with your AWS Lambda endpoint URLs and desired timeout.
4.  Restart your Velocity server.

## Requirements

*   Velocity Proxy Server
*   Java 17 or higher
*   AWS EC2 instance for your Minecraft server
*   AWS Lambda functions (or similar HTTP endpoints) configured to start, stop, and get the status of your EC2 instance.

## Contributing

Feel free to open issues or pull requests on the GitHub repository.

## License

GNU General Public License v3.0 (see LICENSE file)
