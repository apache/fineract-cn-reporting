package io.mifos.reporting.service.internal.specification;

import io.mifos.reporting.api.v1.domain.ReportDefinition;
import io.mifos.reporting.api.v1.domain.ReportPage;
import io.mifos.reporting.api.v1.domain.ReportRequest;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.spi.Report;
import io.mifos.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.EntityManager;

@Report(category = "Loan", identifier = "Listing")
public class LoanListingReportSpecification implements ReportSpecification {

    private static final String TOTAL_CASH_ON_HAND = "Cash on hand";


    private final Logger logger;

    private final EntityManager entityManager;

    @Autowired
    public LoanListingReportSpecification(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
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
