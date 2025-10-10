/*
 * Copyright (C) 2024 Nicola De Nisco
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.rigel5.db.sql;

import com.workingdogs.village.Column;
import com.workingdogs.village.DataSetException;
import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import com.workingdogs.village.Schema;
import com.workingdogs.village.Value;
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

/**
 *
 * @author Nicola De Nisco
 */
public class QueryDataSetFiltroData extends QueryDataSet
{
  public QueryDataSetFiltroData()
     throws SQLException, DataSetException
  {
  }

  public QueryDataSetFiltroData(Connection conn, String selectStmt)
     throws SQLException, DataSetException
  {
    super(conn, selectStmt);
  }

  public QueryDataSetFiltroData(ResultSet resultSet)
     throws SQLException, DataSetException
  {
    super(resultSet);
  }

  public QueryDataSetFiltroData(Connection conn, String select, String from, FiltroData filtro)
     throws SQLException, DataSetException
  {
    String sql1 = buildQueryNoWhere(select, from, filtro) + " WHERE 1 = -1";
    Schema s = new Schema();

    try(Statement stm = conn.createStatement();
       ResultSet rs = stm.executeQuery(sql1))
    {
      s.populate(rs.getMetaData(), null, null, conn);
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

    schema.populate(resultSet.getMetaData(), null, null, conn);
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
    try(QueryDataSetFiltroData qs = new QueryDataSetFiltroData(conn, select, from, filtro))
    {
      return qs.fetchAllRecords();
    }
  }

  public static Pair<Schema, List<Record>> fetchAllRecordsAndSchema(Connection conn, String select, String from, FiltroData filtro)
     throws Exception
  {
    try(QueryDataSetFiltroData qs = new QueryDataSetFiltroData(conn, select, from, filtro))
    {
      return new Pair<>(qs.schema, qs.fetchAllRecords());
    }
  }
}
