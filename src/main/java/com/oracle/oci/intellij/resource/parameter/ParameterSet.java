package com.oracle.oci.intellij.resource.parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ParameterSet {
  Map<String,Parameter> parameterSet ;

  private ParameterSet(){
    parameterSet = new HashMap<>();
  }
  // TODO: Builder objects have a "build"; or could us "of" chaining like enum
  public static ParameterSet Builder() {
    return new ParameterSet();
  }

  public ParameterSet add(Parameter parameter){
    parameterSet.put(parameter.getName(),parameter);
    return this;
  }

  public String getParameter(ParametersEnum parametersEnum){
    Parameter parameter = parameterSet.get(parametersEnum.name());
    return parameter.getValue();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ParameterSet)) return false;
    ParameterSet that = (ParameterSet) o;
    return Objects.equals(parameterSet, that.parameterSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameterSet);
  }
}
