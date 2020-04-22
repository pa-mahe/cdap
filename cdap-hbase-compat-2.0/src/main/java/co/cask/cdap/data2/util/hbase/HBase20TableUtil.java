/*
 * Copyright © 2015-2016 Cask Data, Inc.
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

package co.cask.cdap.data2.util.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceNotFoundException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import co.cask.cdap.data2.increment.hbase20.IncrementHandler;
import co.cask.cdap.data2.transaction.coprocessor.hbase20.DefaultTransactionProcessor;
import co.cask.cdap.data2.transaction.messaging.coprocessor.hbase20.MessageTableRegionObserver;
import co.cask.cdap.data2.transaction.messaging.coprocessor.hbase20.PayloadTableRegionObserver;
import co.cask.cdap.data2.transaction.queue.coprocessor.hbase20.DequeueScanObserver;
import co.cask.cdap.data2.transaction.queue.coprocessor.hbase20.HBaseQueueRegionObserver;
import co.cask.cdap.data2.util.TableId;
import org.apache.hadoop.hbase.client.Table;
import co.cask.cdap.spi.hbase.HBaseDDLExecutor;
import co.cask.cdap.spi.hbase.TableDescriptor;

/**
 *
 */
public class HBase20TableUtil extends HBaseTableUtil {

  private static final Logger LOG = LoggerFactory.getLogger(HBase20TableUtil.class);

  @Override
  public Table createHTable(Configuration conf, TableId tableId) throws IOException {
    Preconditions.checkArgument(tableId != null, "Table id should not be null");
    // orginal code is a connection leak connection :  should be closed as soon as the table is closed in this (not very good) programming model
    // therefore we introduce a convenient proxy class to fix this
    Connection connection = ConnectionFactory.createConnection(conf);
    //return connection.getTable(HTableNameConverter.toTableName(tablePrefix, tableId));
    return new ConnectionAwareTable(connection, connection.getTable(HTableNameConverter.toTableName(tablePrefix, tableId)));
//    return new HTable(conf, HTableNameConverter.toTableName(tablePrefix, tableId));
  }

  private static class ConnectionAwareTable implements Table {
    private boolean closed = false;
    private final Connection connection;
    private final Table delegate;

    public ConnectionAwareTable(Connection connection, Table delegate) {
      this.connection = connection;
      this.delegate = delegate;
    }

    @Override
    public void close() throws IOException {
      Stack<Exception> exceptions = new Stack<>();
      try {
        delegate.close();
      } catch (IOException e) {
        LOG.debug("Exception closing table: {} ", e, e);
        exceptions.push(e);
      }
      finally {
        try {
          connection.close();
        } catch (IOException e) {
          LOG.debug("Exeption closing underlying connection: {}", e, e);
          exceptions.push(e);
        }

      }

      closed = true;
      if (!exceptions.isEmpty()) {
        throw new IOException(String.format("Got exception(s) closing resource: %s", exceptions), exceptions.peek());
      }
    }

    @Override
    public TableName getName() {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.getName();
    }

    @Override
    public Configuration getConfiguration() {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.getConfiguration();
    }

    @Override
    @Deprecated
    public HTableDescriptor getTableDescriptor() throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.getTableDescriptor();
    }

