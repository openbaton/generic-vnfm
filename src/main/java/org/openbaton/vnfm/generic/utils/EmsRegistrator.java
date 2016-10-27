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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

/**
 * Created by lto on 15/09/15.
 */
@Service
@Scope
public class EmsRegistrator implements org.openbaton.common.vnfm_sdk.interfaces.EmsRegistrator {

  private Logger log = LoggerFactory.getLogger(getClass());
  private Gson parser = new GsonBuilder().setPrettyPrinting().create();

  public Set<String> getHostnames() {
    return this.hostnames;
  }

  //TODO consider using DB in case of failure etc...
  private Set<String> hostnames;

  @PostConstruct
  private void init() {
    this.hostnames = new HashSet<>();
  }

  @Override
  public void register(String json) {
    this.log.debug("EMSRegister received: " + json);
    JsonObject object = this.parser.fromJson(json, JsonObject.class);
    this.hostnames.add(object.get("hostname").getAsString().toLowerCase());
  }

  public void unregister(String hostname) {
    this.log.debug("EMSRegister removing: " + hostname);
    this.hostnames.remove(hostname.toLowerCase());
  }
}
