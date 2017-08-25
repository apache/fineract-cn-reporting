package io.mifos.reporting.service.internal.specification;

import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.lang.DateConverter;
import io.mifos.reporting.api.v1.domain.*;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.CriteriaBuilder;
import io.mifos.reporting.service.spi.Report;
import io.mifos.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Report(category = "Teller" , identifier = "Listing")
public class TellerListingReportSpecification implements ReportSpecification {

   // private static final String TOTAL_CASH_ON_HAND = "Cash on hand";
   // private static final String TOTAL_CASH_RECEIVED = "Cash received";
   // private static final String TOTAL_CASH_DISBURSED = "Cash Disbursed";
   // private static final String TOTAL_NEGOTIABLE_INSTRUMENT_RECEIVED = "Negotiable instrument received";
   // private static final String TOTAL_CHEQUES_RECEIVED = "Total cheques received";
    private static final String TELLER = "Teller";
    private static final String EMPLOYEE = "Employee";
    private static final String OFFICE = "Office";
    private static final String CASHDRAW_LIMIT = "Cashdraw limit";
    private static final String STATE = "State";

    private final Logger logger;

    private final EntityManager entityManager;

    private final HashMap<String, String> tellerColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();


    @Autowired
    public TellerListingReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                                        final EntityManager entityManager) {
        super();
        this.logger = logger;
        this.entityManager = entityManager;
        this.initializeMapping();
    }

    private void initializeMapping() {
        this.tellerColumnMapping.put(TELLER, "tl.identifier");
        this.tellerColumnMapping.put(OFFICE, "tl.office_identifier");
        this.tellerColumnMapping.put(CASHDRAW_LIMIT, "tl.cashdraw_limit");
        this.tellerColumnMapping.put(EMPLOYEE, "tl.assigned_employee_identifier");
        this.tellerColumnMapping.put(STATE, "tl.a_state");

        this.allColumnMapping.putAll(tellerColumnMapping);
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

    private List<DisplayableField> buildDisplayableFields() {
        return null;
    }

    private List<QueryParameter> buildQueryParameters() {
        return null;
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
        reportPage.setRows(this.buildRows(reportRequest, tellerResultList));

        reportPage.setHasMore(
                !this.entityManager.createNativeQuery(this.buildTellerQuery(reportRequest, pageIndex + 1, size))
                        .getResultList().isEmpty()
        );

        reportPage.setGeneratedBy(UserContextHolder.checkedGetUser());
        reportPage.setGeneratedOn(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
        return reportPage;
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

    private List<Row> buildRows(ReportRequest reportRequest, List<?> tellerResultList) {
    return null;
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

    @Override
    public void validate(ReportRequest reportRequest) throws IllegalArgumentException {

    }
    
}
