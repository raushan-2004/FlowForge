package com.flowforge.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "flowforge.worker.ssrf")
public class SsrfProperties {

    private boolean blockPrivateRanges = true;
    private List<String> extraBlockedRanges = Collections.emptyList();
    private List<String> allowedHosts = Collections.emptyList();

    public boolean isBlockPrivateRanges() {
        return blockPrivateRanges;
    }

    public void setBlockPrivateRanges(boolean blockPrivateRanges) {
        this.blockPrivateRanges = blockPrivateRanges;
    }

    public List<String> getExtraBlockedRanges() {
        return extraBlockedRanges;
    }

    public void setExtraBlockedRanges(List<String> extraBlockedRanges) {
        this.extraBlockedRanges = extraBlockedRanges;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }
}
