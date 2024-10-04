package com.oracle.oci.intellij.resource.parameter;

import java.util.Objects;

public class ParameterImp implements Parameter {
  ParametersEnum name ;
  String value ;

  public ParameterImp(ParametersEnum name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name.name();
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParameterImp that = (ParameterImp) o;
    return Objects.equals(name, that.name) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }
}
