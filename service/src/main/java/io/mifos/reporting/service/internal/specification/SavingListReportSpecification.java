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
public class SavingListReportSpecification implements ReportSpecification {

    private static final String CUSTOMER = "Customer";
    private static final String FIRST_NAME = "First name";
    private static final String MIDDLE_NAME = "Middle name";
    private static final String LAST_NAME = "Last name";
    private static final String EMPLOYEE = "Employee";
    private static final String ACCOUNT_NUMBER = "Account number";
    private static final String ACCOUNT_TYPE = "Account type";
    private static final String STATE = "State";
    private static final String OFFICE = "Office";
    private static final String DATE_RANGE = "Date created";
    private static final String LAST_ACCOUNT_ACTIVITY = "Last account activity";

    private final EntityManager entityManager;

    private final Logger logger;

    private final HashMap<String, String> customerColumnMapping = new HashMap<>();
    private final HashMap<String, String> accountColumnMapping = new HashMap<>();
    private final HashMap<String, String> officeColumnMapping = new HashMap<>();
    private final HashMap<String, String> employeeColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();


    @Autowired
    public SavingListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger, final EntityManager entityManager) {
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

        final Query depositAccountQuery = this.entityManager.createNativeQuery(this.buildAccountQuery(reportRequest, pageIndex, size));

        final List<?> depositAccountResultList = depositAccountQuery.getResultList();
        reportPage.setRows(this.buildRows(reportRequest, depositAccountResultList));


        reportPage.setHasMore(
                !this.entityManager.createNativeQuery(this.buildAccountQuery(reportRequest, pageIndex + 1, size))
                        .getResultList().isEmpty()
        );

        reportPage.setGeneratedBy(UserContextHolder.checkedGetUser());
        reportPage.setGeneratedOn(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
        return reportPage;
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList(
                QueryParameterBuilder.create(DATE_RANGE, Type.DATE).operator(QueryParameter.Operator.BETWEEN).build(),
                QueryParameterBuilder.create(STATE, Type.TEXT).operator(QueryParameter.Operator.IN).build()
        );
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

        this.officeColumnMapping.put(OFFICE, "cst.assigned_office");

        this.employeeColumnMapping.put(EMPLOYEE, "pi.created_by");

        this.accountColumnMapping.put(ACCOUNT_NUMBER, "pi.customer_identifier, pi.account_identifier");
        this.accountColumnMapping.put(STATE, " pi.a_state");
        this.accountColumnMapping.put(ACCOUNT_TYPE, "pi.product_definition_id");
        this.accountColumnMapping.put(LAST_ACCOUNT_ACTIVITY, "acc_entry.transaction_date, acc_entry.message, acc_entry.amount, acc_entry.balance");
        this.accountColumnMapping.put(DATE_RANGE, "pi.created_on");

        this.allColumnMapping.putAll(customerColumnMapping);
        this.allColumnMapping.putAll(officeColumnMapping);
        this.allColumnMapping.putAll(employeeColumnMapping);
        this.allColumnMapping.putAll(accountColumnMapping);
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


    private List<Row> buildRows(final ReportRequest reportRequest, final List<?> depositAccountResultList) {
        final ArrayList<Row> rows =new ArrayList<>();
        depositAccountResultList.forEach(result -> {
            final Row row = new Row();
            row.setValues(new ArrayList<>());
            //Get the customer identifier to use for join queries.
            final String customerIdentifier;

            if (result instanceof Object[]) {
                final Object[] resultValues = (Object[]) result;

                customerIdentifier = resultValues[0].toString();

                for (final Object resultValue : resultValues) {
                    final Value value = new Value();
                    if (resultValue != null)
                        value.setValues(new String[]{resultValue.toString()});
                    else {
                        value.setValues(new String[]{});
                    }

                    row.getValues().add(value);
                }
            } else {

                customerIdentifier = result.toString();
                final Value value;
                value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            final String accountIdentifier;

            if (result instanceof Object[]) {
                final Object[] resultValues = (Object[]) result;

                accountIdentifier = resultValues[2].toString();

                for (final Object resultValue : resultValues) {
                    final Value value;
                    value = new Value();
                    if (resultValue != null)
                        value.setValues(new String[]{resultValue.toString()});
                    else {
                        value.setValues(new String[]{});
                    }

                    row.getValues().add(value);
                }
            } else {

                accountIdentifier = result.toString();
                final Value value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            final Query customerQuery = this.entityManager.createNativeQuery(this.buildCustomerQuery(reportRequest, customerIdentifier));
            final List<?> accountResultList = customerQuery.getResultList();
            final ArrayList<String> values = new ArrayList<>();
            accountResultList.forEach(customerResult -> {
                if (customerResult instanceof Object[]) {
                    final Object[] customerResultValues = (Object[]) customerResult;
                    final String customerValue = customerResultValues[0].toString();
                    values.add(customerValue);
                }
            });
            final Value customerValue = new Value();
            customerValue.setValues(values.toArray(new String[values.size()]));
            row.getValues().add(customerValue);

            final String officeQueryString = this.buildOfficeQuery(reportRequest, customerIdentifier);
            if (officeQueryString != null) {
                final Query officeQuery;
                officeQuery = this.entityManager.createNativeQuery(officeQueryString);
                final List<?> resultList = officeQuery.getResultList();
                final Value officeValue = new Value();
                officeValue.setValues(new String[]{resultList.get(0).toString()});
                row.getValues().add(officeValue);
            }

            final Query lastAccountActivivityQueryString = this.entityManager.createNativeQuery(this.buildLastAccountActivity(reportRequest, accountIdentifier));
            final List<?> lastActivityResultList = lastAccountActivivityQueryString.getResultList();
            final ArrayList<String> val = new ArrayList<>();
            lastActivityResultList.forEach( lastActivityResult -> {
                if (lastActivityResult instanceof Object[]){
                    final Object[] lastActivityResultValues = (Object[]) lastActivityResult;
                    final String lastActivityValue = lastActivityResultValues[1].toString();
                    val.add(lastActivityValue);
                }
            });
            final Value lastActivityValue = new Value();
            lastActivityValue.setValues(val.toArray(new String[values.size()]));
            row.getValues().add(lastActivityValue);

            rows.add(row);
        });

        return rows;
    }

    private String buildAccountQuery(final ReportRequest reportRequest, int pageIndex, int size) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.accountColumnMapping.get(displayableField.getName())
                    + this.employeeColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("shed_product_instances pi ");
        final List<QueryParameter> queryParameters = reportRequest.getQueryParameters();
        if (!queryParameters.isEmpty()) {
            final ArrayList<String> criteria = new ArrayList<>();
            queryParameters.forEach(queryParameter -> {
                if (queryParameter.getValue() != null && !queryParameter.getValue().isEmpty()) {
                    criteria.add(
                            CriteriaBuilder.buildCriteria(this.accountColumnMapping.get(queryParameter.getName()), queryParameter)
                    );
                    criteria.add(
                            CriteriaBuilder.buildCriteria(this.employeeColumnMapping.get(queryParameter.getName()), queryParameter)
                    );
                }
            });

            if (!criteria.isEmpty()) {
                query.append(" WHERE ");
                query.append(criteria.stream().collect(Collectors.joining(" AND ")));
            }

        }
        query.append(" ORDER BY pi.customer_identifier");

        query.append(" LIMIT ");
        query.append(size);
        if (pageIndex > 0) {
            query.append(" OFFSET ");
            query.append(size * pageIndex);
        }

        return query.toString();

        // return "SELECT ... FROM shed_product_instances pi";

    }

    private String buildCustomerQuery(final ReportRequest reportRequest, final String customerIdentifier) {
            final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
            final ArrayList<String> columns = new ArrayList<>();
            displayableFields.forEach(displayableField -> {
                final String column = this.customerColumnMapping.get(displayableField.getName());
                if (column != null) {
                    columns.add(column);
                }
            });
        return "SELECT " + columns.stream().collect(Collectors.joining(", ")) + " " +
                "FROM maat_customers cst " +
                "LEFT JOIN shed_product_instances pi on cst.identifier = pi.customer_identifier " +
                "WHERE pi.customer_identifier ='" + customerIdentifier + "' " +
                "ORDER BY cst.identifier";
    }

    private String buildOfficeQuery(final ReportRequest reportRequest, final String customerIdentifier) {
        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.officeColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        return "SELECT " + columns.stream().collect(Collectors.joining(", ")) + " " +
                "FROM maat_customers cst " +
                "WHERE cst.identifier ='" + customerIdentifier + "' " +
                "ORDER BY cst.identifier";
    }

    private String buildLastAccountActivity(final ReportRequest reportRequest, final String accountIdentifier){
        final List<DisplayableField> displayableFields = new ArrayList<>();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.accountColumnMapping.get(displayableField.getName());
            if(column != null){
                columns.add(column);
            }
        });

        return "SELECT " + columns.stream().collect(Collectors.joining(",")) + ""  +
                "FROM thoth_account_entries acc_entry" +
                "WHERE acc_entry.account_id ='" + accountIdentifier + "'" +
                "ORDER BY acc_entry.transaction_date";
    }

    private List<DisplayableField> buildDisplayableFields() {

        return Arrays.asList(
                DisplayableFieldBuilder.create(CUSTOMER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(FIRST_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(MIDDLE_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(LAST_NAME, Type.TEXT).build(),
                DisplayableFieldBuilder.create(ACCOUNT_NUMBER, Type.TEXT).mandatory().build(),

                DisplayableFieldBuilder.create(STATE,Type.TEXT).build(),
                DisplayableFieldBuilder.create(LAST_ACCOUNT_ACTIVITY, Type.DATE).build(),

                DisplayableFieldBuilder.create(EMPLOYEE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(OFFICE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(DATE_RANGE, Type.TEXT).build()
        );

    }


}
