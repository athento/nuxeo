/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.runtime.aws;

import org.nuxeo.runtime.api.Framework;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

/**
 * AWS Credentials Provider that uses Nuxeo configuration, or uses the default AWS chain as a fallback.
 *
 * @since 10.3
 */
public class NuxeoAWSCredentialsProvider implements AWSCredentialsProvider {

    protected static final AWSCredentialsProvider INSTANCE = new NuxeoAWSCredentialsProvider();

    protected static final AWSCredentialsProvider DEFAULT = new DefaultAWSCredentialsProviderChain();

    public static AWSCredentialsProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public AWSCredentials getCredentials() {
        AWSConfigurationService service = Framework.getService(AWSConfigurationService.class);
        if (service != null) {
            AWSCredentials credentials = service.getAWSCredentials();
            if (credentials != null) {
                return credentials;
            }
        }
        return DEFAULT.getCredentials();
    }

    @Override
    public void refresh() {
        DEFAULT.refresh();
    }

}
