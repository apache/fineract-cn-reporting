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
import io.mifos.reporting.api.v1.domain.*;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Report(category = "Loan", identifier = "Listing")
public class LoanListReportSpecification implements ReportSpecification {


    private static final String CUSTOMER = "Customer";
    private static final String FIRST_NAME = "First Name";
    private static final String MIDDLE_NAME = "Middle Name";
    private static final String LAST_NAME = "Last Name";
    private static final String LOAN_TERM = "Loan Term";
    private static final String TIME_UNIT = "Time Unit";
    private static final String OFFICE = "Office";
    private static final String PRINCIPAL = "Principal";
    private static final String CASE = "Case Id";

    private static final String LOAN = "Loan";
    private static final String PRODUCT = "Type";
    private static final String STATE = "State";
    private static final String DATE_RANGE = "Created On";
    private static final String EMPLOYEE = "Created By";

    private final Logger logger;

    private final EntityManager entityManager;

    private final HashMap<String, String> customerColumnMapping = new HashMap<>();
    private final HashMap<String, String> loanColumnMapping = new HashMap<>();
    private final HashMap<String, String> caseColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();

    @Autowired
    public LoanListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
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
        reportDefinition.setName("Loan Account Listing");
        reportDefinition.setDescription("List of all loan accounts.");
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

        final Query customerQuery;
        customerQuery = this.entityManager.createNativeQuery(this.buildCustomerQuery(reportRequest, pageIndex, size));
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
        this.customerColumnMapping.put(CUSTOMER, "cst.identifier");
        this.customerColumnMapping.put(FIRST_NAME, "cst.given_name");
        this.customerColumnMapping.put(MIDDLE_NAME, "cst.middle_name");
        this.customerColumnMapping.put(LAST_NAME, "cst.surname");
        this.customerColumnMapping.put(OFFICE, "cst.assigned_office");

        this.loanColumnMapping.put(LOAN_TERM, "il_cases.term_range_maximum");
        this.loanColumnMapping.put(TIME_UNIT, "il_cases.term_range_temporal_unit");
        this.loanColumnMapping.put(PRINCIPAL, "il_cases.balance_range_maximum");
        this.loanColumnMapping.put(CASE, "il_cases.case_id");

        this.caseColumnMapping.put(LOAN, "cases.identifier");
        this.caseColumnMapping.put(PRODUCT, "cases.product_identifier");
        this.caseColumnMapping.put(STATE, "cases.current_state");
        this.caseColumnMapping.put(DATE_RANGE, "cases.created_on");
        this.caseColumnMapping.put(EMPLOYEE, "cases.created_by");

        this.allColumnMapping.putAll(customerColumnMapping);
        this.allColumnMapping.putAll(loanColumnMapping);
        this.allColumnMapping.putAll(caseColumnMapping);
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

    private List<Row> buildRows(ReportRequest reportRequest, List<?> customerResultList) {
        final ArrayList<Row> rows = new ArrayList<>();

        customerResultList.forEach(result -> {
            final Row row = new Row();
            row.setValues(new ArrayList<>());

            final String customerIdentifier;

            if (result instanceof Object[]) {
                final Object[] resultValues;
                resultValues = (Object[]) result;

                customerIdentifier = resultValues[0].toString();

                for (final Object resultValue : resultValues) {
                    final Value value = new Value();
                    if (resultValue != null) {
                        value.setValues(new String[]{resultValue.toString()});
                    } else {
                        value.setValues(new String[]{});
                    }

                    row.getValues().add(value);
                }
            } else {
                customerIdentifier = result.toString();

                final Value value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            final Query accountQuery = this.entityManager.createNativeQuery(this.buildLoanAccountQuery(reportRequest, customerIdentifier));
            final List<?> accountResultList = accountQuery.getResultList();

            accountResultList.forEach(accountResult -> {

                final String caseIdentifier;

                if (accountResult instanceof Object[]) {
                    final Object[] accountResultValues;
                    accountResultValues = (Object[]) accountResult;

                    caseIdentifier = accountResultValues[0].toString();

                    for (final Object loan: accountResultValues) {
                        final Value value = new Value();
                        if (loan != null) {
                            value.setValues(new String[]{loan.toString()});
                        } else {
                            value.setValues(new String[]{});
                        }

                        row.getValues().add(value);
                    }
                }else {
                    caseIdentifier = accountResult.toString();

                    final Value value = new Value();
                    value.setValues(new String[]{accountResult.toString()});
                    row.getValues().add(value);
                }

                final Query caseQuery = this.entityManager.createNativeQuery(this.buildCaseQuery(reportRequest, caseIdentifier));

                final List<?> caseResultList = caseQuery.getResultList();

                caseResultList.forEach(loanCase -> {
                    final Object[] loanCaseResultValues = (Object[]) loanCase;

                    for (final Object loan : loanCaseResultValues) {
                        final Value value = new Value();
                        if (loan != null) {
                            value.setValues(new String[]{loan.toString()});
                        } else {
                            value.setValues(new String[]{});
                        }
                        row.getValues().add(value);
                    }
                });
            });

            rows.add(row);
        });

        return rows;
    }

    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(CUSTOMER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(FIRST_NAME, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(MIDDLE_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LAST_NAME, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(OFFICE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(CASE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(PRINCIPAL, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(LOAN_TERM, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(TIME_UNIT, Type.TEXT).mandatory().build(),

                DisplayableFieldBuilder.create(LOAN, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(STATE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(EMPLOYEE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(PRODUCT, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(DATE_RANGE, Type.TEXT).mandatory().build()
        );
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                //QueryParameterBuilder.create(DATE_RANGE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build()
                //QueryParameterBuilder.create(LOAN_STATE, Type.TEXT).operator(QueryParameter.Operator.IN).build()
        );
    }

    private String buildCustomerQuery(final ReportRequest reportRequest, int pageIndex, int size){
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column;
            column = this.customerColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("maat_customers cst ");

        final List<QueryParameter> queryParameters = reportRequest.getQueryParameters();
        if (!queryParameters.isEmpty()) {
            final ArrayList<String> criteria = new ArrayList<>();
            queryParameters.forEach(queryParameter -> {
                if((queryParameter.getValue() != null) && !queryParameter.getValue().isEmpty()) {
                    criteria.add(
                            CriteriaBuilder.buildCriteria(this.customerColumnMapping.get(queryParameter.getName()), queryParameter)
                    );
                }
            });

            if (!criteria.isEmpty()) {
                query.append(" WHERE ");
                query.append(criteria.stream().collect(Collectors.joining(" AND ")));
            }

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

    private String buildLoanAccountQuery(final ReportRequest reportRequest, final String customerIdentifier){
        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.loanColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        return "SELECT " + columns.stream().collect(Collectors.joining(", ")) + " " +
                "FROM bastet_il_cases il_cases " +
                "LEFT JOIN maat_customers cst on il_cases.customer_identifier = cst.identifier " +
                "WHERE cst.identifier ='" + customerIdentifier + "' " +
                "ORDER BY il_cases.case_id";
    }

    private String buildCaseQuery(final ReportRequest reportRequest, final String caseIdentifier){
        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.caseColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        return "SELECT " + columns.stream().collect(Collectors.joining(", ")) + " " +
                "FROM bastet_cases cases " +
                "LEFT JOIN bastet_il_cases il_cases on cases.id = il_cases.case_id " +
                "WHERE il_cases.case_id ='" + caseIdentifier + "' ";
    }
}
