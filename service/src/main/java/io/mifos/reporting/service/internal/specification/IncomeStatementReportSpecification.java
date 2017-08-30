package io.mifos.reporting.service.internal.specification;

import io.mifos.reporting.api.v1.domain.ReportDefinition;
import io.mifos.reporting.api.v1.domain.ReportPage;
import io.mifos.reporting.api.v1.domain.ReportRequest;
import io.mifos.reporting.service.spi.ReportSpecification;

public class IncomeStatementReportSpecification implements ReportSpecification {

    public IncomeStatementReportSpecification(){

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
