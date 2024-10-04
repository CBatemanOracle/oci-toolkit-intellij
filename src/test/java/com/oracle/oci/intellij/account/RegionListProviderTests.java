package com.oracle.oci.intellij.account;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.oracle.bmc.identity.model.RegionSubscription;
import com.oracle.oci.intellij.Utils;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData.ConfigFileTenancyDataSource;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyDataFactory;
import com.oracle.oci.intellij.account.tenancy.ListTargets;
import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.account.tenancy.TenancyDataSource;
import com.oracle.oci.intellij.account.tenancy.TenancyManager;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.provider.ListProvider;
import com.oracle.oci.intellij.util.ServiceAdapter;

public class RegionListProviderTests {

  private static final File CONFIG_FILE = new File("./tests/resources/internal/config");
  private static File CONFIG_FILE_CONVERTED;
  private static List<ConfigFileTenancyData> CONFIGS;
  
  private static TenancyDataSource IDE_CONFIG;
  private static TenancyDataSource IDE_ADMIN_CONFIG;
  private static TenancyDataSource DEFAULT_CONFIG;

  @BeforeClass
  public static void beforeClass() throws IOException {
    CONFIG_FILE_CONVERTED = Utils.pre_processFile(CONFIG_FILE);
    CONFIGS = ConfigFileTenancyDataFactory.load(CONFIG_FILE_CONVERTED);    IDE_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE, "IDE");
    IDE_ADMIN_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE, "IDE-ADMIN");
    DEFAULT_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE, "DEFAULT");
    
    Logger.setFactory(new Logger.Factory() {
      
      @Override
      public @NotNull Logger getLoggerInstance(@NotNull String category) {
        return new DefaultLogger("Fleuh");
      }
    });
    
  }


  private TenancyManager tenancyManager;

  @Before
  public void before() {
    tenancyManager = new TenancyManager();
    ServiceAdapter.setInstance(new ServiceAdapter() {

      @SuppressWarnings("unchecked")
      @Override
      public <T> T getAppService(Class<T> adapterClass) {
        if (adapterClass == TenancyManager.class) {
          return (T) tenancyManager;
        }
        return null;
      }
      
    });
    for (ConfigFileTenancyData data : CONFIGS) {
      tenancyManager.addTenancyData(data);
    }
  }

  // TODO: @Test
  public void testGetListDefault() {
    TenancyData curData = tenancyManager.getTenancyData(DEFAULT_CONFIG);
    tenancyManager.setCurrentTenancy(curData);
    
    
    @SuppressWarnings("unchecked")
    ListProvider<RegionSubscription, TenancyData> listProvider = 
      (ListProvider<RegionSubscription, TenancyData>) 
         tenancyManager.getListProvider(ListTargets.REGIONS);
    Resource<List<RegionSubscription>> childCompartments = listProvider.list(curData);
    System.out.println(childCompartments.getContent().size());
    childCompartments.getContent().forEach(r -> System.out.println(r.getRegionName()));

  }
}
