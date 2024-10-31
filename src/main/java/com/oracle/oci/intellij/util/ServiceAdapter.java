package com.oracle.oci.intellij.util;

import com.intellij.openapi.application.ApplicationManager;

public class ServiceAdapter {
  // not final so a test class can override;
  private static ServiceAdapter INSTANCE = new ServiceAdapter();
  
  public synchronized static ServiceAdapter getInstance() {
    return INSTANCE;
  }
  
  public synchronized static void setInstance(ServiceAdapter adapter) {
    INSTANCE = adapter;
  }
  
  protected ServiceAdapter() {
    // do nothing; mocks can sub-classs
  }
  // allows manual injection of services for testing.
  public <T> T getAppService(Class<T> adapterClass) {
    return ApplicationManager.getApplication().getService(adapterClass);
  }
}
