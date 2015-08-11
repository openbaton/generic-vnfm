package org.project.openbaton.vnfm.generic;


import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;

import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.springframework.boot.SpringApplication;

import javax.jms.JMSException;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{

    public GenericVNFM(){
        parser = new GsonBuilder().setPrettyPrinting().create();

    }
    @Override
    public CoreMessage instantiate(VirtualNetworkFunctionRecord vnfr) {

        log.info("Instantiation of VirtualNetworkFunctionRecord " + vnfr.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + vnfr);
        boolean allocate=false;

        for (LifecycleEvent event : vnfr.getLifecycle_event())
        {
            if (event.getEvent() == Event.ALLOCATE)
            {

                //Request validation and & processing (MANO: B.3.1.2 step 5)

                allocate=true;
                return getCoreMessage(Action.ALLOCATE_RESOURCES, vnfr);
            }
        }

        if(!allocate)
        {
            for (LifecycleEvent event : vnfr.getLifecycle_event())
            {
                if (event.getEvent() == Event.INSTANTIATE)
                {
                    for (String script : event.getLifecycle_events()) {

                        String command = getJsonObject("EXECUTE", script).toString();
                        log.debug("Sending command: " + command);


                        try {
                            sendToEmsAndUpdate(vnfr, event.getEvent(), command, "generic");
//                            executeActionOnEMS("generic", command);
                        } catch (JMSException e) {
                            return getCoreMessage(Action.ERROR, vnfr);
                        } catch (VnfmSdkException e) {
                            //e.getMessage();
                            return getCoreMessage(Action.ERROR, vnfr);
                        }
//                        updateVnfr(vnfr, event.getEvent(),command);
                    }
                }
            }
        }

        return getCoreMessage(Action.INSTANTIATE, vnfr);
    }

    private CoreMessage getCoreMessage(Action action, VirtualNetworkFunctionRecord payload){
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(action);
        coreMessage.setPayload(payload);
        return coreMessage;
    }



    @Override
    public void query() {

    }

    @Override
    public void scale(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {

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
    public CoreMessage modify(VirtualNetworkFunctionRecord vnfr) {
        return getCoreMessage(Action.MODIFY, vnfr);
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
        message.setPayload(virtualNetworkFunctionRecord);
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
