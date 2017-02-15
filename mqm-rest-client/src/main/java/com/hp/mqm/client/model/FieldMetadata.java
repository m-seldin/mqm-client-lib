/*
 * Copyright 2017 Hewlett-Packard Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.mqm.client.model;

final public class FieldMetadata {

    private final String listName;
    private final String name;
    private final String logicalListName;
    private final boolean extensible;
    private final boolean multiValue;
    private final int order;

    public FieldMetadata(String name, String listName, String logicalListName, boolean extensible, boolean multiValue, int order) {
        this.name = name;
        this.listName = listName;
        this.logicalListName = logicalListName;
        this.extensible = extensible;
        this.multiValue = multiValue;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public String getListName() {
        return listName;
    }

    public String getLogicalListName() {
        return logicalListName;
    }

    public boolean isExtensible() {
        return extensible;
    }

    public boolean isMultiValue() {
        return multiValue;
    }

    public int getOrder() {
        return order;
    }

    public boolean isValid() {
        return this.listName != null &&
                this.name != null &&
                this.logicalListName != null;
    }
}