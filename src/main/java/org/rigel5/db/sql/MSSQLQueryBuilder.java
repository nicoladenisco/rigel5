/*
 * Copyright (C) 2020 Nicola De Nisco
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.commons.logging.*;
import org.commonlib5.utils.*;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Query builder specializzato per Microsoft SQL server.
 *
 * @author Nicola De Nisco
 */
public class MSSQLQueryBuilder extends QueryBuilder
{
  /** Logging */
  private static final Log log = LogFactory.getLog(MSSQLQueryBuilder.class);

  public MSSQLQueryBuilder()
     throws Exception
  {
    super();
  }

  @Override
  public String adjCampo(int type, String campoOrig)
  {
    String campo = campoOrig.trim().toUpperCase();

    switch(type)
    {
      case RigelColumnDescriptor.PDT_BOOLEAN:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case RigelColumnDescriptor.PDT_INTEGER:
      case RigelColumnDescriptor.PDT_FLOAT:
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
      case RigelColumnDescriptor.PDT_NUMBERKEY:
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
      case RigelColumnDescriptor.PDT_TIME:
        return campo;
      case RigelColumnDescriptor.PDT_STRINGKEY:
      case RigelColumnDescriptor.PDT_STRING:
        return campo;
    }

    return campo;
  }

  @Override
  public String adjValue(int type, Object val)
  {
    switch(type)
    {
      case RigelColumnDescriptor.PDT_BOOLEAN:
        return ((Boolean) (val)) ? "t" : "f";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
      case RigelColumnDescriptor.PDT_TIME:
        return "'" + dfIso.format((java.util.Date) (val)) + "'";
      case RigelColumnDescriptor.PDT_INTEGER:
      case RigelColumnDescriptor.PDT_FLOAT:
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
      case RigelColumnDescriptor.PDT_NUMBERKEY:
        return val.toString();
      case RigelColumnDescriptor.PDT_STRINGKEY:
      case RigelColumnDescriptor.PDT_STRING:
        if(ignoreCase)
          return "'" + StringOper.CvtSQLstring(val.toString().toUpperCase().trim()) + "'";
        else
          return "'" + StringOper.CvtSQLstring(val.toString().trim()) + "'";
    }

    return "'" + StringOper.CvtSQLstring(val.toString().trim()) + "'";
  }

  @Override
  public String makeSQLstringNoFiltro(boolean useOrderby)
     throws Exception
  {
    return super.makeSQLstringNoFiltro(useOrderby).toUpperCase();
  }

  /**
   * Aggiunge offset e limit ad una query preesistente.
   * NOTA: questa implementazione funziona solo su SQL Server 2012 e successivi.
   *
   * @param sSQL stringa della query
   * @param offset prima riga da prelevare
   * @param limit numero di righe da prelevare
   * @return
   */
  @Override
  public String addNativeOffsetToQuery(String sSQL, long offset, long limit)
  {
    return sSQL
       + " OFFSET " + offset + " ROWS"
       + " FETCH NEXT " + limit + " ROWS ONLY;";
  }

  @Override
  public String limitQueryToOne(String sSQL)
  {
    return "SELECT TOP 1 " + sSQL.substring(7);
  }

  @Override
  public String getCountRecordsQuery(String genericQuery)
  {
    return "SELECT COUNT(*) FROM (" + genericQuery + ") FOO";
  }

  @Override
  public String queryForSequence(String sequence)
  {
    return "SELECT NEXT VALUE FOR " + sequence;
  }

  @Override
  public boolean disableForeignKeys(String nomeTabella)
  {
    log.debug("Disable foreign keys is not supported for Microsoft SQL.");
    return false;
  }

  @Override
  public boolean enableForeignKeys(String nomeTabella)
  {
    log.debug("Enable foreign keys is not supported for Microsoft SQL.");
    return false;
  }

  @Override
  public String getVista()
     throws Exception
  {
    return "(" + makeSQLstringNoFiltro(false) + ") foo";
  }

  @Override
  public String getTransactionID(Connection con)
     throws Exception
  {
    String sSQL = "SELECT CONVERT(VARCHAR, CURRENT_TRANSACTION_ID())";

    try(Statement st = con.createStatement())
    {
      try(ResultSet rs = st.executeQuery(sSQL))
      {
        if(rs.next())
          return rs.getString(1);
      }
    }

    return super.getTransactionID(con);
  }
}
