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
import java.util.stream.Collectors;

@Report(category = "Saving", identifier = "Listing")
public class SavingListReportSpecification implements ReportSpecification{
    private static final String SAVING_ACCOUNT = "Saving account";
    private static final String CUSTOMER = "Customer";
    private static final String EMPLOYEE = "Employee";
    //private static final String OFFICE = "Office";
    private static final String DATE_RANGE = "Date range";

    private final EntityManager entityManager;

    private final Logger logger;

    private final HashMap<String, String> customerColumnMapping = new HashMap<>();
    private final HashMap<String, String> savingAccountColumnMapping = new HashMap<>();
    // private final HashMap<String, String> officeColumnMapping = new HashMap<>();
    private final HashMap<String, String> employeeColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();


    @Autowired
    public SavingListReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger, final EntityManager entityManager){
        this.entityManager = entityManager;
        this.logger = logger;
    }


    @Override
    public ReportDefinition getReportDefinition() {

        final ReportDefinition reportDefinition = new ReportDefinition();
        reportDefinition.setIdentifier("Listing");
        reportDefinition.setName("Saving Listing");
        reportDefinition.setDescription("List of all savings.");
        reportDefinition.setQueryParameters(this.buildQueryParameters());
        reportDefinition.setDisplayableFields(this.buildDisplayableFields());
        return reportDefinition;
    }

    @Override
    public ReportPage generateReport(ReportRequest reportRequest, int pageIndex, int size) {
        return null;
    }

    @Override
    public void validate(ReportRequest reportRequest) throws IllegalArgumentException {

    }

    public String buildSavingAccountQuery(){

        return "SELECT ... FROM shed_product_instances pi";

    }

    public String buildCustomerQuery(){

        return null;
    }

    private void initializeMapping(){
        this.customerColumnMapping.put(CUSTOMER, "cst.identifier, cst.given_name, cst.middle_name, " +
                "cst.surname, cst.assigned_office");
        this.employeeColumnMapping.put(EMPLOYEE, "pi.created_by");
        this.savingAccountColumnMapping.put(SAVING_ACCOUNT, "pi.customer_identifier, pi.product_definition_id, pi.account_identifier, pi.a_state");
        this.savingAccountColumnMapping.put(DATE_RANGE, "pi.created_on");

        this.allColumnMapping.putAll(customerColumnMapping);
        this.allColumnMapping.putAll(employeeColumnMapping);
        this.allColumnMapping.putAll(savingAccountColumnMapping);
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


    private List<DisplayableField> buildDisplayableFields() {

        return null;
    }

    private List<QueryParameter> buildQueryParameters(){

        return null;
    }

}
