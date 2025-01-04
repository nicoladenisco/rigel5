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

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.SqlEnum;
import org.commonlib5.utils.StringOper;
import org.rigel5.db.DbUtils;
import static org.rigel5.db.DbUtils.TABLES_FILTER;
import static org.rigel5.db.DbUtils.VIEWS_FILTER;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Query Builder specializzato per Oracle.
 *
 * @author Nicola De Nisco
 */
public class OracleQueryBuilder extends QueryBuilder
{
  private static final Log log = LogFactory.getLog(OracleQueryBuilder.class);
  private static final ArrayList<String> usedSchemas = new ArrayList<>();
  private static final ArrayList<String> publicSchemas = new ArrayList<>();
  private static final ArrayList<String> normalizedSchemas = new ArrayList<>();
  private static final ArrayList<String> normalizedPublicSchemas = new ArrayList<>();

  public static String[] getUsedSchemas()
  {
    return usedSchemas.toArray(new String[usedSchemas.size()]);
  }

  public static void addUsedSchema(String usedSchema)
  {
    usedSchemas.add(usedSchema);
  }

  public static String[] getPublicSchemas()
  {
    return publicSchemas.toArray(new String[publicSchemas.size()]);
  }

  public static void addPublicSchema(String publicSchema)
  {
    publicSchemas.add(publicSchema);
  }

