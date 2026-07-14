package com.simpatbos.startstopplugin;

import com.google.inject.Inject;
import com.simpatbos.startstopplugin.AWSManager.EC2ServerStatus;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.*;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(id = "startstop", name = "StartStop Plugin", version = "0.1.0", description = "Plugin to automatically start and stop server when players join and leave.", authors = {
        "Me" })
public class JoinLeavePlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final Timer timer;
    private final AWSManager awsManager;
    private EC2ServerStatus currentServerStatus;

    @Inject
    public JoinLeavePlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.config = new Config(dataDirectory);

        this.currentServerStatus = EC2ServerStatus.unknown;

        try {
            boolean isConfigLoaded = this.config.loadConfig();
            if (!isConfigLoaded) {
                logger.error("First time using StartStop plugin. Please configure StartStop/config.conf and restart.");
                System.exit(0);
            }
        }
        catch (Exception e) {
            logger.error("Error loading StartStop configuration.");
            System.exit(0);
        }

        this.awsManager = new AWSManager(config.getStartUrl(), config.getShutdownUrl(), config.getStatusUrl());
        this.timer = new Timer(this.config.getTimeout(), this.awsManager::shutdownEC2);
    }

    @Subscribe
    public void onLogin(LoginEvent event) {

        if (this.timer.isTiming()) {
            this.logger.info("Player joined. Shutdown timer cancelled.");
            this.timer.cancelTimer();
        }

        if (server.getPlayerCount() == 0 && currentServerStatus != EC2ServerStatus.running) {
            try {
                this.currentServerStatus = this.awsManager.startupEC2();
            }
            catch (Exception e) {
                this.logger.error("Error starting server.\n" + e.getLocalizedMessage());
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (this.server.getPlayerCount() == 0 && 
            this.currentServerStatus != EC2ServerStatus.starting) {
            try {
                this.timer.startTimer();
            }
            catch(Exception e) {
                this.logger.error("Error shutting down server.\n" + e.getLocalizedMessage());
            }
        }
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        Component running = MiniMessage.miniMessage().deserialize(
            "<red><bold> ❌ Connection Failed ❌ </bold></red>\n\n" +
            "<gray>Current server status: <green><bold>Running.</bold>\n" +
            "<gray>It's probably okay. Checklist:\n" +
            "   1. Maybe the server is still starting up. If you just started it, wait ~30 more seconds for the OS to boot.\n" +
            "   2. Make sure you're on version <rainbow>26.1.2</rainbow>.\n" +
            "   3. If you're still having issues, contact Simon."

        );
        
        Component stopped = MiniMessage.miniMessage().deserialize(
            "<dark_red><bold> Stopped </bold></dark_red>\n\n" +
            "<gray>Current server status: <dark_red><bold>Stopped.</bold>\n" +
            "<gray>It should be starting up now. Check the MOTD.");

        Component starting = MiniMessage.miniMessage().deserialize(
            "<gold><bold> Starting </bold></gold>\n\n" +
            "<gray>Current server status: <gold><bold>Starting.</bold>\n" +
            "<gray>The server is starting yay! Check back in a couple minutes.");

        Component stopping = MiniMessage.miniMessage().deserialize(
            "<red><bold>❌ Stopping ❌</bold></red>\n\n" +
            "<gray>Current server status: <red><bold>Stopping.</bold>\n" +
            "<gray>The server is stopping. Boo. Wait for it to finish stopping, then you can trigger a startup.");

        Component unknown = MiniMessage.miniMessage().deserialize(
            "<red><bold>? Unkown Server Status ?</bold></red>\n\n" +
            "<gray>Current server status: <rainbow><bold>Unkown.</bold>\n" +
            "<gray>The server is in a weird state right now. It should be starting? Ask Simon if this continues.");

        Component customKickScreen;
        switch (this.currentServerStatus) {
            case running:
                customKickScreen = running;
                break;

            case stopped:
                customKickScreen = stopped;
                break;

            case starting:
                customKickScreen = starting;
                break;

            case stopping:
                customKickScreen = stopping;
                break;

            default:
                customKickScreen = unknown;
                break;
        }

        event.setResult(DisconnectPlayer.create(customKickScreen));
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing pingData = event.getPing();

        try {
            EC2ServerStatus status = this.awsManager.getEC2Status();
            this.currentServerStatus = status;

            if (this.currentServerStatus == EC2ServerStatus.running 
                && server.getPlayerCount() == 0
                && !this.timer.isTiming()) 
            {
                this.timer.startTimer();
            }
        }
        catch (Exception e) {
            this.logger.info("Failed to get server status. " + e.getLocalizedMessage());
            this.currentServerStatus = EC2ServerStatus.unknown;
        }
        
        Component running = MiniMessage.miniMessage().deserialize(
            "<gray>Status: <green><bold>Running</bold> <gray>Ready to join!"
        );

        Component empty = MiniMessage.miniMessage().deserialize(
            "<gray>Status: <yellow><bold>Empty</bold> <gray>Ready to join!\n" +
            this.timer.getTimeLeft() + " seconds until shutdown."
        );

        Component stopped = MiniMessage.miniMessage().deserialize(
            "<gray>Status: <dark_red><bold>Stopped.</bold> <gray>Join to start it up!"
        );

        Component starting = MiniMessage.miniMessage().deserialize(
            "<gray>Status: <gold><bold>Starting.</bold> <gray>Check back soon."
        );

        Component stopping = MiniMessage.miniMessage().deserialize(
            "<gray>Status: <red><bold>Stopping.</bold> <gray>Let it finish stopping, then join to start up again!"
        );

        Component unknown = MiniMessage.miniMessage().deserialize(
            "<gray>Status: <rainbow><bold>Unkown.</bold> <gray>Join to get it started!"
        );

        Component MOTD;
        switch (this.currentServerStatus) {
            case running:
                if (server.getPlayerCount() > 0) MOTD = running;
                else MOTD = empty;
                break;
            
            case stopped:
                MOTD = stopped;
                break;

            case starting:
                MOTD = starting;
                break;

            case stopping:
                MOTD = stopping;
                break;

            default:
                MOTD = unknown;
                break;
        }
        
        ServerPing.Builder mutatedPing = pingData.asBuilder()
                .description(MOTD); // Set the MOTD

        event.setPing(mutatedPing.build());
    }
}
