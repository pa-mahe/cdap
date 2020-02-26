package co.cask.cdap.etl.spark.dataframe;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.api.java.function.FlatMapFunction;

public class DatasetOperator implements CDatasetOperator {

    private static DatasetOperator datasetOperator = new DatasetOperator();

    private DatasetOperator(){

    }

    public static DatasetOperator getInstance(){
        return datasetOperator;
    }

    @Override
    public CDataset persist(CDataset cDataset, StorageLevel storageLevel) {
        return new CDataset(cDataset.getDataset(null,null).persist(storageLevel), this);
    }

    @Override
    public CDataset flatmap(CDataset cDataset, FlatMapFunction func) {
        //TODO chek if it can be implemented without converting to RDD
        return new CDataset(cDataset.getRDD().flatMap(func));
    }

    @Override
    public CDataset union(CDataset cDataset, CDataset other){
        if(cDataset.getType() == CDataset.CDatasetType.Dataset
                && other.getType() == CDataset.CDatasetType.Dataset){
            return new CDataset(cDataset.getDataset(null, null)
                    .union(other.getDataset(null, null)));
        } else if(cDataset.getType() == CDataset.CDatasetType.Dataset
                || other.getType() == CDataset.CDatasetType.Dataset){
            Dataset<Row> ds1 = null;
            CDataset rddCDataset = null;
            if(cDataset.getType() == CDataset.CDatasetType.Dataset){
                ds1 = cDataset.getDataset(null,null);
                rddCDataset = other;
            } else {
                ds1 = other.getDataset(null,null);
                rddCDataset = cDataset;
            }

            SparkSession sparkSession = ds1.sparkSession();
            StructType schema = ds1.schema();
            return new CDataset(ds1.union(rddCDataset.rddToDataset(schema, sparkSession)));
        } else {
            throw new IllegalArgumentException("one must be Dataset");
        }
    }

}
