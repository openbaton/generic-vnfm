package org.project.openbaton.vnfm.generic;

import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;

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

                CoreMessage coreMessage = new CoreMessage();
                coreMessage.setAction(Action.ALLOCATE_RESOURCES);
                coreMessage.setPayload(vnfr);
                this.sendMessageToQueue("vnfm-core-actions", coreMessage);
                allocate=true;
            }
        }

        if(!allocate)
        {
            for (LifecycleEvent event : vnfr.getLifecycle_event())
            {
                if (event.getEvent() == Event.INSTALL)
                {
                    Set<String> commands = event.getLifecycle_events();
                    //sendMessageToQueue(" ",commands);

                }
            }
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
}
