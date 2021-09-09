package com.github.mcollovati.vertxvaadin.fusion.data.endpoint;

import com.github.mcollovati.vertxvaadin.fusion.data.CrudEndpoint;
import com.github.mcollovati.vertxvaadin.fusion.data.entity.Address;
import com.github.mcollovati.vertxvaadin.fusion.data.service.AddressService;
import com.github.mcollovati.vertxvaadin.fusion.data.service.InMemoryAddressRepository;
import com.vaadin.fusion.Endpoint;

@Endpoint
public class AddressEndpoint extends CrudEndpoint<Address, Integer> {

    private AddressService service;

    public AddressEndpoint(AddressService service) {
        this.service = service;
    }

    public AddressEndpoint() {
        this(new AddressService(new InMemoryAddressRepository()));
    }

    @Override
    protected AddressService getService() {
        return service;
    }

}
