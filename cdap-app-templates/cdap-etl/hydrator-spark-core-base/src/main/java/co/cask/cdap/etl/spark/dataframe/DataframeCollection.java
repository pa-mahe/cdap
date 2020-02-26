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

import co.cask.cdap.api.data.DatasetContext;
import co.cask.cdap.api.spark.JavaSparkExecutionContext;
import co.cask.cdap.etl.api.Alert;
import co.cask.cdap.etl.api.AlertPublisher;
import co.cask.cdap.etl.api.AlertPublisherContext;
import co.cask.cdap.etl.api.StageMetrics;
import co.cask.cdap.etl.api.batch.SparkCompute;
import co.cask.cdap.etl.api.batch.SparkExecutionPluginContext;
import co.cask.cdap.etl.api.batch.SparkSink;
import co.cask.cdap.etl.api.dataframe.SparkDataframeCompute;
import co.cask.cdap.etl.api.dataframe.SparkDataframeSink;
import co.cask.cdap.etl.api.dataframe.SparkDataframeSource;
import co.cask.cdap.etl.api.streaming.Windower;
import co.cask.cdap.etl.common.Constants;
import co.cask.cdap.etl.common.DefaultAlertPublisherContext;
import co.cask.cdap.etl.common.DefaultStageMetrics;
import co.cask.cdap.etl.common.PipelineRuntime;
import co.cask.cdap.etl.common.RecordInfo;
import co.cask.cdap.etl.common.StageStatisticsCollector;
import co.cask.cdap.etl.common.TrackedIterator;
import co.cask.cdap.etl.spark.Compat;
import co.cask.cdap.etl.spark.SparkCollection;
import co.cask.cdap.etl.spark.SparkPairCollection;
import co.cask.cdap.etl.spark.SparkPipelineRuntime;
import co.cask.cdap.etl.spark.batch.BasicSparkExecutionPluginContext;
import co.cask.cdap.etl.spark.batch.PairRDDCollection;
import co.cask.cdap.etl.spark.batch.SparkBatchSinkFactory;
import co.cask.cdap.etl.spark.function.AggregatorAggregateFunction;
import co.cask.cdap.etl.spark.function.AggregatorGroupByFunction;
import co.cask.cdap.etl.spark.function.CountingFunction;
import co.cask.cdap.etl.spark.function.FlatMapFunc;
import co.cask.cdap.etl.spark.function.MultiOutputTransformFunction;
import co.cask.cdap.etl.spark.function.PairFlatMapFunc;
import co.cask.cdap.etl.spark.function.PluginFunctionContext;
import co.cask.cdap.etl.spark.function.TransformFunction;
import co.cask.cdap.etl.spec.StageSpec;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;

import org.apache.spark.sql.Dataset;
import org.apache.spark.api.java.function.FlatMapFunction;

import javax.annotation.Nullable;
import javax.xml.crypto.Data;


/**
 * Implementation of {@link SparkCollection} that is backed by a JavaRDD.
 *
 * @param <T> type of object in the collection
 */
public class DataframeCollection<T> implements SparkCollection<T> {
  private static final Gson GSON = new Gson();
  private final JavaSparkExecutionContext sec;
  private final DatasetContext datasetContext;
  private final SparkDfSinkFactory sinkFactory;
  private final CDataset cDataset;
  private final SparkSession sparkSession;

  private final boolean metricsEnabled;
  private final boolean cachingEnabled;

  public DataframeCollection(SparkSession sparkSession, JavaSparkExecutionContext sec,
                             DatasetContext datasetContext, SparkDfSinkFactory sinkFactory, CDataset cDataset) {
    this.sec = sec;
    this.datasetContext = datasetContext;
    this.sinkFactory = sinkFactory;
    this.cDataset = cDataset;
    this.sparkSession = sparkSession;

    SparkConf sparkConf = sparkSession.sparkContext().getConf();
    metricsEnabled = sparkConf.getBoolean(Constants.SPARK_PIPELINE_METRICS_ENABLE_FLAG, true);
    cachingEnabled = sparkConf.getBoolean(Constants.SPARK_PIPELINE_AUTOCACHE_ENABLE_FLAG, true);
  }

