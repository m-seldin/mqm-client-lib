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

final public class ListItem {

    private final String id;
    private final String logicalName;
    private final String name;
    private final ListItem root;

    public ListItem(String id, String logicalName, String name, ListItem root) {
        this.id = id;
        this.name = name;
        this.logicalName = logicalName;
        this.root = root;
    }

    public String getId() {
        return id;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getName() {
        return name;
    }

    public ListItem getRoot() {
        return root;
    }
}
