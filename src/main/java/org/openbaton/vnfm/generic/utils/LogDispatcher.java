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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

@Service
@ConfigurationProperties
public class LogDispatcher implements org.openbaton.common.vnfm_sdk.interfaces.LogDispatcher {

  @Value("${vnfm.ems.script.logpath:/var/log/openbaton/scriptsLog/}")
  private String logPath;

  @Value("${vnfm.ems.script.old:60}")
  private int old;

  private ScheduledExecutorTask scheduledExecutorTask;
  @Autowired private Gson gson;

  private Logger log = LoggerFactory.getLogger(getClass());

  private static List<String> readFile(String path, Charset encoding) throws IOException {
    return Files.readAllLines(Paths.get(path), encoding);
    //        return new String(encoded, encoding);
  }

  @Override
  public String sendLogs(String request) {
    String vnfrName = this.gson.fromJson(request, JsonObject.class).get("vnfrName").getAsString();
    String hostname = this.gson.fromJson(request, JsonObject.class).get("hostname").getAsString();

    List<String> logs;
    try {
      logs = readFile(this.logPath + vnfrName + '/' + hostname + ".log", Charset.defaultCharset());
    } catch (IOException exception) {
      exception.printStackTrace();
      this.log.error("Unable to retrieve logs: " + exception.getLocalizedMessage());
      return "{ \"answer\": \""
          + "Unable to retrieve logs: "
          + exception.getLocalizedMessage()
          + "\"}";
    }

    return "{ \"answer\": " + this.gson.toJson(logs) + '}';
  }

  @PostConstruct
  private void init() {
    deleteLogs();
  }

  private void deleteFilesRecursively(File top, long now) {
    for (File f : top.listFiles()) {
      if (f.isDirectory()) {
        if (f.listFiles().length > 0) {
          deleteFilesRecursively(f, now);
        } else {
          f.delete();
        }
      } else if (now - f.lastModified() > this.old * 60000) {
        this.log.debug("Removed " + f.getName());
        f.delete();
      }
    }
  }

  @Scheduled(fixedRate = 180000, initialDelay = 180000)
  private void deleteLogs() {
    this.log.debug("Checking if delete is true");
    if (this.old > 0) {
      this.log.info("Removing script log files that are older than " + this.old + " minutes");
      File top = new File(this.logPath);
      if (top.exists()) {
        deleteFilesRecursively(top, new Date().getTime());
      }
    }
  }
}
