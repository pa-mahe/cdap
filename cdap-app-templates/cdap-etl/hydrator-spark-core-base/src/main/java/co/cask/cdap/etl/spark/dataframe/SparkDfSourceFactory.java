/*
 * Copyright © 2015 Cask Data, Inc.
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

import co.cask.cdap.api.data.batch.Input;
import co.cask.cdap.api.data.batch.InputFormatProvider;
import co.cask.cdap.api.data.batch.Split;
import co.cask.cdap.api.data.format.FormatSpecification;
import co.cask.cdap.api.data.format.StructuredRecord;
import co.cask.cdap.api.spark.JavaSparkExecutionContext;
import co.cask.cdap.api.stream.StreamEventDecoder;
import co.cask.cdap.etl.spark.batch.DatasetInfo;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Thread.currentThread;

/**
 * A POJO class for storing source information being set from {@link SparkBatchSourceContext} and used in
 * {@link BatchDfPipelineDriver}.
 */
public final class SparkDfSourceFactory {

  private static final Logger LOG = LoggerFactory.getLogger(SparkDfSourceFactory.class);

  private final Map<String, Input.StreamInput> streams;
  private final Map<String, InputFormatProvider> inputFormatProviders;
  private final Map<String, DatasetInfo> datasetInfos;
  private final Map<String, Set<String>> sourceInputs;

  SparkDfSourceFactory() {
    this.streams = new HashMap<>();
    this.inputFormatProviders = new HashMap<>();
    this.datasetInfos = new HashMap<>();
    this.sourceInputs = new HashMap<>();
  }

  public void addInput(String stageName, Input input) {

    if (input instanceof Input.DatasetInput) {
      // Note if input format provider is trackable then it comes in as DatasetInput
      Input.DatasetInput datasetInput = (Input.DatasetInput) input;
      addInput(stageName, datasetInput.getName(), datasetInput.getAlias(), datasetInput.getArguments(),
               datasetInput.getSplits());
    } else if (input instanceof Input.InputFormatProviderInput) {
      Input.InputFormatProviderInput ifpInput = (Input.InputFormatProviderInput) input;
      addInput(stageName, ifpInput.getAlias(),
               new BasicInputFormatProvider(ifpInput.getInputFormatProvider().getInputFormatClassName(),
                                            ifpInput.getInputFormatProvider().getInputFormatConfiguration()));
    } else if (input instanceof Input.StreamInput) {
      Input.StreamInput streamInput = (Input.StreamInput) input;
      addInput(stageName, streamInput.getAlias(), streamInput);
    }
  }

  private void addInput(String stageName, String alias, Input.StreamInput streamInput) {
    duplicateAliasCheck(alias);
    streams.put(alias, streamInput);
    addStageInput(stageName, alias);
  }

  private void addInput(String stageName, String datasetName, String alias, Map<String, String> datasetArgs,
                        List<Split> splits) {
    duplicateAliasCheck(alias);
    datasetInfos.put(alias, new DatasetInfo(datasetName, datasetArgs, splits));
    addStageInput(stageName, alias);
  }

  private void addInput(String stageName, String alias, BasicInputFormatProvider inputFormatProvider) {
    duplicateAliasCheck(alias);
    inputFormatProviders.put(alias, inputFormatProvider);
    addStageInput(stageName, alias);
  }

  private void duplicateAliasCheck(String alias) {
    if (inputFormatProviders.containsKey(alias) || datasetInfos.containsKey(alias)
      || streams.containsKey(alias)) {
      // this will never happen since alias will be unique since we append it with UUID
      throw new IllegalStateException(alias + " has already been added. Can't add an input with the same alias.");
    }
  }

  public <K, V> CDataset createDf(JavaSparkExecutionContext sec, SparkSession sparkSession, String sourceName) {

    Set<String> inputNames = sourceInputs.get(sourceName);

    if (inputNames == null || inputNames.isEmpty()) {
      // should never happen if validation happened correctly at pipeline configure time
      throw new IllegalArgumentException(
        sourceName + " has no input. Please check that the source calls setInput at some input.");
    }

    JavaPairRDD<K, V> inputRDD = null;
    for (String inputName : inputNames) {
      LOG.info("sbbbbb remove-me inputName :: " + inputName);
      if (inputRDD==null) {
        inputRDD = createInputDf(sec, sparkSession, inputName);
      } else {
        inputRDD = inputRDD.union(createInputDf(sec, sparkSession, inputName));
      }
    }
    return new CDataset(inputRDD);

  }

