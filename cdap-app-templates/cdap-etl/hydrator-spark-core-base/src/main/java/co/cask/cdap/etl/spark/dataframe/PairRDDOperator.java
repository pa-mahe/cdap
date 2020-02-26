package co.cask.cdap.etl.spark.dataframe;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.api.java.function.FlatMapFunction;

public class PairRDDOperator implements CDatasetOperator {

    private static PairRDDOperator pairRDDOperator = new PairRDDOperator();

    private PairRDDOperator(){

    }

    public static PairRDDOperator getInstance(){
        return pairRDDOperator;
    }

    @Override
    public CDataset persist(CDataset cDataset, StorageLevel storageLevel) {
        return new CDataset(cDataset.getPairRDD().persist(storageLevel), this);
    }

    @Override
    public CDataset flatmap(CDataset cDataset, FlatMapFunction func){
        return new CDataset(cDataset.getPairRDD().flatMap(func));
    }

    @Override
    public CDataset union(CDataset cDataset, CDataset other){
        throw new UnsupportedOperationException();
    }

}
