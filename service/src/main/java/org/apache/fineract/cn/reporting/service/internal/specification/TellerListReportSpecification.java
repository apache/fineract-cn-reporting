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
import org.apache.fineract.cn.reporting.service.spi.CriteriaBuilder;
import org.apache.fineract.cn.reporting.service.spi.DisplayableFieldBuilder;
import org.apache.fineract.cn.reporting.service.spi.QueryParameterBuilder;
import org.apache.fineract.cn.reporting.service.spi.Report;
import org.apache.fineract.cn.reporting.service.spi.ReportSpecification;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Report(category = "Teller" , identifier = "Listing")
public class TellerListReportSpecification implements ReportSpecification {

    private static final String TELLER = "Teller";
    private static final String EMPLOYEE = "Employee";
    private static final String OFFICE = "Office";
    private static final String CASHDRAW_LIMIT = "Cashdraw limit";
    private static final String STATE = "State";
    private static final String DATE_RANGE = "Date";

    private final Logger logger;

    private final EntityManager entityManager;

    private final HashMap<String, String> tellerColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();


    @Autowired
    public TellerListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
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
        reportDefinition.setName("Teller Listing");
        reportDefinition.setDescription("List of all Tellers.");
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
        final List<?> tellerResultList =  tellerQuery.getResultList();
        reportPage.setRows(this.buildRows(tellerResultList));

        reportPage.setHasMore(
                !this.entityManager.createNativeQuery(this.buildTellerQuery(reportRequest, pageIndex + 1, size))
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
        this.tellerColumnMapping.put(TELLER, "tl.identifier");
        this.tellerColumnMapping.put(OFFICE, "tl.office_identifier");
        this.tellerColumnMapping.put(CASHDRAW_LIMIT, "tl.cashdraw_limit");
        this.tellerColumnMapping.put(EMPLOYEE, "tl.assigned_employee_identifier");
        this.tellerColumnMapping.put(STATE, "tl.a_state");
        this.tellerColumnMapping.put(DATE_RANGE, "tl.created_on");

        this.allColumnMapping.putAll(tellerColumnMapping);
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

    private List<Row> buildRows(final List<?> tellerResultList) {
        final ArrayList<Row> rows = new ArrayList<>();
        tellerResultList.forEach(result -> {
            final Row row = new Row();
            row.setValues(new ArrayList<>());
            
            if (result instanceof Object[]) {
                final Object[] resultValues = (Object[]) result;

                for(final Object resultVal : resultValues) {
                    final Value val;
                    val = new Value();

                    if (resultVal != null) {
                        val.setValues(new String[]{resultVal.toString()});
                    } else {
                        val.setValues(new String[]{});
                    }

                    row.getValues().add(val);
                }
            } else {
                final Value value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }
            rows.add(row);
        });

        return rows;
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                QueryParameterBuilder.create(DATE_RANGE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
                QueryParameterBuilder.create(STATE, Type.TEXT).operator(QueryParameter.Operator.IN).build()
        );
    }

    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(TELLER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(OFFICE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(EMPLOYEE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(CASHDRAW_LIMIT, Type.TEXT).build(),
                DisplayableFieldBuilder.create(STATE, Type.TEXT).build()
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
                .append("tajet_teller tl ");

        final List<QueryParameter> queryParameters = reportRequest.getQueryParameters();
        if (!queryParameters.isEmpty()) {
            final ArrayList<String> criteria = new ArrayList<>();
            queryParameters.forEach(queryParameter -> {
                if(queryParameter.getValue() != null && !queryParameter.getValue().isEmpty()) {
                    criteria.add(
                            CriteriaBuilder.buildCriteria(this.tellerColumnMapping.get(queryParameter.getName()), queryParameter)
                    );
                }
            });

            if (!criteria.isEmpty()) {
                query.append(" WHERE ");
                query.append(criteria.stream().collect(Collectors.joining(" AND ")));
            }

        }
        query.append(" ORDER BY tl.identifier");

        query.append(" LIMIT ");
        query.append(size);
        if (pageIndex > 0) {
            query.append(" OFFSET ");
            query.append(size * pageIndex);
        }

        return query.toString();
    }
}
