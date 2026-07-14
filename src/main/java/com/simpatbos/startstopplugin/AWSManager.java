package com.simpatbos.startstopplugin;

import java.io.IOException;

public class AWSManager {
    public enum EC2ServerStatus {
        running,
        stopped,
        stopping,
        starting,
        unknown
    }

    private record LambdaResponse(
            int statusCode,
            String body,
            EC2ServerStatus status) {
    }

    private final String EC2StartupUrl;
    private final String EC2ShutdownUrl;
    private final String EC2StatusUrl;
    private final RequestManager<LambdaResponse> requestManager = new RequestManager<LambdaResponse>(LambdaResponse.class);

    public AWSManager(String EC2StartupUrl, String EC2ShutdownUrl, String EC2StatusUrl) {
        this.EC2StartupUrl = EC2StartupUrl;
        this.EC2ShutdownUrl = EC2ShutdownUrl;
        this.EC2StatusUrl = EC2StatusUrl;
    }

    public EC2ServerStatus getEC2Status() throws InterruptedException, IOException, Exception {
        LambdaResponse res = this.requestManager.requestUrl(this.EC2StatusUrl);

        if (res.statusCode == 200) {
            return res.status;
        }
        else {
            throw new Exception("Error getting server status");
        }
    }

    public EC2ServerStatus startupEC2() throws InterruptedException, IOException {
        LambdaResponse res = this.requestManager.requestUrl(this.EC2StartupUrl);

        if (res.statusCode == 200) {
            return EC2ServerStatus.starting;
        } else if (res.statusCode == 500) {
            return EC2ServerStatus.unknown;
        } else if (res.statusCode == 400) {
            if (res.status == EC2ServerStatus.running) {
                return EC2ServerStatus.running;
            }
            if (res.status == EC2ServerStatus.starting) {
                return EC2ServerStatus.starting;
            }
            if (res.status == EC2ServerStatus.stopping) {
                return EC2ServerStatus.stopping;
            }
        }
        return EC2ServerStatus.unknown;
    }

    public EC2ServerStatus shutdownEC2() throws InterruptedException, IOException {
        LambdaResponse res = this.requestManager.requestUrl(this.EC2ShutdownUrl);

        if (res.statusCode == 200) {
            return EC2ServerStatus.stopping;
        } else if (res.statusCode == 500) {
            return EC2ServerStatus.unknown;
        } else if (res.statusCode == 400) {
            if (res.status == EC2ServerStatus.stopped) {
                return EC2ServerStatus.stopped;
            }
            if (res.status == EC2ServerStatus.starting) {
                return EC2ServerStatus.starting;
            }
        }
        return EC2ServerStatus.unknown;
    }
}
