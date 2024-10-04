package com.oracle.oci.intellij.account.authentication;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.ResourceFactory;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;
import com.oracle.oci.intellij.resource.parameter.ParametersEnum;

import java.io.IOException;

public class AuthenticationDetailsFactory implements ResourceFactory<AbstractAuthenticationDetailsProvider> {
    //TODO implement a provider or something that can create the main parameters ,current compartment,region,profile,config file ....
    @Override
    public Resource<AbstractAuthenticationDetailsProvider> request(ParameterSet parameterSet) throws IOException {
        String filePath = parameterSet.getParameter(ParametersEnum.FILE_PATH) ;
        String profile = parameterSet.getParameter(ParametersEnum.PROFILE);

        AbstractAuthenticationDetailsProvider authenticationDetailsProvider ;

        if (filePath == null) {
            authenticationDetailsProvider = new ConfigFileAuthenticationDetailsProvider(profile);
        }
        else {
            authenticationDetailsProvider = new ConfigFileAuthenticationDetailsProvider(filePath, profile);
        }
        return ResourceFactory.createNonExpiringResource(authenticationDetailsProvider);
    }


}
