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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by lto on 15/09/15.
 */
@Service
@Scope
public class EmsRegistrator implements org.openbaton.common.vnfm_sdk.interfaces.EmsRegistrator {

  private Logger log = LoggerFactory.getLogger(getClass());
  private Gson parser = new GsonBuilder().setPrettyPrinting().create();

  //TODO consider using DB in case of failure etc...
  private Set<String> expectedHostnames;

  @PostConstruct
  private void init() {
    this.expectedHostnames = new HashSet<>();
  }

  public Set<String> getExpectedHostnames() {
    return this.expectedHostnames;
  }

  public void register(String hostname) {
    this.log.debug("EMSRegister adding: " + hostname);
    this.expectedHostnames.add(hostname.toLowerCase().replace("_", "-"));
  }

  public void unregister(String hostname) {
    this.log.debug("EMSRegister removing: " + hostname);
    if (this.expectedHostnames.contains(hostname)) this.expectedHostnames.remove(hostname);
  }

  @Override
  public void unregisterFromMsg(String json) {
    this.log.debug("EMSRegister received: " + json);
    JsonObject object = this.parser.fromJson(json, JsonObject.class);
    String hostname = object.get("hostname").getAsString().toLowerCase().replace("_", "-");
    this.log.debug("EMSRegister removing: " + hostname);
    if (this.expectedHostnames.contains(hostname)) this.expectedHostnames.remove(hostname);
  }
}
