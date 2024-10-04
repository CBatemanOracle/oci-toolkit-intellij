package com.oracle.oci.intellij.account.tenancy;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.oci.intellij.account.SystemPreferences;

public class ConfigFileTenancyData extends TenancyData {
  public static class ConfigFileTenancyDataSource extends TenancyDataSource {
    
    public static ConfigFileTenancyDataSource createDefaultConfig(String profile) {
      return create(new File(SystemPreferences.DEFAULT_CONFIG_FILE_PATH), profile);
    }
    public static ConfigFileTenancyDataSource create(File configFile, String profile) {
      return new ConfigFileTenancyDataSource(configFile, profile);
    }
    private final File configFile;
    private final String profile;

    public ConfigFileTenancyDataSource(File configFile, String profile) {
      File absFile = null;
      try {
        absFile = configFile.getCanonicalFile();
      } catch (IOException e) {
        absFile = configFile.getAbsoluteFile();
      }
      this.configFile = absFile;
      this.profile = profile;
    }
    
    
    public File getConfigFile() {
      return configFile;
    }
    public String getProfile() {
      return profile;
    }
    @Override
    public int hashCode() {
      return Objects.hash(configFile, profile);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (obj == this) return true;
      
      if (!(obj instanceof ConfigFileTenancyDataSource)) {
        return false;
      }
      
      return Objects.equals(configFile, ((ConfigFileTenancyDataSource)obj).configFile) 
              && Objects.equals(profile, ((ConfigFileTenancyDataSource)obj).profile);
    }

    @Override
    public String toString() {
      return String.format("%s_%s", configFile.toString(), profile);
    }
    
  }
  private String userId;
  private String keyFile;
  private ConfigFileTenancyDataSource dataSource;
  
  public ConfigFileTenancyData(ConfigFileTenancyDataSource dataSource) {
    this.dataSource = dataSource;
  }
  public ConfigFileTenancyData(File configFile, String profile) {
    this.dataSource = new ConfigFileTenancyDataSource(configFile, profile);
  }

  protected File getConfigFile() {
    return this.dataSource.configFile;
  }

  protected String getProfile() {
    return this.dataSource.profile;
  }

  @Override
  protected void doDispose() {
  }

  protected String getUserId() {
    return userId;
  }

  protected String getKeyFile() {
    return keyFile;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setKeyFile(String keyFile) {
    this.keyFile = keyFile;
  }

  @Override
  public BasicAuthenticationDetailsProvider toAuthProvider() throws IOException {
      return new ConfigFileAuthenticationDetailsProvider(dataSource.configFile.getAbsolutePath(), dataSource.profile);
  }

  @Override
  public TenancyDataSource getSource() {
    return this.dataSource;
  }


}
