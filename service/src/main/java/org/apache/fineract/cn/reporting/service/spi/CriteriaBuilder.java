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
package org.apache.fineract.cn.reporting.service.spi;

import org.apache.fineract.cn.reporting.api.v1.domain.QueryParameter;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.codecs.MySQLCodec;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

public class CriteriaBuilder {

  // https://www.owasp.org/index.php/SQL_Injection_Prevention_Cheat_Sheet
  public static Encoder ENCODER;
  public static MySQLCodec MY_SQL_CODEC;

  static {
    // TODO move this code into bean
    try {
      ENCODER = ESAPI.encoder();
      MY_SQL_CODEC = new MySQLCodec(MySQLCodec.Mode.ANSI);
    } catch(final Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private CriteriaBuilder() {
    super();
  }

  public static String buildCriteria(final String field, final QueryParameter queryParameter) {
    final StringBuilder criteria = new StringBuilder(field);

    switch (queryParameter.getOperator()) {
      case EQUALS:
        criteria.append(" = '");
        criteria.append(CriteriaBuilder.ENCODER.encodeForSQL(CriteriaBuilder.MY_SQL_CODEC, queryParameter.getValue()));
        criteria.append("'");
        break;
      case LIKE:
        criteria.append(" LIKE '%");
        criteria.append(CriteriaBuilder.ENCODER.encodeForSQL(CriteriaBuilder.MY_SQL_CODEC, queryParameter.getValue()));
        criteria.append("%'");
        break;
      case GREATER:
        criteria.append(" > '");
        criteria.append(CriteriaBuilder.ENCODER.encodeForSQL(CriteriaBuilder.MY_SQL_CODEC, queryParameter.getValue()));
        criteria.append("'");
        break;
      case LESSER:
        criteria.append(" < '");
        criteria.append(CriteriaBuilder.ENCODER.encodeForSQL(CriteriaBuilder.MY_SQL_CODEC, queryParameter.getValue()));
        criteria.append("'");
        break;
      case IN:
        criteria.append(" in (");
        final Set<String> strings = StringUtils.commaDelimitedListToSet(queryParameter.getValue());
        criteria.append(
            strings
                .stream()
                .map(s -> "'" + CriteriaBuilder.ENCODER.encodeForSQL(CriteriaBuilder.MY_SQL_CODEC, s) + "'")
                .collect(Collectors.joining(","))
        );
        criteria.append(")");
        break;
      case BETWEEN:
        final String[] splitString = queryParameter.getValue().split("\\.\\.");
        criteria.append(" BETWEEN '");
        criteria.append(CriteriaBuilder.ENCODER.encodeForSQL(CriteriaBuilder.MY_SQL_CODEC, splitString[0]));
        criteria.append("' AND '");
        criteria.append(CriteriaBuilder.ENCODER.encodeForSQL(CriteriaBuilder.MY_SQL_CODEC, splitString[1]));
        criteria.append("'");
        break;
    }

    return criteria.toString();
  }
}
