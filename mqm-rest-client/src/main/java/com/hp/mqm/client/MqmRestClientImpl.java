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

package com.hp.mqm.client;

import com.hp.mqm.client.exception.*;
import com.hp.mqm.client.exception.FileNotFoundException;
import com.hp.mqm.client.model.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.commons.codec.binary.Base64;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class MqmRestClientImpl extends AbstractMqmRestClient implements MqmRestClient {
	private static final Logger logger = Logger.getLogger(MqmRestClientImpl.class.getName());

	private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final String PREFIX_CI = "analytics/ci/";
	private static final String PREFIX_BDI = "analytics/bdi/";
	private static final String URI_TEST_RESULT_PUSH = PREFIX_CI + "test-results?skip-errors={0}";
	private static final String URI_TEST_RESULT_STATUS = PREFIX_CI + "test-results/{0}";
	private static final String URI_TEST_RESULT_LOG = URI_TEST_RESULT_STATUS + "/log";
	private static final String URI_JOB_CONFIGURATION = "analytics/ci/servers/{0}/jobs/{1}/configuration";
	private static final String URI_DELETE_NODES_TESTS = "analytics/ci/pipelines/{0}/jobs/{1}/tests";
	private static final String URI_PREFLIGHT = "analytics/ci/servers/{0}/jobs/{1}/tests-result-preflight";
	private static final String URI_BASE64SUPPORT = "analytics/ci/servers/tests-result-preflight-base64";
	private static final String URI_WORKSPACE_BY_JOB_AND_SERVER = PREFIX_CI + "servers/{0}/jobs/{1}/workspaceId";
	private static final String URI_BDI_CONFIGURATION = PREFIX_BDI + "configuration";
	private static final String URI_BDI_ACCESS_TOKEN = PREFIX_BDI + "token";
	private static final String URI_RELEASES = "releases";
	private static final String URI_WORKSPACES = "workspaces";
	private static final String URI_LIST_ITEMS = "list_nodes";
	private static final String URI_METADATA_FIELDS = "metadata/fields";
	private static final String URI_PUT_EVENTS = "analytics/ci/events";
	private static final String URI_GET_ABRIDGED_TASKS = "analytics/ci/servers/{0}/tasks?self-type={1}&self-url={2}&api-version={3}&sdk-version={4}";
	private static final String URI_PUT_ABRIDGED_RESULT = "analytics/ci/servers/{0}/tasks/{1}/result";
	private static final String URI_TAXONOMY_NODES = "taxonomy_nodes";

	private static final String HEADER_ACCEPT = "Accept";

	private static final int DEFAULT_OFFSET = 0;
	private static final int DEFAULT_LIMIT = 100;
	private static final int MAX_GET_LIMIT = 1000;
	private static final String CONTENT_ENCODING_GZIP = "gzip";

	/**
	 * Constructor for AbstractMqmRestClient.
	 *
	 * @param connectionConfig MQM connection configuration, Fields 'location', 'domain', 'project' and 'clientType' must not be null or empty.
	 */
	public MqmRestClientImpl(MqmConnectionConfig connectionConfig) {
		super(connectionConfig);
	}

	@Override
	public long postTestResult(InputStreamSource inputStreamSource, boolean skipErrors) {
		return postTestResult(createGZipEntity(inputStreamSource.getInputStream()), skipErrors);
	}

	@Override
	public long postTestResult(File testResultReport, boolean skipErrors) {
		try {
			return postTestResult(createGZipEntity(new FileInputStream(testResultReport)), skipErrors);
		} catch (java.io.FileNotFoundException fnfe) {
			logger.severe("file " + testResultReport + " not found");
			return -1;
		}
	}

	@Override
	public Boolean isTestResultRelevant(String serverIdentity, String jobName) {

		URI supportsBase64Uri =createSharedSpaceInternalApiUri(URI_BASE64SUPPORT);
		HttpGet request = new HttpGet(supportsBase64Uri);
		HttpResponse response = null;
		String jobNameForSending = jobName;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				logger.log(Level.INFO,"Octane supports base64 encoding");
				jobNameForSending = Base64.encodeBase64String(jobName.getBytes());
			}
		}catch (IOException ex){
			logger.log(Level.INFO,"Octane does not support base64 encoding");
		}

		logger.log(Level.INFO,String.format("Job name before encoding: %s, after encoding : %s",jobName,jobNameForSending));

		URI getUri = createSharedSpaceInternalApiUri(URI_PREFLIGHT, serverIdentity, jobNameForSending);
		try {
			URIBuilder uriPreflight = new URIBuilder(
					createSharedSpaceInternalApiUri(URI_PREFLIGHT, serverIdentity, jobNameForSending)
				).addParameter("isBase64", "true");

			logger.log(Level.INFO,String.format("test preflight URI: %s",uriPreflight.build().getPath()));

			getUri = uriPreflight.build();
		}catch (URISyntaxException ex){
			logger.log(Level.SEVERE,"Error creating uri for test preflight!",ex);
		}


		request = new HttpGet(getUri);
		response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw createRequestException("Result status retrieval failed", response);
			}
			return Boolean.parseBoolean(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
		} catch (IOException e) {
			throw new RequestErrorException("Cannot obtain status.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public JSONObject getBdiConfiguration() {
		HttpGet request = new HttpGet(createSharedSpaceInternalApiUri(URI_BDI_CONFIGURATION));
		HttpResponse response = null;
		try {
			response = execute(request);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_NO_CONTENT) {
				logger.config("BDI is not configured in Octane");
				return null;
			}

			if (statusCode != HttpStatus.SC_OK) {
				throw createRequestException("BDI configuration retrieval failed", response);
			}

			String bdiConfiguration = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			return JSONObject.fromObject(bdiConfiguration);
		} catch (IOException e) {
			throw new RequestErrorException("Cannot obtain status.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public String getBdiTokenData() {
		HttpGet request = new HttpGet(createSharedSpaceInternalApiUri(URI_BDI_ACCESS_TOKEN));
		HttpResponse response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			} else {
				throw createRequestException("BDI token retrieval failed", response);
			}
		} catch (IOException e) {
			throw new RequestErrorException("failed to parse token data response", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public List<String> getJobWorkspaceId(String ciServerId, String ciJobName) {
		HttpGet request = new HttpGet(createSharedSpaceInternalApiUri(URI_WORKSPACE_BY_JOB_AND_SERVER, ciServerId, ciJobName));
		HttpResponse response = null;
		try {
			response = execute(request);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_NO_CONTENT) {
				logger.info("Job " + ciJobName + " has no build context in Octane");
				return new ArrayList<>();
			}

			if (statusCode != HttpStatus.SC_OK) {
				throw createRequestException("workspace retrieval failed", response);
			}

			JSONArray workspaces = JSONArray.fromObject(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
			return workspaces.subList(0, workspaces.size());
		} catch (IOException e) {
			throw new RequestErrorException("Cannot obtain status.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public TestResultStatus getTestResultStatus(long id) {
		HttpGet request = new HttpGet(createSharedSpaceInternalApiUri(URI_TEST_RESULT_STATUS, id));

		HttpResponse response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw createRequestException("Result status retrieval failed", response);
			}
			String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			JSONObject jsonObject = JSONObject.fromObject(json);
			Date until = null;
			if (jsonObject.has("until")) {
				try {
					until = parseDatetime(jsonObject.getString("until"));
				} catch (ParseException e) {
					throw new RequestErrorException("Cannot obtain status", e);
				}
			}
			return new TestResultStatus(jsonObject.getString("status"), until);
		} catch (IOException e) {
			throw new RequestErrorException("Cannot obtain status.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public void getTestResultLog(long id, LogOutput output) {
		HttpGet request = new HttpGet(createSharedSpaceInternalApiUri(URI_TEST_RESULT_LOG, id));
		HttpResponse response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw createRequestException("Log retrieval failed", response);
			}
			output.setContentType(response.getFirstHeader("Content-type").getValue());
			InputStream is = response.getEntity().getContent();
			IOUtils.copy(is, output.getOutputStream());
			IOUtils.closeQuietly(is);
		} catch (IOException e) {
			throw new RequestErrorException("Cannot obtain log.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public JobConfiguration getJobConfiguration(String serverIdentity, String jobName) {
		HttpGet request = new HttpGet(createSharedSpaceInternalApiUri(URI_JOB_CONFIGURATION, serverIdentity, jobName));
		HttpResponse response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw createRequestException("Job configuration retrieval failed", response);
			}
			String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			try {
				JSONObject jsonObject = JSONObject.fromObject(json);
				List<Pipeline> pipelines = new LinkedList<>();
				for (JSONObject relatedContext : getJSONObjectCollection(jsonObject, "data")) {
					if ("pipeline".equals(relatedContext.getString("contextEntityType"))) {
						pipelines.add(toPipeline(relatedContext));
					} else {
						logger.info("Context type '" + relatedContext.get("contextEntityType") + "' is not supported");
					}
				}
				return new JobConfiguration(pipelines);
			} catch (JSONException e) {
				throw new RequestErrorException("Failed to obtain job configuration", e);
			}
		} catch (IOException e) {
			throw new RequestErrorException("Cannot retrieve job configuration from MQM.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public Pipeline createPipeline(String serverIdentity, String projectName, String pipelineName, long workspaceId, Long releaseId, String structureJson, String serverJson) {
		HttpPost request = new HttpPost(createSharedSpaceInternalApiUri(URI_JOB_CONFIGURATION, serverIdentity, projectName));
		JSONObject pipelineObject = new JSONObject();
		pipelineObject.put("contextEntityType", "pipeline");
		pipelineObject.put("contextEntityName", pipelineName);
		pipelineObject.put("workspaceId", workspaceId);
		pipelineObject.put("releaseId", releaseId);
		pipelineObject.put("server", JSONObject.fromObject(serverJson));
		pipelineObject.put("structure", JSONObject.fromObject(structureJson));
		request.setEntity(new StringEntity(pipelineObject.toString(), ContentType.APPLICATION_JSON));
		HttpResponse response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
				throw createRequestException("Pipeline creation failed", response);
			}
			String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			return getPipelineByName(json, pipelineName, workspaceId);
		} catch (IOException e) {
			throw new RequestErrorException("Cannot create pipeline in MQM.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}


	@Override
	public Pipeline updatePipeline(String serverIdentity, String jobName, Pipeline pipeline) {
		HttpPut request = new HttpPut(createSharedSpaceInternalApiUri(URI_JOB_CONFIGURATION, serverIdentity, jobName));

		JSONObject pipelineObject = new JSONObject();
		pipelineObject.put("contextEntityType", "pipeline");
		pipelineObject.put("contextEntityId", pipeline.getId());
		pipelineObject.put("workspaceId", pipeline.getWorkspaceId());
		if (pipeline.getName() != null) {
			pipelineObject.put("contextEntityName", pipeline.getName());
		}
		if (pipeline.getReleaseId() != null) {
			if (pipeline.getReleaseId() == -1) {
				pipelineObject.put("releaseId", JSONNull.getInstance());
			} else {
				pipelineObject.put("releaseId", pipeline.getReleaseId());
			}
		}
		if (pipeline.getIgnoreTests() != null) {
			pipelineObject.put("ignoreTests", pipeline.getIgnoreTests());
		}
		if (pipeline.getTaxonomies() != null) {
			JSONArray taxonomies = taxonomiesArray(pipeline.getTaxonomies());
			pipelineObject.put("taxonomies", taxonomies);
		}

		if (pipeline.getFields() != null) {
			JSONObject listFields = listFieldsObject(pipeline.getFields());
			pipelineObject.put("listFields", listFields);
		}

		JSONArray data = new JSONArray();
		data.add(pipelineObject);
		JSONObject payload = new JSONObject();
		payload.put("data", data);

		request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
		request.setHeader(HEADER_ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
		HttpResponse response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw createRequestException("Pipeline update failed", response);
			}
			String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			return getPipelineById(json, pipeline.getId());
		} catch (IOException e) {
			throw new RequestErrorException("Cannot update pipeline.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public void deleteTestsFromPipelineNodes(String jobName, Long pipelineId, Long workspaceId) {
		HttpDelete request = new HttpDelete(createWorkspaceInternalApiUriMap(URI_DELETE_NODES_TESTS, workspaceId, pipelineId, jobName));

		HttpResponse response = null;
		try {
			response = execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw createRequestException("delete tests failed", response);
			}

		} catch (IOException e) {
			throw new RequestErrorException("Cannot delete tests.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	private Date parseDatetime(String datetime) throws ParseException {
		return new SimpleDateFormat(DATETIME_FORMAT).parse(datetime);
	}

	private JSONArray taxonomiesArray(List<Taxonomy> taxonomies) {
		JSONArray ret = new JSONArray();
		for (Taxonomy taxonomy : taxonomies) {
			ret.add(fromTaxonomy(taxonomy));
		}
		return ret;
	}

	private JSONObject listFieldsObject(List<ListField> fields) {
		JSONObject ret = new JSONObject();
		for (ListField field : fields) {
			putListField(ret, field);
		}
		return ret;
	}

	private void putListField(JSONObject ret, ListField listField) {
		JSONArray valArray = new JSONArray();
		for (ListItem value : listField.getValues()) {
			JSONObject val = new JSONObject();
			if (value.getId() != null) {
				val.put("id", value.getId());
			} else {
				val.put("name", value.getName());
			}
			valArray.add(val);
		}
		ret.put(listField.getName(), valArray);
	}

	private Taxonomy toTaxonomy(JSONObject t) {
		JSONObject parent = t.optJSONObject("parent");
		String name = t.has("name") ? t.getString("name") : null;
		if (parent != null) {
			return new Taxonomy(t.getLong("id"), name, toTaxonomy(parent));
		} else {
			return new Taxonomy(t.getLong("id"), name, null);
		}
	}

	private JSONObject fromTaxonomy(Taxonomy taxonomy) {
		JSONObject t = new JSONObject();
		if (taxonomy.getId() != null && taxonomy.getId() != 0) {
			t.put("id", taxonomy.getId());
		}
		if (taxonomy.getName() != null) {    //todo seems that name can be ommited in case that id exists
			t.put("name", taxonomy.getName());
		}
		if (taxonomy.getRoot() != null) {
			t.put("parent", fromTaxonomy(taxonomy.getRoot()));
		} else {
			t.put("parent", JSONNull.getInstance());
		}
		return t;
	}

	private Pipeline getPipelineByName(String json, String pipelineName, long workspaceId) {
		try {
			for (JSONObject item : getJSONObjectCollection(JSONObject.fromObject(json), "data")) {
				if (!"pipeline".equals(item.getString("contextEntityType"))) {
					continue;
				}
				if (!item.getBoolean("pipelineRoot")) {
					continue;
				}
				if (!pipelineName.equals(item.getString("contextEntityName"))) {
					continue;
				}
				if (workspaceId != item.getLong("workspaceId")) {
					continue;
				}
				return toPipeline(item);
			}
			throw new RequestErrorException("Failed to obtain pipeline: item not found");
		} catch (JSONException e) {
			throw new RequestErrorException("Failed to obtain pipeline", e);
		}
	}

	private Pipeline getPipelineById(String json, long pipelineId) {
		try {
			for (JSONObject item : getJSONObjectCollection(JSONObject.fromObject(json), "data")) {
				if (!"pipeline".equals(item.getString("contextEntityType"))) {
					continue;
				}
				if (pipelineId != item.getLong("contextEntityId")) {
					continue;
				}
				return toPipeline(item);
			}
			throw new RequestErrorException("Failed to obtain pipeline: item not found");
		} catch (JSONException e) {
			throw new RequestErrorException("Failed to obtain pipeline", e);
		}
	}

	private ListItem toListItem(JSONObject field) {
		String id = null;
		String name = null;
		if (field.has("id")) {
			id = field.getString("id");
		}
		if (field.has("name")) {
			name = field.getString("name");
		}
		return new ListItem(id, null, name, null);
	}

	private Pipeline toPipeline(JSONObject pipelineObject) {
		List<Taxonomy> taxonomies = new LinkedList<>();
		List<ListField> fields = new LinkedList<>();

		if (pipelineObject.has("taxonomies")) {
			for (JSONObject taxonomy : getJSONObjectCollection(pipelineObject, "taxonomies")) {
				taxonomies.add(toTaxonomy(taxonomy));
			}
		}

		if (pipelineObject.has("listFields")) {
			JSONObject listFields = pipelineObject.getJSONObject("listFields");
			Iterator<?> keys = listFields.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				if (listFields.get(key) instanceof JSONArray) {
					List<ListItem> fieldValues = new LinkedList<>();
					for (JSONObject field : getJSONObjectCollection(listFields, key)) {
						fieldValues.add(toListItem(field));
					}
					fields.add(new ListField(key, fieldValues));
				}
			}
		}
		return new Pipeline(pipelineObject.getLong("contextEntityId"),
				pipelineObject.getString("contextEntityName"),
				pipelineObject.getBoolean("pipelineRoot"),
				pipelineObject.getLong("workspaceId"),
				pipelineObject.has("releaseId") && !pipelineObject.get("releaseId").equals(JSONNull.getInstance()) ? pipelineObject.getLong("releaseId") : null, taxonomies, fields,
				pipelineObject.has("ignoreTests") && !pipelineObject.get("ignoreTests").equals(JSONNull.getInstance()) ? pipelineObject.getBoolean("ignoreTests") : null);
	}

	@Override
	public PagedList<Release> queryReleases(String name, long workspaceId, int offset, int limit) {
		List<String> conditions = new LinkedList<>();
		if (!StringUtils.isEmpty(name)) {
			conditions.add(QueryHelper.condition("name", "*" + name + "*"));
		}
		return getEntities(getEntityURI(URI_RELEASES, conditions, workspaceId, offset, limit, "name"), offset, new ReleaseEntityFactory());
	}

	@Override
	public Release getRelease(long releaseId, long workspaceId) {
		int offset = 0;
		int limit = 1;
		List<String> conditions = new LinkedList<>();
		conditions.add(QueryHelper.condition("id", String.valueOf(releaseId)));

		List<Release> releases = getEntities(getEntityURI(URI_RELEASES, conditions, workspaceId, offset, limit, null), offset, new ReleaseEntityFactory()).getItems();
		if (releases.size() != 1) {
			if (releases.size() == 0) {
				return null;
			}
			if (releases.size() > 1) {
				throw new RequestErrorException("More than one releases returned for releaseId: " + releaseId + " in workspaceId: " + workspaceId);
			}
		}
		return releases.get(0);
	}

	@Override
	public PagedList<Workspace> queryWorkspaces(String name, int offset, int limit) {
		List<String> conditions = new LinkedList<>();
		if (!StringUtils.isEmpty(name)) {
			conditions.add(QueryHelper.condition("name", "*" + name + "*"));
		}
		return getEntities(getEntityURI(URI_WORKSPACES, conditions, null, offset, limit, "name"), offset, new WorkspaceEntityFactory());
	}

	@Override
	public List<Workspace> getWorkspaces(List<Long> workspaceIds) {
		if (workspaceIds == null || workspaceIds.size() == 0) {
			return new LinkedList<>();
		}
		if (workspaceIds.size() > DEFAULT_LIMIT) {
			throw new IllegalArgumentException("List of workspaceIds is too long. Only " + DEFAULT_LIMIT + " values are allowed.");
		}

		Set<Long> workspaceIdsSet = new LinkedHashSet<>(workspaceIds);
		StringBuilder conditionBuilder = new StringBuilder();
		for (Long workspaceId : workspaceIdsSet) {
			if (conditionBuilder.length() > 0) {
				conditionBuilder.append("||");
			}
			conditionBuilder.append("id=").append(workspaceId);
		}
		return getEntities(
				getEntityURI(URI_WORKSPACES, Collections.singletonList(conditionBuilder.toString()), null, DEFAULT_OFFSET, DEFAULT_LIMIT, null),
				DEFAULT_OFFSET,
				new WorkspaceEntityFactory()
		).getItems();
	}

	@Override
	public PagedList<Taxonomy> queryTaxonomies(String name, long workspaceId, int offset, int limit) {
		List<String> conditions = new LinkedList<>();
		conditions.add("!category={null}");
		if (!StringUtils.isEmpty(name)) {
			conditions.add("(" + QueryHelper.condition("name", "*" + name + "*") + "||" + QueryHelper.conditionRef("category", "name", "*" + name + "*") + ")");
		}
		return getEntities(
				getEntityURI(URI_TAXONOMY_NODES, conditions, workspaceId, offset, limit, null),
				offset,
				new TaxonomyEntityFactory());
	}

	@Override
	public List<Taxonomy> getTaxonomies(List<Long> taxonomyIds, long workspaceId) {
		if (taxonomyIds == null || taxonomyIds.size() == 0) {
			return new LinkedList<>();
		}
		if (taxonomyIds.size() > DEFAULT_LIMIT) {
			throw new IllegalArgumentException("List of taxonomyIds is too long. Only " + DEFAULT_LIMIT + " values are allowed.");
		}

		Set<Long> taxonomyIdsSet = new LinkedHashSet<>(taxonomyIds);
		StringBuilder conditionBuilder = new StringBuilder();
		for (Long taxonomyId : taxonomyIdsSet) {
			if (conditionBuilder.length() > 0) {
				conditionBuilder.append("||");
			}
			conditionBuilder.append("id=").append(taxonomyId);
		}
		return getEntities(
				getEntityURI(URI_TAXONOMY_NODES, Collections.singletonList(conditionBuilder.toString()), workspaceId, DEFAULT_OFFSET, DEFAULT_LIMIT, null),
				DEFAULT_OFFSET,
				new TaxonomyEntityFactory()
		).getItems();
	}

	@Override
	public PagedList<ListItem> queryListItems(String logicalListName, String name, long workspaceId, int offset, int limit) {
		List<String> conditions = new LinkedList<>();
		if (!StringUtils.isEmpty(name)) {
			conditions.add(QueryHelper.condition("name", "*" + name + "*"));
		}
		if (!StringUtils.isEmpty(logicalListName)) {
			conditions.add(QueryHelper.conditionRef("list_root", "logical_name", logicalListName));
		}
		return getEntities(getEntityURI(URI_LIST_ITEMS, conditions, workspaceId, offset, limit, null), offset, new ListItemEntityFactory());
	}

	@Override
	public List<ListItem> getListItems(List<String> itemIds, long workspaceId) {
		if (itemIds == null || itemIds.size() == 0) {
			return new LinkedList<>();
		}
		if (itemIds.size() > DEFAULT_LIMIT) {
			throw new IllegalArgumentException("List of itemIds is too long. Only " + DEFAULT_LIMIT + " values are allowed.");
		}

		Set<String> itemIdsSet = new LinkedHashSet<>(itemIds);
		StringBuilder conditionBuilder = new StringBuilder();
		for (String itemId : itemIdsSet) {
			if (conditionBuilder.length() > 0) {
				conditionBuilder.append("||");
			}
			conditionBuilder.append("id=").append(itemId);
		}
		return getEntities(
				getEntityURI(URI_LIST_ITEMS, Collections.singletonList(conditionBuilder.toString()), workspaceId, DEFAULT_OFFSET, DEFAULT_LIMIT, null),
				DEFAULT_OFFSET,
				new ListItemEntityFactory()
		).getItems();
	}

	@Override
	public List<FieldMetadata> getFieldsMetadata(long workspaceId) {
		List<FieldMetadata> ret = new LinkedList<>();

		List<String> conditions = new LinkedList<>();
		conditions.add(QueryHelper.condition("entity_name", "pipeline_node"));

		//loading all metadata fields
		PagedList<FieldMetadata> allFieldMetadata = getEntities(getEntityURI(URI_METADATA_FIELDS, conditions, workspaceId, DEFAULT_OFFSET, DEFAULT_LIMIT, null), DEFAULT_OFFSET, new FieldMetadataFactory());

		//filtering metadata fields to only values which we are interested in
		for (FieldMetadata fieldMetadata : allFieldMetadata.getItems()) {
			if (fieldMetadata.isValid()) {
				ret.add(fieldMetadata);
			}
		}
		return ret;
	}


	private long postTestResult(ByteArrayEntity entity, boolean skipErrors) {
		HttpPost request = new HttpPost(createSharedSpaceInternalApiUri(URI_TEST_RESULT_PUSH, skipErrors));
		request.setHeader(HTTP.CONTENT_ENCODING, CONTENT_ENCODING_GZIP);
		request.setEntity(entity);
		HttpResponse response = null;
		try {
			response = execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				throw new TemporarilyUnavailableException("Service not available");
			}
			if (statusCode != HttpStatus.SC_ACCEPTED) {
				throw createRequestException("Test result post failed", response);
			}
			String json = IOUtils.toString(response.getEntity().getContent());
			JSONObject jsonObject = JSONObject.fromObject(json);
			return jsonObject.getLong("id");
		} catch (java.io.FileNotFoundException e) {
			throw new FileNotFoundException("Cannot find test result file.", e);
		} catch (IOException e) {
			throw new RequestErrorException("Cannot post test results to MQM.", e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public JSONObject postEntities(long workspaceId, String entityCollectionName, String entityJson) {
		URI uri = getEntityURI(entityCollectionName, null, null, workspaceId, null, null, null);
		HttpPost request = new HttpPost(uri);
		request.setHeader(HTTP.CONTENT_TYPE, "application/json");
		request.setHeader("Accept", "application/json");

		try {
			request.setEntity(new StringEntity(entityJson));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Failed to create StringEntity :" + e.getMessage(), e);
		}
		HttpResponse response;
		try {
			response = execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				throw new TemporarilyUnavailableException("Service not available");
			}
			if (statusCode != HttpStatus.SC_CREATED) {
				throw createRequestException("Post failed", response);
			}
			String json = IOUtils.toString(response.getEntity().getContent());
			return JSONObject.fromObject(json);
		} catch (IOException e) {
			throw new RequestErrorException("Cannot post entities", e);
		}
	}

	public List<Entity> getEntities(long workspaceId, String entityCollectionName, Collection<String> conditions, Collection<String> fields) {
		List<Entity> result = new ArrayList<>();
		int limit = MAX_GET_LIMIT;
		int offset = DEFAULT_OFFSET;
		boolean fetchedAll = false;
		while (!fetchedAll) {
			URI uri = getEntityURI(entityCollectionName, conditions, fields, workspaceId, offset, limit, null);
			PagedList<Entity> found = getEntities(uri, offset, new GeneralEntityFactory());

			result.addAll(found.getItems());
			offset = offset + found.getItems().size();
			fetchedAll = found.getItems().isEmpty() || found.getTotalCount() == result.size();
		}

		return result;
	}

	@Override
	public JSONObject updateEntity(long workspaceId, String entityCollectionName, long entityId, String entityJson) {
		URI uri = getEntityIdURI(entityCollectionName, entityId, workspaceId);
		return updateEntities(uri, entityJson);
	}

	@Override
	public JSONObject updateEntities(long workspaceId, String entityCollectionName, String entityJson) {
		URI uri = getEntityURI(entityCollectionName, null, null, workspaceId, null, null, null);
		return updateEntities(uri, entityJson);
	}

	private JSONObject updateEntities(URI uri, String entityJson) {
		HttpPut request = new HttpPut(uri);
		request.setHeader(HTTP.CONTENT_TYPE, "application/json");
		request.setHeader("Accept", "application/json");

		try {
			request.setEntity(new StringEntity(entityJson));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Failed to create StringEntity :" + e.getMessage(), e);
		}
		HttpResponse response;
		try {
			response = execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				throw new TemporarilyUnavailableException("Service not available");
			}
			if (statusCode != HttpStatus.SC_OK) {
				throw createRequestException("Put failed", response);
			}
			String json = IOUtils.toString(response.getEntity().getContent());
			return JSONObject.fromObject(json);
		} catch (IOException e) {
			throw new RequestErrorException("Cannot put entities to MQM.", e);
		}
	}

	@Override
	public PagedList<Entity> deleteEntities(long workspaceId, String entityCollectionName, Collection<Long> entitiesIds) {
		//query="id=3011||id=3012"

		if (entitiesIds == null || entitiesIds.isEmpty()) {
			return null;
		}

		List<String> idConditions = new ArrayList<>();
		for (Long id : entitiesIds) {
			idConditions.add(QueryHelper.condition("id", id));
		}
		String finalCondition = StringUtils.join(idConditions, "||");


		URI uri = getEntityURI(entityCollectionName, Arrays.asList(finalCondition), null, workspaceId, null, null, null);
		PagedList<Entity> tests = deleteEntities(uri, new GeneralEntityFactory());
		return tests;
	}

	private ByteArrayEntity createGZipEntity(InputStream inputStream) {
		try {
			ByteArrayOutputStream arr = new ByteArrayOutputStream();
			OutputStream zipper = new GZIPOutputStream(arr);
			byte[] buffer = new byte[1024];

			int len;
			while ((len = inputStream.read(buffer)) > 0) {
				zipper.write(buffer, 0, len);
			}

			try {
				inputStream.close();
			} catch (IOException ioe) {
				logger.warning("failed to close silently input stream of tests result");
			}
			try {
				zipper.close();
			} catch (IOException ioe) {
				logger.warning("failed to close silently zip stream of tests result");
			}

			return new ByteArrayEntity(arr.toByteArray(), ContentType.APPLICATION_XML);
		} catch (IOException ex) {
			throw new RequestErrorException("Failed to create GZip entity.", ex);
		}
	}

	@Override
	public boolean putEvents(String eventsJSON) {
		HttpPut request;
		HttpResponse response = null;
		boolean result = true;
		try {
			request = new HttpPut(createSharedSpaceInternalApiUri(URI_PUT_EVENTS));
			request.setEntity(new StringEntity(eventsJSON, ContentType.APPLICATION_JSON));
			response = execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT) {
				// ad-hoc handling as requested by Jenkins Insight team
				HttpClientUtils.closeQuietly(response);
				login();
				response = execute(request);
			}
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				logger.severe("put request failed while sending events: " + response.getStatusLine().getStatusCode());
				result = false;
			}
		} catch (Exception e) {
			logger.severe("put request failed while sending events: " + e.getClass().getName());
			result = false;
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
		return result;
	}

	@Override
	public String getAbridgedTasks(String selfIdentity, String selfType, String selfLocation, Integer apiVersion, String sdkVersion) {
		HttpGet request;
		HttpResponse response = null;
		String responseBody;
		try {
			request = new HttpGet(createSharedSpaceInternalApiUri(URI_GET_ABRIDGED_TASKS, selfIdentity, selfType, selfLocation, apiVersion, sdkVersion));
			response = execute(request);
			responseBody = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return responseBody;
			} else {
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_REQUEST_TIMEOUT) {
					logger.config("expected timeout disconnection on retrieval of abridged tasks");
					return null;
				} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					throw new AuthenticationException();
				} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
					throw new TemporarilyUnavailableException("");
				} else {
					logger.info("unexpected response; status: " + response.getStatusLine().getStatusCode() + "; content: " + responseBody);
					throw new ServerException("Server failed to process the request with status " + response.getStatusLine().getStatusCode());
				}
			}
		} catch (IOException ioe) {
			logger.severe("failed to retrieve abridged tasks: " + ioe.getMessage());
			throw new RequestErrorException(ioe);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	@Override
	public int putAbridgedResult(String selfIdentity, String taskId, String contentJSON) {
		HttpPut request;
		HttpResponse response = null;
		try {
			request = new HttpPut(createSharedSpaceInternalApiUri(URI_PUT_ABRIDGED_RESULT, selfIdentity, taskId));
			request.setEntity(new StringEntity(contentJSON, ContentType.APPLICATION_JSON));
			response = execute(request);
			return response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			logger.severe("failed to submit abridged task's result: " + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	private static class ListItemEntityFactory extends AbstractEntityFactory<ListItem> {

		@Override
		public ListItem doCreate(JSONObject entityObject) {
			JSONObject list_root = entityObject.optJSONObject("list_root");
			if (list_root != null) {
				return new ListItem(entityObject.getString("id"), entityObject.getString("logical_name"), entityObject.getString("name"), doCreate(list_root));
			} else {
				return new ListItem(entityObject.getString("id"), entityObject.getString("logical_name"), entityObject.getString("name"), null);
			}
		}
	}

	private static class TaxonomyEntityFactory extends AbstractEntityFactory<Taxonomy> {

		@Override
		public Taxonomy doCreate(JSONObject entityObject) {
			JSONObject taxonomy_root = entityObject.optJSONObject("category");
			if (taxonomy_root != null) {
				return new Taxonomy(entityObject.getLong("id"), entityObject.getString("name"), doCreate(taxonomy_root));
			} else {
				return new Taxonomy(entityObject.getLong("id"), entityObject.getString("name"), null);
			}
		}
	}

	private static class ReleaseEntityFactory extends AbstractEntityFactory<Release> {

		@Override
		public Release doCreate(JSONObject entityObject) {
			return new Release(entityObject.getLong("id"), entityObject.getString("name"));
		}
	}

	private static class GeneralEntityFactory extends AbstractEntityFactory<Entity> {

		@Override
		public Entity doCreate(JSONObject entityObject) {
			Entity entity = new Entity(entityObject);
			return entity;
		}
	}

	private static class WorkspaceEntityFactory extends AbstractEntityFactory<Workspace> {

		@Override
		public Workspace doCreate(JSONObject entityObject) {
			return new Workspace(entityObject.getLong("id"), entityObject.getString("name"));
		}
	}

	private static class FieldMetadataFactory extends AbstractEntityFactory<FieldMetadata> {

		@Override
		public FieldMetadata doCreate(JSONObject entityObject) {
			String name = null;
			String label = null;
			String logicalName = null;
			boolean multiple = false;
			boolean isExtensible = false;
			int order = 0;

			int mandatoryElementsFound = 0;

			if (entityObject.has("field_features")) {
				JSONArray fieldFeaturesArray = entityObject.getJSONArray("field_features");
				for (int i = 0; i < fieldFeaturesArray.size(); i++) {
					JSONObject fieldFeature = fieldFeaturesArray.getJSONObject(i);
					if (fieldFeature.has("name") && fieldFeature.getString("name").equals("pipeline_tagging") && fieldFeature.has("extensibility") && fieldFeature.has("order")) {
						order = fieldFeature.getInt("order");
						isExtensible = fieldFeature.getBoolean("extensibility");
						mandatoryElementsFound++;
						break;
					}
				}
			}
			if (entityObject.has("name") && entityObject.has("label")) {
				name = entityObject.getString("name");
				label = entityObject.getString("label");
				mandatoryElementsFound++;
			}
			if (entityObject.has("field_type_data")) {
				JSONObject fieldTypeData = entityObject.getJSONObject("field_type_data");

				if (fieldTypeData.has("multiple") && fieldTypeData.has("targets")) {
					multiple = fieldTypeData.getBoolean("multiple");

					JSONArray targets = fieldTypeData.getJSONArray("targets");
					for (int i = 0; i < targets.size(); i++) {
						if (targets.getJSONObject(i).has("logical_name")) {
							logicalName = targets.getJSONObject(i).getString("logical_name");
							mandatoryElementsFound++;
						}
					}
				}
			}
			if (mandatoryElementsFound != 3) {
				return new FieldMetadata(null, null, null, false, false, 0);
			}

			return new FieldMetadata(name, label, logicalName, isExtensible, multiple, order);
		}
	}

	static abstract class AbstractEntityFactory<E> implements EntityFactory<E> {

		@Override
		public E create(String json) {
			JSONObject jsonObject = JSONObject.fromObject(json);
			return doCreate(jsonObject);
		}

		public abstract E doCreate(JSONObject entityObject);

	}
}
