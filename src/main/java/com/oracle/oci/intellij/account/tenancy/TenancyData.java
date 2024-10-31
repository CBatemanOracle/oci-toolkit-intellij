package com.oracle.oci.intellij.account.tenancy;

import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

public abstract class TenancyData {
  String id;
  String name;
  String description;
  String homeRegionKey;
  private AtomicBoolean disposed = new AtomicBoolean();

//  java.util.Map<String, String> freeformTags,
//  java.util.Map<String, java.util.Map<String, Object>> definedTags
  public String getId() {
    return id;
  }
  public String getName() {
    return name;
  }
  public String getDescription() {
    return description;
  }
  public String getHomeRegionKey() {
    return homeRegionKey;
  }
  public void setId(String id) {
    this.id = id;
  }
  protected void setName(String name) {
    this.name = name;
  }
  protected void setDescription(String description) {
    this.description = description;
  }
  protected void setHomeRegionKey(String homeRegionKey) {
    this.homeRegionKey = homeRegionKey;
  }
 public abstract TenancyDataSource getSource();
  
  public void dispose() {
    if (this.disposed.compareAndSet(false, true)) {
      doDispose();
    }
  }

  @Override
  public String toString() {
    return getName();
  }
  protected void doDispose() {
    // sub-classes should extend.
  }
  
  public abstract BasicAuthenticationDetailsProvider toAuthProvider() throws Exception;
  
}

