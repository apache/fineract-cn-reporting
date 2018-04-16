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
package org.apache.fineract.cn.reporting.service.internal.specification;

import org.apache.fineract.cn.reporting.api.v1.domain.DisplayableField;
import org.apache.fineract.cn.reporting.api.v1.domain.Header;
import org.apache.fineract.cn.reporting.api.v1.domain.QueryParameter;
import org.apache.fineract.cn.reporting.api.v1.domain.ReportDefinition;
import org.apache.fineract.cn.reporting.api.v1.domain.ReportPage;
import org.apache.fineract.cn.reporting.api.v1.domain.ReportRequest;
import org.apache.fineract.cn.reporting.api.v1.domain.Row;
import org.apache.fineract.cn.reporting.api.v1.domain.Type;
import org.apache.fineract.cn.reporting.api.v1.domain.Value;
import org.apache.fineract.cn.reporting.service.ServiceConstants;
import org.apache.fineract.cn.reporting.service.spi.DisplayableFieldBuilder;
import org.apache.fineract.cn.reporting.service.spi.Report;
import org.apache.fineract.cn.reporting.service.spi.ReportSpecification;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.lang.DateConverter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;

@Report(category = "Accounting", identifier = "Incomestatement")
public class IncomeStatementReportSpecification implements ReportSpecification {

    private static final String DATE_RANGE = "Date range";
    private static final String TYPE = "Type";
    private static final String IDENTIFIER = "Identifier";
    private static final String NAME = "Name";
    private static final String BALANCE = "Balance";

    private final Logger logger;

    private final EntityManager entityManager;

    private final HashMap<String, String> accountColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();