  //TODO sb remove createInputDf api
  private <K, V> JavaPairRDD createInputDf(JavaSparkExecutionContext sec, SparkSession sparkSession, String inputName) {

//    if (streams.containsKey(inputName)) {
//      Input.StreamInput streamInput = streams.get(inputName);
//      FormatSpecification formatSpec = streamInput.getBodyFormatSpec();
//      if (formatSpec != null) {
//        return (JavaPairRDD<K, V>) sec.fromStream(streamInput.getName(),
//                                                  formatSpec,
//                                                  streamInput.getStartTime(),
//                                                  streamInput.getEndTime(),
//                                                  StructuredRecord.class);
//      }
//
//      String decoderType = streamInput.getDecoderType();
//      if (decoderType == null) {
//        return (JavaPairRDD<K, V>) sec.fromStream(streamInput.getName(),
//                                                  streamInput.getStartTime(),
//                                                  streamInput.getEndTime(),
//                                                  valueClass);
//      } else {
//        try {
//          Class<StreamEventDecoder<K, V>> decoderClass =
//            (Class<StreamEventDecoder<K, V>>) Thread.currentThread().getContextClassLoader().loadClass(decoderType);
//          return sec.fromStream(streamInput.getName(),
//                                streamInput.getStartTime(),
//                                streamInput.getEndTime(),
//                                decoderClass, keyClass, valueClass);
//        } catch (Exception e) {
//          throw Throwables.propagate(e);
//        }
//      }
//    }

//    if (inputFormatProviders.containsKey(inputName)) {
//      InputFormatProvider inputFormatProvider = inputFormatProviders.get(inputName);
//      Configuration hConf = new Configuration();
//      hConf.clear();
//      for (Map.Entry<String, String> entry : inputFormatProvider.getInputFormatConfiguration().entrySet()) {
//        hConf.set(entry.getKey(), entry.getValue());
//      }
//      ClassLoader classLoader = Objects.firstNonNull(currentThread().getContextClassLoader(),
//                                                     getClass().getClassLoader());
//      try {
//        @SuppressWarnings("unchecked")
//        Class<InputFormat> inputFormatClass = (Class<InputFormat>) classLoader.loadClass(
//          inputFormatProvider.getInputFormatClassName());
//        return jsc.newAPIHadoopRDD(hConf, inputFormatClass, keyClass, valueClass);
//      } catch (ClassNotFoundException e) {
//        throw Throwables.propagate(e);
//      }
//    }
//
    if (datasetInfos.containsKey(inputName)) {
      DatasetInfo datasetInfo = datasetInfos.get(inputName);

      LOG.info("sbbbbb remove-me createDf datasetInfo name :: " + datasetInfo.getDatasetName());
      LOG.info("sbbbbb remove-me createDf datasetInfo args :: " + datasetInfo.getDatasetArgs());

      JavaPairRDD<Object, Object> pairRDD = sec.fromDataset(datasetInfo.getDatasetName(), datasetInfo.getDatasetArgs());
      LOG.info("sbbbbb remove-me createDf pairRDD :: " + pairRDD);
      if(pairRDD!=null){
        LOG.info("sbbbbb remove-me createDf pairRDD  not null ");
      }else{
        LOG.info("sbbbbb remove-me createDf pairRDD  size:: pairRDD is null");
      }

//      JavaRDD<Row> rowRDD = (JavaRDD<Row>) pairRDD.map(tuple -> RowFactory.create(tuple._1(),tuple._2()));

//      StructField[] fields = new StructField[2];
//      fields[0] = new StructField("k", DataTypes.StringType, true, null);
//      fields[1] = new StructField("v", DataTypes.StringType, true, null);
//      StructType structType =  new StructType(fields);
//      Dataset<Row> dataset = sparkSession.createDataFrame(rowRDD, Row.class);
      return pairRDD;
    }
//    // This should never happen since the constructor is private and it only get calls from static create() methods
//    // which make sure one and only one of those source type will be specified.
//    throw new IllegalStateException("Unknown source type");

    throw new UnsupportedOperationException("not implemented yet");
  }

