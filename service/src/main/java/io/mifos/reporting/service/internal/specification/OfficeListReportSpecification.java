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
@Report(category = "Organization", identifier = "Office")
public class OfficeListReportSpecification implements ReportSpecification {

    private static final String OFFICE = "Identifier";
    private static final String OFFICE_NAME = "Office";
    private static final String DESCRIPTION = "Description";
    private static final String CREATED_BY = "Created By";
   // private static final String STREET = "Street";
    //private static final String CITY = "City";
   // private static final String REGION = "Region";
   // private static final String POSTAL_CODE = "Postal Code";
   // private static final String COUNTRY = "Country";
    private static final String ADDRESS = "Address";

    private final Logger logger;

    private final EntityManager entityManager;
    private final HashMap<String, String> officeColumnMapping = new HashMap<>();
    private final HashMap<String, String> addressColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();

    @Autowired
    public OfficeListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                           final EntityManager entityManager) {
        super();
        this.logger = logger;
        this.entityManager = entityManager;
        this.initializeMapping();
    }

    @Override
    public ReportDefinition getReportDefinition() {
        final ReportDefinition reportDefinition = new ReportDefinition();
        reportDefinition.setIdentifier("Office");
        reportDefinition.setName("Office Listing");
        reportDefinition.setDescription("List of all Offices.");
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

        final Query customerQuery = this.entityManager.createNativeQuery(this.buildOfficeQuery(reportRequest, pageIndex, size));
        final List<?> customerResultList =  customerQuery.getResultList();
        reportPage.setRows(this.buildRows(reportRequest, customerResultList));

        reportPage.setHasMore(
                !this.entityManager.createNativeQuery(this.buildOfficeQuery(reportRequest, pageIndex + 1, size))
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
        this.officeColumnMapping.put(OFFICE, "ho.id");
        this.officeColumnMapping.put(OFFICE_NAME, "ho.a_name");
        this.officeColumnMapping.put(DESCRIPTION, "ho.description");
        this.officeColumnMapping.put(CREATED_BY, "ho.created_by");

        this.addressColumnMapping.put(ADDRESS, "CONCAT(IFNULL(ha.street, ', '), " +
                "IFNULL(ha.postal_code, ', '), IFNULL(ha.city, ', ')," +
                " IFNULL(ha.region, ', '), IFNULL(ha.country, ','))");

        this.allColumnMapping.putAll(officeColumnMapping);
        this.allColumnMapping.putAll(addressColumnMapping);
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

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                //QueryParameterBuilder.create(DATE_RANGE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
                //QueryParameterBuilder.create(STATE, Type.TEXT).operator(QueryParameter.Operator.IN).build()
        );
    }

    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(OFFICE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(OFFICE_NAME, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(DESCRIPTION, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(CREATED_BY, Type.TEXT).build(),
                DisplayableFieldBuilder.create(ADDRESS, Type.TEXT).mandatory().build()
        );
    }

    private List<Row> buildRows(final ReportRequest reportRequest, final List<?> officeResultList) {
        final ArrayList<Row> rows = new ArrayList<>();

        officeResultList.forEach(result -> {
            final Row row = new Row();
            row.setValues(new ArrayList<>());

            final String officeIdentifier;

            if (result instanceof Object[]) {
                final Object[] resultValues = (Object[]) result;

                officeIdentifier = resultValues[0].toString();

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
                officeIdentifier = result.toString();

                final Value value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            final String addressQueryString = this.buildAddressQuery(reportRequest, officeIdentifier);
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

    private String buildOfficeQuery(final ReportRequest reportRequest, int pageIndex, int size) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.officeColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("horus_offices ho ");

        final List<QueryParameter> queryParameters = reportRequest.getQueryParameters();
        if (!queryParameters.isEmpty()) {
            final ArrayList<String> criteria = new ArrayList<>();
            queryParameters.forEach(queryParameter -> {
                if(queryParameter.getValue() != null && !queryParameter.getValue().isEmpty()) {
                    criteria.add(
                            CriteriaBuilder.buildCriteria(this.officeColumnMapping.get(queryParameter.getName()), queryParameter)
                    );
                }
            });

            if (!criteria.isEmpty()) {
                query.append(" WHERE ");
                query.append(criteria.stream().collect(Collectors.joining(" AND ")));
            }

        }
        query.append(" ORDER BY ho.a_name");

        query.append(" LIMIT ");
        query.append(size);
        if (pageIndex > 0) {
            query.append(" OFFSET ");
            query.append(size * pageIndex);
        }

        return query.toString();
    }

    private String buildAddressQuery(final ReportRequest reportRequest, final String officeIdentifier) {

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
                    "FROM horus_addresses ha " +
                    "LEFT JOIN horus_offices ho on ha.office_id = ho.id " +
                    "WHERE ho.id ='" + officeIdentifier + "' ";
        }
        return null;
    }
}


