package co.cask.cdap.etl.spark.dataframe;

import co.cask.cdap.api.data.format.StructuredRecord;
import co.cask.cdap.api.spark.sql.DataFrames;
import co.cask.cdap.etl.api.batch.SparkExecutionPluginContext;
import co.cask.cdap.etl.spark.SparkCollection;
import co.cask.cdap.etl.spark.function.StructuredRecordToRowFunction;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.api.java.function.FlatMapFunction;

import co.cask.cdap.api.data.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class CDataset {
    private static final Logger LOG = LoggerFactory.getLogger(CDataset.class);
    //private final CDataset<T> dataset;

    public enum CDatasetType {
        Dataset,RDD, PairRDD;
    }
    private Dataset<Row> dataset;
    private JavaRDD<StructuredRecord> rdd;
    private JavaPairRDD pairRDD;
    private final CDatasetType type;
    private CDatasetOperator operator;

    public CDataset(Object o) {
        if (JavaRDD.class.isInstance(o)) {
            rdd = (JavaRDD) o;
            type = CDatasetType.RDD;
            operator = DatasetOperator.getInstance();
        } else if (JavaPairRDD.class.isInstance(o)) {
            pairRDD = (JavaPairRDD) o;
            type = CDatasetType.PairRDD;
            operator = PairRDDOperator.getInstance();
        } else if(Dataset.class.isInstance(o)) {
            dataset = (Dataset) o;
            type = CDatasetType.Dataset;
            operator = DatasetOperator.getInstance();
        } else {
            throw new IllegalArgumentException("Unknown sparkcollection:: " + o.getClass());
        }
    }

    public CDataset(JavaRDD rdd, CDatasetOperator operator) {
        this.rdd = rdd;
        this.operator = operator;
        type = CDatasetType.RDD;
    }

    public CDataset(JavaPairRDD pairRDD, CDatasetOperator operator) {
        this.pairRDD = pairRDD;
        this.operator = operator;
        type = CDatasetType.PairRDD;
    }

    public CDataset(Dataset dataset, CDatasetOperator operator) {
        this.dataset = dataset;
        this.operator = operator;
        type = CDatasetType.Dataset;
    }

    public CDataset persist(StorageLevel storageLevel) {
        return operator.persist(this, storageLevel);
    }

    public CDataset union(DataframeCollection other) {
        // biased towards dataset
        if(type == CDatasetType.Dataset || other.getUnderlying().getType() == CDatasetType.Dataset){
            return DatasetOperator.getInstance().union(this, other.getUnderlying());
        } else if(type == CDatasetType.RDD && other.getUnderlying().getType() == CDatasetType.RDD) {
            return operator.union(this, other.getUnderlying());
        } else {
            throw new IllegalArgumentException("Union not supported for PairRDD");
        }
    }

    public CDataset flatMap(FlatMapFunction func){
        return operator.flatmap(this, func);
    }

    public JavaRDD getRDD(){
        if(type == CDatasetType.RDD){
            return rdd;
        } else if(type == CDatasetType.Dataset){
            //TODO change to debug
            LOG.info("converting Dataset to RDD");
//            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
//                LOG.info(ste.toString());
//            }
            return convertRowTOStructuredRecordRdd(dataset.javaRDD(), convertStructTypeToSchema(dataset.schema()));
        } else {
            throw new UnsupportedOperationException("not implemented yet");
        }
    }

    private JavaRDD<StructuredRecord> convertRowTOStructuredRecordRdd(JavaRDD<Row> rowJavaRDD, Schema schema) {
        return rowJavaRDD.mapPartitions((FlatMapFunction<Iterator<Row>, StructuredRecord>) rowIterator -> {
            List<StructuredRecord> list = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                list.add(DataFrames.fromRow(row, schema));
            }
            return list.iterator();
        });
    }

    private static Schema convertStructTypeToSchema(@NotNull DataType schema) {
        return DataFrames.toSchema(schema);
    }

    public JavaPairRDD getPairRDD(){
        if(type == CDatasetType.PairRDD){
            return pairRDD;
        } else{
            throw new UnsupportedOperationException("not implemented yet");
        }
    }

    public Dataset<Row> getDataset(SparkExecutionPluginContext context, SparkSession sparkSession){
        if(type == CDatasetType.Dataset){
            return dataset;
        } else if(type == CDatasetType.RDD){

//            LOG.info("converting RDD to Dataset");
//            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
//                LOG.info(ste.toString());
//            }
            Schema inputSchema = context.getInputSchema();
            final StructType st = DataFrames.toDataType(inputSchema);
            return rddToDataset(st, sparkSession);

//            JavaRDD<Row> javardd = rdd.map(new StructuredRecordToRowFunction(st));
//            Dataset<Row> dataSet = sparkSession.createDataFrame(javardd, st);
//            return dataSet;
        } else if (type == CDatasetType.PairRDD){
            throw new UnsupportedOperationException("not implemented yet");
        } else {
            throw new IllegalArgumentException("Unknow type: "+ type);
        }
    }

    public Dataset<Row> rddToDataset(StructType schema, SparkSession sparkSession){
        if(type != CDatasetType.RDD){
            throw new IllegalArgumentException("only rdd is supported");
        }
        //TODO change to debug
        LOG.info("converting RDD to Dataset");
        JavaRDD<Row> javardd = rdd.map(new StructuredRecordToRowFunction(schema));
        Dataset<Row> dataSet = sparkSession.createDataFrame(javardd, schema);
        return dataSet;
    }

    public CDatasetType getType() {
        return type;
    }
}

