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

package com.netflix.bluespar.kato.security.aws

import com.amazonaws.auth.AWSCredentialsProvider
import com.fasterxml.jackson.annotation.JsonIgnore
import com.netflix.bluespar.amazon.security.AmazonCredentials
import com.netflix.bluespar.kato.security.NamedAccountCredentials

import javax.xml.bind.annotation.XmlTransient

class BasicAmazonNamedAccountCredentials implements NamedAccountCredentials<AmazonCredentials> {

  @JsonIgnore
  @XmlTransient
  private final AWSCredentialsProvider provider
  private final String environment

  BasicAmazonNamedAccountCredentials(AWSCredentialsProvider provider, String environment) {
    this.provider = provider
    this.environment = environment
  }

  @JsonIgnore
  @XmlTransient
  public AmazonCredentials getCredentials() {
    new AmazonCredentials(provider.credentials, environment)
  }
}