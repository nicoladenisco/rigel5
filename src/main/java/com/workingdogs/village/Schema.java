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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Schema object represents the <a href="Column.html">Columns</a> in a database table. It contains a collection of <a
 * href="Column.html">Column</a> objects.
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author John D. McNally
 * @version $Revision: 568 $
 */
public final class Schema
{
  /** TODO: DOCUMENT ME! */
  private String tableName;

  /** TODO: DOCUMENT ME! */
  private String columnsAttribute;

  /** TODO: DOCUMENT ME! */
  private int numberOfColumns;

  /** TODO: DOCUMENT ME! */
  private Column[] columns;
  private Map columnNumberByName;

  /** TODO: DOCUMENT ME! */
  private static final Hashtable schemaCache = new Hashtable();

  /**
   * This attribute is used to complement columns in the event that this schema represents more than one table. Its keys
   * are
   * String contains table names and its elements are Hashtables containing columns.
   */
  private Hashtable tableHash = null;

  /** TODO: DOCUMENT ME! */
  private boolean singleTable = true;

  /**
   * A blank Schema object
   */
  public Schema()
  {
    this.tableName = "";
    this.columnsAttribute = null;
    this.numberOfColumns = 0;
  }

  /**
   * Initialize all table schemas reachable from this connection
   *
   * @param conn a database connection
   * @throws SQLException if retrieving the database meta data is unsuccessful
   */
  public static void initSchemas(Connection conn)
     throws SQLException
  {
    ResultSet allCol = null;

    try
    {
      DatabaseMetaData databaseMetaData = conn.getMetaData();
      String connURL = databaseMetaData.getURL();
      allCol = databaseMetaData.getColumns(
         conn.getCatalog(), null, null, null);

      while(true)
      {
        Schema schema = new Schema();

        schema.setAttributes("*");
        schema.singleTable = true;
        schema.populate(allCol);

        if(schema.numberOfColumns > 0)
        {
          String keyValue = connURL + schema.tableName;

          synchronized(schemaCache)
          {
            schemaCache.put(keyValue, schema);
          }
        }
        else
        {
          break;
        }
      }
    }
    finally
    {
      if(allCol != null)
      {
        try
        {
          allCol.close();
        }
        catch(SQLException e)
        {
          //Do nothing
        }
      }
    }
  }

  /**
   * Creates a Schema with all columns
   *
   * @param conn
   * @param tableName
   *
   * @return an instance of myself
   *
   * @exception SQLException
   * @exception DataSetException
   */
  public Schema schema(Connection conn, String tableName)
     throws SQLException, DataSetException
  {
    return schema(conn, tableName, "*");
  }

  /**
   * Creates a Schema with the named columns in the columnsAttribute
   *
   * @param conn
   * @param tableName
   * @param columnsAttribute
   *
   * @return an instance of myself
   *
   * @exception SQLException
   * @exception DataSetException
   */
  public Schema schema(Connection conn, String tableName, String columnsAttribute)
     throws SQLException, DataSetException
  {
    if(columnsAttribute == null)
    {
      columnsAttribute = "*";
    }

    PreparedStatement stmt = null;

    try
    {
      Schema tableSchema = null;
      String keyValue = conn.getMetaData().getURL() + tableName;

      synchronized(schemaCache)
      {
        tableSchema = (Schema) schemaCache.get(keyValue);

        if(tableSchema == null)
        {
          String sql = "SELECT " + columnsAttribute + " FROM " + tableName + " WHERE 1 = -1";

          stmt = conn.prepareStatement(sql);

          if(stmt != null)
          {
            stmt.executeQuery();
            tableSchema = this;
            tableSchema.setTableName(tableName);
            tableSchema.setAttributes(columnsAttribute);
            tableSchema.populate(stmt.getMetaData(), tableName, null);
            schemaCache.put(keyValue, tableSchema);
          }
          else
          {
            throw new DataSetException("Couldn't retrieve schema for " + tableName);
          }
        }
      }

      return tableSchema;
    }
    finally
    {
      if(stmt != null)
      {
        try
        {
          stmt.close();
        }
        catch(SQLException e)
        {
          //Do nothing
        }
      }
    }
  }

  /**
   * Appends data to the tableName that this schema was first created with.
   *
   * <P>
   * </p>
   *
   * @param app String to append to tableName
   *
   * @see TableDataSet#tableQualifier(java.lang.String)
   */
  public void appendTableName(String app)
  {
    this.tableName = this.tableName + " " + app;
  }

  /**
   * List of columns to select from the table
   *
   * @return the list of columns to select from the table
   */
  public String attributes()
  {
    return this.columnsAttribute;
  }

