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

package org.apache.shardingsphere.sharding.algorithm.sharding.classbased;

import com.google.common.collect.Range;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.fixture.ClassBasedComplexKeysShardingAlgorithmFixture;
import org.apache.shardingsphere.sharding.fixture.ClassBasedHintShardingAlgorithmFixture;
import org.apache.shardingsphere.sharding.fixture.ClassBasedStandardShardingAlgorithmFixture;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class ClassBasedShardingAlgorithmTest {
    
    private static final DataNodeInfo DATA_NODE_INFO = new DataNodeInfo("t_order_", 1, '0');
    
    @Test
    public void assertStandardStrategyInit() {
        ClassBasedShardingAlgorithm shardingAlgorithm = getStandardShardingAlgorithm();
        assertThat(shardingAlgorithm.getType(), is("CLASS_BASED"));
        assertThat(shardingAlgorithm.getStrategy(), is(ClassBasedShardingAlgorithmStrategyType.STANDARD));
        assertThat(shardingAlgorithm.getAlgorithmClassName(), is(ClassBasedStandardShardingAlgorithmFixture.class.getName()));
    }
    
    @Test(expected = NullPointerException.class)
    public void assertInitWithNullStrategy() {
        ClassBasedShardingAlgorithm shardingAlgorithm = new ClassBasedShardingAlgorithm();
        shardingAlgorithm.getProps().setProperty("strategy", null);
        shardingAlgorithm.init();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void assertInitWithWrongStrategy() {
        ClassBasedShardingAlgorithm shardingAlgorithm = new ClassBasedShardingAlgorithm();
        shardingAlgorithm.getProps().setProperty("strategy", "wrong");
        shardingAlgorithm.init();
    }
    
    @Test(expected = NullPointerException.class)
    public void assertInitWithNullClass() {
        ClassBasedShardingAlgorithm shardingAlgorithm = new ClassBasedShardingAlgorithm();
        shardingAlgorithm.getProps().setProperty("strategy", "standard");
        shardingAlgorithm.init();
    }
    
    @Test(expected = ClassNotFoundException.class)
    public void assertInitWithUndefinedClass() {
        ClassBasedShardingAlgorithm shardingAlgorithm = new ClassBasedShardingAlgorithm();
        shardingAlgorithm.getProps().setProperty("strategy", "standard");
        shardingAlgorithm.getProps().setProperty("algorithmClassName", "org.apache.shardingsphere.sharding.UndefinedClass");
        shardingAlgorithm.init();
    }
    
    @Test(expected = ShardingSphereException.class)
    public void assertInitWithMismatchStrategy() {
        ClassBasedShardingAlgorithm shardingAlgorithm = new ClassBasedShardingAlgorithm();
        shardingAlgorithm.getProps().setProperty("strategy", "standard");
        shardingAlgorithm.getProps().setProperty("algorithmClassName", ClassBasedComplexKeysShardingAlgorithmFixture.class.getName());
        shardingAlgorithm.init();
    }
    
    @Test
    public void assertPreciseDoSharding() {
        ClassBasedShardingAlgorithm shardingAlgorithm = getStandardShardingAlgorithm();
        Collection<String> availableTargetNames = Arrays.asList("t_order_0", "t_order_1", "t_order_2", "t_order_3");
        assertThat(shardingAlgorithm.doSharding(availableTargetNames, new PreciseShardingValue<>("t_order", "order_id", DATA_NODE_INFO, 0)), is("t_order_0"));
    }
    
    @Test
    public void assertRangeDoSharding() {
        ClassBasedShardingAlgorithm shardingAlgorithm = getStandardShardingAlgorithm();
        Collection<String> availableTargetNames = Arrays.asList("t_order_0", "t_order_1", "t_order_2", "t_order_3");
        Collection<String> actual = shardingAlgorithm.doSharding(availableTargetNames, new RangeShardingValue<>("t_order", "order_id", DATA_NODE_INFO, Range.closed(2, 15)));
        assertThat(actual.size(), is(4));
    }
    
    @Test
    public void assertComplexKeysDoSharding() {
        ClassBasedShardingAlgorithm shardingAlgorithm = getComplexKeysShardingAlgorithm();
        assertThat(shardingAlgorithm.getStrategy(), is(ClassBasedShardingAlgorithmStrategyType.COMPLEX));
        Collection<String> availableTargetNames = Arrays.asList("t_order_0", "t_order_1", "t_order_2", "t_order_3");
        Collection<String> actual = shardingAlgorithm.doSharding(availableTargetNames, new ComplexKeysShardingValue<>("t_order", null, null));
        assertThat(actual.size(), is(4));
    }
    
    @Test
    public void assertHintDoSharding() {
        ClassBasedShardingAlgorithm shardingAlgorithm = getHintShardingAlgorithm();
        assertThat(shardingAlgorithm.getStrategy(), is(ClassBasedShardingAlgorithmStrategyType.HINT));
        Collection<String> availableTargetNames = Arrays.asList("t_order_0", "t_order_1", "t_order_2", "t_order_3");
        Collection<String> actual = shardingAlgorithm.doSharding(availableTargetNames, new HintShardingValue<>("t_order", "order_id", null));
        assertThat(actual.size(), is(4));
    }
    
    private ClassBasedShardingAlgorithm getStandardShardingAlgorithm() {
        ClassBasedShardingAlgorithm result = new ClassBasedShardingAlgorithm();
        result.getProps().setProperty("strategy", "standard");
        result.getProps().setProperty("algorithmClassName", ClassBasedStandardShardingAlgorithmFixture.class.getName());
        result.init();
        return result;
    }
    
    private ClassBasedShardingAlgorithm getComplexKeysShardingAlgorithm() {
        ClassBasedShardingAlgorithm result = new ClassBasedShardingAlgorithm();
        result.getProps().setProperty("strategy", "complex");
        result.getProps().setProperty("algorithmClassName", ClassBasedComplexKeysShardingAlgorithmFixture.class.getName());
        result.init();
        return result;
    }
    
    private ClassBasedShardingAlgorithm getHintShardingAlgorithm() {
        ClassBasedShardingAlgorithm result = new ClassBasedShardingAlgorithm();
        result.getProps().setProperty("strategy", "hint");
        result.getProps().setProperty("algorithmClassName", ClassBasedHintShardingAlgorithmFixture.class.getName());
        result.init();
        return result;
    }
}
