package org.project.openbaton.vnfm.generic;

import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.springframework.boot.SpringApplication;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{
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

    public static void main(String[] args) {
        SpringApplication.run(GenericVNFM.class);
    }
}
