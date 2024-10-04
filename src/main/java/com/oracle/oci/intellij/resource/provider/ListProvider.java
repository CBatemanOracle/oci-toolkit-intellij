package com.oracle.oci.intellij.resource.provider;

import java.util.List;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;

public abstract class ListProvider<T,S> extends OciResourceProvider<List<T>> {

  @Override
  protected Resource<List<T>> request(ParameterSet parent,
                                      AbstractAuthenticationDetailsProvider authenticationProvider) {
    // TODO Auto-generated method stub
    return null;
  }

  public abstract Resource<List<T>> list(S source);
}