    @Override
    public org.apache.hadoop.hbase.client.TableDescriptor getDescriptor() throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.getDescriptor();
    }

    @Override
    public boolean exists(Get get) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.exists(get);
    }

    @Override
    public boolean[] exists(List<Get> gets) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.exists(gets);
    }

    @Override
    @Deprecated
    public boolean[] existsAll(List<Get> gets) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.existsAll(gets);
    }

    @Override
    public void batch(List<? extends Row> actions, Object[] results) throws IOException, InterruptedException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      delegate.batch(actions, results);
    }

    @Override
    public <R> void batchCallback(List<? extends Row> actions, Object[] results, Batch.Callback<R> callback) throws IOException, InterruptedException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      delegate.batchCallback(actions, results, callback);
    }

    @Override
    public Result get(Get get) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.get(get);
    }

    @Override
    public Result[] get(List<Get> gets) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.get(gets);
    }

    @Override
    public ResultScanner getScanner(Scan scan) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.getScanner(scan);
    }

    @Override
    public ResultScanner getScanner(byte[] family) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.getScanner(family);
    }

    @Override
    public ResultScanner getScanner(byte[] family, byte[] qualifier) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.getScanner(family, qualifier);
    }

    @Override
    public void put(Put put) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      delegate.put(put);
    }

    @Override
    public void put(List<Put> puts) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      delegate.put(puts);
    }

    @Override
    @Deprecated
    public boolean checkAndPut(byte[] row, byte[] family, byte[] qualifier, byte[] value, Put put) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.checkAndPut(row, family, qualifier, value, put);
    }

    @Override
    @Deprecated
    public boolean checkAndPut(byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, Put put) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.checkAndPut(row, family, qualifier, compareOp, value, put);
    }

    @Override
    @Deprecated
    public boolean checkAndPut(byte[] row, byte[] family, byte[] qualifier, CompareOperator op, byte[] value, Put put) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      return delegate.checkAndPut(row, family, qualifier, op, value, put);
    }

    @Override
    public void delete(Delete delete) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      delegate.delete(delete);
    }

    @Override
    public void delete(List<Delete> deletes) throws IOException {
      Preconditions.checkArgument(!closed, "Resource was closed");
      delegate.delete(deletes);
    }
  }

  @Override
  public HTableDescriptorBuilder buildHTableDescriptor(TableId tableId) {
    Preconditions.checkArgument(tableId != null, "Table id should not be null");
    return new HBase20HTableDescriptorBuilder(HTableNameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public HTableDescriptorBuilder buildHTableDescriptor(HTableDescriptor descriptorToCopy) {
    Preconditions.checkArgument(descriptorToCopy != null, "Table descriptor should not be null");
    return new HBase20HTableDescriptorBuilder(descriptorToCopy);
  }

  @Override
  public HTableDescriptor getHTableDescriptor(Admin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    return admin.getTableDescriptor(HTableNameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public boolean hasNamespace(Admin admin, String namespace) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(namespace != null, "Namespace should not be null.");
    try {
      admin.getNamespaceDescriptor(HTableNameConverter.encodeHBaseEntity(namespace));
      return true;
    } catch (NamespaceNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean tableExists(Admin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    return admin.tableExists(HTableNameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public void deleteTable(HBaseDDLExecutor ddlExecutor, TableId tableId) throws IOException {
    Preconditions.checkArgument(ddlExecutor != null, "HBaseDDLExecutor should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    TableName tableName = HTableNameConverter.toTableName(tablePrefix, tableId);
    ddlExecutor.deleteTableIfExists(tableName.getNamespaceAsString(), tableName.getQualifierAsString());
  }

  @Override
  public void modifyTable(HBaseDDLExecutor ddlExecutor, HTableDescriptor tableDescriptor) throws IOException {
    Preconditions.checkArgument(ddlExecutor != null, "HBaseDDLExecutor should not be null");
    Preconditions.checkArgument(tableDescriptor != null, "Table descriptor should not be null.");
    TableName tableName = tableDescriptor.getTableName();
    TableDescriptor tbd = HBase20TableDescriptorUtil.getTableDescriptor(tableDescriptor);
    ddlExecutor.modifyTable(tableName.getNamespaceAsString(), tableName.getQualifierAsString(), tbd);
  }

  @Override
  public List<HRegionInfo> getTableRegions(Admin admin, TableId tableId) throws IOException {
    Preconditions.checkArgument(admin != null, "HBaseAdmin should not be null");
    Preconditions.checkArgument(tableId != null, "Table Id should not be null.");
    return admin.getTableRegions(HTableNameConverter.toTableName(tablePrefix, tableId));
  }

  @Override
  public List<TableId> listTablesInNamespace(Admin admin, String namespaceId) throws IOException {
    List<TableId> tableIds = Lists.newArrayList();
    HTableDescriptor[] hTableDescriptors =
      admin.listTableDescriptorsByNamespace(HTableNameConverter.encodeHBaseEntity(namespaceId));
    for (HTableDescriptor hTableDescriptor : hTableDescriptors) {
      if (isCDAPTable(hTableDescriptor)) {
        tableIds.add(HTableNameConverter.from(hTableDescriptor));
      }
    }
    return tableIds;
  }

  @Override
  public List<TableId> listTables(Admin admin) throws IOException {
    List<TableId> tableIds = Lists.newArrayList();
    HTableDescriptor[] hTableDescriptors = admin.listTables();
    for (HTableDescriptor hTableDescriptor : hTableDescriptors) {
      if (isCDAPTable(hTableDescriptor)) {
        tableIds.add(HTableNameConverter.from(hTableDescriptor));
      }
    }
    return tableIds;
  }

  @Override
  public void setCompression(HColumnDescriptor columnDescriptor, CompressionType type) {
    switch (type) {
      case LZO:
        columnDescriptor.setCompressionType(Compression.Algorithm.LZO);
        break;
      case SNAPPY:
        columnDescriptor.setCompressionType(Compression.Algorithm.SNAPPY);
        break;
      case GZIP:
        columnDescriptor.setCompressionType(Compression.Algorithm.GZ);
        break;
      case NONE:
        columnDescriptor.setCompressionType(Compression.Algorithm.NONE);
        break;
      default:
        throw new IllegalArgumentException("Unsupported compression type: " + type);
    }
  }

  @Override
  public void setBloomFilter(HColumnDescriptor columnDescriptor, BloomType type) {
    switch (type) {
      case ROW:
        columnDescriptor.setBloomFilterType(org.apache.hadoop.hbase.regionserver.BloomType.ROW);
        break;
      case ROWCOL:
        columnDescriptor.setBloomFilterType(org.apache.hadoop.hbase.regionserver.BloomType.ROWCOL);
        break;
      case NONE:
        columnDescriptor.setBloomFilterType(org.apache.hadoop.hbase.regionserver.BloomType.NONE);
        break;
      default:
        throw new IllegalArgumentException("Unsupported bloom filter type: " + type);
    }
  }

  @Override
  public CompressionType getCompression(HColumnDescriptor columnDescriptor) {
    Compression.Algorithm type = columnDescriptor.getCompressionType();
    switch (type) {
      case LZO:
        return CompressionType.LZO;
      case SNAPPY:
        return CompressionType.SNAPPY;
      case GZ:
        return CompressionType.GZIP;
      case NONE:
        return CompressionType.NONE;
      default:
        throw new IllegalArgumentException("Unsupported compression type: " + type);
    }
  }

  @Override
  public BloomType getBloomFilter(HColumnDescriptor columnDescriptor) {
    org.apache.hadoop.hbase.regionserver.BloomType type = columnDescriptor.getBloomFilterType();
    switch (type) {
      case ROW:
        return BloomType.ROW;
      case ROWCOL:
        return BloomType.ROWCOL;
      case NONE:
        return BloomType.NONE;
      default:
        throw new IllegalArgumentException("Unsupported bloom filter type: " + type);
    }
  }

  @Override
  public boolean isGlobalAdmin(Configuration hConf) throws IOException {
    try (Connection connection = ConnectionFactory.createConnection(hConf)) {
      if (!AccessControlClient.isAccessControllerRunning(connection)) {
        return true;
      }
      try {
        AccessControlClient.getUserPermissions(connection, "");
        return true;
      } catch (Throwable t) {
        LOG.warn("Failed to list user permissions to ensure global admin privilege", t);
        return false;
      }
    }
  }

  @Override
  public Class<? extends Coprocessor> getTransactionDataJanitorClassForVersion() {
    return DefaultTransactionProcessor.class;
  }

  @Override
  public Class<? extends Coprocessor> getQueueRegionObserverClassForVersion() {
    return HBaseQueueRegionObserver.class;
  }

  @Override
  public Class<? extends Coprocessor> getDequeueScanObserverClassForVersion() {
    return DequeueScanObserver.class;
  }

  @Override
  public Class<? extends Coprocessor> getIncrementHandlerClassForVersion() {
    return IncrementHandler.class;
  }

  @Override
  public Class<? extends Coprocessor> getMessageTableRegionObserverClassForVersion() {
    return MessageTableRegionObserver.class;
  }

  @Override
  public Class<? extends Coprocessor> getPayloadTableRegionObserverClassForVersion() {
    return PayloadTableRegionObserver.class;
  }

  @Override
  public ScanBuilder buildScan() {
    return new HBase20ScanBuilder();
  }

  @Override
  public ScanBuilder buildScan(Scan scan) throws IOException {
    return new HBase20ScanBuilder(scan);
  }

  @Override
  public IncrementBuilder buildIncrement(byte[] row) {
    return new HBase20IncrementBuilder(row);
  }

  @Override
  public PutBuilder buildPut(byte[] row) {
    return new HBase20PutBuilder(row);
  }

  @Override
  public PutBuilder buildPut(Put put) {
    return new HBase20PutBuilder(put);
  }

  @Override
  public GetBuilder buildGet(byte[] row) {
    return new HBase20GetBuilder(row);
  }

  @Override
  public GetBuilder buildGet(Get get) {
    return new HBase20GetBuilder(get);
  }

  @Override
  public DeleteBuilder buildDelete(byte[] row) {
    return new HBase20DeleteBuilder(row);
  }

  @Override
  public DeleteBuilder buildDelete(Delete delete) {
    return new HBase20DeleteBuilder(delete);
  }

}
