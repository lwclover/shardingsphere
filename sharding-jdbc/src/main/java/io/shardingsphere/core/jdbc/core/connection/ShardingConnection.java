/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
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
 * </p>
 */

package io.shardingsphere.core.jdbc.core.connection;

import io.shardingsphere.core.jdbc.adapter.AbstractConnectionAdapter;
import io.shardingsphere.core.jdbc.core.ShardingContext;
import io.shardingsphere.core.jdbc.core.statement.ShardingPreparedStatement;
import io.shardingsphere.core.jdbc.core.statement.ShardingStatement;
import io.shardingsphere.core.rule.DataNode;
import io.shardingsphere.core.rule.MasterSlaveRule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Connection that support sharding.
 * 
 * @author zhangliang
 * @author caohao
 * @author gaohongtao
 */
@RequiredArgsConstructor
public final class ShardingConnection extends AbstractConnectionAdapter {
    
    @Getter
    private final ShardingContext shardingContext;
    
    /**
     * Release connection.
     *
     * @param connection to be released connection
     */
    public void release(final Connection connection) {
        removeCache(connection);
        try {
            connection.close();
        } catch (final SQLException ignore) {
        }
    }
    
    /**
     * Get all connections via logic table for each actual tables.
     * 
     * @param logicTableName logic table name
     * @return map of all connections
     * @throws SQLException SQL exception
     */
    public Map<String, Connection> getConnections(final String logicTableName) throws SQLException {
        Map<String, Connection> result = new HashMap<>();
        for (DataNode each : shardingContext.getShardingRule().getTableRuleByLogicTableName(logicTableName).getActualDataNodes()) {
            String dataSourceName = shardingContext.getShardingRule().getShardingDataSourceNames().getRawMasterDataSourceName(each.getDataSourceName());
            result.put(dataSourceName, getConnection(dataSourceName));
        }
        return result;
    }
    
    @Override
    protected Map<String, DataSource> getDataSourceMap() {
        return shardingContext.getDataSourceMap();
    }
    
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        Collection<MasterSlaveRule> masterSlaveRules = shardingContext.getShardingRule().getMasterSlaveRules();
        if (masterSlaveRules.isEmpty()) {
            return getConnection(shardingContext.getDataSourceMap().keySet().iterator().next()).getMetaData();
        }
        for (MasterSlaveRule each : masterSlaveRules) {
            if (getDataSourceMap().containsKey(each.getMasterDataSourceName())) {
                return getConnection(each.getMasterDataSourceName()).getMetaData();
            }
        }
        throw new UnsupportedOperationException();
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql) {
        return new ShardingPreparedStatement(this, sql);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) {
        return new ShardingPreparedStatement(this, sql, resultSetType, resultSetConcurrency);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
        return new ShardingPreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) {
        return new ShardingPreparedStatement(this, sql, autoGeneratedKeys);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) {
        return new ShardingPreparedStatement(this, sql, Statement.RETURN_GENERATED_KEYS);
    }
    
    @Override
    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) {
        return new ShardingPreparedStatement(this, sql, Statement.RETURN_GENERATED_KEYS);
    }
    
    @Override
    public Statement createStatement() {
        return new ShardingStatement(this);
    }
    
    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) {
        return new ShardingStatement(this, resultSetType, resultSetConcurrency);
    }
    
    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
        return new ShardingStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
}
