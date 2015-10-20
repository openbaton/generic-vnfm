package org.openbaton.vnfm.generic;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.mano.common.Ip;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.ConfigurationParameter;
import org.openbaton.catalogue.nfvo.DependencyParameters;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.openbaton.common.vnfm_sdk.jms.VnfmSpringHelper;
import org.openbaton.common.vnfm_sdk.utils.VnfmUtils;
import org.openbaton.vnfm.generic.utils.EmsRegistrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;

import java.util.*;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS {

    private static final String nfvoQueue = "vnfm-core-actions";

    @Autowired
    private EmsRegistrator emsRegistrator;
    private Gson parser = new GsonBuilder().setPrettyPrinting().create();
    private String scriptPath;

    public static void main(String[] args) {
        SpringApplication.run(GenericVNFM.class);
    }

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Object scripts) throws Exception {

        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());

        this.saveScriptOnEms(virtualNetworkFunctionRecord, scripts);

        for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu())
            for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance())
                log.debug("VNFCInstance: " + vnfcInstance);

        log.info("Executed script for INSTANTIATE: \n" +
                "\n" + this.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.INSTANTIATE));

        log.debug("added parameter to config");
        log.debug("CONFIGURATION: " + virtualNetworkFunctionRecord.getConfigurations());
        ConfigurationParameter cp = new ConfigurationParameter();
        cp.setConfKey("new_key");
        cp.setValue("new_value");
        virtualNetworkFunctionRecord.getConfigurations().getConfigurationParameters().add(cp);
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void query() {

    }

    @Override
    public VirtualNetworkFunctionRecord scale(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance component, Object scripts, VNFRecordDependency dependency) throws Exception {

        log.info("Created VNFComponent");

        saveScriptOnEms(component, scripts);
        log.debug("Executed scripts for event INSTANTIATE " + this.executeScriptsForEvent(virtualNetworkFunctionRecord, component, Event.INSTANTIATE));

        if (dependency != null)
            log.debug("Executed scripts for event CONFIGURE " + this.executeScriptsForEvent(virtualNetworkFunctionRecord, component, Event.CONFIGURE, dependency));

        if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.START) != null)
            log.debug("Executed scripts for event START " + this.executeScriptsForEvent(virtualNetworkFunctionRecord, component, Event.START));

        log.trace("HB_VERSION == " + virtualNetworkFunctionRecord.getHb_version());
        return virtualNetworkFunctionRecord;
    }

    private List<String> executeScriptsForEvent(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance, Event event) throws Exception {
        Map<String, String> env = getMap(virtualNetworkFunctionRecord);
        List<String> res = new LinkedList<>();
        LifecycleEvent le = VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);

        if (le != null) {
            log.trace("The number of scripts for " + virtualNetworkFunctionRecord.getName() + " are: " + le.getLifecycle_events());
            for (String script : le.getLifecycle_events()) {
                log.info("Sending script: " + script + " to VirtualNetworkFunctionRecord: " + virtualNetworkFunctionRecord.getName());
                Map<String, String> tempEnv = new HashMap<>();
                for (Ip ip : vnfcInstance.getIps()) {
                    log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
                    tempEnv.put(ip.getNetName(), ip.getIp());
                }
                log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
                tempEnv.put("floatingIp", vnfcInstance.getFloatingIps());

                env.putAll(tempEnv);
                log.info("Environment Variables are: " + env);

                String command = getJsonObject("EXECUTE", script, env).toString();
                res.add(executeActionOnEMS(vnfcInstance.getHostname(), command));

                for (String key : tempEnv.keySet()) {
                    env.remove(key);
                }
            }
            return res;
        }
        throw new VnfmSdkException("Error executing script");
    }

    private List<String> executeScriptsForEvent(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance, Event event, VNFRecordDependency dependency) throws Exception {
        Map<String, String> env = getMap(virtualNetworkFunctionRecord);
        List<String> res = new LinkedList<>();
        LifecycleEvent le = VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);
        log.trace("The number of scripts for " + virtualNetworkFunctionRecord.getName() + " are: " + le.getLifecycle_events());
        if (le != null) {
            for (String script : le.getLifecycle_events()) {

                String type = script.substring(0, script.indexOf("_"));
                log.info("Sending command: " + script + " to adding relation with type: " + type + " from VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());

                Map<String, String> tempEnv = new HashMap<>();

                //Adding own ips
                for (Ip ip : vnfcInstance.getIps()) {
                    log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
                    tempEnv.put(ip.getNetName(), ip.getIp());
                }

                //Adding own floating ip
                log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
                tempEnv.put("floatingIp", vnfcInstance.getFloatingIps());

                //Adding foreign parameters such as ip
                Map<String, String> parameters = dependency.getParameters().get(type).getParameters();
                for (Map.Entry<String, String> param : parameters.entrySet())
                    tempEnv.put(type + "_" + param.getKey(), param.getValue());

                env.putAll(tempEnv);
                log.info("Environment Variables are: " + env);

                String command = getJsonObject("EXECUTE", script, tempEnv).toString();
                res.add(executeActionOnEMS(vnfcInstance.getHostname(), command));

                for (String key : tempEnv.keySet()) {
                    env.remove(key);
                }
            }
            return res;
        }
        throw new VnfmSdkException("Error executing script");
    }

    @Override
    public void checkInstantiationFeasibility() {

    }

    @Override
    public void heal() {

    }

    @Override
    public void updateSoftware() {
    }

    @Override
    public void upgradeSoftware() {
    }

    @Override
    public VirtualNetworkFunctionRecord modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency) throws Exception {
        log.debug("VirtualNetworkFunctionRecord VERSION is: " + virtualNetworkFunctionRecord.getHb_version());
        log.debug("VirtualNetworkFunctionRecord NAME is: " + virtualNetworkFunctionRecord.getName());
        log.debug("Got dependency: " + dependency);
        log.debug("Parameters are: ");
        for (Map.Entry<String, DependencyParameters> entry : dependency.getParameters().entrySet()) {
            log.debug("Source type: " + entry.getKey());
            log.debug("Parameters: " + entry.getValue().getParameters());
        }

        log.debug("LifeCycle events: " + VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.CONFIGURE).getLifecycle_events());

        log.info("-----------------------------------------------------------------------");
        log.info("Result script for CONFIGURE: \n\n" + this.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.CONFIGURE, dependency));
        log.info("-----------------------------------------------------------------------");

        return virtualNetworkFunctionRecord;
    }

    //When the EMS reveive a script which terminate the vnf, the EMS is still running.
    //Once the vnf is terminated NFVO requests deletion of resources (MANO B.5) and the EMS will be terminated.
    @Override
    public VirtualNetworkFunctionRecord terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
        log.debug("Termination of VNF: " + virtualNetworkFunctionRecord.getName());
        log.info("Executed script: " + this.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.TERMINATE));
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {

    }

    @Override
    protected void fillSpecificProvides(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        for (ConfigurationParameter configurationParameter : virtualNetworkFunctionRecord.getProvides().getConfigurationParameters()) {
            if (!configurationParameter.getConfKey().startsWith("#nfvo:")) {
                configurationParameter.setValue("" + ((int) (Math.random() * 100)));
                log.debug("Setting: " + configurationParameter.getConfKey() + " with value: " + configurationParameter.getValue());
            }
        }
    }

    @Override
    public VirtualNetworkFunctionRecord start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
        log.debug("Starting vnfr: " + virtualNetworkFunctionRecord.getName());
        if (VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.CONFIGURE).getLifecycle_events() != null)
        {
            log.info("Executed script: " + this.executeScriptsForEvent(virtualNetworkFunctionRecord, Event.START));
        }
        return virtualNetworkFunctionRecord;
    }

    @Override
    public VirtualNetworkFunctionRecord configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void NotifyChange() {

    }

    @Override
    protected void checkEmsStarted(String hostname) {
        if (!emsRegistrator.getHostnames().contains(hostname))
            throw new RuntimeException("no ems for hostame: " + hostname);
    }

    private String executeActionOnEMS(String vduHostname, String command) throws Exception {
        log.trace("Sending message: " + command + " to " + vduHostname);
        ((VnfmSpringHelper) vnfmHelper).sendMessageToQueue("vnfm-" + vduHostname + "-actions", command);

        log.info("Waiting answer from EMS - " + vduHostname);

        String response = ((VnfmSpringHelper) vnfmHelper).receiveTextFromQueue(vduHostname + "-vnfm-actions");

        log.debug("Received from EMS (" + vduHostname + "): " + response);

        if (response == null) {
            throw new NullPointerException("Response from EMS is null");
        }

        JsonObject jsonObject = parser.fromJson(response, JsonObject.class);

        if (jsonObject.get("status").getAsInt() == 0) {
            try {
                log.debug("Output from EMS (" + vduHostname + ") is: " + jsonObject.get("output"));
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        } else {
            log.error(jsonObject.get("err").getAsString());
            throw new VnfmSdkException("EMS (" + vduHostname + ") had the following error: " + jsonObject.get("err").getAsString());
        }
        return response;
    }

    public Iterable<String> executeScriptsForEvent(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Event event) throws Exception {//TODO make it parallel
        Map<String, String> env = getMap(virtualNetworkFunctionRecord);
        List<String> res = new LinkedList<>();
        LifecycleEvent le = VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);
        log.trace("The number of scripts for " + virtualNetworkFunctionRecord.getName() + " are: " + le.getLifecycle_events());

        if (le != null) {
            for (String script : le.getLifecycle_events()) {
                log.info("Sending script: " + script + " to VirtualNetworkFunctionRecord: " + virtualNetworkFunctionRecord.getName());
                for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
                    for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                        Map<String, String> tempEnv = new HashMap<>();
                        for (Ip ip : vnfcInstance.getIps()) {
                            log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
                            tempEnv.put(ip.getNetName(), ip.getIp());
                        }
                        log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
                        tempEnv.put("floatingIp", vnfcInstance.getFloatingIps());

                        env.putAll(tempEnv);
                        log.info("Environment Variables are: " + env);

                        String command = getJsonObject("EXECUTE", script, env).toString();
                        res.add(executeActionOnEMS(vnfcInstance.getHostname(), command));

                        for (String key : tempEnv.keySet()) {
                            env.remove(key);
                        }
                    }
                }
            }
            return res;
        }
        throw new VnfmSdkException("Error executing script");
    }

    public List<String> executeScriptsForEvent(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Event event, VNFRecordDependency dependency) throws Exception {
        Map<String, String> env = getMap(virtualNetworkFunctionRecord);
        LifecycleEvent le = VnfmUtils.getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), event);
        List<String> res = new LinkedList<>();
        if (le != null) {
            for (String script : le.getLifecycle_events()) {

                String type = null;
                if (script.contains("_")) {
                    type = script.substring(0, script.indexOf("_"));
                    log.info("Sending command: " + script + " to adding relation with type: " + type + " from VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
                }

                for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
                    for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                        Map<String, String> tempEnv = new HashMap<>();

                        //Adding own ips
                        for (Ip ip : vnfcInstance.getIps()) {
                            log.debug("Adding net: " + ip.getNetName() + " with value: " + ip.getIp());
                            tempEnv.put(ip.getNetName(), ip.getIp());
                        }

                        //Adding own floating ip
                        log.debug("adding floatingIp: " + vnfcInstance.getFloatingIps());
                        tempEnv.put("floatingIp", vnfcInstance.getFloatingIps());

                        if (script.contains("_")) {
                            //Adding foreign parameters such as ip
                            Map<String, String> parameters = dependency.getParameters().get(type).getParameters();
                            for (Map.Entry<String, String> param : parameters.entrySet())
                                tempEnv.put(type + "_" + param.getKey(), param.getValue());
                        }
                        env.putAll(tempEnv);
                        log.info("Environment Variables are: " + env);

                        String command = getJsonObject("EXECUTE", script, tempEnv).toString();
                        res.add(executeActionOnEMS(vnfcInstance.getHostname(), command));

                        for (String key : tempEnv.keySet()) {
                            env.remove(key);
                        }

                    }
                }
            }
            return res;
        }
        throw new VnfmSdkException("Error executing script");
    }

    public void saveScriptOnEms(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Object scripts) throws Exception {

        log.debug("Scripts are: " + scripts.getClass().getName());

        if (scripts instanceof String) {
            String scriptLink = (String) scripts;
            log.debug("Scripts are: " + scriptLink);
            JsonObject jsonMessage = getJsonObject("CLONE_SCRIPTS", scriptLink, scriptPath);

            for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
                for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance()) {
                    executeActionOnEMS(vnfcInstance.getHostname(), jsonMessage.toString());
                }
            }
        } else if (scripts instanceof Set) {
            Set<Script> scriptSet = (Set<Script>) scripts;

            for (Script script : scriptSet) {
                log.debug("Sending script encoded base64 ");
                String base64String = Base64.encodeBase64String(script.getPayload());
                log.trace("The base64 string is: " + base64String);
                JsonObject jsonMessage = getJsonObjectForScript("SAVE_SCRIPTS", base64String, script.getName(), scriptPath);
                for (VirtualDeploymentUnit virtualDeploymentUnit : virtualNetworkFunctionRecord.getVdu()) {
                    for (VNFCInstance vnfcInstance : virtualDeploymentUnit.getVnfc_instance()) {
                        executeActionOnEMS(vnfcInstance.getHostname(), jsonMessage.toString());
                    }
                }
            }

        }
    }

    public void saveScriptOnEms(VNFCInstance component, Object scripts) throws Exception {

        log.debug("Scripts are: " + scripts.getClass().getName());

        if (scripts instanceof String) {
            String scriptLink = (String) scripts;
            log.debug("Scripts are: " + scriptLink);
            JsonObject jsonMessage = getJsonObject("CLONE_SCRIPTS", scriptLink, scriptPath);
            executeActionOnEMS(component.getHostname(), jsonMessage.toString());
        } else if (scripts instanceof Set) {
            Set<Script> scriptSet = (Set<Script>) scripts;
            for (Script script : scriptSet) {
                log.debug("Sending script encoded base64 ");
                String base64String = Base64.encodeBase64String(script.getPayload());
                log.trace("The base64 string is: " + base64String);
                JsonObject jsonMessage = getJsonObjectForScript("SAVE_SCRIPTS", base64String, script.getName(), scriptPath);
                executeActionOnEMS(component.getHostname(), jsonMessage.toString());
            }

        }
    }

    private JsonObject getJsonObject(String action, String payload, String scriptPath) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("action", action);
        jsonMessage.addProperty("payload", payload);
        jsonMessage.addProperty("script-path", scriptPath);
        return jsonMessage;
    }

    private JsonObject getJsonObject(String action, String payload, Map<String, String> dependencyParameters) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("action", action);
        jsonMessage.addProperty("payload", payload);
        jsonMessage.add("env", parser.fromJson(parser.toJson(dependencyParameters), JsonObject.class));
        return jsonMessage;
    }

    private JsonObject getJsonObjectForScript(String save_scripts, String payload, String name, String scriptPath) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("action", save_scripts);
        jsonMessage.addProperty("payload", payload);
        jsonMessage.addProperty("name", name);
        jsonMessage.addProperty("script-path", scriptPath);
        return jsonMessage;
    }

    private Map<String, String> getMap(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        Map<String, String> res = new HashMap<>();
        for (ConfigurationParameter configurationParameter : virtualNetworkFunctionRecord.getProvides().getConfigurationParameters())
            res.put(configurationParameter.getConfKey(), configurationParameter.getValue());
        for (ConfigurationParameter configurationParameter : virtualNetworkFunctionRecord.getConfigurations().getConfigurationParameters()) {
            res.put(configurationParameter.getConfKey(), configurationParameter.getValue());
        }
        return res;
    }

    @Override
    protected void setup() {
        super.setup();
        this.scriptPath = properties.getProperty("script-path", "/opt/openbaton/scripts");
    }
}
