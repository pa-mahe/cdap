package co.cask.cdap.etl.spark.dataframe;

import org.apache.spark.storage.StorageLevel;

public interface CDatasetOperator<T> {

    CDataset<T> persist(CDataset<T> cDataset, StorageLevel storageLevel);

}
