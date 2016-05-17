package org.openbaton.vnfm.generic.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@Service
@ConfigurationProperties
public class LogDispatcher implements org.openbaton.common.vnfm_sdk.interfaces.LogDispatcher{

    @Value("${vnfm.ems.script.logpath:/var/log/openbaton/scriptsLog/}")
    private String logPath;
    @Value("${vnfm.ems.script.delete:true}")
    private boolean delete;

    private ScheduledExecutorTask scheduledExecutorTask;
    @Autowired
    private Gson gson;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static List<String> readFile(String path, Charset encoding) throws IOException
    {
        return Files.readAllLines(Paths.get(path), encoding);
//        return new String(encoded, encoding);
    }

    @Override
    public String sendLogs(String request) {
        String vnfrName = gson.fromJson(request, JsonObject.class).get("vnfrName").getAsString();
        String hostname = gson.fromJson(request, JsonObject.class).get("hostname").getAsString();

        List<String> logs;
        try {
            logs = readFile(logPath + vnfrName + "/" + hostname + ".log", Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Unable to retrieve logs: " + e.getLocalizedMessage());
            return "{ \"answer\": \"" + "Unable to retrieve logs: " + e.getLocalizedMessage() + "\"}";

        }

        return "{ \"answer\": " + gson.toJson(logs) + "}";
    }

    @PostConstruct
    private void init(){
        deleteLogs();
    }

    @Scheduled(cron = "0 0 0 */3 * *")
    private void deleteLogs() {
        log.debug("Checking if delete is true");
        if (delete) {
            log.debug("Removing script log files!");
            FileSystemUtils.deleteRecursively(new File(logPath));
        }
    }
}
