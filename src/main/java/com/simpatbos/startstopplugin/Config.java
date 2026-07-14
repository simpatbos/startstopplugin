package com.simpatbos.startstopplugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.nio.file.Files;

@ConfigSerializable
public class Config {
    private final Path dataDir;

    @Setting("start-url")
    private String startUrl = "<your-start-url>";

    @Setting("shutdown-url")
    private String shutdownUrl = "<your-stop-url>";

    @Setting("status-url")
    private String statusUrl = "<your-status-url>";

    @Setting("timeout")
    private int timeout = 300;

    public Config(Path dataDir) {
        this.dataDir = dataDir;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public String getShutdownUrl() {
        return shutdownUrl;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public int getTimeout() {
        return timeout;
    }

     // returns whether or not config was successfully loaded and not created.
    public boolean loadConfig() throws IOException {
        Path configFile = this.dataDir.resolve("config.toml");

        if (!Files.exists(this.dataDir)) {
            Files.createDirectories(this.dataDir);
        }

        if (!Files.exists(configFile)) {
            InputStream in = getClass().getResourceAsStream("/config.toml");
            if (in != null) {
                Files.copy(in, configFile);
            } else {
                Files.createFile(configFile);
            }
            return false;
        }
        
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().path(configFile).build();
        CommentedConfigurationNode rootNode = loader.load();
        rootNode.get(Config.class, this);

        return true;
    }
}