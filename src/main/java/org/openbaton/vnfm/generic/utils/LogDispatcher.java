/*
 * Copyright (c) 2016 Open Baton (http://www.openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.openbaton.vnfm.generic.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties
public class LogDispatcher implements org.openbaton.common.vnfm_sdk.interfaces.LogDispatcher {

  @Value("${vnfm.ems.script.logpath:/var/log/openbaton/scriptsLog/}")
  private String logPath;

  @Value("${vnfm.ems.script.old:60}")
  private int old;

  @Autowired private Gson gson;

  private Logger log = LoggerFactory.getLogger(getClass());

  private static List<String> readFile(String path, Charset encoding) throws IOException {
    try {

      return Files.readAllLines(Paths.get(path), encoding);
    } catch (java.nio.file.NoSuchFileException e) {
      return new ArrayList<>();
    }
    //        return new String(encoded, encoding);
  }

  @Override
  public String sendLogs(String request) {
    String vnfrName = this.gson.fromJson(request, JsonObject.class).get("vnfrName").getAsString();
    String hostname = this.gson.fromJson(request, JsonObject.class).get("hostname").getAsString();
    log.debug("Received request for retrieving logs for: " + vnfrName);
    Map<String, List<String>> logs = new HashMap<>();
    try {
      logs.put(
          "output",
          readFile(this.logPath + vnfrName + '/' + hostname + ".log", Charset.defaultCharset()));
      logs.put(
          "error",
          readFile(
              this.logPath + vnfrName + '/' + hostname + "-error.log", Charset.defaultCharset()));
    } catch (IOException exception) {
      exception.printStackTrace();
      this.log.error("Unable to retrieve logs: " + exception.getLocalizedMessage());
      return "{ \"answer\": \""
          + "Unable to retrieve logs: "
          + exception.getLocalizedMessage()
          + "\"}";
    }

    String answer = "{ \"answer\": " + this.gson.toJson(logs) + '}';
    log.debug("Answering: " + answer);
    return answer;
  }

  @PostConstruct
  private void init() {
    File logPathFolder = new File(this.logPath);
    if (!logPathFolder.exists())
      if (!logPathFolder.mkdirs()) {
        log.error(
            "Not able to create folder: "
                + logPath
                + " where i would like to store the logs. You won't be able to see the logs from the NFVO if you don't fix that issue. Either you create it on your own, or you give me the permission to create it :)");
      }
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
