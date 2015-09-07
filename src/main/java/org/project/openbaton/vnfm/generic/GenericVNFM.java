package org.project.openbaton.vnfm.generic;


import com.google.gson.JsonObject;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.ConfigurationParameter;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.springframework.boot.SpringApplication;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{

    public GenericVNFM(){
    }
    @Override
    public CoreMessage instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {

        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);
        boolean allocate=false;



        if (getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(),Event.ALLOCATE) != null)
            if (getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event_history(), Event.ALLOCATE) != null)
                allocate=false;
            else
                allocate = true;


        if(!allocate)
        {
            for (LifecycleEvent event : virtualNetworkFunctionRecord.getLifecycle_event())
            {
                log.debug("the event is: " + event);
                if (event.getEvent() == Event.INSTANTIATE)
                {
                    for (String script : event.getLifecycle_events()) {

                        String command = getJsonObject("EXECUTE", script).toString();
                        log.debug("Sending command: " + command);


                        try {
                            sendToEmsAndUpdate(virtualNetworkFunctionRecord, event.getEvent(), command, "generic");
//                            executeActionOnEMS("generic", command);
                        } catch (JMSException e) {
                            e.printStackTrace();
                            return getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
                        } catch (VnfmSdkException e) {
                            e.printStackTrace();
                            return getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
                        }
//                        updateVnfr(vnfr, event.getEvent(),command);
                    }
                }
            }
        }else
            return getCoreMessage(Action.ALLOCATE_RESOURCES, virtualNetworkFunctionRecord);


        /**
         * Before ending, need to get all the "provides" filled
         */

        Map<String, String> map = new HashMap<>();
        log.debug("Provides is: " + virtualNetworkFunctionRecord.getProvides());
        for (ConfigurationParameter configurationParameter : virtualNetworkFunctionRecord.getProvides().getConfigurationParameters()){
            map.put(configurationParameter.getConfKey(), "");
        }

        String command = "{ \"action\": \"CONFIGURATION_UPDATE\", \"payload\":" + parser.toJson(map) + "}";
        try {
            String result = executeActionOnEMS("generic",command);

            JsonObject object = parser.fromJson(result, JsonObject.class);
            Map<String, String > res = parser.fromJson(object.get("output"), Map.class);
            for (ConfigurationParameter configurationParameter : virtualNetworkFunctionRecord.getProvides().getConfigurationParameters()){
                for (Map.Entry<String, String> entry:res.entrySet()) {
                    if (configurationParameter.getConfKey().equals(entry.getKey())){
                        configurationParameter.setValue(entry.getValue());
                        log.debug("Got Configuration Parameter: " + configurationParameter);
                    }
                }
            }
        } catch (JMSException e) {
            e.printStackTrace();
            return getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
        } catch (VnfmSdkException e) {
            e.printStackTrace();
            return getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
        }

        return getCoreMessage(Action.INSTANTIATE, virtualNetworkFunctionRecord);
    }

    private CoreMessage getCoreMessage(Action action, VirtualNetworkFunctionRecord payload){
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(action);
        coreMessage.setVirtualNetworkFunctionRecord(payload);
        return coreMessage;
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
    public CoreMessage modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency) {
        return getCoreMessage(Action.MODIFY, virtualNetworkFunctionRecord);
    }

    @Override
    public void upgradeSoftware() {

    }

    @Override
    public CoreMessage terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        return null;
    }

    @Override
    public CoreMessage handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        return null;
    }

    @Override
    protected CoreMessage start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {

        log.debug("Starting vnfr: " + virtualNetworkFunctionRecord);
        virtualNetworkFunctionRecord.setStatus(Status.ACTIVE);
        CoreMessage message = new CoreMessage();
        message.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
        message.setAction(Action.START);
        return message;
    }

    @Override
    protected CoreMessage configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        String scriptsLink = virtualNetworkFunctionRecord.getVnfPackage().getScriptsLink();
        log.debug("Scripts are: " + scriptsLink);
        JsonObject jsonMessage = getJsonObject("SAVE_SCRIPTS", scriptsLink);

        try {
            executeActionOnEMS("generic", jsonMessage.toString());
        } catch (JMSException e) {
            return getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
        } catch (VnfmSdkException e) {
            //e.getMessage();
            return getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
        }
        return getCoreMessage(Action.CONFIGURE, virtualNetworkFunctionRecord);
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
}
