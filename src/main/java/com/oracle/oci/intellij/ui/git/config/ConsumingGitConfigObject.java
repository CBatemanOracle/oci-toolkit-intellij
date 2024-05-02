package com.oracle.oci.intellij.ui.git.config;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.intellij.util.containers.hash.LinkedHashMap;
import com.intellij.util.net.IOExceptionDialog;
import com.oracle.oci.intellij.ui.git.config.GitParser.GitParserException;
import com.oracle.oci.intellij.util.StrippingLineNumberReader;

public abstract class ConsumingGitConfigObject {
  private final LinkedHashMap<String, String> values = new LinkedHashMap<>();
  
  public String getValue(String key) {
    return this.values.get(key);
  }

  protected void consume(StrippingLineNumberReader reader) throws GitParserException {
    boolean consumedLastLine = true;
    try {
      startLine(reader);
      while (consumedLastLine = consumeLine(reader)) {
        startLine(reader);
      }
    } catch (IOException ioe) {
      throw new GitParserException(ioe);
    } finally {
      if (!consumedLastLine) {
        try {
          endLine(reader);
        } catch (IOException e) {
          try {
            // just ignore it if we've hit EOF
            if (reader.read() > -1) {
              throw new GitParserException(e);
            }
          }
          catch (EOFException ioe) {
            // ignore
          }
          catch (IOException ioe) {
            throw new GitParserException(ioe);
            
          }
        }
      }
    }
  }

  protected void startLine(StrippingLineNumberReader reader) throws IOException {
    reader.mark(1024);
  }

  private void endLine(StrippingLineNumberReader reader) throws IOException {
    reader.reset();
  }

  protected boolean consumeLine(StrippingLineNumberReader reader) throws IOException {
    reader.mark(0);
    String strippedLine = reader.readLine();
    if (strippedLine == null) {
      return false;
    }

    // don't consume the line (push it back) if it's a new section
    // a false return will cause it to wind back the line.
    if (strippedLine.charAt(0) == '[') {
      return false;
    }
    // ignore comments
    if (strippedLine.charAt(0) == ';' || strippedLine.charAt(0) == '#') {
      return true;
    }
    // String[] split = strippedLine.split("=");
    int lineSplit = strippedLine.indexOf("=");
    if (lineSplit >= 0) {
      String key = strippedLine.substring(0, lineSplit);
      String value = strippedLine.substring(lineSplit + 1);
      value = value.strip();
      consumeKeyValue(key.strip(), value);
      return true;
    }
    return strippedLine.length() == 0 || strippedLine.charAt(0) != '[';
  }

  protected void consumeKeyValue(String key, String value) {
    this.values.put(key, value);
  }

  public Map<String, String> getValues() {
    return Collections.unmodifiableMap(values);
  }
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(getSectionLine());
    builder.append('\n');
    getValues().forEach((k,v) -> builder.append(String.format("\t%s = %s\n", k, v)));
    return builder.toString();
  }

  protected abstract String getSectionLine();

  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    return keysAreEqual((ConsumingGitConfigObject)obj);
  }

  protected abstract boolean keysAreEqual(ConsumingGitConfigObject obj);
}