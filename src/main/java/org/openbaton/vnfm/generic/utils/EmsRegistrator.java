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

/**
 * Created by lto on 15/09/15.
 */
@Service
@Scope
public class EmsRegistrator implements org.openbaton.common.vnfm_sdk.interfaces.EmsRegistrator{

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private Gson parser = new GsonBuilder().setPrettyPrinting().create();

    public HashSet<String> getHostnames() {
        return hostnames;
    }

    //TODO consider using DB in case of failure etc...
    private HashSet<String> hostnames;

    @PostConstruct
    private void init(){
        hostnames = new HashSet<>();
    }

    public void register(String json){
        log.debug("EMSRegister received: " + json);
        JsonObject object = parser.fromJson(json, JsonObject.class);
        hostnames.add(object.get("hostname").getAsString());
    }

    public void unregister(String hostname){
        log.debug("removing: " + hostname);
        hostnames.remove(hostname);
    }
}
