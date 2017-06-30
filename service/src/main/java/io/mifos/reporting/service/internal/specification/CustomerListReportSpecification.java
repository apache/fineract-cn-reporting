/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.reporting.service.internal.specification;

import io.mifos.reporting.api.v1.domain.DisplayableField;
import io.mifos.reporting.api.v1.domain.QueryParameter;
import io.mifos.reporting.api.v1.domain.ReportDefinition;
import io.mifos.reporting.api.v1.domain.ReportPage;
import io.mifos.reporting.api.v1.domain.ReportRequest;
import io.mifos.reporting.api.v1.domain.Type;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.DisplayableFieldBuilder;
import io.mifos.reporting.service.spi.QueryParameterBuilder;
import io.mifos.reporting.service.spi.Report;
import io.mifos.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Report(category = "Customer", identifier = "Listing")
public class CustomerListReportSpecification implements ReportSpecification {

  private final Logger logger;
  private final HashMap<String, String> columnMapping = new HashMap<>();

  @Autowired
  public CustomerListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger) {
    super();
    this.logger = logger;
    this.initializeMapping();
  }

  @Override
  public ReportDefinition getReportDefinition() {
    final ReportDefinition reportDefinition = new ReportDefinition();
    reportDefinition.setIdentifier("Listing");
    reportDefinition.setName("Customer Listing");
    reportDefinition.setDescription("List of all customers.");
    reportDefinition.setQueryParameters(this.buildQueryParameters());
    reportDefinition.setDisplayableFields(this.buildDisplayableFields());
    return reportDefinition;
  }

  @Override
  public ReportPage generateReport(final ReportRequest reportRequest) {
    return null;
  }

  @Override
  public void validate(final ReportRequest reportRequest) throws IllegalArgumentException {
    final ArrayList<String> unknownFields =  new ArrayList<>();
    reportRequest.getQueryParameters().forEach(queryParameter -> {
      if (!this.columnMapping.keySet().contains(queryParameter.getName())) {
        unknownFields.add(queryParameter.getName());
      }
    });

    reportRequest.getDisplayableFields().forEach(displayableField -> {
      if (!this.columnMapping.keySet().contains(displayableField.getName())) {
        unknownFields.add(displayableField.getName());
      }
    });

    if (!unknownFields.isEmpty()) {
      throw new IllegalArgumentException(
          "Unspecified fields requested: " + unknownFields.stream().collect(Collectors.joining(", "))
      );
    }
  }

  private void initializeMapping() {
    this.columnMapping.put("Date Range", "createdBy");
    this.columnMapping.put("State", "currentState");
    this.columnMapping.put("Customer", "identifier");
    this.columnMapping.put("Account number", "identifier");
    this.columnMapping.put("First name", "givenName");
    this.columnMapping.put("Middle Name", "middleName");
    this.columnMapping.put("Last Name", "surname");
    this.columnMapping.put("Balance", "balance");
    this.columnMapping.put("Address", "address");
  }

  private List<QueryParameter> buildQueryParameters() {
    return Arrays.asList(
        QueryParameterBuilder.create("Date range", Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
        QueryParameterBuilder.create("State", Type.TEXT).operator(QueryParameter.Operator.IN).build()
    );
  }

  private List<DisplayableField> buildDisplayableFields() {
    return Arrays.asList(
        DisplayableFieldBuilder.create("Customer", Type.TEXT).mandatory().build(),
        DisplayableFieldBuilder.create("First name", Type.TEXT).build(),
        DisplayableFieldBuilder.create("Middle name", Type.TEXT).build(),
        DisplayableFieldBuilder.create("Last name", Type.TEXT).build(),
        DisplayableFieldBuilder.create("Account number", Type.TEXT).mandatory().build(),
        DisplayableFieldBuilder.create("Balance", Type.NUMBER).build(),
        DisplayableFieldBuilder.create("Address", Type.TEXT).build()
    );
  }
}
