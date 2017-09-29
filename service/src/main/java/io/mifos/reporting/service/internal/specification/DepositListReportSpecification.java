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

@Report(category = "Deposit", identifier = "Listing")
public class DepositListReportSpecification implements ReportSpecification {

    private static final String CUSTOMER = "Customer Account";
    private static final String FIRST_NAME = "First Name";
    private static final String MIDDLE_NAME = "Middle Name";
    private static final String LAST_NAME = "Last Name";
    private static final String EMPLOYEE = "Created By";
    private static final String ACCOUNT_NUMBER = "Deposit Account";
    private static final String PRODUCT = "Product";
    private static final String ACCOUNT_TYPE = "Deposit Type";
    private static final String STATE = "Status";
    private static final String OFFICE = "Office";
    private static final String DATE_RANGE = "Date Created";

    private final EntityManager entityManager;

    private final Logger logger;

    private final HashMap<String, String> customerColumnMapping = new HashMap<>();
    private final HashMap<String, String> depositAccountColumnMapping = new HashMap<>();
    private final HashMap<String, String> depositProductColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();


    @Autowired
    public DepositListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger, final EntityManager entityManager) {
        this.entityManager = entityManager;
        this.logger = logger;
        this.initializeMapping();
    }


    @Override
    public ReportDefinition getReportDefinition() {

        final ReportDefinition reportDefinition = new ReportDefinition();
        reportDefinition.setIdentifier("Listing");
        reportDefinition.setName("Deposit Account Listing");
        reportDefinition.setDescription("List of all deposit accounts.");
        reportDefinition.setQueryParameters(this.buildQueryParameters());
        reportDefinition.setDisplayableFields(this.buildDisplayableFields());
        return reportDefinition;
    }

    @Override
    public ReportPage generateReport(ReportRequest reportRequest, int pageIndex, int size) {
        final ReportDefinition reportDefinition = this.getReportDefinition();
        this.logger.info("Generating report {0} ", reportDefinition.getIdentifier());

        final ReportPage reportPage = new ReportPage();
        reportPage.setName(reportDefinition.getName());
        reportPage.setDescription(reportDefinition.getDescription());
        reportPage.setHeader(this.createHeader(reportRequest.getDisplayableFields()));

        final Query customerQuery = this.entityManager.createNativeQuery(this.buildCustomerQuery(reportRequest, pageIndex, size));

        final List<?> customerResultList = customerQuery.getResultList();
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
        final ArrayList<String> unknownFields = new ArrayList<>();
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

        this.depositAccountColumnMapping.put(EMPLOYEE, "pi.created_by");
        this.depositAccountColumnMapping.put(ACCOUNT_NUMBER, "pi.account_identifier");
        this.depositAccountColumnMapping.put(STATE, "pi.a_state");
        this.depositAccountColumnMapping.put(PRODUCT, "pi.product_definition_id");
        this.depositAccountColumnMapping.put(DATE_RANGE, "pi.created_on");

        this.depositProductColumnMapping.put(ACCOUNT_TYPE, "pd.a_name, pd.a_type");

        this.allColumnMapping.putAll(customerColumnMapping);
        this.allColumnMapping.putAll(depositProductColumnMapping);
        this.allColumnMapping.putAll(depositAccountColumnMapping);
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

            final String customerIdentifier;

            if (result instanceof Object[]) {
                final Object[] resultValues = (Object[]) result;

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

            final Query accountQuery = this.entityManager.createNativeQuery(this.buildDepositAccountQuery(reportRequest, customerIdentifier));
            final List<?> accountResultList = accountQuery.getResultList();

            final ArrayList<String> products = new ArrayList<>();
            final ArrayList<String> depositAccountNumber = new ArrayList<>();
            final ArrayList<String> depositType = new ArrayList<>();
            final ArrayList<String> status = new ArrayList<>();
            final ArrayList<String> createdBy = new ArrayList<>();
            final ArrayList<String> dateCreated = new ArrayList<>();

            accountResultList.forEach(accountResult -> {

                final String productIdentifier;
                if (accountResult instanceof Object[]) {
                    final Object[] accountResultValues = (Object[]) accountResult;

                    productIdentifier = accountResultValues[0].toString();

                    final Query depositProductQuery = this.entityManager.createNativeQuery(this.buildDepositProductQuery(reportRequest, productIdentifier));
                    final List<?> depositProductResultList = depositProductQuery.getResultList();

                    depositProductResultList.forEach(product -> {
                        final Object[] productResultValues = (Object[]) product;

                        for (int i = 0; i < productResultValues.length; i++) {

                            if (i == 0 && productResultValues[0] != null) {
                                products.add(productResultValues[0].toString());
                            }

                            if (i == 1 && productResultValues[1] != null) {
                                depositType.add(productResultValues[1].toString());
                            }

                        }
                    });


                    for (int i = 1; i < accountResultValues.length ; i++) {
                        if (i == 1 && accountResultValues[1] != null){
                            depositAccountNumber.add(accountResultValues[1].toString());
                        }

                        if (i == 2  && accountResultValues[2] != null){
                            status.add(accountResultValues[2].toString());
                        }

                        if (i == 3 && accountResultValues[3] != null){
                            createdBy.add(accountResultValues[3].toString());
                        }

                        if (i == 4 && accountResultValues[4] != null){
                            dateCreated.add(accountResultValues[4].toString());
                        }

                    }
                }
            });

            final Value productValue = new Value();
            productValue.setValues(products.toArray(new String[products.size()]));
            row.getValues().add(productValue);

            final Value depositTypeValue = new Value();
            depositTypeValue.setValues(depositType.toArray(new String[depositAccountNumber.size()]));
            row.getValues().add(depositTypeValue);

            final Value depositAccountNumberValue = new Value();
            depositAccountNumberValue.setValues(depositAccountNumber.toArray(new String[depositType.size()]));
            row.getValues().add(depositAccountNumberValue);

            final Value statusValue = new Value();
            statusValue.setValues(status.toArray(new String[status.size()]));
            row.getValues().add(statusValue);

            final Value createdByValue = new Value();
            createdByValue.setValues(createdBy.toArray(new String[createdBy.size()]));
            row.getValues().add(createdByValue);

            final Value dateCreatedValue = new Value();
            dateCreatedValue.setValues(dateCreated.toArray(new String[dateCreated.size()]));
            row.getValues().add(dateCreatedValue);

            rows.add(row);
        });

        return rows;
    }

    private String buildCustomerQuery(final ReportRequest reportRequest, int pageIndex, int size) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields;
        displayableFields = reportRequest.getDisplayableFields();
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
            final ArrayList<String> criteria = new ArrayList<>();
            queryParameters.forEach(queryParameter -> {
                if(queryParameter.getValue() != null && !queryParameter.getValue().isEmpty()) {
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

    private String buildDepositAccountQuery(final ReportRequest reportRequest, final String customerIdentifier) {
        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.depositAccountColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        return "SELECT " + columns.stream().collect(Collectors.joining(", ")) + " " +
                "FROM shed_product_instances pi " +
                "LEFT JOIN maat_customers cst on pi.customer_identifier = cst.identifier " +
                "WHERE cst.identifier ='" + customerIdentifier + "' " +
                "ORDER BY pi.account_identifier";
    }

    private String buildDepositProductQuery(final ReportRequest reportRequest, final String productIdentifier){
        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.depositProductColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        return "SELECT DISTINCT " + columns.stream().collect(Collectors.joining(", ")) + " " +
                "FROM shed_product_definitions pd " +
                "LEFT JOIN shed_product_instances pi on pd.id = pi.product_definition_id " +
                "WHERE pi.product_definition_id ='" + productIdentifier + "' ";
    }

    private List<DisplayableField> buildDisplayableFields() {

        return Arrays.asList(
                DisplayableFieldBuilder.create(CUSTOMER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(FIRST_NAME, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(MIDDLE_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LAST_NAME, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(OFFICE, Type.TEXT).build(),

                DisplayableFieldBuilder.create(PRODUCT, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(ACCOUNT_TYPE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(ACCOUNT_NUMBER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(STATE,Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(EMPLOYEE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(DATE_RANGE, Type.DATE).mandatory().build()
        );

    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                QueryParameterBuilder.create(DATE_RANGE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
                QueryParameterBuilder.create(STATE, Type.TEXT).operator(QueryParameter.Operator.IN).build()
        );
    }


}
