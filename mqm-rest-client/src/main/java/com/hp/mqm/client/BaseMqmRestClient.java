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

import com.hp.mqm.client.exception.SharedSpaceNotExistException;

public interface BaseMqmRestClient {

	/**
	 * Tries login and when it passes it tries to connect to project.
	 *
	 * @throws com.hp.mqm.client.exception.AuthenticationException  when authentication fails
	 * @throws SharedSpaceNotExistException                         when shared space does not exist
	 * @throws com.hp.mqm.client.exception.LoginErrorException      in case of IO error or error in the HTTP protocol
	 *                                                              during login (authentication or session creation)
	 * @throws com.hp.mqm.client.exception.RequestErrorException    in case of IO error or error in the HTTP protocol
	 *                                                              during either authentication or session creation or project, domain check
	 *                                                              (for authentication and session creation issues {@link com.hp.mqm.client.exception.LoginErrorException} as special
	 *                                                              case of RequestErrorException is thrown)
	 */
	void validateConfiguration();

	void validateConfigurationWithoutLogin();
}
