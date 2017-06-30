/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.reporting.service.rest;

import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.internal.service.ReportingService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/")
public class ReportingRestController {

  private final Logger logger;
  private final CommandGateway commandGateway;
  private final ReportingService reportingService;

  @Autowired
  public ReportingRestController(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                 final CommandGateway commandGateway,
                                 final ReportingService reportingService) {
    super();
    this.logger = logger;
    this.commandGateway = commandGateway;
    this.reportingService = reportingService;
  }
}
