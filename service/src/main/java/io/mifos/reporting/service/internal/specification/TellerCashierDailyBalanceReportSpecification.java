package io.mifos.reporting.service.internal.specification;

import io.mifos.reporting.api.v1.domain.*;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.Report;
import io.mifos.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.EntityManager;
import java.util.List;

@Report(category = "Teller" , identifier = "Transaction")
public class TellerCashierDailyBalanceReportSpecification implements ReportSpecification {

    private static final String TOTAL_CASH_ON_HAND = "Cash on hand";
    private static final String TOTAL_CASH_RECEIVED = "Cash received";
    private static final String TOTAL_CASH_DISBURSED = "Cash Disbursed";
    private static final String TOTAL_NEGOTIABLE_INSTRUMENT_RECEIVED = "Negotiable instrument received";
    private static final String TOTAL_CHEQUES_RECEIVED = "Total cheques received";
    private static final String TELLER = "Teller";
    private static final String EMPLOYEE = "Employee";
    private static final String OFFICE = "Office";
    private static final String CASHDRAW_LIMIT = "Cashdraw limit";

    private final Logger logger;

    private final EntityManager entityManager;

    @Autowired
    public TellerCashierDailyBalanceReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
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
        reportDefinition.setIdentifier("Transactions");
        reportDefinition.setName("Teller transactions");
        reportDefinition.setDescription("List total teller/cashier transactions.");
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
}
