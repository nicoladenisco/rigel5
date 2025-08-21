/*
 * Copyright (C) 2025 Nicola De Nisco
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
package org.rigel5.db;

import com.workingdogs.village.Record;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.sql.Query;
import org.apache.torque.sql.SqlBuilder;
import org.apache.torque.util.ExceptionMapper;
import org.apache.torque.util.TorqueConnection;
import org.apache.torque.util.Transaction;
import org.rigel5.SetupHolder;
import static org.rigel5.db.DbUtils.executeQuery;
import static org.rigel5.db.DbUtils.setPreparedStatementReplacements;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Utility per il conteggio dei records su una tabella.
 *
 * @author Nicola De Nisco
 */
public class RecordCounter
{
  private Connection con;
  private QueryBuilder qb;
  private boolean usaStatoRec;
  public static final Pattern pWhere = Pattern.compile("\\s+WHERE\\s+", Pattern.CASE_INSENSITIVE);

  public RecordCounter()
  {
    try
    {
      this.qb = SetupHolder.getQueryBuilder();
    }
    catch(Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public RecordCounter(Connection con)
  {
    try
    {
      this.con = con;
      this.qb = SetupHolder.getQueryBuilder();
    }
    catch(Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public RecordCounter(Connection con, boolean usaStatoRec)
  {
    try
    {
      this.con = con;
      this.qb = SetupHolder.getQueryBuilder();
      this.usaStatoRec = usaStatoRec;
    }
    catch(Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public boolean isUsaStatoRec()
  {
    return usaStatoRec;
  }

  public void setUsaStatoRec(boolean usaStatoRec)
  {
    this.usaStatoRec = usaStatoRec;
  }

  /**
   * Calcola il numero di record totali di una tabella/vista.
   * Vengono esclusi quelli cancellati logicamente.
   * @param tableName nome della tabella
   * @return numero di record in tabella
   * @throws java.lang.Exception
   */
  public long getRecordCount(String tableName)
     throws Exception
  {
    return getRecordCount(tableName, null);
  }

  /**
   * Calcola il numero di record totali di una tabella/vista.
   * Vengono esclusi quelli cancellati logicamente.
   * @param tableName nome della tabella
   * @param extraWhere eventuale clausola where aggiuntiva (puo' essere null)
   * @return numero di record in tabella
   * @throws java.lang.Exception
   */
  public long getRecordCount(String tableName, String extraWhere)
     throws Exception
  {
    qb.setSelect("COUNT(*)");
    qb.setFrom(tableName);
    String where = "";

    if(usaStatoRec)
    {
      String campo = qb.adjCampo(RigelColumnDescriptor.PDT_INTEGER, "stato_rec");
      where = "(" + campo + " IS NULL) OR (" + campo + " < 10)";
      if(extraWhere != null)
        where += " AND (" + extraWhere + ")";
    }
    else
    {
      if(extraWhere != null)
        if(pWhere.matcher(tableName).find())
          where += " AND (" + extraWhere + ")";
        else
          where += " WHERE (" + extraWhere + ")";
    }

    qb.setWhere(where);
    qb.setLimit(1);
    String sSQL = qb.makeSQLstring();

    List<Record> records = con == null ? executeQuery(sSQL) : executeQuery(sSQL, con);
    if(records.isEmpty())
      return 0;

    Record rec = records.get(0);
    return rec.getValue(1).asLong();
  }

  /**
   * Calcola il numero di record totali di un Criteria.
   * Vengono conteggiati tutti i record selezionabili dal criteria
   * specificato.
   *
   * @param c criteria da conteggiare
   * @return numero di record
   * @throws java.lang.Exception
   */
  public long getRecordCount(Criteria c)
     throws Exception
  {
    if(con == null)
    {
      try(TorqueConnection connection = Transaction.begin())
      {
        con = connection;
        long rv = getRecordCountInternal(c);
        Transaction.commit(connection);
        return rv;
      }
      finally
      {
        con = null;
      }
    }
    else
    {
      return getRecordCountInternal(c);
    }
  }

  public long getRecordCountInternal(Criteria c)
     throws Exception
  {
    Query query = SqlBuilder.buildQuery(c);
    if(query.getFromClause().isEmpty())
      throw new TorqueException("Missing from clause.");

    try
    {
      String sSQL = query.toString();
      int idx = sSQL.indexOf(" FROM ");
      if(idx == -1)
        throw new TorqueException("Invalid syntax in query.");

      String sSQL1 = "SELECT * " + sSQL.substring(idx);
      String sSQL2 = qb.getCountRecordsQuery(sSQL1);

      try(PreparedStatement statement = con.prepareStatement(sSQL2))
      {
        if(query.getFetchSize() != null)
          statement.setFetchSize(query.getFetchSize());

        setPreparedStatementReplacements(
           statement,
           query.getPreparedStatementReplacements(),
           0);

        try(ResultSet resultSet = statement.executeQuery())
        {
          if(resultSet.next())
            return resultSet.getLong(1);
        }

        return 0;
      }
    }
    catch(SQLException e)
    {
      throw ExceptionMapper.getInstance().toTorqueException(e);
    }
  }
}
