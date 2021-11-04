package io.metersphere.api.jmeter;

import com.esotericsoftware.minlog.Log;
import io.metersphere.api.controller.request.RunRequest;
import io.metersphere.api.jmeter.utils.JmeterProperties;
import io.metersphere.api.jmeter.utils.MSException;
import io.metersphere.node.util.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.BackendListener;
import org.apache.jorphan.collections.HashTree;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import sun.security.util.Debug;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.lang.reflect.Field;

@Service
public class JMeterService {

    @Resource
    private JmeterProperties jmeterProperties;

    @PostConstruct
    public void init() {
        String JMETER_HOME = getJmeterHome();

        String JMETER_PROPERTIES = JMETER_HOME + "/bin/jmeter.properties";
        JMeterUtils.loadJMeterProperties(JMETER_PROPERTIES);
        JMeterUtils.setJMeterHome(JMETER_HOME);
        JMeterUtils.setLocale(LocaleContextHolder.getLocale());
    }

    public String getJmeterHome() {
        String home = getClass().getResource("/").getPath() + "jmeter";
        try {
            File file = new File(home);
            if (file.exists()) {
                return home;
            } else {
                return jmeterProperties.getHome();
            }
        } catch (Exception e) {
            return jmeterProperties.getHome();
        }
    }

    public static HashTree getHashTree(Object scriptWrapper) throws Exception {
        Field field = scriptWrapper.getClass().getDeclaredField("testPlan");
        field.setAccessible(true);
        return (HashTree) field.get(scriptWrapper);
    }


    private void addBackendListener(HashTree testPlan, RunRequest request) {
        BackendListener backendListener = new BackendListener();
        backendListener.setName(request.getTestId());
        Arguments arguments = new Arguments();
        if (request.getConfig() != null && request.getConfig().getMode().equals("serial") && request.getConfig().getReportType().equals("setReport")) {
            arguments.addArgument(APIBackendListenerClient.TEST_REPORT_ID, request.getConfig().getReportName());
        }
        arguments.addArgument(APIBackendListenerClient.TEST_ID, request.getTestId());
        if (StringUtils.isNotBlank(request.getRunMode())) {
            arguments.addArgument("runMode", request.getRunMode());
        }
        arguments.addArgument("DEBUG", request.isDebug() ? "DEBUG" : "RUN");
        arguments.addArgument("USER_ID", request.getUserId());
        backendListener.setArguments(arguments);
        backendListener.setClassname(APIBackendListenerClient.class.getCanonicalName());
        testPlan.add(testPlan.getArray()[0], backendListener);
    }

    private void addArgument(HashTree testPlan, RunRequest request,String input) {

        Arguments arguments = new Arguments();
        arguments.setName("input");
        for(String kv:input.split(",")){
            arguments.addArgument(kv.split(":")[0],kv.split(":")[1]);
        }
        //arguments.addArgument("a","qa3");
        testPlan.add(testPlan.getArray()[0],arguments);

        LogUtil.info("afteradd:"+testPlan);
        Log.info("afteradd"+testPlan);

    }

    public void run(RunRequest request, HashTree testPlan) {
        try {
            init();
            addBackendListener(testPlan, request);
            LocalRunner runner = new LocalRunner(testPlan);
            runner.run();
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException("读取脚本失败");
        }
    }

    public void run(RunRequest request, HashTree testPlan,String jinput) {
        try {
            init();
            addBackendListener(testPlan, request);
            addArgument(testPlan, request,jinput);
            LocalRunner runner = new LocalRunner(testPlan);
            runner.run();
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException("读取脚本失败");
        }
    }
}
