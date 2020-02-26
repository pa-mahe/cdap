package co.cask.cdap.etl.spark.dataframe;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.api.java.function.FlatMapFunction;

public class RDDOperator implements CDatasetOperator {

    private static RDDOperator rddOperator = new RDDOperator();

    private RDDOperator(){

    }

    public static RDDOperator getInstance(){
        return rddOperator;
    }

    @Override
    public CDataset persist(CDataset cDataset, StorageLevel storageLevel) {
        return new CDataset(cDataset.getRDD().persist(storageLevel), this);
    }

    @Override
    public CDataset flatmap(CDataset cDataset, FlatMapFunction func){
        return new CDataset(cDataset.getRDD().flatMap(func));
    }

    @Override
    public CDataset union(CDataset cDataset, CDataset other){
        return new CDataset(cDataset.getRDD().union(other.getRDD()));
    }

}
