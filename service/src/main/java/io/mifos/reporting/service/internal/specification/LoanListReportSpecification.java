package io.mifos.reporting.service.internal.specification;

import io.mifos.reporting.api.v1.domain.*;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.Report;
import io.mifos.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;

@Report(category = "Loan", identifier = "Listing")
public class LoanListReportSpecification implements ReportSpecification {

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

    private void initializeMapping() {
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

    private List<DisplayableField> buildDisplayableFields() {
        return null;
    }

    private List<QueryParameter> buildQueryParameters() {
        return null;
    }

    @Override
    public ReportPage generateReport(ReportRequest reportRequest, int pageIndex, int size) {
        return null;
    }

    @Override
    public void validate(ReportRequest reportRequest) throws IllegalArgumentException {

    }

    private String buildCustomerQuery(final ReportRequest reportRequest, int pageIndex, int size){
        return null;
    }

    private String buildLoanAccountQuery(final ReportRequest reportRequest, final String customerIdentifier){
        return null;
    }

    private String buildOfficeQuery(final ReportRequest reportRequest, final String customerIdentifier){
        return null;
    }
}
