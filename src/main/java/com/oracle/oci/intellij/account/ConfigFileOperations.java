/**
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.oci.intellij.account;

//import static com.oracle.bmc.util.internal.FileUtils.expandUserHome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple implementation to read OCI configuration files.
 * <p>
 * Note, config files <b>MUST</b> contain a "DEFAULT" profile, else validation
 * will fail. Additional profiles are optional.
 */
public final class ConfigFileOperations {
  /**
   * Default location of the config file.
   */
  public static final String DEFAULT_CONFIG_FILE_PATH = "config";
  public static final String DEFAULT_CONFIG_FOLDER_PATH = ".oci";

  public static String getConfigFilePath() {
    return SystemPropertiesUtils.userHome() + File.separator
        + DEFAULT_CONFIG_FOLDER_PATH + File.separator
        + DEFAULT_CONFIG_FILE_PATH;
  }

  /**
   * The fallback default location of the config file. If and only if the  does not exist,
   * this fallback default location will be used.
   */
  public static final String FALLBACK_DEFAULT_FILE_PATH = "~/.oraclebmc/config";

  private static final String DEFAULT_PROFILE_NAME = "DEFAULT";

  /**
   * Creates a new ConfigFile instance using the configuration at the default location,
   * using the default profile.
   *
   * @return A new ConfigFile instance.
   * @throws IOException if the file could not be read.
   */
  public static ConfigFile parseDefault() throws IOException {
    return parseDefault(null);
  }

  /**
   * Creates a new ConfigFile instance using the configuration at the default location,
   * using the given profile.
   *
   * @param profile The profile name to load, or null if you want to load the
   *                "DEFAULT" profile.
   * @return A new ConfigFile instance.
   * @throws IOException if the file could not be read.
   */
  public static ConfigFile parseDefault(String profile) throws IOException {
    File effectiveFile = null;

    File defaultFile = new File(expandUserHome(getConfigFilePath()));
    File fallbackDefaultFile = new File(
        expandUserHome(FALLBACK_DEFAULT_FILE_PATH));

    if (defaultFile.exists() && defaultFile.isFile()) {
      effectiveFile = defaultFile;
    }
    else if (fallbackDefaultFile.exists() && fallbackDefaultFile.isFile()) {
      effectiveFile = fallbackDefaultFile;
    }

    if (effectiveFile != null) {
      //LOG.debug("Loading config file from: {}", effectiveFile);
      return parse(effectiveFile.getAbsolutePath(), profile);
    }
    else {
      throw new IOException(String.format(
          "Can't load the default config from '%s' or '%s' because it does not exist or it is not a file.",
          defaultFile.getAbsolutePath(),
          fallbackDefaultFile.getAbsolutePath()));
    }
  }

  private static String expandUserHome(String configFilePath) {
    return configFilePath; // TODO : Change to use BMC utils like eclipse plugin
  }

  /**
   * Create a new instance using a file at a given location.
   * <p>
   * This method is the same as calling {@link #parse(String, String)} with
   * "DEFAULT" as the profile.
   *
   * @param configurationFilePath The path to the config file.
   * @return A new ConfigFile instance.
   * @throws IOException if the file could not be read.
   */
  public static ConfigFile parse(String configurationFilePath)
      throws IOException {
    File f = new File(expandUserHome(configurationFilePath));
    if (!f.exists()) {
      final ConfigAccumulator accumulator = new ConfigAccumulator();
      return new ConfigFile(accumulator, null);
    }
    return parse(configurationFilePath, null);
  }

  /**
   * Create a new instance using a file at a given location.
   *
   * @param configurationFilePath The path to the config file.
   * @param profile               The profile name to load, or null if you want to load the
   *                              "DEFAULT" profile.
   * @return A new ConfigFile instance.
   * @throws IOException if the file could not be read.
   */
  public static ConfigFile parse(String configurationFilePath, String profile)
      throws IOException {
    return parse(
        new FileInputStream(new File(expandUserHome(configurationFilePath))),
        profile);
  }

  /**
   * Create a new instance using an UTF-8 input stream.
   *
   * @param configurationStream The path to the config file.
   * @param profile             The profile name to load, or null if you want to load the
   *                            "DEFAULT" profile.
   * @return A new ConfigFile instance.
   * @throws IOException if the file could not be read.
   */
  public static ConfigFile parse(InputStream configurationStream,
      String profile) throws IOException {
    return parse(configurationStream, profile, StandardCharsets.UTF_8);
  }

  /**
   * Create a new instance using an input stream.
   *
   * @param configurationStream The path to the config file.
   * @param profile             The profile name to load, or null if you want to load the
   *                            "DEFAULT" profile.
   * @param charset             The charset used when parsing the input stream
   * @return A new ConfigFile instance.
   * @throws IOException if the file could not be read.
   */
  public static ConfigFile parse(InputStream configurationStream,
      String profile, Charset charset) throws IOException {
    final ConfigAccumulator accumulator = new ConfigAccumulator();
    try (final BufferedReader reader = new BufferedReader(
        new InputStreamReader(configurationStream, charset))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        accumulator.accept(line);
      }
    }
    if (!accumulator.foundDefaultProfile) {
      throw new IllegalStateException(
          "No DEFAULT profile was specified in the configuration");
    }
    if (profile != null && !accumulator.configurationsByProfile
        .containsKey(profile)) {
      throw new IllegalArgumentException(
          "No profile named " + profile + " exists in the configuration file");
    }

