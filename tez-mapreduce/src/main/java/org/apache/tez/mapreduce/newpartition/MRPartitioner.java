/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tez.mapreduce.newpartition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.lib.HashPartitioner;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.tez.common.TezJobConfig;
import org.apache.tez.mapreduce.hadoop.MRJobConfig;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MRPartitioner implements org.apache.tez.engine.api.Partitioner {

  static final Log LOG = LogFactory.getLog(MRPartitioner.class);

  private final boolean useNewApi;
  private int partitions = 1;

  private org.apache.hadoop.mapreduce.Partitioner newPartitioner;
  Partitioner oldPartitioner;

  public MRPartitioner(Configuration conf) {
    this.useNewApi = conf.getBoolean("mapred.mapper.new-api", false);
    this.partitions = conf.getInt(TezJobConfig.TEZ_ENGINE_NUM_EXPECTED_PARTITIONS, 1);

    if (useNewApi) {
      if (partitions > 1) {
        newPartitioner = (org.apache.hadoop.mapreduce.Partitioner) ReflectionUtils
            .newInstance(
                (Class<? extends org.apache.hadoop.mapreduce.Partitioner<?, ?>>) conf
                    .getClass(MRJobConfig.PARTITIONER_CLASS_ATTR,
                        HashPartitioner.class), conf);
      } else {
        newPartitioner = new org.apache.hadoop.mapreduce.Partitioner() {
          @Override
          public int getPartition(Object key, Object value, int numPartitions) {
            return numPartitions - 1;
          }
        };
      }
    } else {
      if (partitions > 1) {
        oldPartitioner = (Partitioner) ReflectionUtils.newInstance(
            (Class<? extends Partitioner>) conf.getClass(
                "mapred.partitioner.class", HashPartitioner.class), conf);
      } else {
        oldPartitioner = new Partitioner() {
          @Override
          public void configure(JobConf job) {
          }

          @Override
          public int getPartition(Object key, Object value, int numPartitions) {
            return numPartitions - 1;
          }
        };
      }
    }
  }

  @Override
  public int getPartition(Object key, Object value, int numPartitions) {
    if (useNewApi) {
      return newPartitioner.getPartition(key, value, numPartitions);
    } else {
      return oldPartitioner.getPartition(key, value, numPartitions);
    }
  }
}