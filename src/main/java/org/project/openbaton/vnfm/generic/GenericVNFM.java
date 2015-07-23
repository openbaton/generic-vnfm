package org.project.openbaton.vnfm.generic;

import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.springframework.boot.SpringApplication;

import javax.jms.JMSException;
import java.util.Set;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{
    @Override
    public void instantiate(VirtualNetworkFunctionRecord vnfr) {

        log.info("Instantiation of VirtualNetworkFunctionRecord " + vnfr.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + vnfr);
        boolean allocate=false;

        for (LifecycleEvent event : vnfr.getLifecycle_event())
        {
            if (event.getEvent() == Event.ALLOCATE)
            {

                //Request validation and & processing (MANO: B.3.1.2 step 5)


                sendMessageToCore(Action.ALLOCATE_RESOURCES,vnfr);
                allocate=true;
            }
        }

        if(!allocate)
        {
            for (LifecycleEvent event : vnfr.getLifecycle_event())
            {
                if (event.getEvent() == Event.INSTALL)
                {
                    toEMS(event.getLifecycle_events());
                    sendMessageToCore(Action.INSTANTIATE_FINISH,vnfr);
                    break;
                }
            }
        }

    }

    private void sendMessageToCore(Action action, VirtualNetworkFunctionRecord payload){
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(action);
        coreMessage.setPayload(payload);
        this.sendMessageToQueue("vnfm-core-actions", coreMessage);
    }

    private void toEMS(Set<String> commands){

        for(String command : commands){
            String answer=null;
            try
            {
                answer = sendAndReceiveStringMessage("ems-vnfm-actions","vnfm-ems-actions",command);
            } catch (JMSException e)
            {
                e.printStackTrace();
            }
            log.debug("Received from EMS: " + answer);
        }

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
    public void modify(VirtualNetworkFunctionRecord vnfr) {

    }

    @Override
    public void upgradeSoftware() {

    }

    @Override
    public void terminate() {

    }

    public static void main(String[] args) {
        SpringApplication.run(GenericVNFM.class);
    }
}