    return new ConfigFile(accumulator, profile);
  }

  // Set Permission to 600
  public static File setConfigFilePermissions(Path filePath)
      throws IOException {
    if (SystemPropertiesUtils.isWindows()) {
      File file = filePath.toFile();
      file.setReadable(true, true);
      file.setWritable(true, true);
      file.setExecutable(false);
    }
    else if (SystemPropertiesUtils.isLinux() || SystemPropertiesUtils.isMac()) {
      Set<PosixFilePermission> perms = new HashSet<>();
      perms.add(PosixFilePermission.OWNER_READ);
      perms.add(PosixFilePermission.OWNER_WRITE);
      Files.setPosixFilePermissions(filePath, perms);
    }
    return filePath.toFile();
  }

  public static void save(String configurationFilePath, ConfigFile profile,
      String profileName) throws IOException {

    StringBuffer formatProfile = new StringBuffer();
    formatProfile.append(System.getProperty("line.separator"));
    formatProfile.append("[" + profileName.toUpperCase() + "]");
    for (String entry : profile.getProfile(profileName).keySet()) {
      String value = profile.get(entry);
      formatProfile.append(System.getProperty("line.separator"));
      formatProfile.append(entry + " = " + value);
    }
    formatProfile.append(System.getProperty("line.separator"));

    File f = new File(expandUserHome(configurationFilePath));

    File directory = f.getParentFile();
    if (!directory.exists()) {
      directory.mkdirs();
    }

    StandardOpenOption option = StandardOpenOption.APPEND;
    if (!f.exists()) {
      option = StandardOpenOption.CREATE;
    }

    try {
      Files.write(Paths.get(expandUserHome(configurationFilePath)),
          formatProfile.toString().getBytes(), option);
      if (option == StandardOpenOption.CREATE)
        setConfigFilePermissions(
            Paths.get(expandUserHome(configurationFilePath)));
    }
    catch (IOException e) {
    }
  }

  private ConfigFileOperations() {
  }

  /**
   * ConfigFile represents a simple lookup mechanism for a OCI config file.
   */
  public static final class ConfigFile {
    private ConfigAccumulator accumulator = null;
    private String profile = null;

    private ConfigFile(ConfigAccumulator accumulator, String profile) {
      this.accumulator = accumulator;
      this.profile = profile;
    }

    /**
     * Gets the value associated with a given key. The value returned will
     * be the one for the selected profile (if available), else the value in
     * the DEFAULT profile (if specified), else null.
     *
     * @param key The key to look up.
     * @return The value, or null if it didn't exist.
     */
    public String get(String key) {
      if (profile != null && (accumulator.configurationsByProfile.get(profile)
          .containsKey(key))) {
        return accumulator.configurationsByProfile.get(profile).get(key);
      }
      return accumulator.configurationsByProfile.get(DEFAULT_PROFILE_NAME)
          .get(key);
    }

    public String get_if_present(String key) {
      return accumulator.configurationsByProfile.get(profile).get(key);
    }

    public Set<String> getProfileNames() {
      return accumulator.configurationsByProfile.keySet();
    }

    public void update(String profileName, Map<String, String> newProfile) {
      accumulator.configurationsByProfile.put(profileName, newProfile);
      this.profile = profileName;
    }

    public Map<String, String> getProfile(String profileName) {
      return accumulator.configurationsByProfile.get(profileName);
    }
  }

  private static final class ConfigAccumulator {
    final Map<String, Map<String, String>> configurationsByProfile = new HashMap<>();

    private String currentProfile = null;
    private boolean foundDefaultProfile = false;

    private void accept(String line) {
      final String trimmedLine = line.trim();

      // no blank lines
      if (trimmedLine.isEmpty()) {
        return;
      }

      // skip comments
      if (trimmedLine.charAt(0) == '#') {
        return;
      }

      if (trimmedLine.charAt(0) == '['
          && trimmedLine.charAt(trimmedLine.length() - 1) == ']') {
        currentProfile = trimmedLine.substring(1, trimmedLine.length() - 1)
            .trim();
        if (currentProfile.isEmpty()) {
          throw new IllegalStateException(
              "Cannot have empty profile name: " + line);
        }
        if (currentProfile.equals(DEFAULT_PROFILE_NAME)) {
          foundDefaultProfile = true;
        }
        if (!configurationsByProfile.containsKey(currentProfile)) {
          configurationsByProfile
              .put(currentProfile, new HashMap<String, String>());
        }

        return;
      }

      final int splitIndex = trimmedLine.indexOf('=');
      if (splitIndex == -1) {
        throw new IllegalStateException(
            "Found line with no key-value pair: " + line);
      }

      final String key = trimmedLine.substring(0, splitIndex).trim();
      final String value = trimmedLine.substring(splitIndex + 1).trim();
      if (key.isEmpty()) {
        throw new IllegalStateException("Found line with no key: " + line);
      }

      if (currentProfile == null) {
        throw new IllegalStateException(
            "Config parse error, attempted to read configuration without specifying a profile: "
                + line);
      }

      configurationsByProfile.get(currentProfile).put(key, value);
    }
  }
}
