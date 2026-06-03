package com.campus.campus_life_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.gray")
public class GatewayGrayProperties {

    private boolean enabled = false;
    private String headerName = "X-Gray";
    private String headerValue = "1";
    private String internalPrefix = "/__gray";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    public String getInternalPrefix() {
        return internalPrefix;
    }

    public void setInternalPrefix(String internalPrefix) {
        this.internalPrefix = internalPrefix;
    }
}
