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

package co.cask.cdap.etl.api.dataframe;

import co.cask.cdap.api.annotation.Beta;
import co.cask.cdap.etl.api.MultiInputPipelineConfigurable;
import co.cask.cdap.etl.api.MultiInputPipelineConfigurer;
import co.cask.cdap.etl.api.batch.BatchJoinerContext;
import co.cask.cdap.etl.api.batch.MultiInputBatchConfigurable;
import co.cask.cdap.etl.api.batch.SparkExecutionPluginContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;

import java.io.Serializable;
import java.util.Map;

/**
 * Spark Joiner stage: enable to compute a complex join between multiple inputs by using the spark framework.
 * It is actually a Spark Compute job that is able to accept multiple RDD as inputs
 *
 * @param <OUT> Type of output object
 */
@Beta
public abstract class SparkDataframeJoiner<OUT> extends MultiInputBatchConfigurable<BatchJoinerContext>
  implements MultiInputPipelineConfigurable, Serializable {
  public static final String PLUGIN_TYPE = "sparkdataframejoiner";

  private static final long serialVersionUID = -3363927310251482445L;

  /**
   * Configure a pipeline.
   *
   * @param multiInputPipelineConfigurer the configurer used to add required datasets and streams
   * @throws IllegalArgumentException if the given config is invalid
   */
   @Override
  public void configurePipeline(MultiInputPipelineConfigurer multiInputPipelineConfigurer) throws IllegalArgumentException {
    // no-op
  }

  /**
   * Initialize the plugin. Will be called before any calls to {@link #join(SparkExecutionPluginContext, Map<String, JavaRDD>)}
   * are made.
   *
   * @param context {@link SparkExecutionPluginContext} for this job
   * @throws Exception if there is an error initializing
   */
  public void initialize(SparkExecutionPluginContext context) throws Exception {
    //no-op
  }

  @Override
  public void prepareRun(BatchJoinerContext context) throws Exception {
    //no op
  }


  /**
   * Transform the input and return the output to be sent to the next stage in the pipeline.
   *
   * @param context {@link SparkExecutionPluginContext} for this job
   * @param inputs A map of (stage name -> input data) that need to be joined
   * @throws Exception if there is an error during this method invocation
   */
  public abstract Dataset join(SparkExecutionPluginContext context, Map<String, Dataset> inputs) throws Exception;

}
