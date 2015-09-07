package org.project.openbaton.vnfm.generic;


import com.google.gson.JsonObject;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.common.VNFDeploymentFlavour;
import org.project.openbaton.catalogue.mano.descriptor.VirtualLinkDescriptor;
import org.project.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.ConfigurationParameter;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.catalogue.nfvo.DependencyParameters;
import org.project.openbaton.common.vnfm_sdk.exception.BadFormatException;
import org.project.openbaton.common.vnfm_sdk.exception.NotFoundException;
import org.project.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.project.openbaton.common.vnfm_sdk.utils.VNFRUtils;
import org.springframework.boot.SpringApplication;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{

    public GenericVNFM(){

    }

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionDescriptor vnfd, VNFDeploymentFlavour deploymentFlavour, String vnfInstanceName, List<VirtualLinkDescriptor> virtualLinkDescriptors, Map<String,String> extention){

        VirtualNetworkFunctionRecord vnfr=null;

        try {
            vnfr = VNFRUtils.createVirtualNetworkFunctionRecord(vnfd, extention.get("nsr-id"));
        } catch (NotFoundException e) {
            e.printStackTrace();
            sendToNfvo(getCoreMessage(Action.ERROR, vnfr));
            return null;
        } catch (BadFormatException e) {
            e.printStackTrace();
            sendToNfvo(getCoreMessage(Action.ERROR, vnfr));
            return null;
        }


        log.info("Instantiation of VirtualNetworkFunctionRecord " + vnfr.getName());
        //log.trace("Instantiation of VirtualNetworkFunctionRecord " + vnfr);
        boolean allocate=false;


        if (getLifecycleEvent(vnfr.getLifecycle_event(),Event.ALLOCATE) != null)
            if (getLifecycleEvent(vnfr.getLifecycle_event_history(), Event.ALLOCATE) != null)
                allocate=false;
            else
                allocate = true;


        if(!allocate)
        {
            LifecycleEvent le = getLifecycleEvent(vnfr.getLifecycle_event_history(), Event.INSTANTIATE);
            if (le != null)
            {
                for (String script : le.getLifecycle_events()) {

                    String command = getJsonObject("EXECUTE", script).toString();
                    log.debug("Sending command: " + command);


                    /*try {
                        sendToEmsAndUpdate(vnfr, le.getEvent(), command, "generic");
                    } catch (JMSException e) {
                        e.printStackTrace();
                        sendToNfvo(getCoreMessage(Action.ERROR, vnfr));
                        return null;
                    } catch (VnfmSdkException e) {
                        e.printStackTrace();
                        sendToNfvo(getCoreMessage(Action.ERROR, vnfr));
                        return null;
                    }*/
                }
            }

        }
        //else  return getCoreMessage(Action.ALLOCATE_RESOURCES, vnfr);


        /**
         * Before ending, need to get all the "provides" filled
         */

        log.debug("Provides is: " + vnfr.getProvides());
        for (ConfigurationParameter configurationParameter : vnfr.getProvides().getConfigurationParameters()){
            if (!configurationParameter.getConfKey().startsWith("nfvo:")){
                configurationParameter.setValue("" + ((int) (Math.random() * 100)));
                log.debug("Setting: "+configurationParameter.getConfKey()+" with value: "+configurationParameter.getValue());
            }
        }

        //log.debug("waiting for send CONFIGURATION UPDATE to EMS");

        try {
            Thread.sleep(1000* ((int )(Math.random() * 3 + 1))  );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

        return vnfr;
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
    public CoreMessage modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency) {
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
        try {
            Thread.sleep(3000 + ((int) (Math.random()*7000)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateVnfr(virtualNetworkFunctionRecord, Event.CONFIGURE);
        return getCoreMessage(Action.MODIFY, virtualNetworkFunctionRecord);
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
    public CoreMessage terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        return null;
    }

    @Override
    public CoreMessage handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        return null;
    }

    @Override
    protected CoreMessage start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {

        log.debug("Starting vnfr: " + virtualNetworkFunctionRecord.getName());
        try {
            Thread.sleep(3000 + ((int) (Math.random()*7000)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getCoreMessage(Action.START,virtualNetworkFunctionRecord);
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

    @Override
    public void NotifyChange() {

    }
}
