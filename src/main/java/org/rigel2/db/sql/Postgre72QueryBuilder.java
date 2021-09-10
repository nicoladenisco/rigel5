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
package org.rigel2.db.sql;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.TorqueException;
import org.commonlib.utils.StringOper;
import org.rigel2.RigelI18nInterface;
import org.rigel2.db.DbUtils;
import org.rigel2.table.RigelColumnDescriptor;

/**
 * Query Builder specializzato per PostgreSQL.
 * Questa implementazione è adatta a versioni
 * del db minori o uguali 7.2.
 *
 * @author Nicola De Nisco
 */
public class Postgre72QueryBuilder extends QueryBuilder
{
  private static final Log log = LogFactory.getLog(Postgre72QueryBuilder.class);

  @Override
  public String adjCampo(int type, String campo)
  {
    String qryCampo = campo.trim().toUpperCase();

    switch(type)
    {
      default:
      case RigelColumnDescriptor.PDT_BOOLEAN:
      case RigelColumnDescriptor.PDT_INTEGER:
      case RigelColumnDescriptor.PDT_FLOAT:
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
      case RigelColumnDescriptor.PDT_NUMBERKEY:
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
        return qryCampo;
      case RigelColumnDescriptor.PDT_STRINGKEY:
      case RigelColumnDescriptor.PDT_STRING:
        if(ignoreCase)
          return "UPPER(" + qryCampo + ")";
        else
          return qryCampo;
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
        return "date_trunc('day', " + qryCampo + ")";
      case RigelColumnDescriptor.PDT_TIME:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
        return "to_char(" + qryCampo + ", 'HH24:MI:SS')";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
        return "date_trunc('second', " + qryCampo + ")";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
        return "date_trunc('minute', " + qryCampo + ")";
    }
  }

  @Override
  public String adjValue(int type, Object val)
  {
    switch(type)
    {
      case RigelColumnDescriptor.PDT_BOOLEAN:
        return ((Boolean) (val)) ? "t" : "f";
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
        return "'" + dfIso.format((java.util.Date) (val)) + "'";
      case RigelColumnDescriptor.PDT_TIME:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
        return "'" + hhIso.format((java.util.Date) (val)) + "'";
      case RigelColumnDescriptor.PDT_TIMESTAMP:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
        return "'" + dsIso.format((java.util.Date) (val)) + "'";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
        return "'" + dmIso.format((java.util.Date) (val)) + "'";
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
  public String adjLike(String campo, Object val)
  {
    String sVal = StringOper.CvtSQLstring(StringOper.okStr(val));

    if(ignoreCase)
      return campo + " ILIKE '%" + sVal + "%'";
    else
      return campo + " LIKE '%" + sVal + "%'";
  }

  @Override
  public String getCountRecordsQuery(String genericQuery)
  {
    return "SELECT COUNT(*) FROM (" + genericQuery + ") AS FOO";
  }

  @Override
  public String makeSQLstring(boolean useOrderby, boolean useLimit, boolean fetchRecord)
     throws Exception
  {
    if(!fetchRecord)
      useOrderby = useLimit = false;

    String sSQL = makeSQLstringNoFiltro(useOrderby);

    if(haveFilter())
    {
      // attiva subselect per il filtro
      sSQL = "SELECT * FROM (" + sSQL + ") AS FOO";

      if(filtro.haveWhere())
        sSQL += " WHERE " + makeFiltroWhere(filtro);

      if(useOrderby && filtro.haveOrderby())
        sSQL += " ORDER BY " + makeFiltroOrderby(filtro);
    }

    if(useLimit && haveLimit())
      sSQL = addNativeOffsetToQuery(sSQL, offset, limit);

    if(!fetchRecord)
      sSQL = limitQueryToOne(sSQL);

    if(macroResolver != null)
      sSQL = macroResolver.resolveMacro(sSQL);

    return sSQL;
  }

  @Override
  public String addNativeOffsetToQuery(String sSQL, long offset, long limit)
  {
    return sSQL + " LIMIT " + limit + "," + offset;
  }

  @Override
  public String limitQueryToOne(String sSQL)
  {
    return sSQL + " LIMIT 1";
  }

  @Override
  public String getVista()
     throws Exception
  {
    return "(" + makeSQLstringNoFiltro(false) + ") AS foo";
  }

  @Override
  public boolean disableForeignKeys(String nomeTabella)
  {
    String sSQL = "ALTER TABLE " + nomeTabella + " DISABLE TRIGGER ALL";
    try
    {
      DbUtils.executeStatement(sSQL);
      return true;
    }
    catch(TorqueException ex)
    {
      log.error(sSQL, ex);
      return false;
    }
  }

  @Override
  public boolean enableForeignKeys(String nomeTabella)
  {
    String sSQL = "ALTER TABLE " + nomeTabella + " ENABLE TRIGGER ALL";
    try
    {
      DbUtils.executeStatement(sSQL);
      return true;
    }
    catch(TorqueException ex)
    {
      log.error(sSQL, ex);
      return false;
    }
  }

  public static final String ERROR_STATE_DUPLICATE_KEY = "23505";
  public static final String ERROR_STATE_FOREIGN_KEY = "23503";

  public static final Pattern PAT_SQL_ERR_PARAMS = Pattern.compile(" \\((.+)\\)=\\((.+)\\)", Pattern.CASE_INSENSITIVE);

  @Override
  public String formatNonFatalError(SQLException ex, RigelI18nInterface i18n)
     throws SQLException
  {
    String state = ex.getSQLState();
    String message = ex.getMessage();

    if(state == null || message == null)
      throw ex;

    // violazione di chiave (indice)
    // ERROR: duplicate key value violates unique constraint "db_prestazioni_idx_1" Dettaglio: Key (codice)=(1) already exists.
    if(state.equals(ERROR_STATE_DUPLICATE_KEY))
    {
      Matcher m = PAT_SQL_ERR_PARAMS.matcher(message);
      if(m.find() && m.groupCount() == 2)
      {
        String campo = m.group(1);
        String valore = m.group(2);
        return i18n.msg("Il valore del campo %s non può essere duplicato: %s è già in uso.", campo, valore);
      }
    }

    // violazione di chiave esterna
    // ERROR: insert or update on table "ac_accettazioni" violates foreign key constraint "ac_accettazioni_fk_15" Dettaglio: Key (idasl)=(999999) is not present in table "cod_asl".
    if(state.equals(ERROR_STATE_FOREIGN_KEY))
    {
      Matcher m = PAT_SQL_ERR_PARAMS.matcher(message);
      if(m.find() && m.groupCount() == 2)
      {
        String campo = m.group(1);
        String valore = m.group(2);
        return i18n.msg("Il valore del campo %s è sottoposto a vincoli: %s non è un valore valido.", campo, valore);
      }
    }

    throw ex;
  }
}
