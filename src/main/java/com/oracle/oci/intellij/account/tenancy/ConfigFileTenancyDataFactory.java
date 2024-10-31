package com.oracle.oci.intellij.account.tenancy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.oracle.oci.intellij.account.ConfigFileHandler;
import com.oracle.oci.intellij.account.ConfigFileHandler.Profile;
import com.oracle.oci.intellij.account.ConfigFileHandler.ProfileSet;
import com.oracle.oci.intellij.account.tenancy.ConfigFileTenancyData.ConfigFileTenancyDataSource;

public class ConfigFileTenancyDataFactory {
  public static List<ConfigFileTenancyData> load(File configFile) throws IOException {
    ProfileSet profileSet = ConfigFileHandler.parse(configFile.getAbsolutePath());
    List<ConfigFileTenancyData> profiles = new ArrayList<ConfigFileTenancyData>();
    for (String profileName : profileSet.getProfileNames()) {
      Profile profile = profileSet.get(profileName);
      ConfigFileTenancyData data = createFromProfile(configFile.getAbsolutePath(), profile);
      profiles.add(data);
    }
    return profiles;
  }
  
  public static ConfigFileTenancyData load(ConfigFileTenancyDataSource dataSource) throws IOException {
    ProfileSet profileSet = ConfigFileHandler.parse(dataSource.getConfigFile().getAbsolutePath());
    Profile profile = profileSet.get(dataSource.getProfile());
    return createFromProfile(dataSource.getConfigFile().getAbsolutePath(), profile);
  }

  private static ConfigFileTenancyData createFromProfile(String configFile, Profile profile) {
    ConfigFileTenancyData data = 
      new ConfigFileTenancyData(new File(configFile), profile.getName());
    data.setId(profile.get("tenancy"));
    String name = profile.get("name");
    data.setName(name != null ? name : profile.getName());
    data.setHomeRegionKey(profile.get("region"));
    data.setUserId(profile.get("user"));
    data.setKeyFile(profile.get("key_file"));
    
    return data;
  }
}