  @SuppressWarnings("unchecked")
//  private <T> CDataset<T> createInputDf(JavaSparkExecutionContext sec, SparkSession sparkSession, String inputName,
//                                                  Class<K> keyClass, Class<V> valueClass) {
//    if (streams.containsKey(inputName)) {
//      Input.StreamInput streamInput = streams.get(inputName);
//      FormatSpecification formatSpec = streamInput.getBodyFormatSpec();
//      if (formatSpec != null) {
//        return (JavaPairRDD<K, V>) sec.fromStream(streamInput.getName(),
//                                                  formatSpec,
//                                                  streamInput.getStartTime(),
//                                                  streamInput.getEndTime(),
//                                                  StructuredRecord.class);
//      }
//
//      String decoderType = streamInput.getDecoderType();
//      if (decoderType == null) {
//        return (JavaPairRDD<K, V>) sec.fromStream(streamInput.getName(),
//                                                  streamInput.getStartTime(),
//                                                  streamInput.getEndTime(),
//                                                  valueClass);
//      } else {
//        try {
//          Class<StreamEventDecoder<K, V>> decoderClass =
//            (Class<StreamEventDecoder<K, V>>) Thread.currentThread().getContextClassLoader().loadClass(decoderType);
//          return sec.fromStream(streamInput.getName(),
//                                streamInput.getStartTime(),
//                                streamInput.getEndTime(),
//                                decoderClass, keyClass, valueClass);
//        } catch (Exception e) {
//          throw Throwables.propagate(e);
//        }
//      }
//    }
//
//    if (inputFormatProviders.containsKey(inputName)) {
//      InputFormatProvider inputFormatProvider = inputFormatProviders.get(inputName);
//      Configuration hConf = new Configuration();
//      hConf.clear();
//      for (Map.Entry<String, String> entry : inputFormatProvider.getInputFormatConfiguration().entrySet()) {
//        hConf.set(entry.getKey(), entry.getValue());
//      }
//      ClassLoader classLoader = Objects.firstNonNull(currentThread().getContextClassLoader(),
//                                                     getClass().getClassLoader());
//      try {
//        @SuppressWarnings("unchecked")
//        Class<InputFormat> inputFormatClass = (Class<InputFormat>) classLoader.loadClass(
//          inputFormatProvider.getInputFormatClassName());
//        return jsc.newAPIHadoopRDD(hConf, inputFormatClass, keyClass, valueClass);
//      } catch (ClassNotFoundException e) {
//        throw Throwables.propagate(e);
//      }
//    }
//
//    if (datasetInfos.containsKey(inputName)) {
//      DatasetInfo datasetInfo = datasetInfos.get(inputName);
//      return sec.fromDataset(datasetInfo.getDatasetName(), datasetInfo.getDatasetArgs());
//    }
//    // This should never happen since the constructor is private and it only get calls from static create() methods
//    // which make sure one and only one of those source type will be specified.
//    throw new IllegalStateException("Unknown source type");
//  }

  private void addStageInput(String stageName, String inputName) {
    Set<String> inputs = sourceInputs.get(stageName);
    if (inputs == null) {
      inputs = new HashSet<>();
    }
    inputs.add(inputName);
    sourceInputs.put(stageName, inputs);
  }

  static final class BasicInputFormatProvider implements InputFormatProvider {

    private final String inputFormatClassName;
    private final Map<String, String> configuration;

    BasicInputFormatProvider(String inputFormatClassName, Map<String, String> configuration) {
      this.inputFormatClassName = inputFormatClassName;
      this.configuration = ImmutableMap.copyOf(configuration);
    }

    BasicInputFormatProvider() {
      this.inputFormatClassName = "";
      this.configuration = new HashMap<>();
    }

    @Override
    public String getInputFormatClassName() {
      return inputFormatClassName;
    }

    @Override
    public Map<String, String> getInputFormatConfiguration() {
      return configuration;
    }
  }
}
