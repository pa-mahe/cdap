package co.cask.cdap.etl.spark.dataframe;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.api.java.function.FlatMapFunction;

public interface CDatasetOperator {

    CDataset persist(CDataset cDataset, StorageLevel storageLevel);

    CDataset flatmap(CDataset cDataset, FlatMapFunction func);

    CDataset union(CDataset cDataset, CDataset other);
}