  /**
   * Returns the requested Column object at index i
   *
   * @param i
   *
   * @return the requested column
   *
   * @exception DataSetException
   */
  public Column column(int i)
     throws DataSetException
  {
    if(i == 0)
    {
      throw new DataSetException("Columns are 1 based");
    }
    else if(i > numberOfColumns)
    {
      throw new DataSetException("There are only " + numberOfColumns() + " available!");
    }

    try
    {
      return columns[i];
    }
    catch(Exception e)
    {
      throw new DataSetException("Column number: " + numberOfColumns() + " does not exist!");
    }
  }

  /**
   * Returns the requested Column object by name
   *
   * @param colName
   *
   * @return the requested column
   *
   * @exception DataSetException
   */
  public Column column(String colName)
     throws DataSetException
  {
    return column(index(colName));
  }

  /**
   * Returns the requested Column object by name
   *
   * @param colName
   *
   * @return the requested column
   *
   * @exception DataSetException
   */
  public Column getColumn(String colName)
     throws DataSetException
  {
    int dot = colName.indexOf('.');

    if(dot > 0)
    {
      String table = colName.substring(0, dot);
      String col = colName.substring(dot + 1);

      return getColumn(table, col);
    }

    return column(index(colName));
  }

  /**
   * Returns the requested Column object belonging to the specified table by name
   *
   * @param tableName
   * @param colName
   *
   * @return the requested column, null if a column by the specified name does not exist.
   *
   * @exception DataSetException
   */
  public Column getColumn(String tableName, String colName)
     throws DataSetException
  {
    return (Column) ((Hashtable) tableHash.get(tableName)).get(colName);
  }

  /**
   * Returns an array of columns
   *
   * @return an array of columns
   */
  public Column[] getColumns()
  {
    return this.columns;
  }

  /**
   * returns the table name that this Schema represents
   *
   * @return the table name that this Schema represents
   *
   * @throws DataSetException TODO: DOCUMENT ME!
   */
  public String getTableName()
     throws DataSetException
  {
    if(singleTable)
    {
      return tableName;
    }
    else
    {
      throw new DataSetException("This schema represents several tables.");
    }
  }

  /**
   * returns all table names that this Schema represents
   *
   * @return the table names that this Schema represents
   */
  public String[] getAllTableNames()
  {
    Enumeration e = tableHash.keys();
    String[] tableNames = new String[tableHash.size()];

    for(int i = 0; e.hasMoreElements(); i++)
    {
      tableNames[i] = (String) e.nextElement();
    }

    return tableNames;
  }

  /**
   * Gets the index position of a named column. If multiple tables are represented and they have columns with the same
   * name,
   * this method returns the first one listed, if the table name is not specified.
   *
   * @param colName
   *
   * @return the requested column index integer
   *
   * @exception DataSetException
   */
  public int index(String colName)
     throws DataSetException
  {
    Integer position = (Integer) columnNumberByName.get(colName);

    if(position != null)
    {
      return position;
    }
    else
    {
      throw new DataSetException("Column name: " + colName + " does not exist!");
    }
  }

  /**
   * Gets the index position of a named column.
   *
   * @param tableName
   * @param colName
   *
   * @return the requested column index integer
   *
   * @exception DataSetException
   */
  public int index(String tableName, String colName)
     throws DataSetException
  {
    return index(tableName + "." + colName);
  }

  /**
   * Checks to see if this DataSet represents one table in the database.
   *
   * @return true if only one table is represented, false otherwise.
   */
  public boolean isSingleTable()
  {
    return singleTable;
  }

  /**
   * Gets the number of columns in this Schema
   *
   * @return integer number of columns
   */
  public int numberOfColumns()
  {
    return this.numberOfColumns;
  }

