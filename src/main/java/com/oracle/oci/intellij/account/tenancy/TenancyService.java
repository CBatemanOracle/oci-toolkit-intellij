package com.oracle.oci.intellij.account.tenancy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData.ConfigFileTenancyDataSource;
import com.oracle.oci.intellij.util.LogHandler;
import com.oracle.oci.intellij.util.ServiceAdapter;

@Service(Service.Level.APP)
@State(name = "OCITenancyService",
        storages = @Storage(value = "oracleocitoolkit_tenancy.xml"))
public final class TenancyService  implements PersistentStateComponent<TenancyManager.State>, PropertyChangeListener {
  
  
  private TenancyManager tenancyManager = new TenancyManager();

  public TenancyService() {
    // TODO
  }
  // need a migration strategy
  public TenancyManager getTenancyManager() {
    return this.tenancyManager;
  }


  public static TenancyService getInstance() {
    TenancyService service =
      ServiceAdapter.getInstance().getAppService(TenancyService.class);
    return service;
  }

  @Override
  public TenancyManager.State getState() {
    return tenancyManager.getState();
  }

  @Override
  public void loadState(TenancyManager.State state) {
    // loaded from storage.
    try {
      this.tenancyManager.setState(state);
      this.tenancyManager.addTenancyData(state.getCurrentTenancy());
    } catch (IOException e) {
      LogHandler.error("Error loading tenancy manager data", e);
    }
  }

  @Override
  public void noStateLoaded() {
    TenancyManager.State state = new TenancyManager.State();
    String configFilePath = SystemPreferences.getConfigFilePath();
    state.setConfigFile(configFilePath);
    String profile = SystemPreferences.getProfileName();
    state.setProfile(profile);
    try {
      this.tenancyManager.setState(state);
      this.tenancyManager.addTenancyData(state.getCurrentTenancy());
    } catch (IOException e) {
      LogHandler.error("Error loading tenancy manager data", e);
    }
  }

  @Override
  public void initializeComponent() {
    // TODO Auto-generated method stub
    PersistentStateComponent.super.initializeComponent();
  }
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    System.out.println(evt);
    if (SystemPreferences.EVENT_REGION_UPDATE.equals(evt.getPropertyName())) {
      tenancyManager.setCurrentRegion((String)evt.getNewValue());
    } 
    else if (SystemPreferences.EVENT_SETTINGS_UPDATE.equals(evt.getPropertyName())) {
      try {
        ConfigFileTenancyDataSource configFileTenancyDataSource = 
          ConfigFileTenancyDataSource.create(new File(SystemPreferences.getConfigFilePath()), 
                                           SystemPreferences.getProfileName());
        ConfigFileTenancyData configFileTenancyData = 
          ConfigFileTenancyDataFactory.load(configFileTenancyDataSource);
        tenancyManager.setCurrentRegion(SystemPreferences.getRegionName());
        this.getState().setConfigFile(SystemPreferences.getConfigFilePath());
        this.getState().setProfile(SystemPreferences.getProfileName());
        tenancyManager.addTenancyData(configFileTenancyData);
        tenancyManager.setCurrentTenancy(configFileTenancyData);
      }
      catch (IOException ioe) {
        LogHandler.error("Error updating property change on tenancy manager", ioe);
      }
    }
    else if (SystemPreferences.EVENT_COMPARTMENT_UPDATE.equals(evt.getPropertyName())) {
      System.out.println(evt);
    }
  }
   
  
}
