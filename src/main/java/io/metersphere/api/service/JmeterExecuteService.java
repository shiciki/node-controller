package io.metersphere.api.service;

import io.metersphere.api.controller.request.RunRequest;
import io.metersphere.api.jmeter.JMeterService;
import io.metersphere.api.jmeter.utils.FileUtils;
import io.metersphere.api.jmeter.utils.MSException;
import io.metersphere.api.service.utils.ZipSpiderUtil;
import io.metersphere.node.util.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.NewDriver;
import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;

@Service
public class JmeterExecuteService {
    @Resource
    private JMeterService jMeterService;
    @Resource
    LoadTestProducer loadTestProducer;

    private String serverUrl;

    private static InputStream getStrToStream(String sInputString) {
        if (StringUtils.isNotEmpty(sInputString)) {
            try {
                ByteArrayInputStream tInputStringStream = new ByteArrayInputStream(sInputString.getBytes());
                return tInputStringStream;
            } catch (Exception ex) {
                ex.printStackTrace();
                MSException.throwException("生成脚本异常");
            }
        }
        return null;
    }

    private void loadJar(String path) {
        try {
            NewDriver.addPath(path);
        } catch (MalformedURLException e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException(e.getMessage());
        }
    }

    public String run(RunRequest request, MultipartFile[] bodyFiles) {
        if (request == null || request.getJmx() == null) {
            return "执行文件为空，无法执行！";
        }
        try {
            LogUtil.info(request.getJmx());
            // 检查KAFKA
            loadTestProducer.checkKafka();
            if (StringUtils.isEmpty(serverUrl)) {
                serverUrl = request.getServerUrl();
                // 第一次执行
                this.loadJar();
            }
            serverUrl = request.getServerUrl();
            // 生成附件
            if (bodyFiles != null && bodyFiles.length > 0) {
                FileUtils.createFiles(bodyFiles, FileUtils.BODY_FILE_DIR);
            }
            // 生成执行脚本
            InputStream inputSource = getStrToStream(request.getJmx());
            Object scriptWrapper = SaveService.loadElement(inputSource);
            HashTree testPlan = JMeterService.getHashTree(scriptWrapper);
            // 开始执行
            LogUtil.info("request:"+request);
            if ( request.getJinput() == null) {
                jMeterService.run(request, testPlan);
            }else{
                jMeterService.run(request, testPlan,request.getJinput());
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage());
            return e.getMessage();
        }
        return "SUCCESS";
    }

    public void loadJar() {
        if (StringUtils.isNotEmpty(serverUrl)) {
            File file = ZipSpiderUtil.downloadFile(serverUrl, FileUtils.JAR_FILE_DIR);
            if (file != null) {
                ZipSpiderUtil.unzip(file.getPath(), FileUtils.JAR_FILE_DIR);
                this.loadJar(FileUtils.JAR_FILE_DIR);
                FileUtils.deleteFile(file.getPath());
            }
        }
    }

}
