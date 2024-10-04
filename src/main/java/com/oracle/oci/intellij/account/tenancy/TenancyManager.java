package com.oracle.oci.intellij.account.tenancy;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.jetbrains.annotations.NotNull;

import com.intellij.util.xmlb.annotations.Transient;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData.ConfigFileTenancyDataSource;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.cache.CachedResourceFactory;
import com.oracle.oci.intellij.resource.provider.ListProvider;

public class TenancyManager {
  public static class State {
    private String configFile;
    private String profile;
    @Transient
    private TenancyData curTenancyData;
    @Transient
    private ConfigFileTenancyDataSource curTenancySource;
    public TenancyDataSource getCurTenancySource() {
      return curTenancySource;
    }
    public void init() throws IOException {
      if (profile != null && configFile != null) {
        this.curTenancySource = new ConfigFileTenancyDataSource(new File(configFile), profile);
        this.curTenancyData = ConfigFileTenancyDataFactory.load(this.curTenancySource);
        setCurrentTenancy(curTenancyData);
      }
      else {
        throw new IllegalStateException(String.format("%s, %s", configFile, profile));
      }
    }
    
    public String getConfigFile() {
      return configFile;
    }
    public void setConfigFile(String configFile) {
      this.configFile = configFile;
    }
    public String getProfile() {
      return profile;
    }
    public void setProfile(String profile) {
      this.profile = profile;
    }
    
    @Transient
    public TenancyData getCurrentTenancy() {
      return this.curTenancyData;
    }
    
    @Transient
    public void setCurrentTenancy(TenancyData curTenancyData) {
      this.curTenancyData = curTenancyData;
    }
  }

  private LinkedHashMap<TenancyDataSource, TenancyData> tenancies =
    new LinkedHashMap<>();
  private LinkedHashMap<String, CacheManager> cacheManagers =
    new LinkedHashMap<>();
  private LinkedHashMap<ListTargets, ListProvider<?, ?>> listProviders =
    new LinkedHashMap<>();
  private State state;

  public TenancyManager() {
    this.state = new State();
  }

  public synchronized TenancyData getTenancyData(TenancyDataSource source) {
    return tenancies.get(source);
  }

  public synchronized void addTenancyData(TenancyData data) {
    initTenancy(data);
    tenancies.put(data.getSource(), data);
  }

  private void initTenancy(TenancyData data) {
    addCacheManager(data.getId(), new CacheManager(data));
  }

  public synchronized void setCurrentTenancy(@NotNull TenancyData data) {
    this.state.setCurrentTenancy(data);
  }

  public synchronized TenancyData getCurrentTenancy() {
    return this.state.getCurrentTenancy();
  }

  public synchronized CacheManager addCacheManager(TenancyData forThisTenancy,
                                                   CacheManager cacheManager) {
    return addCacheManager(forThisTenancy.getId(), cacheManager);
  }

  public synchronized CacheManager addCacheManager(String tenancyId,
                                                   CacheManager cacheManager) {
    return this.cacheManagers.put(tenancyId, cacheManager);
  }

  public synchronized CacheManager getCacheManager(TenancyData forThisTenancy) {
    return getCacheManager(forThisTenancy.getId());
  }

  public synchronized CacheManager getCacheManager(String tenancyId) {
    return this.cacheManagers.get(tenancyId);
  }

  public synchronized ListProvider<?, ?> getListProvider(ListTargets target) {
    TenancyData currentTenancy = getCurrentTenancy();
    ListProvider<?, ?> targetListProvider = this.listProviders.get(target);
    if (targetListProvider == null) {
      targetListProvider =
        ListProviderFactory.createListProvider(target, currentTenancy);
      this.listProviders.put(target, targetListProvider);
      CacheManager cacheManager = cacheManagers.get(currentTenancy.getId());
      @SuppressWarnings("unchecked")
      CachedResourceFactory<Resource<?>> cache =
        (CachedResourceFactory<Resource<?>>) cacheManager.getCache(target.getTarget());
      if (cache == null) {
        throw new IllegalStateException("Missing list provider for " + target);
      }
    }
    return targetListProvider;
  }

  public synchronized void putListProvider(ListTargets target,
                                           ListProvider<?, ?> listProvider) {
    this.listProviders.put(target, listProvider);
  }

  public synchronized Collection<TenancyData> getAllTenancies() {
    return Collections.unmodifiableCollection(this.tenancies.values());
  }

  public void setState(State state) throws IOException {
    this.state = state;
    this.state.init();
    this.tenancies.put(state.getCurTenancySource(), state.getCurrentTenancy());
    this.setCurrentTenancy(state.getCurrentTenancy());
  }

  public State getState() {
    return this.state;
  }
}
