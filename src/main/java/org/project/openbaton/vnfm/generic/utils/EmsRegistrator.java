package org.project.openbaton.vnfm.generic.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;

/**
 * Created by lto on 15/09/15.
 */
@Service
@Scope
public class EmsRegistrator {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private Gson parser = new GsonBuilder().setPrettyPrinting().create();

    public HashSet<String> getHostnames() {
        return hostnames;
    }

    private HashSet<String> hostnames;

    @PostConstruct
    private void init(){
        hostnames = new HashSet<>();
    }

    @JmsListener(destination = "ems-generic-register", containerFactory = "jmsListenerContainerFactory")
    public void register(String json){
        log.debug("EMSRegister received: " + json);
        JsonObject object = parser.fromJson(json, JsonObject.class);
        hostnames.add(object.get("hostname").getAsString());
    }

}
