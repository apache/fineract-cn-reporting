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
package org.apache.fineract.cn.reporting.service.spi;

import org.apache.fineract.cn.reporting.api.v1.domain.AutoCompleteResource;
import org.apache.fineract.cn.reporting.api.v1.domain.QueryParameter;
import org.apache.fineract.cn.reporting.api.v1.domain.Type;

import java.util.Arrays;

public class QueryParameterBuilder {

  private String name;
  private Type type;
  private QueryParameter.Operator operator;
  private Boolean mandatory;
  private AutoCompleteResource autoCompleteResource;

  private QueryParameter queryParameter;

  private QueryParameterBuilder(final String name, final Type type) {
    super();
    this.name = name;
    this.type = type;
  }

  public static QueryParameterBuilder create(final String name, final Type type) {
    return new QueryParameterBuilder(name, type);
  }

  public QueryParameterBuilder operator(final QueryParameter.Operator operator) {
    this.operator = operator;
    return this;
  }

  public QueryParameterBuilder mandatory() {
    this.mandatory = Boolean.TRUE;
    return this;
  }

  public QueryParameterBuilder autoComplete(final String path, final String... terms) {
    final AutoCompleteResource autoCompleteResource = new AutoCompleteResource();
    autoCompleteResource.setPath(path);
    autoCompleteResource.setTerms(Arrays.asList(terms));
    this.autoCompleteResource = autoCompleteResource;
    return this;
  }

  public QueryParameter build() {
    final QueryParameter queryParameter = new QueryParameter();
    queryParameter.setName(this.name);
    queryParameter.setType(this.type);
    queryParameter.setOperator(this.operator != null ? this.operator : QueryParameter.Operator.EQUALS);
    queryParameter.setMandatory(this.mandatory != null ? this.mandatory : Boolean.FALSE);
    queryParameter.setAutoCompleteResource(this.autoCompleteResource);
    return queryParameter;
  }
}
