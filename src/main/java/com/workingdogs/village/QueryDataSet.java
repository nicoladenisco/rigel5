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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

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
 *    for ( int i = 0; i < qds.size(); i++ )
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

    selectString = new StringBuffer(selectStmt);

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
    schema = new Schema();
    Connection conn = resultSet.getStatement().getConnection();
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
    return this.selectString.toString();
  }
}