  @Override
  public String getCountRecordsQuery(String genericQuery)
  {
    return "SELECT COUNT(*) FROM (" + genericQuery + ") FOO";
  }

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
        return "trunc(" + qryCampo + ", 'DD')";
      case RigelColumnDescriptor.PDT_TIME:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
        return "trunc(" + qryCampo + ", 'HH')";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
        return "trunc(" + qryCampo + ", 'MI')";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
        return "trunc(" + qryCampo + ", 'MI')";
    }
  }

  // TO_DATE('2015/05/15 8:30:25', 'YYYY/MM/DD HH:MI:SS')
  @Override
  public String adjValue(int type, Object val)
  {
    switch(type)
    {
      case RigelColumnDescriptor.PDT_BOOLEAN:
        return ((Boolean) (val)) ? "t" : "f";
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
        return "TO_DATE('" + dfIso.format((java.util.Date) (val)) + "', 'YYYY-MM-DD')";
      case RigelColumnDescriptor.PDT_TIME:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
        return "TRUNC(TO_DATE('" + hhIso.format((java.util.Date) (val)) + "', 'HH24:MI:SS'), 'HH')";
      case RigelColumnDescriptor.PDT_TIMESTAMP:
        return "TO_DATE('" + dsIso.format((java.util.Date) (val)) + "', 'YYYY-MM-DD HH24:MI:SS')";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
        return "TO_DATE('" + dsIso.format((java.util.Date) (val)) + "', 'YYYY-MM-DD HH24:MI:SS')";
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
        return "TO_DATE('" + dmIso.format((java.util.Date) (val)) + "', 'YYYY-MM-DD HH24:MI')";
      case RigelColumnDescriptor.PDT_INTEGER:
      case RigelColumnDescriptor.PDT_FLOAT:
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
      case RigelColumnDescriptor.PDT_NUMBERKEY:
        return val.toString();
      case RigelColumnDescriptor.PDT_STRINGKEY:
      case RigelColumnDescriptor.PDT_STRING:
        String value = StringOper.CvtSQLstring(val.toString().trim());
        if(ignoreCase)
          return "'" + value.toUpperCase() + "'";
        else
          return "'" + value + "'";
    }

    return "'" + StringOper.CvtSQLstring(val.toString().trim()) + "'";
  }

  @Override
  public synchronized String makeSQLstringNoFiltro(boolean useOrderby)
     throws Exception
  {
    String sSQL = super.makeSQLstringNoFiltro(useOrderby);
    return sSQL.replaceAll("\\b+AS\\b+", " ").replaceAll("\\b+as\\b+", " ");
  }

  @Override
  public String getVista()
     throws Exception
  {
    return "(" + makeSQLstringNoFiltro(false) + ") foo";
  }

  @Override
  public String adjLike(String campo, Object val)
  {
    String value = StringOper.CvtSQLstring(val.toString().trim());

    if(ignoreCase)
      return "UPPER(" + campo + ") LIKE '%" + value.toUpperCase() + "%'";
    else
      return campo + " LIKE '%" + value + "%'";
  }

  @Override
  public String addNativeOffsetToQuery(String sSQL, long offset, long limit)
  {
    long r1 = offset, r2 = offset + limit;

    return "select * from\n"
       + " (select addNativeOffsetToQuery.*, rownum rnum\n"
       + "    from\n"
       + " ( " + sSQL + " ) addNativeOffsetToQuery\n"
       + "  where rownum <= " + r2
       + " )\n"
       + "  where rnum >= " + r1;
  }

  @Override
  public String limitQueryToOne(String sSQL)
  {
    return "SELECT * FROM (" + sSQL + ") WHERE ROWNUM <= 1";
  }

  @Override
  public boolean disableForeignKeys(String nomeTabella)
  {
    String sSQL = "ALTER TABLE " + nomeTabella + " DISABLE ALL TRIGGERS";
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
    String sSQL = "ALTER TABLE " + nomeTabella + " ENABLE ALL TRIGGERS";
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

  /**
   * Funzione generica di scansione colonne.
   * La ricerca del nome tabella è case insensitive.
   * @param <T> il tipo tornato da sfun
   * @param con connessione al db
   * @param nomeTabella nome della tabella (eventualmente con schema)
   * @param nomeColonna nome della colonna
   * @param sfun funzione lambda per la scansione dei campi della tabella individuata
   * @return int {@code =>} SQL type from java.sql.Types 0=non trovato
   * @throws Exception
   */
  @Override
  public <T> T scanTabelleColonne(Connection con, String nomeTabella, String nomeColonna, ScanColumn<T> sfun)
     throws Exception
  {
    normalizeSchema(con);
    String nomeSchema = null;
    int pos = nomeTabella.indexOf('.');
    if(pos != -1)
    {
      nomeSchema = nomeTabella.substring(0, pos);
      nomeTabella = nomeTabella.substring(pos + 1);
    }

    for(String schema : normalizedSchemas)
    {
      boolean spub = !isSchemaPublic(schema) && StringOper.isOkStr(nomeSchema);

      try(ResultSet rSet = con.getMetaData().getTables(null, schema, null, TABLES_FILTER))
      {
        while(rSet.next())
        {
          if(rSet.getString("TABLE_TYPE").equals("TABLE"))
          {
            String tableName = rSet.getString("TABLE_NAME");

            if(spub)
            {
              if(StringOper.isEquNocase(nomeSchema, schema) && StringOper.isEquNocase(nomeTabella, tableName))
                return sfun.scan(con, schema, tableName, nomeColonna);
            }
            else
            {
              if(StringOper.isEquNocase(nomeTabella, tableName))
                return sfun.scan(con, schema, tableName, nomeColonna);
            }
          }
        }
      }
    }

    return null;
  }

  /**
   * Ritorna vero se lo schema è lo schema di default.
   * Gli schemi che vengono considerati default sono quelli aggiunti con addPublicSchema.
   * @param nomeSchema nome da testare
   * @return vero se è lo schema di default del db
   */
  @Override
  public boolean isSchemaPublic(String nomeSchema)
  {
    return nomeSchema == null || normalizedPublicSchemas.contains(nomeSchema);
  }

  /**
   * Lista delle viste di un database.
   * @param con connessione al db
   * @return lista di tutte le viste presenti (schema di default)
   * @throws Exception
   */
  @Override
  public List<String> getAllViews(Connection con)
     throws Exception
  {
    normalizeSchema(con);
    DatabaseMetaData databaseMetaData = con.getMetaData();
    ArrayList<String> viewNames = new ArrayList<>();

    for(String schema : normalizedSchemas)
    {
      boolean spub = isSchemaPublic(schema);

      try(ResultSet rSet = databaseMetaData.getTables(null, schema, null, VIEWS_FILTER))
      {
        while(rSet.next())
        {
          if(rSet.getString("TABLE_TYPE").equals("VIEW"))
          {
            String tableName = rSet.getString("TABLE_NAME");
            if(!spub)
              viewNames.add(schema + "." + tableName);
            else
              viewNames.add(tableName);
          }
        }
      }
    }

    return viewNames;
  }

  /**
   * Lista delle tabelle di un database.
   * @param con connessione al db
   * @return lista di tutte le tabelle presenti (schema di default)
   * @throws Exception
   */
  @Override
  public List<String> getAllTables(Connection con)
     throws Exception
  {
    normalizeSchema(con);
    DatabaseMetaData databaseMetaData = con.getMetaData();
    ArrayList<String> tableNames = new ArrayList<>();

    for(String schema : normalizedSchemas)
    {
      boolean spub = isSchemaPublic(schema);

      try(ResultSet rSet = databaseMetaData.getTables(null, schema, null, TABLES_FILTER))
      {
        while(rSet.next())
        {
          if(rSet.getString("TABLE_TYPE").equals("TABLE"))
          {
            String tableName = rSet.getString("TABLE_NAME");
            if(!spub)
              tableNames.add(schema + "." + tableName);
            else
              tableNames.add(tableName);
          }
        }
      }
    }

    return tableNames;
  }

  /**
   * Carica in normalizedSchemas gli schema con il case corretto come da database.
   * @param con connessione al db
   * @throws Exception
   */
  protected void normalizeSchema(Connection con)
     throws Exception
  {
    if(usedSchemas.size() == normalizedSchemas.size() && publicSchemas.size() == normalizedPublicSchemas.size())
      return;

    normalizedSchemas.clear();
    try(ResultSet rs = con.getMetaData().getSchemas())
    {
      while(rs.next())
      {
        String schema = rs.getString("TABLE_SCHEM");

        usedSchemas.stream()
           .filter((s) -> s.equalsIgnoreCase(schema))
           .forEach((s) -> normalizedSchemas.add(schema));

        publicSchemas.stream()
           .filter((s) -> s.equalsIgnoreCase(schema))
           .forEach((s) -> normalizedPublicSchemas.add(schema));
      }
    }
  }

  @Override
  public String getTransactionID(Connection con)
     throws Exception
  {
    String sSQL
       = "SELECT RAWTOHEX(tx.xid)\n"
       + "FROM v$transaction tx\n"
       + "JOIN v$session s ON tx.ses_addr = s.saddr";

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

  @Override
  public synchronized String makeFiltroWhere(FiltroData fd)
  {
    StringBuilder whre = new StringBuilder();

    // nota MINUS e MINUS_ALL sono in realta usati per le regular expression
    for(FiltroData.whereInfo wi : fd.vWhere)
    {
      if(SqlEnum.ISNULL.equals(wi.criteria))
        whre.append(" AND ").append(wi.nomecampo).append(" IS NULL");
      else if(SqlEnum.ISNOTNULL.equals(wi.criteria))
        whre.append(" AND ").append(wi.nomecampo).append(" IS NOT NULL");
      else if(SqlEnum.MINUS.equals(wi.criteria))
        whre.append(" AND (regexp_like(").append(wi.nomecampo).append(", '").append(simpleVal(wi)).append("', 'c'))");
      else if(SqlEnum.MINUS_ALL.equals(wi.criteria))
        whre.append(" AND (regexp_like(").append(wi.nomecampo).append(", '").append(simpleVal(wi)).append("', 'i'))");
      else if(SqlEnum.IN.equals(wi.criteria))
      {
        ArrayList<String> sVals = new ArrayList<>();

        if(wi.val instanceof Collection)
          for(Object oVal : (Collection) wi.val)
            sVals.add(adjValue(wi.type, oVal));
        else if(wi.val.getClass().isArray())
          for(int i = 0; i < Array.getLength(wi.val); i++)
            sVals.add(adjValue(wi.type, Array.get(wi.val, i)));
        else if(wi.val instanceof String)
          sVals.add(adjValue(wi.type, wi.val.toString()));

        if(!sVals.isEmpty())
          whre.append(" AND (").append(adjCampo(wi.type, wi.nomecampo))
             .append(" IN (").append(StringOper.join(sVals.iterator(), ',')).append("))");
      }
      else if(wi.val != null)
        whre.append(" AND (").append(adjCampo(wi.type, wi.nomecampo)).append(" ").append(wi.criteria)
           .append(" ").append(adjValue(wi.type, wi.val)).append(")");
    }

    for(FiltroData.betweenInfo bi : fd.vBetween)
    {
      String nomeCampo = adjCampo(bi.type, bi.nomecampo);
      String valMin = adjValue(bi.type, bi.val1);
      String valMax = adjValue(bi.type, bi.val2);

      whre.append(" AND ((").append(nomeCampo).append(" >= ").append(valMin)
         .append(") AND (").append(nomeCampo).append(" <= ").append(valMax).append("))");
    }

    for(String stm : fd.vFreeWhere)
    {
      whre.append(" AND (").append(stm).append(")");
    }

    return whre.length() == 0 ? null : whre.substring(5);
  }
}
