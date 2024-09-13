package com.oracle.oci.intellij;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Utils {
  public static File pre_processFile(File file) throws IOException {
    Path tempFile = Files.createTempFile("pre-processed-config-file",".temp");
    try(BufferedReader bufferedReader = Files.newBufferedReader(file.toPath())){
      List<String> lines = new ArrayList<>();
      String line ;
      while ((line = bufferedReader.readLine())!=null){
        String trimmedLine = line.trim();

        int splitIndex = trimmedLine.indexOf("=");
        if (splitIndex!=-1 && !trimmedLine.startsWith("#")){
          final String key = trimmedLine.substring(0, splitIndex).trim();
          // Trim the value.
          String value = trimmedLine.substring(splitIndex + 1).trim();

          // Check if the value contains an environment variable placeholder like ${...}
          if (value.startsWith("${") && value.endsWith("}")) {
            String envVarName = value.substring(2, value.length() - 1);
            // Fetch the environment variable value
            String envVarValue = System.getenv(envVarName);
            if (envVarValue != null) {
              StringBuilder trimmedLineBuilder = new StringBuilder(trimmedLine);
              trimmedLineBuilder.replace(splitIndex+2,envVarValue.length(),envVarValue);
              trimmedLine = trimmedLineBuilder.toString();
            } else {
              throw new IllegalStateException("Environment variable " + envVarName + " not found for key " + key);
            }
          }
        }

        lines.add(trimmedLine);
      }
      Files.write(tempFile,lines);
    }catch (Exception e){
      throw new RuntimeException(e);
    }
    return tempFile.toFile();
  }
}
