/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.reporting.api.v1.domain;

public class QueryParameter {

  public enum Operator {
    EQUALS,
    IN,
    LIKE,
    BETWEEN,
    GREATER,
    LESSER
  }

  private String name;
  private Type type;
  private Operator operator;
  private String value;
  private Boolean mandatory;
  private AutoCompleteResource autoCompleteResource;

  public QueryParameter() {
    super();
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Type getType() {
    return this.type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  public Operator getOperator() {
    return this.operator;
  }

  public void setOperator(final Operator operator) {
    this.operator = operator;
  }

  public String getValue() {
    return this.value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

  public Boolean getMandatory() {
    return this.mandatory;
  }

  public void setMandatory(final Boolean mandatory) {
    this.mandatory = mandatory;
  }

  public AutoCompleteResource getAutoCompleteResource() {
    return this.autoCompleteResource;
  }

  public void setAutoCompleteResource(final AutoCompleteResource autoCompleteResource) {
    this.autoCompleteResource = autoCompleteResource;
  }
}
