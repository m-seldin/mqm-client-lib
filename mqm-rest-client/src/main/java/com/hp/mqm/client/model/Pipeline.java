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

import java.util.List;

final public class Pipeline {

	private long id;
	private String name;
	private Boolean root;
	private long workspaceId;
	private Long releaseId;
	private Boolean ignoreTests;
	private List<Taxonomy> taxonomies;
	private List<ListField> fields;

	public Pipeline(long id, String name, Boolean root, long workspaceId, Long releaseId, List<Taxonomy> taxonomies, List<ListField> fields, Boolean ignoreTests) {
		this.id = id;
		this.name = name;
		this.root = root;
		this.workspaceId = workspaceId;
		this.releaseId = releaseId;
		this.taxonomies = taxonomies;
		this.fields = fields;
		this.ignoreTests = ignoreTests;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getWorkspaceId() {
		return workspaceId;
	}

	public Long getReleaseId() {
		return releaseId;
	}

	public void setReleaseId(Long releaseId) {
		this.releaseId = releaseId;
	}

	public List<Taxonomy> getTaxonomies() {
		return taxonomies;
	}

	public void setTaxonomies(List<Taxonomy> taxonomies) {
		this.taxonomies = taxonomies;
	}

	public List<ListField> getFields() {
		return fields;
	}

	public void setFields(List<ListField> fields) {
		this.fields = fields;
	}

	public Boolean isRoot() {
		return root;
	}

	public Boolean getIgnoreTests() {
		return ignoreTests;
	}

	public void setIgnoreTests(Boolean ignoreTests) {
		this.ignoreTests = ignoreTests;
	}

}
