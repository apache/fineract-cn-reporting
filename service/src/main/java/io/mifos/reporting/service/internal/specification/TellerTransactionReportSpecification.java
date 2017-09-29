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

@Report(category = "Teller", identifier = "Transactions")
public class TellerTransactionReportSpecification implements ReportSpecification {

    private static final String TELLER_ID = "Teller Id";
    private static final String TELLER = "Teller";
    private static final String TRANSACTION_TYPE = "Transaction Type";
    private static final String TRANSACTION_DATE = "Transaction Date";
    private static final String CUSTOMER = "Customer";
    private static final String SOURCE = "Source Account";
    private static final String TARGET = "Target Account";
    private static final String CLERK = "Clerk";
    private static final String AMOUNT = "Amount";
    private static final String STATUS = "Status";

    private final Logger logger;

    private final EntityManager entityManager;
    private final HashMap<String, String> tellerColumnMapping = new HashMap<>();
    private final HashMap<String, String> transactionColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();

    @Autowired
    public TellerTransactionReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                                final EntityManager entityManager) {
        super();
        this.logger = logger;
        this.entityManager = entityManager;
        this.initializeMapping();
    }

    @Override
    public ReportDefinition getReportDefinition() {
        final ReportDefinition reportDefinition = new ReportDefinition();
        reportDefinition.setIdentifier("Transactions");
        reportDefinition.setName("Teller Transactions");
        reportDefinition.setDescription("List all teller-cashier transactions.");
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

        final Query tellerQuery = this.entityManager.createNativeQuery(this.buildTellerQuery(reportRequest, pageIndex, size));
        final List<?> tellerResultList = tellerQuery.getResultList();
        reportPage.setRows(this.buildRows(reportRequest, tellerResultList));

        reportPage.setHasMore(
                !this.entityManager.createNativeQuery(this.buildTellerQuery(reportRequest, pageIndex + 1, size))
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
        this.tellerColumnMapping.put(TELLER_ID, "teller.id");
        this.tellerColumnMapping.put(TELLER, "teller.identifier");

        this.transactionColumnMapping.put(TRANSACTION_TYPE, "trx.transaction_type");
        this.transactionColumnMapping.put(TRANSACTION_DATE, "trx.transaction_date");
        this.transactionColumnMapping.put(CUSTOMER, "trx.customer_identifier");
        this.transactionColumnMapping.put(SOURCE, "trx.customer_account_identifier");
        this.transactionColumnMapping.put(TARGET, "trx.target_account_identifier");
        this.transactionColumnMapping.put(CLERK, "trx.clerk");
        this.transactionColumnMapping.put(AMOUNT, "trx.amount");
        this.transactionColumnMapping.put(STATUS, "trx.a_state");

        this.allColumnMapping.putAll(tellerColumnMapping);
        this.allColumnMapping.putAll(transactionColumnMapping);

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


    private List<Row> buildRows(ReportRequest reportRequest, List<?> tellerResultList) {
        final ArrayList<Row> rows = new ArrayList<>();

        tellerResultList.forEach(result -> {
            final Row row = new Row();
            row.setValues(new ArrayList<>());

            final String tellerIdentifier;

            if (result instanceof Object[]) {
                final Object[] resultValues = (Object[]) result;

                tellerIdentifier = resultValues[0].toString();

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
                tellerIdentifier = result.toString();

                final Value value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            final String transactionQueryString = this.buildTellerTransactionQuery(reportRequest, tellerIdentifier);

            final Query transactionQuery = this.entityManager.createNativeQuery(transactionQueryString);
            final List<?> resultList = transactionQuery.getResultList();

            final ArrayList<String> transactionType = new ArrayList<>();
            final ArrayList<String> transactionDate = new ArrayList<>();
            final ArrayList<String> customer = new ArrayList<>();
            final ArrayList<String> source = new ArrayList<>();
            final ArrayList<String> target = new ArrayList<>();
            final ArrayList<String> clerk = new ArrayList<>();
            final ArrayList<String> amount = new ArrayList<>();
            final ArrayList<String> status = new ArrayList<>();
            resultList.forEach(transaction -> {
                        final Object[] transactionValue = (Object[]) transaction;

                        for (int i = 0; i < transactionValue.length; i++) {
                            if (i == 0 && transactionValue[0] != null) {
                                transactionType.add(transactionValue[0].toString());
                            }

                            if (i == 1 && transactionValue[1] != null) {
                                transactionDate.add(transactionValue[1].toString());
                            }

                            if (i == 2 && transactionValue[2] != null) {
                                customer.add(transactionValue[2].toString());
                            }

                            if (i == 3 && transactionValue[3] != null) {
                                source.add(transactionValue[3].toString());
                            }

                            if (i == 4 && transactionValue[4] != null) {
                                target.add(transactionValue[4].toString());
                            }
                            if (i == 5 && transactionValue[5] != null) {
                                clerk.add(transactionValue[5].toString());
                            }
                            if (i == 6 && transactionValue[6] != null) {
                                amount.add(transactionValue[6].toString());
                            }
                            if (i == 7 && transactionValue[7] != null) {
                                status.add(transactionValue[7].toString());
                            }
                        }
                    }
            );

            final Value transactionTypeValue = new Value();
            transactionTypeValue.setValues(transactionType.toArray(new String[transactionType.size()]));
            row.getValues().add(transactionTypeValue);

            final Value transactionDateValue = new Value();
            transactionDateValue.setValues(transactionDate.toArray(new String[transactionDate.size()]));
            row.getValues().add(transactionDateValue);

            final Value customerValue = new Value();
            customerValue.setValues(customer.toArray(new String[customer.size()]));
            row.getValues().add(customerValue);

            final Value sourceValue = new Value();
            sourceValue.setValues(source.toArray(new String[source.size()]));
            row.getValues().add(sourceValue);

            final Value targetValue = new Value();
            targetValue.setValues(target.toArray(new String[target.size()]));
            row.getValues().add(targetValue);

            final Value clerkValue = new Value();
            clerkValue.setValues(clerk.toArray(new String[clerk.size()]));
            row.getValues().add(clerkValue);

            final Value amountValue = new Value();
            amountValue.setValues(amount.toArray(new String[amount.size()]));
            row.getValues().add(amountValue);

            final Value statusValue = new Value();
            statusValue.setValues(status.toArray(new String[status.size()]));
            row.getValues().add(statusValue);

            rows.add(row);
        });

        return rows;
    }


    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(TELLER_ID, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(TELLER, Type.TEXT).mandatory().build(),

                DisplayableFieldBuilder.create(TRANSACTION_TYPE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(TRANSACTION_DATE, Type.DATE).mandatory().build(),
                DisplayableFieldBuilder.create(CUSTOMER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(SOURCE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(TARGET, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(CLERK, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(AMOUNT, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(STATUS, Type.TEXT).mandatory().build()
        );
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                QueryParameterBuilder.create(TRANSACTION_DATE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
                QueryParameterBuilder.create(STATUS, Type.TEXT).operator(QueryParameter.Operator.IN).build()
        );
    }

    private String buildTellerQuery(ReportRequest reportRequest, int pageIndex, int size) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.tellerColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("tajet_teller teller ");

        query.append(" ORDER BY teller.id");

        query.append(" LIMIT ");
        query.append(size);
        if (pageIndex > 0) {
            query.append(" OFFSET ");
            query.append(size * pageIndex);
        }

        return query.toString();
    }

    private String buildTellerTransactionQuery(final ReportRequest reportRequest, final String tellerIdentifier) {

        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.transactionColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("tajet_teller_transactions trx " +
                        "LEFT JOIN tajet_teller teller on trx.teller_id = teller.id ");

        query.append("WHERE teller.id ='" + tellerIdentifier + "'");

        final List<QueryParameter> queryParameters = reportRequest.getQueryParameters();
        if (!queryParameters.isEmpty()) {
            final ArrayList<String> criteria = new ArrayList<>();
            queryParameters.forEach(queryParameter -> {
                if (queryParameter.getValue() != null && !queryParameter.getValue().isEmpty()) {
                    criteria.add(
                            CriteriaBuilder.buildCriteria(this.transactionColumnMapping.get(queryParameter.getName()), queryParameter)
                    );
                }
            });

            if (!criteria.isEmpty()) {
                query.append(" AND ");
                query.append(criteria.stream().collect(Collectors.joining(" AND ")));
            }

        }

        return query.toString();
    }


}
