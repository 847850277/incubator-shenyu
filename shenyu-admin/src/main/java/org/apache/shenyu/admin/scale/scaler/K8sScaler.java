/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.admin.scale.scaler;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1ScaleSpec;
import org.apache.shenyu.admin.scale.config.DeploymentProperties;
import org.apache.shenyu.admin.scale.scaler.dynamic.ScaleAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class K8sScaler {

    private static final Logger LOG = LoggerFactory.getLogger(K8sScaler.class);

    private final AppsV1Api appsV1Api;

    private final DeploymentProperties deploymentProperties;

    public K8sScaler(final K8sApiClientProvider k8sApiClientProvider,
                     final DeploymentProperties deploymentProperties) throws IOException {
        this.appsV1Api = new AppsV1Api(k8sApiClientProvider.createApiClient());
        this.deploymentProperties = deploymentProperties;
    }

    /**
     * scale by action.
     *
     * @param action action
     */
    public void scaleByAction(final ScaleAction action) throws Exception {
        V1Scale scale = appsV1Api.readNamespacedDeploymentScale(
                deploymentProperties.getName(),
                deploymentProperties.getNamespace(),
                null);
        V1ScaleSpec spec = scale.getSpec();
        int currentReplicas = (spec != null && spec.getReplicas() != null)
                ? spec.getReplicas()
                : 0;
        int newReplicas = calculateNewReplicaCount(currentReplicas, action);
        scaleByNum(newReplicas);
    }

    /**
     * scale by number.
     *
     * @param replicaCount replicaCount
     */
    public void scaleByNum(final int replicaCount) {
        try {
            V1Scale scale = new V1Scale();
            Objects.requireNonNull(scale.getSpec()).setReplicas(replicaCount);

            appsV1Api.replaceNamespacedDeploymentScale(
                    deploymentProperties.getName(),
                    deploymentProperties.getNamespace(),
                    scale,
                    // pretty
                    null,
                    // dryRun
                    null,
                    // fieldManager
                    null,
                    // fieldValidation
                    null
            );

        } catch (ApiException e) {
            LOG.error("Failed to scale deployment. Cause: {}", e.getMessage());
        }
    }

    private int calculateNewReplicaCount(final int currentReplicas, final ScaleAction action) {
        return switch (action.getActionType()) {
            case SCALE_UP -> currentReplicas + 1;
            case SCALE_DOWN -> Math.max(0, currentReplicas - 1);
            default -> currentReplicas;
        };
    }
}
