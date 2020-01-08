/*
 * Copyright © 2017 Cask Data, Inc.
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

package co.cask.cdap.etl.spark.plugin;

import co.cask.cdap.etl.api.PipelineConfigurer;
import co.cask.cdap.etl.api.batch.SparkCompute;
import co.cask.cdap.etl.api.batch.SparkExecutionPluginContext;
import co.cask.cdap.etl.api.dataframe.SparkDataframeCompute;
import co.cask.cdap.etl.common.plugin.Caller;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;

import java.util.concurrent.Callable;

/**
 * Wrapper around a {@link SparkCompute} that makes sure logging, classloading, and other pipeline capabilities
 * are setup correctly.
 *
 * @param <IN> Type of input object
 * @param <OUT> Type of output object
 */
public class WrappedSparkDataframeCompute<IN, OUT> extends SparkDataframeCompute<IN, OUT> {
  private final SparkDataframeCompute<IN, OUT> compute;
  private final Caller caller;

  public WrappedSparkDataframeCompute(SparkDataframeCompute<IN, OUT> compute, Caller caller) {
    this.compute = compute;
    this.caller = caller;
  }

  @Override
  public void configurePipeline(final PipelineConfigurer pipelineConfigurer) throws IllegalArgumentException {
    caller.callUnchecked(new Callable<Void>() {
      @Override
      public Void call() {
        compute.configurePipeline(pipelineConfigurer);
        return null;
      }
    });
  }

  @Override
  public void initialize(final SparkExecutionPluginContext context) throws Exception {
    caller.call(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        compute.initialize(context);
        return null;
      }
    });
  }

  @Override
  public Dataset transform(final SparkExecutionPluginContext context, final Dataset input) throws Exception {
    return caller.call(new Callable<Dataset>() {
      @Override
      public Dataset call() throws Exception {
        return compute.transform(context, input);
      }
    });
  }
}
