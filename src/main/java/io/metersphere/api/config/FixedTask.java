package io.metersphere.api.config;

import io.metersphere.api.jmeter.utils.CommonBeanFactory;
import io.metersphere.api.service.JmeterExecuteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FixedTask {

    @Scheduled(cron = "0 0/5 * * * ?")
    public void execute() {
        JmeterExecuteService service = CommonBeanFactory.getBean(JmeterExecuteService.class);
        if (service != null) {
            service.loadJar();
        }
    }
}
