/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.etl.spark.dataframe;


import java.util.Map;

/**
 * Stores all the information of {@link SparkDfSinkFactory} and stagePartitions
 */
public class SparkDfSourceSinkFactoryInfo {
  private final SparkDfSourceFactory sparkBatchSourceFactory;
  private final SparkDfSinkFactory sparkBatchSinkFactory;
  private final Map<String, Integer> stagePartitions;

  public SparkDfSourceSinkFactoryInfo(SparkDfSourceFactory sparkBatchSourceFactory,
                                      SparkDfSinkFactory sparkBatchSinkFactory,
                                      Map<String, Integer> stagePartitions) {
    this.sparkBatchSourceFactory = sparkBatchSourceFactory;
    this.sparkBatchSinkFactory = sparkBatchSinkFactory;
    this.stagePartitions = stagePartitions;
  }

  public SparkDfSourceFactory getSparkBatchSourceFactory() {
    return sparkBatchSourceFactory;
  }

  public SparkDfSinkFactory getSparkBatchSinkFactory() {
    return sparkBatchSinkFactory;
  }

  public Map<String, Integer> getStagePartitions() {
    return stagePartitions;
  }
}
