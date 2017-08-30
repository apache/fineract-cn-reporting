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
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Report(category = "Loan", identifier = "Listing")
public class LoanListReportSpecification implements ReportSpecification {

    private static final String DATE_RANGE = "Date created";
    private static final String CUSTOMER = "Customer";
    private static final String FIRST_NAME = "First name";
    private static final String MIDDLE_NAME = "Middle name";
    private static final String LAST_NAME = "Last name";
    private static final String EMPLOYEE = "Employee";
    private static final String LOAN_STATE = "State";
    private static final String LOAN_ACCOUNT_NUMBER = "Account number";
    private static final String LOAN_TYPE = "Account type";
    private static final String LOAN_TERM = "Loan term";
    private static final String OFFICE = "Office";


    private final Logger logger;

    private final EntityManager entityManager;

    private final HashMap<String, String> customerColumnMapping = new HashMap<>();
    private final HashMap<String, String> loanColumnMapping = new HashMap<>();
    private final HashMap<String, String> officeColumnMapping = new HashMap<>();
    private final HashMap<String, String> employeeColumnMapping = new HashMap<>();
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

        this.loanColumnMapping.put(DATE_RANGE, "cases.created_on");
        this.loanColumnMapping.put(LOAN_STATE, "cases.current_state");
        this.loanColumnMapping.put(LOAN_TYPE, "cases.product_identifier");
        this.loanColumnMapping.put(EMPLOYEE, "cases.created_by");
        this.loanColumnMapping.put(LOAN_TERM,
                "il_cases.term_range_temporal_unit, " +
                "il_cases.term_range_minimum, " +
                "il_cases.term_range_maximum, " +
                "il_cases.balance_range_maximum");

        this.loanColumnMapping.put(LOAN_ACCOUNT_NUMBER, "il_cases.case_id");

        this.allColumnMapping.putAll(customerColumnMapping);
        this.allColumnMapping.putAll(loanColumnMapping);
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

            final DecimalFormat decimalFormat = new DecimalFormat("0.00");
            final Query accountQuery = this.entityManager.createNativeQuery(this.buildLoanAccountQuery(reportRequest, customerIdentifier));
            final List<?> accountResultList = accountQuery.getResultList();
            final ArrayList<String> values = new ArrayList<>();
            accountResultList.forEach(accountResult -> {
                if (accountResult instanceof Object[]) {
                    final Object[] accountResultValues;
                    accountResultValues = (Object[]) accountResult;
                    final String accountValue = accountResultValues[0].toString() + " (" +
                            decimalFormat.format(Double.valueOf(accountResultValues[1].toString())) + ")";
                    values.add(accountValue);
                }
            });
            final Value accountValue = new Value();
            accountValue.setValues(values.toArray(new String[values.size()]));
            row.getValues().add(accountValue);

            rows.add(row);
        });

        return rows;
    }

    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(CUSTOMER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(FIRST_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(MIDDLE_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LAST_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LOAN_TYPE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LOAN_ACCOUNT_NUMBER, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LOAN_STATE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LOAN_TERM, Type.TEXT).build(),
                DisplayableFieldBuilder.create(EMPLOYEE, Type.TEXT).build(),
                DisplayableFieldBuilder.create(OFFICE, Type.TEXT).build()
        );
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                QueryParameterBuilder.create(DATE_RANGE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
                QueryParameterBuilder.create(LOAN_STATE, Type.TEXT).operator(QueryParameter.Operator.IN).build()
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
                "ORDER BY il_cases.cases_id";
    }

    //Need this for getting details from cases table
    private String buildLoanCaseQuery(final ReportRequest reportRequest, final String customerIdentifier){
        return null;
    }
}
