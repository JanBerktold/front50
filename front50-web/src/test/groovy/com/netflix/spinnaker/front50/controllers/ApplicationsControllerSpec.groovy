/*
 * Copyright 2014 Netflix, Inc.
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



package com.netflix.spinnaker.front50.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.front50.exception.NotFoundException
import com.netflix.spinnaker.front50.model.application.Application
import com.netflix.spinnaker.front50.model.application.ApplicationDAO
import com.netflix.spinnaker.front50.security.NamedAccount
import com.netflix.spinnaker.front50.security.NamedAccountProvider
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by aglover on 4/18/14.
 */
class ApplicationsControllerSpec extends Specification {

  @Shared
  MockMvc mockMvc

  @Shared
  ApplicationsController controller

  @Shared
  NamedAccountProvider namedAccountProvider

  void setup() {
    this.controller = new ApplicationsController()
    this.namedAccountProvider = Mock(NamedAccountProvider)
    this.controller.namedAccountProvider = this.namedAccountProvider
    this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
  }

  void 'a put should update an application'() {
    setup:
    def sampleApp = new Application("SAMPLEAPP", null, "web@netflix.com", "Andy McEntee",
      null, null, null, null, null, null, null, null)
    def application = new Application()
    def dao = Mock(ApplicationDAO)
    dao.findByName(_) >> sampleApp
    application.dao = dao
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(put("/test/applications").
      contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(sampleApp)))

    then:
    1 * namedAccountProvider.get("test") >> namedAccount
    response.andExpect status().isOk()
    response.andExpect content().string(new ObjectMapper().writeValueAsString(sampleApp))
    1 * dao.update("SAMPLEAPP", ["email": "web@netflix.com", "owner": "Andy McEntee"])
  }

  void 'a put should not update an application if no name is provided'() {
    setup:
    def sampleApp = new Application(null, null, "web@netflix.com", "Andy McEntee",
      null, null, null, null, null, null, null, null)
    def application = new Application()

    def dao = Mock(ApplicationDAO)
    application.dao = dao
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(put("/test/applications").
      contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(sampleApp)))

    then:
    1 * namedAccountProvider.get("test") >> namedAccount
    response.andExpect status().is4xxClientError()
  }

  void 'a post w/o a name will throw an error'() {
    setup:
    def sampleApp = new Application(null, "Standalone App", "web@netflix.com", "Kevin McEntee",
      "netflix.com application", "Standalone Application", null, null, null, null, null, null)
    def application = new Application()

    def dao = Mock(ApplicationDAO)
    dao.create(_, _) >> sampleApp
    application.dao = dao
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(post("/test/applications/name/app").
      contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(sampleApp)))

    then:
    1 * namedAccountProvider.get("test") >> namedAccount
    response.andExpect status().is4xxClientError()
  }

  void 'a post w/a new application should yeild a success'() {
    setup:
    def sampleApp = new Application("SAMPLEAPP", "Standalone App", "web@netflix.com", "Kevin McEntee",
      "netflix.com application", "Standalone Application", null, null, null, null, null, null)
    def application = new Application()

    def dao = Mock(ApplicationDAO)
    dao.create(_, _) >> sampleApp
    application.dao = dao
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(post("/test/applications/name/SAMPLEAPP").
      contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(sampleApp)))

    then:
    1 * namedAccountProvider.get("test") >> namedAccount
    response.andExpect status().isOk()
    response.andExpect content().string(new ObjectMapper().writeValueAsString(sampleApp))
  }

  void 'a get w/a name should return a JSON document for the found app'() {
    setup:
    def sampleApp = new Application("SAMPLEAPP", "Standalone App", "web@netflix.com", "Kevin McEntee",
      "netflix.com application", "Standalone Application", null, null, null, null, "1265752693581l", "1265752693581l")
    def application = Mock(Application)
    application.findByName("SAMPLEAPP") >> sampleApp
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(get("/test/applications/name/SAMPLEAPP"))

    then:
    1 * namedAccountProvider.get("test") >> namedAccount
    response.andExpect status().isOk()
    response.andExpect content().string(new ObjectMapper().writeValueAsString(sampleApp))
  }

  void 'a get w/a invalid name should return 404'() {
    setup:
    def application = Mock(Application)
    application.findByName(_) >> { throw new NotFoundException("not found!") }
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(get("/test/applications/name/blah"))

    then:
    1 * namedAccountProvider.get("test") >> namedAccount
    response.andExpect status().is(404)
  }

  void 'delete should remove a domain'() {
    setup:
    def application = Mock(Application)
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(delete("/test/applications/name/SAMPLEAPP"))

    then:
    1 * application.initialize(_) >> application
    1 * namedAccountProvider.get("test") >> namedAccount
    1 * application.delete()
    response.andExpect status().isAccepted()

  }

  void 'index should return a list of applications'() {
    setup:
    def account = "test"
    def sampleApps = [new Application("SAMPLEAPP", "Standalone App", "web@netflix.com", "Kevin McEntee",
      "netflix.com application", "Standalone Application", null, null, null, null, "1265752693581l", "1265752693581l"),
                      new Application("SAMPLEAPP-2", "Standalone App", "web@netflix.com", "Kevin McEntee",
                        "netflix.com application", "Standalone Application", null, null, null, null, "1265752693581l", "1265752693581l")]
    def application = Mock(Application)
    application.findAll() >> sampleApps
    def namedAccount = Mock(NamedAccount)
    namedAccount.getApplication() >> application

    when:
    def response = mockMvc.perform(get("/test/applications"))

    then:
    1 * namedAccountProvider.get(account) >> namedAccount
    response.andExpect status().isOk()
    response.andExpect content().string(new ObjectMapper().writeValueAsString(sampleApps))
  }

}