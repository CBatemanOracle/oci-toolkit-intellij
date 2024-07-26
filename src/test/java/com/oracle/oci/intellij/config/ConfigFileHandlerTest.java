package com.oracle.oci.intellij.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.oci.intellij.account.ConfigFileHandler;
import com.oracle.oci.intellij.account.ConfigFileHandler.Profile;

public class ConfigFileHandlerTest {
  
private static File CONFIG_FILE;
  
  @BeforeClass
  public static void beforeClass() {
    CONFIG_FILE = new File("./src/test/resources/oci.config");
    assertTrue(CONFIG_FILE.isFile());
  }
  
  @Before
  public void before() {
    
  }
  
  @Test
  public void testProfileNames() throws IOException {
    ConfigFileHandler.ProfileSet profileSet = 
      ConfigFileHandler.parse(CONFIG_FILE.getAbsolutePath());
    assertNotNull(profileSet);
    
    Set<String> actualProfileNames = profileSet.getProfileNames();
    List<String> expectedProfileNames = ExpectedConfigValues.getExpectedProfileNames();
    assertEquals(expectedProfileNames.size(), profileSet.getProfileNames().size());
    actualProfileNames.forEach(pname -> assertTrue(expectedProfileNames.contains(pname)));
  }

  @Test
  public void testDefaultConfig() throws IOException {
    verifyProfile("DEFAULT", ExpectedConfigValues.createDefaultExpectedMap());
  }
  
  @Test
  public void testAshburnConfig() throws IOException {
    Map<String, String> expectedMap = ExpectedConfigValues.createDefaultExpectedMap();
    // ashburn just overrides region
    expectedMap.put("region", "us-ashburn-1");
    verifyProfile("ASHBURN", expectedMap);
  }
  
  private void verifyProfile(String profileName, Map<String,String> expectedMap) throws IOException {
    ConfigFileHandler.ProfileSet profileSet = 
      ConfigFileHandler.parse(CONFIG_FILE.getAbsolutePath());
    assertNotNull(profileSet);

    Profile profile = profileSet.get(profileName);
    assertNotNull(profile);
    
    assertEquals(profileName, profile.getName());
    Properties actualKeyVal = profile.getEntries();
    assertEquals(expectedMap.size(), actualKeyVal.size());
    actualKeyVal.forEach((key,val) -> 
      {
        assertEquals(expectedMap.get(key), val);
      });

  }
}
