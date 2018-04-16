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
package org.apache.fineract.cn.reporting.service.internal.provider;

import org.apache.fineract.cn.reporting.api.v1.domain.ReportDefinition;
import org.apache.fineract.cn.reporting.service.ServiceConstants;
import org.apache.fineract.cn.reporting.service.spi.Report;
import org.apache.fineract.cn.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ReportSpecificationProvider implements ApplicationContextAware {

  private final Logger logger;
  private final HashMap<String, ReportSpecification> reportSpecificationCache = new HashMap<>();
  private final HashMap<String, List<ReportDefinition>> reportCategoryCache = new HashMap<>();

  private ApplicationContext applicationContext;

  @Autowired
  public ReportSpecificationProvider(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger) {
    super();
    this.logger = logger;
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
    this.initialize();
  }

  public List<String> getAvailableCategories() {
    return new ArrayList<>(this.reportCategoryCache.keySet());
  }

  public List<ReportDefinition> getAvailableReports(final String category) {
    this.logger.debug("Looking up report definitions for category {}.", category);
    return this.reportCategoryCache.getOrDefault(category, Collections.emptyList());
  }

  public Optional<ReportSpecification> getReportSpecification(final String category, final String identifier) {
    final String keyForReportSpecificationCache = this.buildKeyForSpecificationCache(category, identifier);
    this.logger.debug("Looking up report specification for {}.", keyForReportSpecificationCache);
    return Optional.ofNullable(this.reportSpecificationCache.get(keyForReportSpecificationCache));
  }

  private void initialize() {
    final Map<String, Object> beansWithAnnotation = this.applicationContext.getBeansWithAnnotation(Report.class);

    beansWithAnnotation.values().forEach(bean -> {
      final ReportSpecification reportSpecification = ReportSpecification.class.cast(bean);
      final Report report = reportSpecification.getClass().getAnnotation(Report.class);
      final String keyForReportSpecificationCache =
          this.buildKeyForSpecificationCache(report.category(), report.identifier());
      this.logger.debug("Adding report specification for {}", keyForReportSpecificationCache);

      this.reportCategoryCache.computeIfAbsent(report.category(), (key) -> new ArrayList<>());
      this.reportCategoryCache.get(report.category()).add(reportSpecification.getReportDefinition());
      this.reportSpecificationCache.put(keyForReportSpecificationCache, reportSpecification);
    });
  }

  private String buildKeyForSpecificationCache(final String category, final String identifier) {
    return category + "~" + identifier;
  }

  public Optional<ReportDefinition> findReportDefinition(final String category, final String identifier) {
    final List<ReportDefinition> reportDefinitions = this.reportCategoryCache.get(category);
    if (reportDefinitions != null) {
      return reportDefinitions
          .stream()
          .filter(reportDefinition -> reportDefinition.getIdentifier().equals(identifier))
          .findAny();
    } else {
      return Optional.empty();
    }
  }
}
