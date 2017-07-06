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

import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.lang.DateConverter;
import io.mifos.reporting.api.v1.domain.DisplayableField;
import io.mifos.reporting.api.v1.domain.Header;
import io.mifos.reporting.api.v1.domain.QueryParameter;
import io.mifos.reporting.api.v1.domain.ReportDefinition;
import io.mifos.reporting.api.v1.domain.ReportPage;
import io.mifos.reporting.api.v1.domain.ReportRequest;
import io.mifos.reporting.api.v1.domain.Row;
import io.mifos.reporting.api.v1.domain.Type;
import io.mifos.reporting.api.v1.domain.Value;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.CriteriaBuilder;
import io.mifos.reporting.service.spi.DisplayableFieldBuilder;
import io.mifos.reporting.service.spi.QueryParameterBuilder;
import io.mifos.reporting.service.spi.Report;
import io.mifos.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Report(category = "Customer", identifier = "Listing")
public class CustomerListReportSpecification implements ReportSpecification {

  private final Logger logger;
  private final EntityManager entityManager;
  private final HashMap<String, String> customerColumnMapping = new HashMap<>();
  private final HashMap<String, String> addressColumnMapping = new HashMap<>();
  private final HashMap<String, String> accountColumnMapping = new HashMap<>();

  @Autowired
  public CustomerListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                         final EntityManager entityManager) {
    super();
    this.logger = logger;
    this.entityManager = entityManager;
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
  public ReportPage generateReport(final ReportRequest reportRequest, final int pageIndex, final int size) {
    final ReportDefinition reportDefinition = this.getReportDefinition();
    this.logger.info("Generating report {0}.", reportDefinition.getIdentifier());

    final ReportPage reportPage = new ReportPage();
    reportPage.setName(reportDefinition.getName());
    reportPage.setDescription(reportDefinition.getDescription());
    reportPage.setHeader(this.createHeader(reportRequest.getDisplayableFields()));

    final Query customerQuery = this.entityManager.createNativeQuery(this.buildCustomerQuery(reportRequest, pageIndex, size));
    final List<?> customerResultList =  customerQuery.getResultList();
    reportPage.setRows(this.buildRows(reportRequest, customerResultList));

    reportPage.setHasMore(
        !this.entityManager.createNativeQuery(this.buildCustomerQuery(reportRequest, pageIndex + 1, size))
            .getResultList().isEmpty()
    );

    reportPage.setGeneratedBy(UserContextHolder.checkedGetUser());
    reportPage.setGeneratedOn(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
    return reportPage;
  }

  @Override
  public void validate(final ReportRequest reportRequest) throws IllegalArgumentException {
    final ArrayList<String> unknownFields =  new ArrayList<>();
    reportRequest.getQueryParameters().forEach(queryParameter -> {
      if (!this.customerColumnMapping.keySet().contains(queryParameter.getName())) {
        unknownFields.add(queryParameter.getName());
      }
    });

    reportRequest.getDisplayableFields().forEach(displayableField -> {
      if (!this.customerColumnMapping.keySet().contains(displayableField.getName())) {
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
    this.customerColumnMapping.put("Date Range", "cst.created_on");
    this.customerColumnMapping.put("State", "cst.current_state");
    this.customerColumnMapping.put("Customer", "cst.identifier");
    this.customerColumnMapping.put("First name", "cst.given_name");
    this.customerColumnMapping.put("Middle Name", "cst.middle_name");
    this.customerColumnMapping.put("Last Name", "cst.surname");

    this.accountColumnMapping.put("Account number", "acc.identifier, acc.balance");

    this.addressColumnMapping.put("Address", "CONCAT(adr.street, ', ', adr.postal_code, ', ', adr.city)");
  }

  private Header createHeader(final List<DisplayableField> displayableFields) {
    final Header header = new Header();
    header.setColumnNames(
        displayableFields
            .stream()
            .map(DisplayableField::getName)
            .collect(Collectors.toList())
    );
    return header;
  }

  private List<Row> buildRows(final ReportRequest reportRequest, final List<?> customerResultList) {
    final ArrayList<Row> rows = new ArrayList<>();

    customerResultList.forEach(result -> {
      final Row row = new Row();
      row.setValues(new ArrayList<>());
      final Object[] resultValues = (Object[]) result;
      for (final Object resultValue : resultValues) {
        final Value value = new Value();
        value.setValues(new String[]{resultValue.toString()});
        row.getValues().add(value);
      }

      final DecimalFormat decimalFormat = new DecimalFormat(".##");
      final Query accountQuery = this.entityManager.createNativeQuery(this.buildAccountQuery(reportRequest, resultValues[0].toString()));
      final List<?> accountResultList = accountQuery.getResultList();
      final ArrayList<String> values = new ArrayList<>();
      accountResultList.forEach(accountResult -> {
        if (accountResult instanceof Object[]) {
          final Object[] accountResultValues = (Object[]) accountResult;
          final String accountValue = accountResultValues[0].toString() + " (" +
              decimalFormat.format(Double.valueOf(accountResultValues[1].toString())) + ")";
          values.add(accountValue);
        }
      });
      final Value accountValue = new Value();
      accountValue.setValues(values.toArray(new String[values.size()]));
      row.getValues().add(accountValue);

      final String addressQueryString = this.buildAddressQuery(reportRequest, resultValues[0].toString());
      if (addressQueryString != null) {
        final Query addressQuery = this.entityManager.createNativeQuery(addressQueryString);
        final List<?> resultList = addressQuery.getResultList();
        final Value addressValue = new Value();
        addressValue.setValues(new String[]{resultList.get(0).toString()});
        row.getValues().add(addressValue);
      }

      rows.add(row);
    });

    return rows;
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
        DisplayableFieldBuilder.create("Address", Type.TEXT).build()
    );
  }

  private String buildCustomerQuery(final ReportRequest reportRequest, int pageIndex, int size) {
    final StringBuilder query = new StringBuilder("SELECT ");

    final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
    final ArrayList<String> columns = new ArrayList<>();
    displayableFields.forEach(displayableField -> {
      final String column = this.customerColumnMapping.get(displayableField.getName());
      if (column != null) {
        columns.add(column);
      }
    });

    query.append(columns.stream().collect(Collectors.joining(", ")))
        .append(" FROM ")
        .append("maat_customers cst ");

    final List<QueryParameter> queryParameters = reportRequest.getQueryParameters();
    if (!queryParameters.isEmpty()) {
      query.append(" WHERE ");
      final ArrayList<String> criteria = new ArrayList<>();
      queryParameters.forEach(queryParameter -> criteria.add(
          CriteriaBuilder.buildCriteria(this.customerColumnMapping.get(queryParameter.getName()), queryParameter))
      );
      query.append(criteria.stream().collect(Collectors.joining(" AND ")));
    }
    query.append(" ORDER BY cst.identifier");

    query.append(" LIMIT ");
    query.append(size);
    if (pageIndex > 0) {
      query.append(" OFFSET ");
      query.append(size * pageIndex);
    }

    return query.toString();
  }

  private String buildAccountQuery(final ReportRequest reportRequest, final String customerIdentifier) {
    final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
    final ArrayList<String> columns = new ArrayList<>();
    displayableFields.forEach(displayableField -> {
      final String column = this.accountColumnMapping.get(displayableField.getName());
      if (column != null) {
        columns.add(column);
      }
    });

    return "SELECT " + columns.stream().collect(Collectors.joining(", ")) + " " +
        "FROM thoth_accounts acc " +
            "LEFT JOIN maat_customers cst on acc.holders = cst.identifier " +
        "WHERE cst.identifier ='" + customerIdentifier + "' " +
        "ORDER BY acc.identifier";
  }

  private String buildAddressQuery(final ReportRequest reportRequest, final String customerIdentifier) {

    final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
    final ArrayList<String> columns = new ArrayList<>();
    displayableFields.forEach(displayableField -> {
      final String column = this.addressColumnMapping.get(displayableField.getName());
      if (column != null) {
        columns.add(column);
      }
    });

    if (!columns.isEmpty()) {
      return "SELECT " + columns.stream().collect(Collectors.joining(", ")) + " " +
          "FROM thoth_accounts acc " +
              "LEFT JOIN maat_customers cst on acc.holders = cst.identifier " +
          "WHERE cst.identifier ='" + customerIdentifier + "' " +
          "ORDER BY acc.identifier";
    }
    return null;
  }
}
