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
import co.cask.cdap.etl.api.PipelineConfigurable;
import co.cask.cdap.etl.api.PipelineConfigurer;
import co.cask.cdap.etl.api.batch.BatchConfigurable;
import co.cask.cdap.etl.api.batch.BatchContext;
import co.cask.cdap.etl.api.batch.SparkExecutionPluginContext;
import co.cask.cdap.etl.api.batch.SparkPluginContext;
import org.apache.spark.sql.Dataset;

import java.io.Serializable;

/**
 * Spark Compute stage.
 *
 * @param <OUT> Type of output object
 */
@Beta
public abstract class SparkDataframeSource<OUT> extends BatchConfigurable<BatchContext> implements Serializable {
  public static final String PLUGIN_TYPE = "sparkdataframesource";
  private static final long serialVersionUID = -913193725689627596L;


  /**
   * Configure a pipeline.
   *
   * @param pipelineConfigurer the configurer used to add required datasets and streams
   * @throws IllegalArgumentException if the given config is invalid
   */
  @Override
  public void configurePipeline(PipelineConfigurer pipelineConfigurer) throws IllegalArgumentException {
    //no-op
  }

  @Override
  public void prepareRun(BatchContext context) throws Exception {
    //no-op
  }

  @Override
  public void onRunFinish(boolean succeeded, BatchContext context) {
    //no-op
  }

  /**
   * Initialize the plugin. Will be called before any calls to {@link #load(SparkExecutionPluginContext)}
   * are made.
   *
   * @param context {@link SparkExecutionPluginContext} for this job
   * @throws Exception if there is an error initializing
   */
  public void initialize(SparkExecutionPluginContext context) throws Exception {
    //no-op
  }

  /**
   * Transform the input and return the output to be sent to the next stage in the pipeline.
   *
   * @param context {@link SparkExecutionPluginContext} for this job
   * @throws Exception if there is an error during this method invocation
   */
  public abstract Dataset load(SparkExecutionPluginContext context) throws Exception;

}
