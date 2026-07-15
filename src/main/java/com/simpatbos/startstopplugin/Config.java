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
    private String shutdownUrl = "<your-shutdown-url>";

    @Setting("status-url")
    private String statusUrl = "<your-status-url>";

    @Setting("timeout")
    private long timeout = 300;

    public Config() {
        this.dataDir = null;
    }

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

    public long getTimeout() {
        return timeout;
    }

     // returns whether or not config was successfully loaded and not created.
    public boolean loadConfig() throws IOException {
        Path configFile = this.dataDir.resolve("config.conf");

        if (!Files.exists(this.dataDir)) {
            Files.createDirectories(this.dataDir);
        }

        if (!Files.exists(configFile)) {
            InputStream in = getClass().getResourceAsStream("/config.conf");
            if (in != null) {
                Files.copy(in, configFile);
            } else {
                Files.createFile(configFile);
            }
            return false;
        }
        
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().path(configFile).build();
        CommentedConfigurationNode rootNode = loader.load();
        
        Config loadedData = rootNode.get(Config.class);

        if (loadedData != null) {
            this.startUrl = loadedData.getStartUrl();
            this.shutdownUrl = loadedData.getShutdownUrl();
            this.statusUrl = loadedData.getStatusUrl();
            this.timeout = loadedData.getTimeout();
            return true;
        }

        return true;
    }
}