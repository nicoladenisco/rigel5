package com.workingdogs.village;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.torque.criteria.SqlEnum;
import org.commonlib5.utils.Pair;
import org.commonlib5.utils.StringOper;
import org.rigel5.db.sql.FiltroData;

/**
 * This class is used for doing SQL select statements on the database.
 * It should not be used for doing modifications via
 * update/delete/insert statements. If you would like to perform those functions,
 * please use a <a href="TableDataSet.html">TableDataSet</a>.
 *
 * <P>
 * Here is some example code for using a QueryDataSet.
 * <PRE>
 *  try(QueryDataSet qds = new QueryDataSet ( connection, "SELECT * from my_table" )) {
 *    qds.fetchRecords(10); // fetch the first 10 records
 *    for ( int i = 0; i &lt; qds.size(); i++ )
 *    {
 *      Record rec = qds.getRecord(i);
 *      int value = rec.getValue("column").asInt();
 *      log.debug ( "The value is: " + value );
 *    }
 *  }
 * </PRE>
 * It is important to always remember to close() a QueryDataSet in order to free the allocated resources.
 * </p>
 *
 * @author
 * <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @version $Revision: 564 $
 */
public class QueryDataSet
   extends DataSet
{
  /**
   * Costruttore per classi derivate.
   *
   * @exception SQLException
   * @exception DataSetException
   */
  public QueryDataSet()
     throws SQLException, DataSetException
  {
  }

  /**
   * Creates a new QueryDataSet based on a connection and a select string.
   *
   * @param conn
   * @param selectStmt
   *
   * @exception SQLException
   * @exception DataSetException
   */
  public QueryDataSet(Connection conn, String selectStmt)
     throws SQLException, DataSetException
  {
    this.conn = conn;
    selectString = new StringBuilder(selectStmt);

    boolean ok = false;
    try
    {
      stmt = conn.createStatement();
      resultSet = stmt.executeQuery(selectStmt);
      schema = new Schema();
      schema.populate(resultSet.getMetaData(), null, conn);
      ok = true;
    }
    finally
    {
      if(!ok)
      {
        try
        {
          close();
        }
        catch(Exception ignored)
        {
          // ignore as another exception is already thrown
        }
      }
    }
  }

  /**
   * Create a new QueryDataSet based on an existing resultSet.
   *
   * @param resultSet
   *
   * @exception SQLException
   * @exception DataSetException
   */
  public QueryDataSet(ResultSet resultSet)
     throws SQLException, DataSetException
  {
    this.resultSet = resultSet;
    this.conn = resultSet.getStatement().getConnection();
    selectString = new StringBuilder();
    schema = new Schema();
    schema.populate(resultSet.getMetaData(), null, conn);
  }

  /**
   * get the Select String that was used to create this QueryDataSet.
   *
   * @return a select string
   */
  @Override
  public String getSelectString()
  {
    return selectString == null ? "" : selectString.toString();
  }

  /**
   * Returns numberOfResults records in a QueryDataSet as a List
   * of Record objects. Starting at record start. Used for
   * functionality like util.LargeSelect.
   *
   * @param start The index from which to start retrieving
   * <code>Record</code> objects from the data set.
   * @param numberOfResults The number of results to return (or
   * <code> -1</code> for all results).
   * @return A <code>List</code> of <code>Record</code> objects.
   * @exception Exception
   */
  public List<Record> getSelectResults(int start, int numberOfResults)
     throws Exception
  {
    List<Record> results = null;

    if(numberOfResults < 0)
    {
      results = new ArrayList<>();
      fetchRecords();
    }
    else
    {
      results = new ArrayList<>(numberOfResults);
      fetchRecords(start, numberOfResults);
    }

    int startRecord = 0;

    // Offset the correct number of records
    if(start > 0 && numberOfResults <= 0)
    {
      startRecord = start;
    }

    // Return a List of Record objects.
    for(int i = startRecord; i < size(); i++)
    {
      Record rec = getRecord(i);
      results.add(rec);
    }

    return results;
  }

  public static Record fetchFirstRecord(Connection dbCon, String sSQL)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(dbCon, sSQL))
    {
      qs.fetchRecords(1);

      if(qs.size() == 0)
        return null;

      return qs.getRecord(0);
    }
  }

  public static Pair<Schema, Record> fetchFirstRecordAndSchema(Connection dbCon, String sSQL)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(dbCon, sSQL))
    {
      qs.fetchRecords(1);

      if(qs.size() == 0)
        return new Pair<>(qs.schema, null);

      return new Pair<>(qs.schema, qs.getRecord(0));
    }
  }

  public static List<Record> fetchAllRecords(Connection dbCon, String sSQL)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(dbCon, sSQL))
    {
      return qs.fetchAllRecords();
    }
  }

  public static Pair<Schema, List<Record>> fetchAllRecordsAndSchema(Connection dbCon, String sSQL)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(dbCon, sSQL))
    {
      return new Pair<>(qs.schema, qs.fetchAllRecords());
    }
  }

  public static List<Record> fetchAllRecords(ResultSet rs)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(rs))
    {
      return qs.fetchAllRecords();
    }
  }

  public static Pair<Schema, List<Record>> fetchAllRecordsAndSchema(ResultSet rs)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(rs))
    {
      return new Pair<>(qs.schema, qs.fetchAllRecords());
    }
  }

  public QueryDataSet(Connection conn, String select, String from, FiltroData filtro)
     throws SQLException, DataSetException
  {
    String sql1 = buildQueryNoWhere(select, from, filtro) + " WHERE 1 = -1";
    Schema s = new Schema();

    try (Statement stm = conn.createStatement(); ResultSet rs = stm.executeQuery(sql1))
    {
      s.populate(rs.getMetaData(), null, conn);
    }

    String sql2 = buildQuery(select, from, filtro, s);
    PreparedStatement lstm = conn.prepareStatement(sql2);
    int ps = 1;

    for(FiltroData.betweenInfo bi : filtro.vBetween)
    {
      Column col = s.findInSchemaIgnoreCase(bi.nomecampo);
      Value v1 = new Value(ps, col, col.typeEnum(), bi.val1);
      Value v2 = new Value(ps, col, col.typeEnum(), bi.val2);

      if(v1.isNull() || v2.isNull())
        throw new DataSetException("Null not allowed in between values.");

      v1.setPreparedStatementValue(lstm, ps++);
      v2.setPreparedStatementValue(lstm, ps++);
    }

    for(FiltroData.whereInfo wi : filtro.vWhere)
    {
      Column col = s.findInSchemaIgnoreCase(wi.nomecampo);

      if(SqlEnum.ISNULL.equals(wi.criteria))
        ;
      else if(SqlEnum.ISNOTNULL.equals(wi.criteria))
        ;
      else if(SqlEnum.IN.equals(wi.criteria))
      {
        Collection sVals = (Collection) wi.val;
        for(Object v : sVals)
        {
          Value val = new Value(ps, col, col.typeEnum(), v);

          if(val.isNull())
            throw new DataSetException("Null not allowed in SqlEnum.IN values; use SqlEnum.ISNULL instead.");

          val.setPreparedStatementValue(lstm, ps++);
        }
      }
      else
      {
        Value val = new Value(ps, col, col.typeEnum(), wi.val);

        if(val.isNull())
          throw new DataSetException("Null not allowed in where values; use SqlEnum.ISNULL instead.");

        val.setPreparedStatementValue(lstm, ps++);
      }
    }

    this.stmt = lstm;
    this.resultSet = lstm.executeQuery();
    this.selectString = new StringBuilder(sql2);
    this.schema = new Schema();

    schema.populate(resultSet.getMetaData(), null, conn);
  }

  private String buildQueryNoWhere(String select, String from, FiltroData filtro)
     throws DataSetException
  {
    if(StringOper.isOkStr(select))
    {
      if(filtro.haveSelect())
        select += "," + StringOper.join(filtro.vSelect.iterator(), ',');
    }
    else
    {
      if(!filtro.haveSelect())
        throw new DataSetException("Missing SELECT clausole for query.");

      select = StringOper.join(filtro.vSelect.iterator(), ',');
    }

    return "SELECT " + select + " FROM " + from;
  }

  private String buildQuery(String select, String from, FiltroData filtro, Schema s)
     throws DataSetException
  {
    int count = 0;
    StringBuilder sb = new StringBuilder(256);

    for(FiltroData.betweenInfo bi : filtro.vBetween)
    {
      Column c = s.findInSchemaIgnoreCase(bi.nomecampo);

      if(count != 0)
        sb.append(" AND ");

      sb.append("((").append(c.name()).append(" >= ?) AND (").append(c.name()).append(" <= ?))");
      count++;
    }

    for(FiltroData.whereInfo wi : filtro.vWhere)
    {
      Column c = s.findInSchemaIgnoreCase(wi.nomecampo);

      if(count != 0)
        sb.append(" AND ");

      if(SqlEnum.ISNULL.equals(wi.criteria))
        sb.append("(").append(c.name()).append(" IS NULL)");
      else if(SqlEnum.ISNOTNULL.equals(wi.criteria))
        sb.append("(").append(c.name()).append(" IS NOT NULL)");
      else if(SqlEnum.IN.equals(wi.criteria))
      {
        Collection sVals = new ArrayList();

        if(wi.val instanceof Collection)
          sVals = (Collection) wi.val;
        else if(wi.val.getClass().isArray())
          for(int i = 0; i < Array.getLength(wi.val); i++)
            sVals.add(Array.get(wi.val, i));
        else if(wi.val instanceof String)
          sVals.add(wi.val.toString());

        if(sVals.isEmpty())
          throw new DataSetException("An SqlEnum.IN not have values.");

        // salva la collection ricavata per il successivo inserimento dei valori
        wi.val = sVals;

        sb.append("(").append(c.name()).append(" IN (");
        for(int i = 0; i < sVals.size(); i++)
        {
          if(i != 0)
            sb.append(",");
          sb.append("?");
        }
        sb.append("))");
      }
      else
      {
        sb.append("(").append(c.name()).append(" ").append(wi.criteria).append(" ?)");
      }

      count++;
    }

    for(String sfree : filtro.vFreeWhere)
    {
      if(count != 0)
        sb.append(" AND ");

      sb.append("(").append(sfree).append(")");
      count++;
    }

    String sSQL = buildQueryNoWhere(select, from, filtro);
    if(count != 0)
      sSQL += " WHERE " + sb.toString();

    if(filtro.vOrderby.isEmpty())
      return sSQL;

    count = 0;
    StringBuilder so = new StringBuilder(256);
    for(FiltroData.orderbyInfo obi : filtro.vOrderby)
    {
      if(count != 0)
        so.append(",");

      so.append(obi.nomecampo);
      if(StringOper.isOkStr(obi.dir))
        so.append(" ").append(obi.dir);

      count++;
    }

    return sSQL + " ORDER BY " + so.toString();
  }

  public static List<Record> fetchAllRecords(Connection conn, String select, String from, FiltroData filtro)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(conn, select, from, filtro))
    {
      return qs.fetchAllRecords();
    }
  }

  public static Pair<Schema, List<Record>> fetchAllRecordsAndSchema(Connection conn, String select, String from, FiltroData filtro)
     throws Exception
  {
    try (QueryDataSet qs = new QueryDataSet(conn, select, from, filtro))
    {
      return new Pair<>(qs.schema, qs.fetchAllRecords());
    }
  }
}
