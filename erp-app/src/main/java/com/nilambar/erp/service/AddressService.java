package com.nilambar.erp.service;

import com.nilambar.erp.domain.Address;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.dto.AddressForm;
import com.nilambar.erp.repository.AddressRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<Address> listFor(User user) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdAsc(user.getId());
    }

    @Transactional(readOnly = true)
    public Address require(User user, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new BusinessException("Address not found."));
    }

    @Transactional
    public Address create(User user, AddressForm form) {
        Address address = new Address();
        address.setUser(user);
        apply(form, address);

        boolean first = addressRepository.findByUserIdOrderByDefaultAddressDescIdAsc(user.getId()).isEmpty();
        if (first || form.isMakeDefault()) {
            addressRepository.clearDefaultFor(user.getId());
            address.setDefaultAddress(true);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public void makeDefault(User user, Long addressId) {
        Address address = require(user, addressId);
        addressRepository.clearDefaultFor(user.getId());
        address.setDefaultAddress(true);
    }

    @Transactional
    public void delete(User user, Long addressId) {
        Address address = require(user, addressId);
        addressRepository.delete(address);
    }

    private void apply(AddressForm form, Address address) {
        address.setLabel(form.getLabel());
        address.setLine1(form.getLine1());
        address.setLine2(form.getLine2());
        address.setCity(form.getCity());
        address.setState(form.getState());
        address.setPincode(form.getPincode());
        address.setLatitude(form.getLatitude());
        address.setLongitude(form.getLongitude());
    }
}
