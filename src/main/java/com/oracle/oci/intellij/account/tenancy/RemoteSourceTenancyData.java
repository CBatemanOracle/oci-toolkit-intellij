package com.oracle.oci.intellij.account.tenancy;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

public class RemoteSourceTenancyData extends TenancyData {

//TODO;
  @Override
  public BasicAuthenticationDetailsProvider toAuthProvider() throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public TenancyDataSource getSource() {
    throw new UnsupportedOperationException();
  }
}
