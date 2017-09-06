package io.mifos.reporting;

import io.mifos.reporting.api.v1.domain.ReportDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestSavingListReportSpecification extends AbstractReportingSpecificationTest {
    public TestSavingListReportSpecification() {
        super();
    }

    @Test
    public void shouldReturnReportDefinition() {
        final List<ReportDefinition> reportDefinitions = super.testSubject.fetchReportDefinitions("Deposit");
        Assert.assertTrue(
                reportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Listing"))
        );
    }
}