  @SuppressWarnings("unchecked")
  @Override
  public CDataset getUnderlying() {
    return cDataset;
  }

  @Override
  public SparkCollection<T> cache() {
    SparkConf sparkConf = sparkSession.sparkContext().getConf();
    if (sparkConf.getBoolean(Constants.SPARK_PIPELINE_AUTOCACHE_ENABLE_FLAG, true)) {
      String cacheStorageLevelString = sparkConf.get(Constants.SPARK_PIPELINE_CACHING_STORAGE_LEVEL, 
                                                     Constants.DEFAULT_CACHING_STORAGE_LEVEL);
      StorageLevel cacheStorageLevel = StorageLevel.fromString(cacheStorageLevelString);
      return wrap(cDataset.persist(cacheStorageLevel));
    } else {
      return wrap(cDataset);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public SparkCollection<T> union(SparkCollection<T> other) {
    return wrap(cDataset.union(((DataframeCollection)other)));
  }

  @Override
  public SparkCollection<RecordInfo<Object>> transform(StageSpec stageSpec, StageStatisticsCollector collector) {
    PluginFunctionContext pluginFunctionContext = new PluginFunctionContext(stageSpec, sec, collector);
    //return wrap(rdd.flatMap(Compat.convert(new TransformFunction<T>(pluginFunctionContext))));
    return wrap(cDataset.flatMap(Compat.convert(new TransformFunction<T>(pluginFunctionContext))));
  }

  @Override
  public SparkCollection<RecordInfo<Object>> multiOutputTransform(StageSpec stageSpec,
                                                                  StageStatisticsCollector collector) {
    PluginFunctionContext pluginFunctionContext = new PluginFunctionContext(stageSpec, sec, collector);
    return wrap(cDataset.flatMap(Compat.convert(new MultiOutputTransformFunction<T>(pluginFunctionContext))));
  }

  @Override
  public <U> SparkCollection<U> flatMap(StageSpec stageSpec, FlatMapFunction<T, U> function) {
    return wrap(cDataset.flatMap(function));
  }

  @Override
  public SparkCollection<RecordInfo<Object>> aggregate(StageSpec stageSpec, @Nullable Integer partitions,
                                                       StageStatisticsCollector collector) {
    PluginFunctionContext pluginFunctionContext = new PluginFunctionContext(stageSpec, sec, collector);
    PairFlatMapFunc<T, Object, T> groupByFunction = new AggregatorGroupByFunction<>(pluginFunctionContext);
    PairFlatMapFunction<T, Object, T> sparkGroupByFunction = Compat.convert(groupByFunction);

    JavaPairRDD<Object, T> keyedCollection = cDataset.getRDD().flatMapToPair(sparkGroupByFunction);

    JavaPairRDD<Object, Iterable<T>> groupedCollection = partitions == null ?
      keyedCollection.groupByKey() : keyedCollection.groupByKey(partitions);

    FlatMapFunc<Tuple2<Object, Iterable<T>>, RecordInfo<Object>> aggregateFunction =
      new AggregatorAggregateFunction<>(pluginFunctionContext);
    FlatMapFunction<Tuple2<Object, Iterable<T>>, RecordInfo<Object>> sparkAggregateFunction =
      Compat.convert(aggregateFunction);

    return wrap(groupedCollection.flatMap(sparkAggregateFunction));
  }

  @Override
  public <K, V> SparkPairCollection<K, V> flatMapToPair(PairFlatMapFunction<T, K, V> function) {
    //return new PairRDDCollection(sec, sparkSession, datasetContext, sinkFactory, cDataset.getRDD().flatMapToPair(function));
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public <U> SparkCollection<U> load(StageSpec stageSpec, SparkDataframeSource<U> source) throws Exception {
    String stageName = stageSpec.getName();
    PipelineRuntime pipelineRuntime = new SparkPipelineRuntime(sec);
    SparkExecutionPluginContext sparkPluginContext =
            new BasicSparkExecutionPluginContext(sec, sparkSession, datasetContext, pipelineRuntime, stageSpec);
    source.initialize(sparkPluginContext);

    Dataset inputDataset = source.load(sparkPluginContext);
    if (metricsEnabled) {
      Encoder encoder = RowEncoder.apply(inputDataset.schema());
      MapFunction mapFunction = new CountingFunction(stageName, sec.getMetrics(), "records.out",
              sec.getDataTracer(stageName));
      inputDataset = inputDataset.map(mapFunction, encoder);
    }
    return wrap(inputDataset);

  }

  @Override
  public <U> SparkCollection<U> compute(StageSpec stageSpec, SparkCompute<T, U> compute) throws Exception {
    String stageName = stageSpec.getName();
    PipelineRuntime pipelineRuntime = new SparkPipelineRuntime(sec);
    SparkExecutionPluginContext sparkPluginContext =
      new BasicSparkExecutionPluginContext(sec, sparkSession, datasetContext, pipelineRuntime, stageSpec);
    compute.initialize(sparkPluginContext);

    JavaRDD<T> inputRDD = cDataset.getRDD();
    if (metricsEnabled) {
      inputRDD = inputRDD.map(new CountingFunction(stageName, sec.getMetrics(), "records.in", null));
    }

    if (cachingEnabled) {
      inputRDD = inputRDD.cache();
    }

    JavaRDD outputRDD = compute.transform(sparkPluginContext, inputRDD);
    if (metricsEnabled) {
      outputRDD = outputRDD.map(new CountingFunction<U>(stageName, sec.getMetrics(), "records.out",
              sec.getDataTracer(stageName)));
    }
    return wrap(outputRDD);
  }

  @Override
  public <U> SparkCollection<U> compute(StageSpec stageSpec, SparkDataframeCompute<T, U> compute) throws Exception {
    String stageName = stageSpec.getName();
    PipelineRuntime pipelineRuntime = new SparkPipelineRuntime(sec);
    SparkExecutionPluginContext sparkPluginContext =
            new BasicSparkExecutionPluginContext(sec, sparkSession, datasetContext, pipelineRuntime, stageSpec);
    compute.initialize(sparkPluginContext);


    //cDataset.getDataset().map()
    //JavaRDD<T> countedInput = rdd.map(new CountingFunction<T>(stageName, sec.getMetrics(), "records.in", null));
    Dataset inputDataset = cDataset.getDataset(sparkPluginContext, sparkSession);
    if (metricsEnabled) {
      Encoder inputEncoder = RowEncoder.apply(inputDataset.schema());
      MapFunction inputMapFunction = new CountingFunction(stageName, sec.getMetrics(), "records.in", null);
      inputDataset = inputDataset.map(inputMapFunction, inputEncoder);
    }

    if (cachingEnabled) {
      inputDataset.cache();
    }

//    return wrap(compute.transform(sparkPluginContext, countedInput)
//            .map(new CountingFunction<U>(stageName, sec.getMetrics(), "records.out",
//                    sec.getDataTracer(stageName))));
    Dataset outputDataset = compute.transform(sparkPluginContext, inputDataset);

    if (metricsEnabled) {
      Encoder outputEncoder = RowEncoder.apply(outputDataset.schema());
      MapFunction outputMapFunction = new CountingFunction(stageName, sec.getMetrics(), "records.out", sec.getDataTracer(stageName));
      outputDataset = outputDataset.map(outputMapFunction, outputEncoder);
    }

    return wrap(outputDataset);
  }

  @Override
  public Runnable createStoreTask(final StageSpec stageSpec,
                                  final PairFlatMapFunction<T, Object, Object> sinkFunction) {
    return new Runnable() {
      @Override
      public void run() {
        JavaPairRDD<Object, Object> sinkRDD = cDataset.getRDD().flatMapToPair(sinkFunction);
        sinkFactory.writeRDDFromDf(new CDataset(sinkRDD), sec, stageSpec.getName(), Object.class, Object.class);
      }
    };
  }

  @Override
  public Runnable createStoreTask(final StageSpec stageSpec, final SparkSink<T> sink) throws Exception {
    return new Runnable() {
      @Override
      public void run() {
        String stageName = stageSpec.getName();
        PipelineRuntime pipelineRuntime = new SparkPipelineRuntime(sec);
        SparkExecutionPluginContext sparkPluginContext =
          new BasicSparkExecutionPluginContext(sec, sparkSession, datasetContext, pipelineRuntime, stageSpec);

        JavaRDD<T> inputRDD = cDataset.getRDD();
        if(metricsEnabled){
          inputRDD = cDataset.getRDD().map(new CountingFunction<T>(stageName, sec.getMetrics(), "records.in", null));
        }

        if (cachingEnabled) {
          inputRDD = inputRDD.cache();
        }
    
        try {
          sink.run(sparkPluginContext, inputRDD);
        } catch (Exception e) {
          Throwables.propagate(e);
        }
      }
    };
  }

  @Override
  public Runnable createStoreTask(StageSpec stageSpec, SparkDataframeSink<T> sink) throws Exception {

    return new Runnable() {
      @Override
      public void run() {
        String stageName = stageSpec.getName();
        PipelineRuntime pipelineRuntime = new SparkPipelineRuntime(sec);
        SparkExecutionPluginContext sparkPluginContext =
                new BasicSparkExecutionPluginContext(sec, sparkSession, datasetContext, pipelineRuntime, stageSpec);

        Dataset inputDataset = cDataset.getDataset(sparkPluginContext, sparkSession);
        if (cachingEnabled) {
          inputDataset = inputDataset.cache();
        }

        if (metricsEnabled) {
          Encoder inputEncoder = RowEncoder.apply(inputDataset.schema());
          MapFunction inputMapFunction = new CountingFunction(stageName, sec.getMetrics(), "records.in", null);
          inputDataset = inputDataset.map(inputMapFunction, inputEncoder);
        }

        try {
          sink.run(sparkPluginContext, inputDataset);
        } catch (Exception e) {
          Throwables.propagate(e);
        }
      }
    };
  }

  @Override
  public void publishAlerts(StageSpec stageSpec, StageStatisticsCollector collector) throws Exception {
    PluginFunctionContext pluginFunctionContext = new PluginFunctionContext(stageSpec, sec, collector);
    AlertPublisher alertPublisher = pluginFunctionContext.createPlugin();
    PipelineRuntime pipelineRuntime = new SparkPipelineRuntime(sec);

    AlertPublisherContext alertPublisherContext =
      new DefaultAlertPublisherContext(pipelineRuntime, stageSpec, sec.getMessagingContext(), sec.getAdmin());
    alertPublisher.initialize(alertPublisherContext);
    StageMetrics stageMetrics = new DefaultStageMetrics(sec.getMetrics(), stageSpec.getName());
    TrackedIterator<Alert> trackedAlerts =
      new TrackedIterator<>(((JavaRDD<Alert>) cDataset.getRDD()).collect().iterator(), stageMetrics, Constants.Metrics.RECORDS_IN);
    alertPublisher.publish(trackedAlerts);
    alertPublisher.destroy();
  }

  @Override
  public SparkCollection<T> window(StageSpec stageSpec, Windower windower) {
    throw new UnsupportedOperationException("Windowing is not supported on RDDs.");
  }

  private <U> DataframeCollection<U> wrap(CDataset cDataset) {
    return new DataframeCollection<>(sparkSession, sec, datasetContext, sinkFactory, cDataset);
  }

  private <U> DataframeCollection<U> wrap(JavaRDD rdd) {
    return new DataframeCollection<>(sparkSession, sec, datasetContext, sinkFactory, new CDataset(rdd));
  }

  private <U> DataframeCollection<U> wrap(Dataset dataset) {
    return new DataframeCollection<>(sparkSession, sec, datasetContext, sinkFactory, new CDataset(dataset));
  }

}
