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

package org.apache.shardingsphere.mode.manager.cluster.coordinator.registry.cache.subscriber;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.eventbus.ShardingSphereEventBus;
import org.apache.shardingsphere.mode.manager.cluster.coordinator.registry.cache.event.StartScalingEvent;
import org.apache.shardingsphere.mode.manager.cluster.coordinator.registry.config.event.rule.ScalingTaskFinishedEvent;
import org.apache.shardingsphere.mode.manager.cluster.coordinator.registry.config.event.version.SchemaVersionPreparedEvent;
import org.apache.shardingsphere.mode.metadata.persist.node.DatabaseMetaDataNode;
import org.apache.shardingsphere.mode.metadata.persist.service.DatabaseVersionPersistService;
import org.apache.shardingsphere.mode.repository.cluster.ClusterPersistRepository;

import java.util.Optional;

/**
 * Scaling registry subscriber.
 */
@Slf4j
// TODO move to scaling module
public final class ScalingRegistrySubscriber {
    
    private final ClusterPersistRepository repository;
    
    private final DatabaseVersionPersistService databaseVersionPersistService;
    
    public ScalingRegistrySubscriber(final ClusterPersistRepository repository) {
        this.repository = repository;
        databaseVersionPersistService = new DatabaseVersionPersistService(repository);
        ShardingSphereEventBus.getInstance().register(this);
    }
    
    /**
     * Start scaling after new schema version prepared.
     *
     * @param event Schema version prepared event.
     */
    @Subscribe
    public void startScaling(final SchemaVersionPreparedEvent event) {
        String activeVersion = databaseVersionPersistService.getDatabaseActiveVersion(event.getSchemaName()).get();
        String sourceDataSource = repository.get(DatabaseMetaDataNode.getMetaDataDataSourcePath(event.getSchemaName(), activeVersion));
        String targetDataSource = repository.get(DatabaseMetaDataNode.getMetaDataDataSourcePath(event.getSchemaName(), event.getVersion()));
        String sourceRule = repository.get(DatabaseMetaDataNode.getRulePath(event.getSchemaName(), activeVersion));
        String targetRule = repository.get(DatabaseMetaDataNode.getRulePath(event.getSchemaName(), event.getVersion()));
        log.info("start scaling job, locked the schema name, event={}", event);
        StartScalingEvent startScalingEvent = new StartScalingEvent(event.getSchemaName(), sourceDataSource, sourceRule, targetDataSource, targetRule,
                Integer.parseInt(activeVersion), Integer.parseInt(event.getVersion()));
        ShardingSphereEventBus.getInstance().post(startScalingEvent);
    }
    
    /**
     * Scaling task finished.
     *
     * @param event scaling task finished event
     */
    @Subscribe
    public void scalingTaskFinished(final ScalingTaskFinishedEvent event) {
        log.info("scalingTaskFinished, event={}", event);
        int targetActiveVersion = event.getTargetActiveVersion();
        Optional<String> activeVersion = databaseVersionPersistService.getDatabaseActiveVersion(event.getTargetSchemaName());
        if (activeVersion.isPresent() && targetActiveVersion == Integer.parseInt(activeVersion.get())) {
            databaseVersionPersistService.persistActiveVersion(event.getTargetSchemaName(), event.getTargetNewVersion() + "");
            databaseVersionPersistService.deleteVersion(event.getTargetSchemaName(), targetActiveVersion + "");
        } else {
            log.error("targetActiveVersion does not match current activeVersion, targetActiveVersion={}, activeVersion={}", targetActiveVersion, activeVersion.orElse(null));
        }
    }
}
