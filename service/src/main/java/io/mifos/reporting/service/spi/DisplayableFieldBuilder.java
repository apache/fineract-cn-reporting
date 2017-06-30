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
package io.mifos.reporting.service.spi;

import io.mifos.reporting.api.v1.domain.DisplayableField;
import io.mifos.reporting.api.v1.domain.Type;

public class DisplayableFieldBuilder {

  private String name;
  private Type type;
  private Boolean mandatory;

  private DisplayableFieldBuilder(final String name, final Type type) {
    super();
    this.name = name;
    this.type = type;
  }

  public static DisplayableFieldBuilder create(final String name, final Type type) {
    return new DisplayableFieldBuilder(name, type);
  }

  public DisplayableFieldBuilder mandatory() {
    this.mandatory = Boolean.TRUE;
    return this;
  }

  public DisplayableField build() {
    final DisplayableField displayableField = new DisplayableField();
    displayableField.setName(this.name);
    displayableField.setType(this.type);
    displayableField.setMandatory(this.mandatory != null ? this.mandatory : Boolean.FALSE);
    return displayableField;
  }
}
