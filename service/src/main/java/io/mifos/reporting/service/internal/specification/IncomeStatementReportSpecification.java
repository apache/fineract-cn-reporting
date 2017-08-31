package io.mifos.reporting.service.internal.specification;

import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.lang.DateConverter;
import io.mifos.reporting.api.v1.domain.*;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.*;
import org.slf4j.Logger;
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

@Report(category = "Accounting", identifier = "Incomestatement")
public class IncomeStatementReportSpecification implements ReportSpecification {

    private static final String DATE_RANGE = "Date range";
    private static final String TYPE = "Type";
    private static final String IDENTIFIER = "Identifier";
    private static final String NAME = "Name";
    private static final String HOLDER = "Holder";
    private static final String BALANCE = "Balance";
    private static final String STATE = "State";

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
        reportDefinition.setDescription("Income statement listing.");
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
        this.accountColumnMapping.put(HOLDER, "acc.holders");
        this.accountColumnMapping.put(BALANCE, "acc.balance");
        this.accountColumnMapping.put(STATE, "acc.a_state");

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

    private List<Row> buildRows(ReportRequest reportRequest, List<?> customerResultList) {
        return null;
    }

    private String buildAccountQuery(ReportRequest reportRequest, int pageIndex, int size) {
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
                .append("thoth_accounts acc ");

        final List<QueryParameter> queryParameters = reportRequest.getQueryParameters();
        if (!queryParameters.isEmpty()) {
            final ArrayList<String> criteria = new ArrayList<>();
            queryParameters.forEach(queryParameter -> {
                if(queryParameter.getValue() != null && !queryParameter.getValue().isEmpty()) {
                    criteria.add(
                            CriteriaBuilder.buildCriteria(this.accountColumnMapping.get(queryParameter.getName()), queryParameter)
                    );
                }
            });

            if (!criteria.isEmpty()) {
                query.append(" WHERE ");
                query.append(criteria.stream().collect(Collectors.joining(" AND ")));
            }

        }
        query.append(" ORDER BY acc.identifier");

        query.append(" LIMIT ");
        query.append(size);
        if (pageIndex > 0) {
            query.append(" OFFSET ");
            query.append(size * pageIndex);
        }

        return query.toString();
    }

    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(TYPE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(IDENTIFIER, Type.TEXT).build(),
                DisplayableFieldBuilder.create(NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(HOLDER, Type.TEXT).build(),
                DisplayableFieldBuilder.create(BALANCE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(STATE, Type.TEXT).build()
        );
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                QueryParameterBuilder.create(DATE_RANGE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
                QueryParameterBuilder.create(STATE, Type.TEXT).operator(QueryParameter.Operator.IN).build()
        );
    }
}
