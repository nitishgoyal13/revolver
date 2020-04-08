package io.dropwizard.revolver.core.config;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RevolverConfigHolder {

    private AtomicReference<RevolverConfig> configRef;
    private AtomicLong lastUpdatedTimestamp;

    public RevolverConfigHolder(RevolverConfig initialRevolverConfig) {
        this.configRef = new AtomicReference<>();
        this.configRef.set(initialRevolverConfig);
        this.lastUpdatedTimestamp = new AtomicLong(0L);
        this.lastUpdatedTimestamp.set(System.currentTimeMillis());
    }

    public RevolverConfig getConfig() {
        return configRef.get();
    }

    public void setConfig(RevolverConfig config) {
        configRef.set(config);
        lastUpdatedTimestamp.set(System.currentTimeMillis());
    }
}
