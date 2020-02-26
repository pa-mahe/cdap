package co.cask.cdap.etl.spark.function;

import co.cask.cdap.api.data.format.StructuredRecord;
import co.cask.cdap.api.spark.sql.DataFrames;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

public class StructuredRecordToRowFunction<S extends StructuredRecord, R extends Row> implements Function<StructuredRecord, Row> {
  private StructType st;
  public StructuredRecordToRowFunction(StructType st){
    this.st = st;
  }

  @Override
  public Row call(StructuredRecord structuredRecord) throws Exception {
    return DataFrames.toRow(structuredRecord, st);
  }
}