    public IncomeStatementReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                              final EntityManager entityManager){
        super();
        this.logger = logger;
        this.entityManager = entityManager;
        this.initializeMapping();
    }

    @Override
    public ReportDefinition getReportDefinition() {
        final ReportDefinition reportDefinition = new ReportDefinition();
        reportDefinition.setIdentifier("Incomestatement");
        reportDefinition.setName("Income Statement");
        reportDefinition.setDescription("Income statement report");
        reportDefinition.setQueryParameters(this.buildQueryParameters());
        reportDefinition.setDisplayableFields(this.buildDisplayableFields());
        return reportDefinition;
    }

    @Override
    public ReportPage generateReport(ReportRequest reportRequest, int pageIndex, int size) {
        final ReportDefinition reportDefinition = this.getReportDefinition();
        this.logger.info("Generating report {0}.", reportDefinition.getIdentifier());

        final ReportPage reportPage = new ReportPage();
        reportPage.setName(reportDefinition.getName());
        reportPage.setDescription(reportDefinition.getDescription());
        reportPage.setHeader(this.createHeader(reportRequest.getDisplayableFields()));

        final Query accountQuery = this.entityManager.createNativeQuery(this.buildAccountQuery(reportRequest, pageIndex, size));
        final List<?> accountResultList =  accountQuery.getResultList();
        reportPage.setRows(this.buildRows(reportRequest, accountResultList));

        reportPage.setHasMore(
                !this.entityManager.createNativeQuery(this.buildAccountQuery(reportRequest, pageIndex + 1, size))
                        .getResultList().isEmpty()
        );

        reportPage.setGeneratedBy(UserContextHolder.checkedGetUser());
        reportPage.setGeneratedOn(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
        return reportPage;
    }

    @Override
    public void validate(ReportRequest reportRequest) throws IllegalArgumentException {
        final ArrayList<String> unknownFields =  new ArrayList<>();
        reportRequest.getQueryParameters().forEach(queryParameter -> {
            if (!this.allColumnMapping.keySet().contains(queryParameter.getName())) {
                unknownFields.add(queryParameter.getName());
            }
        });

        reportRequest.getDisplayableFields().forEach(displayableField -> {
            if (!this.allColumnMapping.keySet().contains(displayableField.getName())) {
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
        this.accountColumnMapping.put(DATE_RANGE, "acc.created_on");
        this.accountColumnMapping.put(TYPE, "acc.a_type");
        this.accountColumnMapping.put(IDENTIFIER, "acc.identifier");
        this.accountColumnMapping.put(NAME, "acc.a_name");
        this.accountColumnMapping.put(BALANCE, "acc.balance");

        this.allColumnMapping.putAll(accountColumnMapping);
    }

    private Header createHeader(List<DisplayableField> displayableFields) {
        final Header header = new Header();
        header.setColumnNames(
                displayableFields
                        .stream()
                        .map(DisplayableField::getName)
                        .collect(Collectors.toList())
        );
        return header;
    }

    private List<Row> buildRows(ReportRequest reportRequest, List<?> accountResultList) {
        final ArrayList<Row> rows = new ArrayList<>();
        
        final Row totalRevenueRow = new Row();
        totalRevenueRow.setValues(new ArrayList<>());

        final Value subRevenueTotal = new Value();

        final BigDecimal[] revenueSubTotal = {new BigDecimal("0.000")};

        accountResultList.forEach(result -> {

            final Row row = new Row();
            row.setValues(new ArrayList<>());

            if (result instanceof Object[]) {
                final Object[] resultValues;
                resultValues = (Object[]) result;

                for (int i = 0; i < resultValues.length; i++){
                    final Value revValue = new Value();
                    if (resultValues[i] != null){
                        revValue.setValues(new String[]{resultValues[i].toString()});
                    }else revValue.setValues(new String[]{});

                    row.getValues().add(revValue);

                    revenueSubTotal[0] = revenueSubTotal[0].add((BigDecimal)resultValues[3]);

                }
            } else {
                final Value value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            rows.add(row);
        });

        subRevenueTotal.setValues(new String[]{new StringBuilder().append("TOTAL REVENUES ").append(revenueSubTotal[0]).toString()});
        totalRevenueRow.getValues().add(subRevenueTotal);

        rows.add(totalRevenueRow);


        final String expenseQueryString = this.buildExpenseQuery(reportRequest);
        final Query expenseQuery = this.entityManager.createNativeQuery(expenseQueryString);
        final List<?> expenseResultList = expenseQuery.getResultList();

        final Row totalExpenseRow = new Row();
        totalExpenseRow.setValues(new ArrayList<>());
        final Value subExpenseTotal = new Value();

        final Row netIncomeRow = new Row();
        netIncomeRow.setValues(new ArrayList<>());
        final Value netIncomeTotal = new Value();

        final BigDecimal[] expenseSubTotal = {new BigDecimal("0.000")};

        expenseResultList.forEach(result -> {

            final Row row = new Row();
            row.setValues(new ArrayList<>());

            if (result instanceof Object[]) {
                final Object[] resultValues;
                resultValues = (Object[]) result;

                for (int i = 0; i < resultValues.length; i++){
                    final Value expValue = new Value();
                    if (resultValues[i] != null) expValue.setValues(new String[]{resultValues[i].toString()});
                    else expValue.setValues(new String[]{});

                    row.getValues().add(expValue);

                    expenseSubTotal[0] = expenseSubTotal[0].add((BigDecimal)resultValues[3]);

                }
            } else {
                final Value value;
                value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            rows.add(row);
        });

        subExpenseTotal.setValues(new String[]{new StringBuilder().append("TOTAL EXPENSES ").append(expenseSubTotal[0]).toString()});
        totalExpenseRow.getValues().add(subExpenseTotal);
        rows.add(totalExpenseRow);

        final BigDecimal netIncome = revenueSubTotal[0].subtract(expenseSubTotal[0]);
        netIncomeTotal.setValues(new String[]{new StringBuilder().append("NET INCOME ").append(netIncome).toString()});
        netIncomeRow.getValues().add(netIncomeTotal);
        rows.add(netIncomeRow);

        return rows;
    }

    private String buildAccountQuery(final ReportRequest reportRequest, int pageIndex, int size) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.accountColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("thoth_accounts acc ")
                .append("WHERE acc.a_type = 'REVENUE' ");

        query.append(" ORDER BY acc.identifier");

        return query.toString();
    }

    private String buildExpenseQuery(final ReportRequest reportRequest) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.accountColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("thoth_accounts acc ")
                .append("WHERE acc.a_type = 'EXPENSE' ");

        query.append(" ORDER BY acc.identifier");

        return query.toString();
    }

    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(TYPE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(IDENTIFIER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(NAME, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(BALANCE, Type.TEXT).mandatory().build()
        );
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList();
    }
}
