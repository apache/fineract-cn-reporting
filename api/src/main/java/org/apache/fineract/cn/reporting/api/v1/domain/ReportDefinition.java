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

import java.util.List;

public class ReportDefinition {

  private String identifier;
  private String name;
  private String description;
  private List<QueryParameter> queryParameters;
  private List<DisplayableField> displayableFields;

  public ReportDefinition() {
    super();
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public void setIdentifier(final String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public List<QueryParameter> getQueryParameters() {
    return this.queryParameters;
  }

  public void setQueryParameters(final List<QueryParameter> queryParameters) {
    this.queryParameters = queryParameters;
  }

  public List<DisplayableField> getDisplayableFields() {
    return this.displayableFields;
  }

  public void setDisplayableFields(final List<DisplayableField> displayableFields) {
    this.displayableFields = displayableFields;
  }
}
