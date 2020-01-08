package co.cask.cdap.etl.spark.dataframe;

import co.cask.cdap.etl.spark.SparkCollection;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.api.java.function.FlatMapFunction;

public class CDataset<T> {
    //private final CDataset<T> dataset;

    public enum CDatasetType {
        Dataset,RDD;
    }
    private Dataset<T> df;
    private JavaRDD<T> rdd;
    private final CDatasetType type;
    private CDatasetOperator operator;

    public CDataset(Object o) {
        if (JavaRDD.class.isInstance(o)) {
            rdd = (JavaRDD<T>) o;
            type = CDatasetType.RDD;
            operator = new RDDOperator();
        } else if(Dataset.class.isInstance(o)) {
            df = (Dataset) o;
            type = CDatasetType.Dataset;
            operator = new DatasetOperator();
        } else {
            throw new IllegalArgumentException("Unknown sparkcollection:: " + o.getClass());
        }
    }

    public CDataset<T> persist(StorageLevel storageLevel) {
        return operator.persist(this, storageLevel);
    }

    public CDataset<T> union(SparkCollection other){
        throw new UnsupportedOperationException("not implemented yet");
    }

    public CDataset<T> flatMap(FlatMapFunction func){
        throw new UnsupportedOperationException("not implemented yet");
    }

    public JavaRDD<T> getRDD(){
        if(type == CDatasetType.RDD){
            return rdd;
        }else{
            throw new UnsupportedOperationException("not implemented yet");
        }
    }

    public Dataset<T> getDataset(){
        if(type == CDatasetType.Dataset){
            return df;
        }else{
            throw new UnsupportedOperationException("not implemented yet");
        }
    }


}
