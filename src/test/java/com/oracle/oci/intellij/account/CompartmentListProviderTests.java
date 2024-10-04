package com.oracle.oci.intellij.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.Compartment.LifecycleState;
import com.oracle.oci.intellij.Utils;
import com.oracle.oci.intellij.account.CacheUtil.LRUCacheMBeanImpl;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData.ConfigFileTenancyDataSource;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyDataFactory;
import com.oracle.oci.intellij.account.tenancy.ListTargets;
import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.account.tenancy.TenancyDataSource;
import com.oracle.oci.intellij.account.tenancy.TenancyManager;
import com.oracle.oci.intellij.account.tenancy.TenancyService;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.cache.CachedResourceFactory;
import com.oracle.oci.intellij.resource.provider.ListProvider;
import com.oracle.oci.intellij.util.ServiceAdapter;

public class CompartmentListProviderTests {

  private static final File CONFIG_FILE = new File("./tests/resources/internal/config");
  private static File CONFIG_FILE_CONVERTED;
  private static List<ConfigFileTenancyData> CONFIGS;
  
  private static TenancyDataSource IDE_CONFIG;
  private static TenancyDataSource IDE_ADMIN_CONFIG;
  private static TenancyDataSource DEFAULT_CONFIG;

  @BeforeClass
  public static void beforeClass() throws IOException {
    CONFIG_FILE_CONVERTED = Utils.pre_processFile(CONFIG_FILE);
    CONFIGS = ConfigFileTenancyDataFactory.load(CONFIG_FILE_CONVERTED);
    IDE_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE_CONVERTED, "IDE");
    IDE_ADMIN_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE_CONVERTED, "IDE-ADMIN");
    DEFAULT_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE_CONVERTED, "DEFAULT");
    
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
    TenancyService ts = new TenancyService();
    tenancyManager = ts.getTenancyManager();
    ServiceAdapter.setInstance(new ServiceAdapter() {

      @SuppressWarnings("unchecked")
      @Override
      public <T> T getAppService(Class<T> adapterClass) {
        if (adapterClass == TenancyService.class) {
          return (T) ts;
        }
        return null;
      }
      
    });
    for (ConfigFileTenancyData data : CONFIGS) {
      tenancyManager.addTenancyData(data);
    }
    
    
    TenancyData curData = tenancyManager.getTenancyData(DEFAULT_CONFIG);
    assertNotNull(curData);
    tenancyManager.setCurrentTenancy(curData);

  }

  @Test
  public void testGetListDefault() throws Exception {
    TenancyData curData = tenancyManager.getCurrentTenancy();
    String compartmentId = curData.getId();
    Compartment rootComp = Compartment.builder()
      .compartmentId(curData.getId())
      .id(compartmentId)
      .name("[[ROOT]]")
      .lifecycleState(LifecycleState.Active)
      .build();
    tenancyManager.setCurrentTenancy(curData);

    @SuppressWarnings("unchecked")
    ListProvider<Compartment, Compartment> listProvider = 
      (ListProvider<Compartment, Compartment>) 
         tenancyManager.getListProvider(ListTargets.COMPARTMENTS);
    Resource<List<Compartment>> childCompartments = listProvider.list(rootComp);
    System.out.println(childCompartments.getContent().size());
    childCompartments.getContent().forEach(c -> System.out.println(c.getId()));
    
    CachedResourceFactory<?> factory = 
      tenancyManager.getCacheManager(curData).getCache(Compartment.class);
    CacheUtil.LRUCacheMBeanImpl mbean = new LRUCacheMBeanImpl(factory);
    assertEquals(0, mbean.getHits());
    assertEquals(1, mbean.getMisses());

    childCompartments = listProvider.list(rootComp);
    assertFalse(childCompartments.getContent().isEmpty());
    assertEquals(1, mbean.getHits());
    assertEquals(1, mbean.getMisses());
  }
}
