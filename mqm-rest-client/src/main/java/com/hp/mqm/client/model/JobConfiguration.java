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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final public class JobConfiguration {

    final private List<Pipeline> relatedPipelines;

    public JobConfiguration(List<Pipeline> relatedPipelines) {
        this.relatedPipelines = relatedPipelines;
    }

    public List<Pipeline> getRelatedPipelines() {
        return relatedPipelines;
    }

    //map of related workspaces and pipelines related to that workspace <workspaceId, List<Pipeline>>
    public Map<Long, List<Pipeline>> getWorkspacePipelinesMap() {
        Map<Long, List<Pipeline>> ret = new HashMap<Long, List<Pipeline>>();
        for (Pipeline pipeline : relatedPipelines) {
            if (ret.containsKey(pipeline.getWorkspaceId())) {
                ret.get(pipeline.getWorkspaceId()).add(pipeline);
            } else {
                ret.put(pipeline.getWorkspaceId(), new LinkedList<Pipeline>(Arrays.asList(pipeline)));
            }
        }
        return ret;
    }
}
