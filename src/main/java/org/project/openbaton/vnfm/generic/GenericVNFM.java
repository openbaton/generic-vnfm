package org.project.openbaton.vnfm.generic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.springframework.boot.SpringApplication;

import javax.jms.JMSException;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{
    private Gson parser;
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
                if (event.getEvent() == Event.INSTALL)
                {
                    for (String script : event.getLifecycle_events()) {
//                        toEMS(script, );
                        getCoreMessage(Action.INSTANTIATE, vnfr);
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

    private void toEMS(String command, String vduHostname){

            String answer=null;
            this.sendMessageToQueue("vnfm-" + vduHostname + "-actions", command);
            log.debug("Received from EMS: " + answer);

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
    protected CoreMessage configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        String scriptsLink = virtualNetworkFunctionRecord.getVnfPackage().getScriptsLink();
        log.debug("Scripts are: " + scriptsLink);
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("action", "SAVE_SCRIPTS");
        jsonMessage.addProperty("payload", scriptsLink);
        toEMS(jsonMessage.toString(), "generic");

        String result=null;
        try {
            result = receiveTextFromQueue("generic-vnfm-actions");
        } catch (JMSException e) {
            e.printStackTrace();
            getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
        }

        if(!checkResult(result))
            return getCoreMessage(Action.ERROR, virtualNetworkFunctionRecord);
        return getCoreMessage(Action.CONFIGURE, virtualNetworkFunctionRecord);
    }

    private boolean checkResult(String resultFromEms){
        if(resultFromEms==null)
            return false;
        JsonObject jsonObject = parser.fromJson(resultFromEms,JsonObject.class);
        boolean result=false;
        if(jsonObject.get("status").getAsInt()==0){
            result = true;
        }
        else{
            log.error(jsonObject.get("err").getAsString());
        }
        log.debug(jsonObject.get("out").getAsString());
        return result;

    }
    public static void main(String[] args) {
        SpringApplication.run(GenericVNFM.class);
    }
}
