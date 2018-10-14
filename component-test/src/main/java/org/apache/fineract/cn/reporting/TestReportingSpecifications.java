/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.reporting;

import org.apache.fineract.cn.reporting.api.v1.domain.ReportDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestReportingSpecifications extends AbstractReportingSpecificationTest {

  @Test
  public void shouldReturnBalanceSheetReportDefinition() {
    final List<ReportDefinition> balanceSheetReportDefinitions = super.testSubject.fetchReportDefinitions("Accounting");
    Assert.assertTrue(
            balanceSheetReportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Balancesheet"))
    );
  }

  @Test
  public void shouldReturnCustomerListReportDefinition() {
    final List<ReportDefinition> customerListReportDefinitions = super.testSubject.fetchReportDefinitions("Customer");
    Assert.assertTrue(
            customerListReportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Listing"))
    );
  }

  @Test
  public void shouldReturnDepositListReportDefinition() {
    final List<ReportDefinition> depositListReportDefinitions = super.testSubject.fetchReportDefinitions("Deposit");
    Assert.assertTrue(
            depositListReportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Listing"))
    );
  }

  @Test
  public void shouldReturnIncomeStatementReportDefinition() {
    final List<ReportDefinition> incomeStatementReportDefinitions = super.testSubject.fetchReportDefinitions("Accounting");
    Assert.assertTrue(
            incomeStatementReportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Incomestatement"))
    );
  }

  @Test
  public void shouldReturnLoanListReportDefinition() {
    final List<ReportDefinition> loanListReportDefinitions = super.testSubject.fetchReportDefinitions("Loan");
    Assert.assertTrue(
            loanListReportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Listing"))
    );
  }

  @Test
  public void shouldReturnTellerListReportDefinition() {
    final List<ReportDefinition> tellerListReportDefinitions = super.testSubject.fetchReportDefinitions("Teller");
    Assert.assertTrue(
            tellerListReportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Listing"))
    );
  }

  @Test
  public void shouldReturnTellerTransactionReportDefinition() {
    final List<ReportDefinition> tellerTransactionReportDefinitions = super.testSubject.fetchReportDefinitions("Teller");
    Assert.assertTrue(
            tellerTransactionReportDefinitions.stream().anyMatch(reportDefinition -> reportDefinition.getIdentifier().equals("Transactions"))
    );
  }
}
