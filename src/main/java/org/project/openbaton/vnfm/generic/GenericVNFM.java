package org.project.openbaton.vnfm.generic;


import com.google.gson.JsonObject;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.ConfigurationParameter;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.catalogue.nfvo.DependencyParameters;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.project.openbaton.vnfm.generic.utils.EmsRegistrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;

import java.util.Map;

/**
 * Created by mob on 16.07.15.
 */
public class GenericVNFM extends AbstractVnfmSpringJMS{

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {

        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());

        virtualNetworkFunctionRecord = grantLifecycleOperation(virtualNetworkFunctionRecord);
        if (virtualNetworkFunctionRecord != null) { // if you reach this line than there was no error...
            virtualNetworkFunctionRecord = allocateResources(virtualNetworkFunctionRecord);
            for (Map.Entry<String, String> entry : executeScriptsForEvent(virtualNetworkFunctionRecord, Event.INSTANTIATE).entrySet()) {
                log.info("Executed script: " + entry.getKey());
                log.info("result is: " + entry.getValue());
            }
        }else log.error("Grant lifecycle operation failed");

        Thread.sleep(1000 * ((int) (Math.random() * 3 + 1)));

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
        log.debug("VirtualNetworkFunctionRecord NAME is: " + virtualNetworkFunctionRecord.getName());
        log.debug("Got dependency: " + dependency);
        log.debug("Parameters are: ");
        for (Map.Entry<String, DependencyParameters> entry : dependency.getParameters().entrySet()){
            log.debug("Source type: " + entry.getKey());
            log.debug("Parameters: " + entry.getValue().getParameters());
        }


        LifecycleEvent le = getLifecycleEvent(virtualNetworkFunctionRecord.getLifecycle_event(), Event.CONFIGURE);
        if(le!=null){
            for(String script: le.getLifecycle_events()){
                script = resolveScriptParameters(script,dependency.getParameters());
                log.debug("Script to send to EMS is: "+script);
            }
        }

        for (Map.Entry<String, String> entry : executeScriptsForEvent(virtualNetworkFunctionRecord, Event.CONFIGURE).entrySet()){
            log.info("-----------------------------------------------------------------------");
            log.info("Executed script: \t" + entry.getKey());
            log.info("result is: \n" + entry.getValue());
            log.info("-----------------------------------------------------------------------");
        }

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
    protected void fillSpecificProvides(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        for (ConfigurationParameter configurationParameter : virtualNetworkFunctionRecord.getProvides().getConfigurationParameters()){
            if (!configurationParameter.getConfKey().startsWith("#nfvo:")){
                configurationParameter.setValue("" + ((int) (Math.random() * 100)));
                log.debug("Setting: "+configurationParameter.getConfKey()+" with value: "+configurationParameter.getValue());
            }
        }
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

    public static void main(String[] args) {
        SpringApplication.run(GenericVNFM.class);
    }

    @Override
    public void NotifyChange() {

    }

    @Autowired
    private EmsRegistrator emsRegistrator;

    @Override
    protected void checkEmsStarted(String hostname) {
        if (!emsRegistrator.getHostnames().contains(hostname))
            throw new RuntimeException("no ems for hostame: " + hostname);
    }
}