  /**
   * Internal method which populates this Schema object with Columns.
   *
   * @param meta The meta data of the ResultSet used to build this Schema.
   * @param tableName The name of the table referenced in this schema, or null if unknown or multiple tables are
   * involved.
   * @param conn The connection whose URL serves as a cache key prefix
   *
   * @exception SQLException
   * @exception DataSetException
   */
  public void populate(ResultSetMetaData meta, String tableName, Connection conn)
     throws SQLException, DataSetException
  {
    this.numberOfColumns = meta.getColumnCount();
    columns = new Column[numberOfColumns() + 1];
    columnNumberByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    String connURL = (conn != null) ? conn.getMetaData().getURL() : null;

    for(int i = 1; i <= numberOfColumns(); i++)
    {
      String metaColumnName = meta.getColumnName(i);
      String metaTableName = null;

      // Workaround for Sybase jConnect 5.2 and older.
      try
      {
        metaTableName = meta.getTableName(i);

        // ResultSetMetaData may report table name as the empty
        // string when a database-specific function has been
        // called to generate a Column.
        if((metaTableName == null) || metaTableName.equals(""))
        {
          if(tableName != null)
          {
            metaTableName = tableName;
          }
          else
          {
            metaTableName = "";
          }
        }
      }
      catch(RuntimeException e)
      {
        if(tableName != null)
        {
          metaTableName = tableName;
        }
        else
        {
          metaTableName = "";
        }
      }

      Column col = null;

      if(metaTableName.length() > 0 && connURL != null)
      {
        Schema tableSchema = null; // schema(conn, metaTableName);

        synchronized(schemaCache)
        {
          tableSchema = (Schema) schemaCache.get(connURL + metaTableName);
        }

        if(tableSchema != null)
        {
          try
          {
            col = tableSchema.column(metaColumnName);
          }
          catch(DataSetException e)
          {
            // column does not exist, ignore
          }
        }
      }

      // Not found in cache
      if(col == null)
      {
        col = new Column();
        col.populate(meta, i, metaTableName, metaColumnName);
      }

      columns[i] = col;
      columnNumberByName.put(metaColumnName, i);
      columnNumberByName.put(metaTableName + "." + metaColumnName, i);

      if((i > 1) && !col.getTableName().equalsIgnoreCase(columns[i - 1].getTableName()))
      {
        singleTable = false;
      }
    }

    // Avoid creating a Hashtable in the most common case where only one
    // table is involved, even though this makes the multiple table case
    // more expensive because the table/column info is duplicated.
    if(singleTable)
    {
      // If available, use a the caller supplied table name.
      if((tableName != null) && (tableName.length() > 0))
      {
        setTableName(tableName);
      }
      else
      {
        // Since there's only one table involved, attempt to set the
        // table name to that of the first column.  Sybase jConnect
        // 5.2 and older will fail, in which case we are screwed.
        try
        {
          setTableName(columns[1].getTableName());
        }
        catch(Exception e)
        {
          setTableName("");
        }
      }
    }
    else
    {
      tableHash = new Hashtable((int) ((1.25 * numberOfColumns) + 1));

      for(int i = 1; i <= numberOfColumns(); i++)
      {
        Hashtable columnHash;

        if(tableHash.containsKey(columns[i].getTableName()))
        {
          columnHash = (Hashtable) tableHash.get(columns[i].getTableName());
        }
        else
        {
          columnHash = new Hashtable((int) ((1.25 * numberOfColumns) + 1));
          tableHash.put(columns[i].getTableName(), columnHash);
        }

        columnHash.put(columns[i].name(), columns[i]);
      }
    }
  }

  /**
   * Internal method which populates this Schema object with Columns.
   *
   * @param meta The meta data of the database connection used to build this Schema.
   *
   * @exception SQLException
   */
  public void populate(ResultSet dbMeta)
     throws SQLException
  {
    List cols = new ArrayList();
    String tableName = null;
    columnNumberByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    while(dbMeta.next())
    {
      if(tableName == null)
      {
        tableName = dbMeta.getString(3); // table name
        setTableName(tableName);
      }
      else if(!tableName.equals(dbMeta.getString(3))) // not same table name
      {
        dbMeta.previous(); // reset result set pointer
        break;
      }

      Column c = new Column();

      c.populate(tableName,
         dbMeta.getString(4), // column name
         dbMeta.getString(6), // Data source dependent type name
         dbMeta.getInt(5), // SQL type from java.sql.Types
         dbMeta.getInt(11) == DatabaseMetaData.columnNullable); // is NULL allowed.

      cols.add(c);

      int position = dbMeta.getInt(17); // ordinal number
      columnNumberByName.put(c.name(), position);
      columnNumberByName.put(tableName + "." + c.name(), position);
    }

    if(!cols.isEmpty())
    {
      this.numberOfColumns = cols.size();
      columns = new Column[numberOfColumns() + 1];

      int i = 1;
      for(Iterator col = cols.iterator(); col.hasNext();)
      {
        columns[i++] = (Column) col.next();
      }
    }
  }

  /**
   * Sets the columns to select from the table
   *
   * @param attributes comma separated list of column names
   */
  public void setAttributes(String attributes)
  {
    this.columnsAttribute = attributes;
  }

  /**
   * Sets the table name that this Schema represents
   *
   * @param tableName
   */
  public void setTableName(String tableName)
  {
    this.tableName = tableName;
  }

  /**
   * returns the table name that this Schema represents
   *
   * @return the table name that this Schema represents
   *
   * @throws DataSetException TODO: DOCUMENT ME!
   */
  public String tableName()
     throws DataSetException
  {
    return getTableName();
  }

  /**
   * This returns a representation of this Schema
   *
   * @return a string
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(512);
    sb.append('{');

    for(int i = 1; i <= numberOfColumns; i++)
    {
      sb.append('\'');

      if(!singleTable)
      {
        sb.append(columns[i].getTableName()).append('.');
      }

      sb.append(columns[i].name()).append('\'');

      if(i < numberOfColumns)
      {
        sb.append(',');
      }
    }

    sb.append('}');
    return sb.toString();
  }
}
