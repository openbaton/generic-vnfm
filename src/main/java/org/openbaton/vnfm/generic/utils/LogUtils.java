/*
 *
 *  * Copyright (c) 2016 Fraunhofer FOKUS
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package org.openbaton.vnfm.generic.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by mpa on 14.12.16.
 */
public class LogUtils {

  private static Gson parser = new GsonBuilder().setPrettyPrinting().create();

  @Value("${vnfm.ems.script.logpath:/var/log/openbaton/scriptsLog/}")
  private static String scriptsLogPath;

  @Value("${vnfm.ems.script.old:60}")
  private static int old;

  public static void init() {
    if (old > 0) {
      File f = new File(scriptsLogPath);
      f.mkdirs();
    }
  }

  public static void saveLogToFile(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      String script,
      VNFCInstance vnfcInstance1,
      String output)
      throws IOException {
    saveLogToFile(virtualNetworkFunctionRecord, script, vnfcInstance1, output, false);
  }

  public static void saveLogToFile(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      String script,
      VNFCInstance vnfcInstance1,
      String output,
      boolean error)
      throws IOException {
    if (old > 0) {
      String path = "";
      if (!error) {
        path =
            scriptsLogPath
                + virtualNetworkFunctionRecord.getName()
                + "/"
                + vnfcInstance1.getHostname()
                + ".log";
      } else {
        path =
            scriptsLogPath
                + virtualNetworkFunctionRecord.getName()
                + "/"
                + vnfcInstance1.getHostname()
                + "-error.log";
      }
      File f = new File(path);

      if (!f.exists()) {
        f.getParentFile().mkdirs();
        f.createNewFile();
      }

      if (!error) {
        Files.write(
            Paths.get(path),
            ("Output of Script : " + script + "\n\n").getBytes(),
            StandardOpenOption.APPEND);
        Files.write(
            Paths.get(path),
            parser
                .fromJson(output, JsonObject.class)
                .get("output")
                .getAsString()
                .replaceAll("\\\\n", "\n")
                .getBytes(),
            StandardOpenOption.APPEND);
      } else {
        Files.write(
            Paths.get(path),
            ("Error log of Script : " + script + "\n\n").getBytes(),
            StandardOpenOption.APPEND);
        Files.write(
            Paths.get(path),
            parser
                .fromJson(output, JsonObject.class)
                .get("err")
                .getAsString()
                .replaceAll("\\\\n", "\n")
                .getBytes(),
            StandardOpenOption.APPEND);
      }
      Files.write(
          Paths.get(path),
          "\n\n\n~~~~~~~~~~~~~~~~~~~~~~~~~\n#########################\n~~~~~~~~~~~~~~~~~~~~~~~~~\n\n\n"
              .getBytes(),
          StandardOpenOption.APPEND);
    }
  }
}
