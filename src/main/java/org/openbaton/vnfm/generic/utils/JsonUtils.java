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
import java.util.Map;

/** Created by mpa on 14.12.16. */
public class JsonUtils {

  private static Gson parser = new GsonBuilder().setPrettyPrinting().create();

  public static JsonObject getJsonObject(String action, String payload, String scriptPath) {
    JsonObject jsonMessage = new JsonObject();
    jsonMessage.addProperty("action", action);
    jsonMessage.addProperty("payload", payload);
    jsonMessage.addProperty("script-path", scriptPath);
    return jsonMessage;
  }

  public static JsonObject getJsonObject(String action, String payload, Map<String, String> env) {
    JsonObject jsonMessage = new JsonObject();
    jsonMessage.addProperty("action", action);
    jsonMessage.addProperty("payload", payload);
    jsonMessage.add("env", parser.fromJson(parser.toJson(env), JsonObject.class));
    return jsonMessage;
  }

  public static JsonObject getJsonObjectForScript(
      String save_scripts, String payload, String name, String scriptPath) {
    JsonObject jsonMessage = new JsonObject();
    jsonMessage.addProperty("action", save_scripts);
    jsonMessage.addProperty("payload", payload);
    jsonMessage.addProperty("name", name);
    jsonMessage.addProperty("script-path", scriptPath);
    return jsonMessage;
  }

  public static String parse(String json) {
    return parser
        .fromJson(json, JsonObject.class)
        .get("output")
        .getAsString()
        .replaceAll("\\\\n", "\n");
  }
}
