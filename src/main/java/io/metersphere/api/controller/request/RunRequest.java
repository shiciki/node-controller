package io.metersphere.api.controller.request;

import io.metersphere.api.jmeter.utils.RunModeConfig;
import lombok.Data;

@Data
public class RunRequest {
    private String testId;
    private String userId;
    private boolean isDebug;
    private String runMode;
    private String jmx;
    private RunModeConfig config;
    private String serverUrl;
    private String jinput;
}
