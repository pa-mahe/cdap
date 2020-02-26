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

package co.cask.cdap.etl.spark.streaming.function;

import co.cask.cdap.api.spark.JavaSparkExecutionContext;
import co.cask.cdap.etl.api.batch.SparkCompute;
import co.cask.cdap.etl.api.batch.SparkExecutionPluginContext;
import co.cask.cdap.etl.api.dataframe.SparkDataframeCompute;
import co.cask.cdap.etl.spark.function.CountingFunction;
import co.cask.cdap.etl.spark.streaming.SparkStreamingExecutionContext;
import co.cask.cdap.etl.spec.StageSpec;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.streaming.Time;

import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.api.java.function.MapFunction;

/**
 * Function used to implement a SparkDataframeCompute stage in a DStream.
 *
 * @param <T> type of object in the input Dataset
 * @param <U> type of object in the output Dataset
 */
public class ComputeDataframeTransformFunction<T, U> implements Function2<Dataset, Time, Dataset> {
  private final JavaSparkExecutionContext sec;
  private final StageSpec stageSpec;
  private final SparkDataframeCompute<T, U> compute;

  public ComputeDataframeTransformFunction(JavaSparkExecutionContext sec, StageSpec stageSpec,
                                           SparkDataframeCompute<T, U> compute) {
    this.sec = sec;
    this.stageSpec = stageSpec;
    this.compute = compute;
  }

  @Override
  public Dataset call(Dataset data, Time batchTime) throws Exception {
    SparkExecutionPluginContext sparkPluginContext =
      new SparkStreamingExecutionContext(sec, JavaSparkContext.fromSparkContext(data.sparkSession().sparkContext()),
                                         batchTime.milliseconds(), stageSpec);
    String stageName = stageSpec.getName();

    Encoder inputEncoder = RowEncoder.apply(data.schema());
    MapFunction inputMapFunction = new CountingFunction(stageName, sec.getMetrics(), "records.in", null);
    Dataset countedInput = data.map(inputMapFunction, inputEncoder);

    Dataset outputDataset = compute.transform(sparkPluginContext, countedInput);
    Encoder outputEncoder = RowEncoder.apply(outputDataset.schema());
    MapFunction outputMapFunction = new CountingFunction(stageName, sec.getMetrics(), "records.out", sec.getDataTracer(stageName));
    Dataset countedOutput = outputDataset.map(outputMapFunction, outputEncoder);
    return countedOutput;
  }
}
