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

package org.apache.shardingsphere.sharding.route.engine.type.complex;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.route.engine.condition.ShardingConditions;
import org.apache.shardingsphere.sharding.route.engine.type.ShardingRouteEngine;
import org.apache.shardingsphere.sharding.route.engine.type.standard.ShardingStandardRoutingEngine;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rule.TableRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Sharding complex routing engine.
 */
@RequiredArgsConstructor
public final class ShardingComplexRoutingEngine implements ShardingRouteEngine {
    
    private final ShardingConditions shardingConditions;
    
    private final ConfigurationProperties props;

    private final Collection<String> logicTables;
    
    @Override
    public RouteContext route(final ShardingRule shardingRule) {
        RouteContext result = new RouteContext();
        Collection<String> bindingTableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Collection<RouteContext> routeContexts = new ArrayList<>(logicTables.size());
        for (String each : logicTables) {
            Optional<TableRule> tableRule = shardingRule.findTableRule(each);
            if (tableRule.isPresent()) {
                if (!bindingTableNames.contains(each)) {
                    routeContexts.add(new ShardingStandardRoutingEngine(tableRule.get().getLogicTable(), shardingConditions, props).route(shardingRule));
                }
                shardingRule.findBindingTableRule(each).ifPresent(bindingTableRule -> bindingTableNames.addAll(bindingTableRule.getTableRules().keySet()));
            }
        }
        if (routeContexts.isEmpty()) {
            throw new ShardingSphereException("Cannot find table rule and default data source with logic tables: '%s'", logicTables);
        }
        if (1 == routeContexts.size()) {
            RouteContext newRouteContext = routeContexts.iterator().next();
            result.getOriginalDataNodes().addAll(newRouteContext.getOriginalDataNodes());
            result.getRouteUnits().addAll(newRouteContext.getRouteUnits());
        } else {
            RouteContext routeContext = new ShardingCartesianRoutingEngine(routeContexts).route(shardingRule);
            result.getOriginalDataNodes().addAll(routeContext.getOriginalDataNodes());
            result.getRouteUnits().addAll(routeContext.getRouteUnits());    
        }
        return result;
    }
}
