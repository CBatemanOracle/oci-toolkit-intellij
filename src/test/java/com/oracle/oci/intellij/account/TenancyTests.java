package com.oracle.oci.intellij.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData.ConfigFileTenancyDataSource;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyDataFactory;
import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.account.tenancy.TenancyDataSource;
import com.oracle.oci.intellij.account.tenancy.TenancyManager;



public class TenancyTests {

  private static final File CONFIG_FILE = new File("./tests/resources/internal/config");
  private static List<ConfigFileTenancyData> CONFIGS;
  
  private static TenancyDataSource IDE_CONFIG;
  private static TenancyDataSource IDE_ADMIN_CONFIG;
  private static TenancyDataSource DEFAULT_CONFIG;

  @BeforeClass
  public static void beforeClass() throws IOException {
    CONFIGS =
      ConfigFileTenancyDataFactory.load(CONFIG_FILE);
    IDE_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE, "IDE");
    IDE_ADMIN_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE, "IDE-ADMIN");
    DEFAULT_CONFIG = ConfigFileTenancyDataSource.create(CONFIG_FILE, "DEFAULT");
  }

  private TenancyManager tenancyManager;

  @Before
  public void before() {
    this.tenancyManager = new TenancyManager();
    for (TenancyData tdata : CONFIGS) {
      this.tenancyManager.addTenancyData(tdata);
    }
  }

  @Test
  public void testConfigFileTenancy() throws IOException {
    Collection<TenancyData> allTenancies = this.tenancyManager.getAllTenancies();
    assertEquals(3, allTenancies.size());
    Set<TenancyDataSource> tenancies = 
      new HashSet<TenancyDataSource>(Set.of(IDE_CONFIG, IDE_ADMIN_CONFIG, DEFAULT_CONFIG));
    for (TenancyData tenancy : allTenancies) {
      assertTrue(tenancy instanceof ConfigFileTenancyData);
      ConfigFileTenancyData data = (ConfigFileTenancyData) tenancy;
      assertTrue(tenancies.contains(data.getSource()));
      tenancies.remove(data.getSource());
    }
    assertEquals(0, tenancies.size());
  }
  
  public static void main(String[] args) {
    System.out.println(System.getProperty("FOO"));
  }
}
