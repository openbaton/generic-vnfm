package org.project.openbaton.vnfm.generic;


import com.google.gson.JsonObject;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.catalogue.nfvo.DependencyParameters;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.springframework.boot.SpringApplication;

import java.util.Map;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{

    public GenericVNFM(){

    }

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {


        boolean allocate=false;
        if (getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(),Event.ALLOCATE) != null)
            if (getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event_history(), Event.ALLOCATE) != null)
                allocate=false;
            else
                allocate = true;

        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());

        if(!allocate)
        {
            LifecycleEvent le = getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event_history(), Event.INSTANTIATE);
            if (le != null)
            {
                for (String script : le.getLifecycle_events()) {

                    String command = getJsonObject("EXECUTE", script).toString();
                    log.debug("Sending command: " + command);
                    sendToEmsAndUpdate(virtualNetworkFunctionRecord, le.getEvent(), command, "generic");
                }
            }

        }
        else{
            sendToNfvo(getCoreMessage(Action.ALLOCATE_RESOURCES, virtualNetworkFunctionRecord));
            return null;
        }

        Thread.sleep(1000 * ((int) (Math.random() * 3 + 1)));

        /*String command = "{ \"action\": \"CONFIGURATION_UPDATE\", \"payload\":" + parser.toJson(map) + "}";
        try {
            String result = executeActionOnEMS("generic",command);

            JsonObject object = parser.fromJson(result, JsonObject.class);
            Map<String, String > res = parser.fromJson(object.get("output"), Map.class);
            for (ConfigurationParameter configurationParameter : vnfr.getProvides().getConfigurationParameters()){
                for (Map.Entry<String, String> entry:res.entrySet()) {
                    if (configurationParameter.getConfKey().equals(entry.getKey())){
                        configurationParameter.setValue(entry.getValue());
                        log.debug("Got Configuration Parameter: " + configurationParameter);
                    }
                }
            }
        } catch (JMSException e) {
            e.printStackTrace();
            sendToNfvo(getCoreMessage(Action.ERROR, vnfr));
            return null;
        } catch (VnfmSdkException e) {
            e.printStackTrace();
            sendToNfvo(getCoreMessage(Action.ERROR, vnfr));
            return null;
        }*/

        return virtualNetworkFunctionRecord;
    }





    @Override
    public void query() {

    }

    @Override
    public void scale() {

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
        log.debug("Got dependency: " + dependency);
        log.debug("Parameters are: ");
        for (Map.Entry<String, DependencyParameters> entry : dependency.getParameters().entrySet()){
            log.debug("Source type: " + entry.getKey());
            log.debug("Parameters: " + entry.getValue().getParameters());
        }
        LifecycleEvent le = getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.CONFIGURE);
        if(le!=null){
            for(String script: le.getLifecycle_events()){
                String resolvedScript = resolveScriptParameters(script,dependency.getParameters());
                log.debug("Script to send to EMS is: "+resolvedScript);

            }
        }
        Thread.sleep(3000 + ((int) (Math.random() * 7000)));
//        updateVnfr(virtualNetworkFunctionRecord, Event.CONFIGURE);
        return virtualNetworkFunctionRecord;
    }

    private String resolveScriptParameters(String script, Map<String, DependencyParameters> parameters) {
        if(script.indexOf('#')==-1)
            return script;

        String[] splittedScript = script.split(" ");
        StringBuilder sb= new StringBuilder();
        for(String s : splittedScript){
            if(s.startsWith("#")){
                DependencyParameters dp = parameters.get(s.substring(1).split(":")[0]);
                String param = dp.getParameters().get(s.substring(1).split(":")[1]);
                if(param!=null)
                    sb.append(param+" ");
            }
            else sb.append(s+" ");
        }
        return sb.toString();
    }

    @Override
    public VirtualNetworkFunctionRecord terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        //TODO implemenet termination
        return virtualNetworkFunctionRecord;
    }

    @Override
    public CoreMessage handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        return null;
    }

    @Override
    protected VirtualNetworkFunctionRecord start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {

        log.debug("Starting vnfr: " + virtualNetworkFunctionRecord.getName());
        Thread.sleep(3000 + ((int) (Math.random() * 7000)));
        return virtualNetworkFunctionRecord;
    }

    @Override
    protected VirtualNetworkFunctionRecord configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
        String scriptsLink = virtualNetworkFunctionRecord.getVnfPackage().getScriptsLink();
        log.debug("Scripts are: " + scriptsLink);
        JsonObject jsonMessage = getJsonObject("SAVE_SCRIPTS", scriptsLink);

            executeActionOnEMS("generic", jsonMessage.toString());
        return virtualNetworkFunctionRecord;
    }

    private JsonObject getJsonObject(String action, String payload) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("action", action);
        jsonMessage.addProperty("payload", payload);
        return jsonMessage;
    }

    public static void main(String[] args) {
        SpringApplication.run(GenericVNFM.class);
    }

    @Override
    public void NotifyChange() {

    }
}
