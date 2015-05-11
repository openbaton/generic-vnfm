package org.project.neutrino.nfvo.core.nfvo_core;

import org.project.neutrino.nfvo.catalogue.mano.descriptor.NetworkServiceDescriptor;
import org.project.neutrino.nfvo.repositories_interfaces.GenericRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.persistence.NoResultException;
import java.util.List;

/**
 * Created by lto on 11/05/15.
 */
@Service
@Scope // TODO think if singleton or prototype
public class NetworkServiceDescriptorManagement implements org.project.neutrino.nfvo.core.interfaces.NetworkServiceDescriptorManagement {

    @Autowired
    @Qualifier("NSDRepository")
    private GenericRepository<NetworkServiceDescriptor> nsdRepository;

    /**
     * This operation allows submitting and
     * validating a Network Service	Descriptor (NSD),
     * including any related VNFFGD and VLD.
     */
    @Override
    public NetworkServiceDescriptor onboard(NetworkServiceDescriptor networkServiceDescriptor) {
        return nsdRepository.create(networkServiceDescriptor);
    }

    /**
     * This operation allows disabling a
     * Network Service Descriptor, so that it
     * is not possible to instantiate it any
     * further.
     * @param id: the id of the {@Link NetworkServiceDescriptor} to disable
     */
    @Override
    public boolean disable(String id) throws NoResultException {
        NetworkServiceDescriptor networkServiceDescriptor = nsdRepository.find(id);
        networkServiceDescriptor.setEnabled(false);
        return networkServiceDescriptor.isEnabled();
    }

    /**
     * This operation allows enabling a
     * Network Service Descriptor.
     * @param id: the id of the {@Link NetworkServiceDescriptor} to enable
     */
    @Override
    public boolean enable(String id) throws NoResultException {
        NetworkServiceDescriptor networkServiceDescriptor = nsdRepository.find(id);
        networkServiceDescriptor.setEnabled(true);
        return networkServiceDescriptor.isEnabled();
    }

    /**
     * This operation allows updating a Network
     * Service Descriptor (NSD), including any
     * related VNFFGD and VLD.This update might
     * include creating/deleting new VNFFGDs
     * and/or new VLDs.
     * @param new_nsd: the new values to be updated
     * @param old_id: the id of the old NetworkServiceDescriptor
     */
    @Override
    public NetworkServiceDescriptor update(NetworkServiceDescriptor new_nsd, String old_id) {
        throw new NotImplementedException();
    }

    /**
     * This operation is used to query the
     * information of the Network Service
     * Descriptor (NSD), including any
     * related VNFFGD and VLD.
     */
    @Override
    public List<NetworkServiceDescriptor> query() {
        return nsdRepository.findAll();
    }

    /**
     * This operation is used to query the
     * information of the Network Service
     * Descriptor (NSD), including any
     * related VNFFGD and VLD.
     */
    @Override
    public NetworkServiceDescriptor query(String id){
        return nsdRepository.find(id);
    }

    /**
     * This operation is used to remove a
     * disabled Network Service Descriptor.
     * @param id
     */
    @Override
    public void delete(String id) {
        nsdRepository.remove(nsdRepository.find(id));
    }
}
