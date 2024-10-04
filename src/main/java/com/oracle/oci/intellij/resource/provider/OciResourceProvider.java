package com.oracle.oci.intellij.resource.provider;

import java.io.IOException;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.oci.intellij.account.authentication.AuthenticationDetailsFactory;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;

abstract public class OciResourceProvider<TO> { //implements ResourceFactory<TO> {
    AuthenticationDetailsFactory authenticationProviderFactory  = new AuthenticationDetailsFactory();

    protected abstract Resource<TO> request(ParameterSet parent, AbstractAuthenticationDetailsProvider authenticationProvider);

    public Resource<TO> request(ParameterSet parent) throws IOException {
        AbstractAuthenticationDetailsProvider authenticationProvider = authenticationProviderFactory.request(parent).getContent();
        return request(parent,authenticationProvider);
    }

    

}
