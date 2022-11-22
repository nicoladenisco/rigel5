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
package org.rigel5.db;

import com.workingdogs.village.*;
import com.workingdogs.village.Column;
import java.io.Reader;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.*;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.map.*;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.Persistent;
import org.apache.torque.om.SimpleKey;
import org.apache.torque.sql.Query;
import org.apache.torque.sql.SqlBuilder;
import org.apache.torque.util.BasePeer;
import org.apache.torque.util.ColumnValues;
import org.apache.torque.util.ExceptionMapper;
import org.apache.torque.util.JdbcTypedValue;
import org.apache.torque.util.Transaction;
import org.commonlib5.lambda.LEU;
import org.commonlib5.utils.ArrayMap;
import org.commonlib5.utils.Pair;
import org.commonlib5.utils.StringOper;
import org.rigel5.SetupHolder;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.db.torque.PeerTransactAgent;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Funzioni di utilita' per accesso al database.
 *
 * @author Nicola De Nisco
 */
public class DbUtils
{
  private static final Log log = LogFactory.getLog(DbUtils.class);
  //
  public static final String[] TABLES_FILTER = new String[]
  {
    "TABLE"
  };
  public static final String[] VIEWS_FILTER = new String[]
  {
    "VIEW"
  };
  public static final String[] TABLES_VIEWS_FILTER = new String[]
  {
    "TABLE",
    "VIEW"
  };

  public static final String NESSUNO_INDEFINITO = "'Nessuno/indefinito'";

  private static QueryBuilder __qbStaticForDbUtils = null;

  private static QueryBuilder getQueryBuilder()
     throws Exception
  {
    if(__qbStaticForDbUtils == null)
      __qbStaticForDbUtils = SetupHolder.getQueryBuilder();

    return __qbStaticForDbUtils;
  }

  /**
   * Calcola il numero di record totali di una tabella/vista.
   * Vengono esclusi quelli cancellati logicamente.
   * @param tableName nome della tabella
   * @return numero di record in tabella
   * @throws java.lang.Exception
   */
  public static long getRecordCount(String tableName)
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
  public static long getRecordCount(String tableName, String extraWhere)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    qb.setSelect("COUNT(*)");
    qb.setFrom(tableName);
    String campo = qb.adjCampo(RigelColumnDescriptor.PDT_INTEGER, "stato_rec");
    String where = "((" + campo + " IS NULL) OR (" + campo + " < 10))";
    if(extraWhere != null)
      where += " AND (" + extraWhere + ")";
    qb.setWhere(where);
    qb.setLimit(1);
    String sSQL = qb.makeSQLstring();
    List<Record> records = executeQuery(sSQL);
    if(records.isEmpty())
      return 0;

    Record rec = (Record) (records.get(0));
    return rec.getValue(1).asLong();
  }

  /**
   * Calcola il numero di record totali di una tabella/vista.
   * Vengono esclusi quelli cancellati logicamente.
   * @param tableName nome della tabella
   * @param extraWhere eventuale clausola where aggiuntiva (puo' essere null)
   * @param con connessione al db
   * @return numero di record in tabella
   * @throws java.lang.Exception
   */
  public static long getRecordCount(String tableName, String extraWhere, Connection con)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    qb.setSelect("COUNT(*)");
    qb.setFrom(tableName);
    String campo = qb.adjCampo(RigelColumnDescriptor.PDT_INTEGER, "stato_rec");
    String where = "(" + campo + " IS NULL) OR (" + campo + " < 10)";
    if(extraWhere != null)
      where += " AND (" + extraWhere + ")";
    qb.setWhere(where);
    qb.setLimit(1);
    String sSQL = qb.makeSQLstring();
    List records = executeQuery(sSQL, con);
    if(records.isEmpty())
      return 0;

    Record rec = (Record) (records.get(0));
    return rec.getValue(1).asLong();
  }

  /**
   * Calcola il numero di record totali di un Criteria.
   * Vengono conteggiati tutti i record selezionabili dal criteria specificato.
   * @param c criteria da conteggiare
   * @return numero di record
   * @throws java.lang.Exception
   */
  public static long getRecordCount(Criteria c)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    String sSQL = qb.getCountRecordsQuery(c);

    List<Record> records = executeQuery(sSQL);
    if(records.isEmpty())
    {
      return 0;
    }

    Record rec = (Record) (records.get(0));
    return rec.getValue(1).asLong();
  }

  /**
   * Calcola il numero di record totali di un Criteria. Vengono conteggiati tutti i record selezionabili dal criteria
   * specificato.
   *
   * @param c criteria da conteggiare
   * @param con
   * @return numero di record
   * @throws java.lang.Exception
   */
  public static long getRecordCount(Criteria c, Connection con)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    String sSQL = qb.getCountRecordsQuery(c);

    List<Record> records = executeQuery(sSQL, con);
    if(records.isEmpty())
    {
      return 0;
    }

    Record rec = (Record) (records.get(0));
    return rec.getValue(1).asLong();
  }

  public static long deleteFromCriteria(Criteria c)
     throws Exception
  {
    String subSQL = createQueryString(c);

    if(subSQL.startsWith("SELECT  FROM") || subSQL.startsWith("SELECT DISTINCT  FROM"))
    {
      subSQL = "DELETE " + subSQL.substring(subSQL.indexOf("FROM"));
    }

    return BasePeer.executeStatement(subSQL);
  }

  public static long deleteFromCriteria(Criteria c, Connection con)
     throws Exception
  {
    String subSQL = createQueryString(c);

    if(subSQL.startsWith("SELECT  FROM") || subSQL.startsWith("SELECT DISTINCT  FROM"))
    {
      subSQL = "DELETE " + subSQL.substring(subSQL.indexOf("FROM"));
    }

    return BasePeer.executeStatement(subSQL, con);
  }

  /**
   * Cancella tutti i record di una tabella.
   *
   * @param tableName tabella da cancellare
   * @return numero di record cancellati
   * @throws java.lang.Exception
   */
  public static long clearTable(String tableName)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    qb.setFrom(tableName);
    String sSQL = qb.queryForDelete();
    return executeStatement(sSQL);
  }

  /**
   * Ritorna il valore massimo di un campo su una tabella.
   *
   * @param tabella
   * @param campo
   * @param dbCon
   * @return
   * @throws Exception
   */
  public static long getMaxField(String tabella, String campo, Connection dbCon)
     throws Exception
  {
    return getMaxField(tabella, campo, null, dbCon);
  }

  /**
   * Ritorna il valore massimo di un campo su una tabella.
   *
   * @param tabella
   * @param campo
   * @param where
   * @param dbCon
   * @return
   * @throws Exception
   */
  public static long getMaxField(String tabella, String campo, String where, Connection dbCon)
     throws Exception
  {
    String sSQL = "SELECT MAX(" + campo + ") FROM " + tabella;

    if(where != null)
    {
      sSQL += " WHERE " + where;
    }

    List records = dbCon == null ? executeQuery(sSQL) : executeQuery(sSQL, dbCon);
    if(records.isEmpty())
    {
      return 0;
    }

    Record rec = (Record) (records.get(0));
    return rec.getValue(1).asLong();
  }

  public static long getMaxPrimaryKey(String tabella, Connection dbCon)
     throws Exception
  {
    DatabaseMap dbMap = Torque.getDatabaseMap();
    TableMap tm = dbMap.getTable(tabella);
    return getMaxPrimaryKey(tm, dbCon);
  }

  public static long getMaxPrimaryKey(TableMap tm, Connection dbCon)
     throws Exception
  {
    ColumnMap cmp = null;
    ColumnMap cms[] = tm.getColumns();
    for(int i = 0; i < cms.length; i++)
    {
      ColumnMap cm = cms[i];
      if(cm.isPrimaryKey())
      {
        if(cmp == null)
        {
          cmp = cm;
        }
        else
        {
          return -1; // solo per chiave primaria non multipla
        }
      }
    }

    String tableName = tm.getFullyQualifiedTableName();

    return cmp == null || !(cmp.getType() instanceof Integer) ? -1
              : getMaxField(tableName, cmp.getColumnName(), dbCon);
  }

//  /**
//   * Cancella a cascata una record da una tabella, cancellando tutte le tabelle con chiavi esterne alla tabella
//   * principale.
//   *
//   * @param tableName tabella da cancellare
//   * @param recObj
//   * @throws Exception
//   */
//  public static void deleteCascade(String tableName, BaseObject recObj)
//     throws Exception
//  {
//    deleteCascadeWorker(new Stack(), tableName, recObj);
//  }
//
//  private static void deleteCascadeWorker(Stack deepStack, String tableName, BaseObject recObj)
//     throws Exception
//  {
//    // verifica per riferimento circolare
//    if(deepStack.contains(tableName))
//    {
//      return;
//    }
//
//    // aggiugne per controllo riferimento circolare
//    deepStack.push(tableName);
//
//    // aggiunge le tabelle mancanti
//    TableMap[] arMaps = Torque.getDatabaseMap().getTables();
//    for(int i = 0; i < arMaps.length; i++)
//    {
//      TableMap tm = arMaps[i];
//      if(StringOper.isEqu(tableName, tm.getName()))
//      {
//        continue;
//      }
//
//      ColumnMap[] arCols = tm.getColumns();
//      for(int j = 0; j < arCols.length; j++)
//      {
//        ColumnMap cm = arCols[j];
//        if(StringOper.isEqu(tableName, cm.getRelatedTableName()))
//        {
//          // tabella con chiave esterna
//          Object valKey = recObj.getByPeerName(cm.getRelatedName());
//          if(valKey != null)
//          {
//            String classname = "";
//            BaseObject relObj = null;
//            deleteCascadeWorker(deepStack, tm.getName(), relObj);
//          }
//        }
//      }
//    }
//
//    deepStack.pop();
//  }
  /**
   * Conversione da array di interi a chiave primaria.
   *
   * @param pks collezione di interi
   * @return lista di chiavi primarie
   */
  public static List<ObjectKey> convertIntegerKeys(int[] pks)
  {
    ArrayList<ObjectKey> lsPks = new ArrayList<>();
    for(int pk : pks)
      lsPks.add(SimpleKey.keyFor(pk));
    return lsPks;
  }

  /**
   * Conversione da collezione di interi a chiave primaria.
   *
   * @param pks collezione di interi
   * @return lista di chiavi primarie
   */
  public static List<ObjectKey> convertIntegerKeys(Collection<Integer> pks)
  {
    ArrayList<ObjectKey> lsPks = new ArrayList<>();
    pks.forEach((idDoc) -> lsPks.add(SimpleKey.keyFor(idDoc)));
    return lsPks;
  }

  /**
   * Conversione da collezione di stringhe a chiave primaria.
   *
   * @param pks collezione di stringhe
   * @return lista di chiavi primarie
   */
  public static List<ObjectKey> convertStringKeys(Collection<String> pks)
  {
    ArrayList<ObjectKey> lsPks = new ArrayList<>();
    pks.forEach((idDoc) -> lsPks.add(SimpleKey.keyFor(idDoc)));
    return lsPks;
  }

  /**
   * Conversione da collezione di stringhe a chiave primaria.
   * Le stringhe vengono racchiuse fra apici (uno, due, tre ... 'uno', 'due', 'tre').
   * @param pks collezione di stringhe
   * @return lista di chiavi primarie
   */
  public static List<ObjectKey> convertStringKeysApici(Collection<String> pks)
  {
    ArrayList<ObjectKey> lsPks = new ArrayList<>();
    pks.forEach((idDoc) -> lsPks.add(SimpleKey.keyFor("'" + idDoc + "'")));
    return lsPks;
  }

  /**
   * Estrazione di interi da una collezioni di oggetti.
   * @param <T>
   * @param objs collezione di oggetti
   * @param fun espressione lambda per l'estrazione interi
   * @return lista di interi senza duplicazioni e senza 0
   * @throws java.lang.Exception
   */
  public static <T> int[] extractIntArray(Collection<T> objs,
     LEU.ToIntFunction_WithExceptions<T, Exception> fun)
     throws Exception
  {
    return objs.stream()
       .mapToInt(LEU.rethrowFunctionInt(fun))
       .filter((i) -> i != 0)
       .distinct()
       .sorted()
       .toArray();
  }

  /**
   * Estrazione di chiavi primarie da una collezioni di oggetti.
   * @param <T>
   * @param objs collezione di oggetti
   * @param fun espressione lambda per l'estrazione della chiave primaria (integer)
   * @return lista di stringhe senza duplicazioni scartando null e stringhe vuote
   * @throws java.lang.Exception
   */
  public static <T> String[] extractStringArray(Collection<T> objs,
     LEU.Function_WithExceptions<T, String, Exception> fun)
     throws Exception
  {
    return objs.stream()
       .map(LEU.rethrowFunction(fun))
       .filter((s) -> s != null && !s.isEmpty())
       .distinct()
       .sorted()
       .toArray(String[]::new);
  }

  /**
   * Estrazione di chiavi primarie da una collezioni di oggetti.
   * @param <T>
   * @param objs collezione di oggetti
   * @param fun espressione lambda per l'estrazione della chiave primaria (integer)
   * @return lista di chiavi primarie
   * @throws java.lang.Exception
   */
  public static <T> List<ObjectKey> extractIntegerKeys(Collection<T> objs,
     LEU.ToIntFunction_WithExceptions<T, Exception> fun)
     throws Exception
  {
    int[] ids = objs.stream()
       .mapToInt(LEU.rethrowFunctionInt(fun))
       .distinct()
       .sorted()
       .toArray();

    return convertIntegerKeys(ids);
  }

  /**
   * Estrazione di chiavi primarie da una collezioni di oggetti.
   * @param <T>
   * @param objs collezione di oggetti
   * @param fun espressione lambda per l'estrazione della chiave primaria (integer)
   * @return lista di chiavi primarie
   * @throws java.lang.Exception
   */
  public static <T> List<ObjectKey> extractStringKeys(Collection<T> objs,
     LEU.Function_WithExceptions<T, String, Exception> fun)
     throws Exception
  {
    List<String> keys = objs.stream()
       .map(LEU.rethrowFunction(fun))
       .distinct()
       .sorted()
       .collect(Collectors.toList());

    return convertStringKeys(keys);
  }

  /**
   * Estrazione di chiavi primarie da una collezioni di oggetti.
   * Le stringhe vengono racchiuse fra apici (uno, due, tre ... 'uno', 'due', 'tre').
   * @param <T>
   * @param objs collezione di oggetti
   * @param fun espressione lambda per l'estrazione della chiave primaria (integer)
   * @return lista di chiavi primarie
   * @throws java.lang.Exception
   */
  public static <T> List<ObjectKey> extractStringKeysApici(Collection<T> objs,
     LEU.Function_WithExceptions<T, String, Exception> fun)
     throws Exception
  {
    List<String> keys = objs.stream()
       .map(LEU.rethrowFunction(fun))
       .distinct()
       .sorted()
       .collect(Collectors.toList());

    return convertStringKeysApici(keys);
  }

  /**
   * Estrazione di chiave primaria.
   *
   * @param objs collezione di oggetti Torque
   * @return lista di tutte le chiavi primarie
   */
  public static List<ObjectKey> convertBaseObjectKeys(Collection<Persistent> objs)
  {
    ArrayList<ObjectKey> lsPks = new ArrayList<>();
    objs.forEach((o) -> lsPks.add(o.getPrimaryKey()));
    return lsPks;
  }

  public static boolean disableForeignKeys(String nomeTabella)
  {
    try
    {
      QueryBuilder qb = getQueryBuilder();
      return qb.disableForeignKeys(nomeTabella);
    }
    catch(Exception ex)
    {
      log.error("Non fatal: " + ex.getMessage());
      return false;
    }
  }

  public static boolean enableForeignKeys(String nomeTabella)
  {
    try
    {
      QueryBuilder qb = getQueryBuilder();
      return qb.enableForeignKeys(nomeTabella);
    }
    catch(Exception ex)
    {
      log.error("Non fatal: " + ex.getMessage());
      return false;
    }
  }

  /**
   * Lista delle viste di un database.
   *
   * @param con connessione al db
   * @return lista di tutte le viste presenti (schema di default)
   * @throws Exception
   */
  public static List<String> getAllViews(Connection con)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    return qb.getAllViews(con);
  }

  /**
   * Lista delle tabelle di un database.
   *
   * @param con connessione al db
   * @return lista di tutte le tabelle presenti (schema di default)
   * @throws Exception
   */
  public static List<String> getAllTables(Connection con)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    return qb.getAllTables(con);
  }

  /**
   * Ritorna il tipo per una colonna di tabella.
   * La ricerca del nome tabella e nome colonna è case insensitive.
   * @param con connessione al db
   * @param nomeTabella nome della tabella (eventualmente con schema)
   * @param nomeColonna nome della colonna
   * @return int {@code =>} SQL type from java.sql.Types 0=non trovato
   * @throws Exception
   */
  public static int getTipoColonna(Connection con, String nomeTabella, String nomeColonna)
     throws Exception
  {
    Integer rv = scanTabelleColonne(con, nomeTabella, nomeColonna, DbUtils::getTipoColonna);
    return rv == null ? 0 : rv;
  }

  /**
   * Ritorna i tipi delle colonne di una tabella.
   * La ricerca del nome tabella è case insensitive.
   * @param con connessione al db
   * @param nomeTabella nome della tabella (eventualmente con schema)
   * @return int {@code =>} SQL type from java.sql.Types 0=non trovato
   * @throws Exception
   */
  public static ArrayMap<String, Integer> getTipiColonne(Connection con, String nomeTabella)
     throws Exception
  {
    return scanTabelleColonne(con, nomeTabella, null, DbUtils::getTipiColonne);
  }

  /**
   * Ritorna i tipi delle colonne di una query.
   *
   * @param con connessione al db
   * @param query query sul db
   * @return int {@code =>} SQL type from java.sql.Types 0=non trovato
   * @throws Exception
   */
  public static ArrayMap<String, Integer> getTipiColonneQuery(Connection con, String query)
     throws Exception
  {
    Schema sc = schemaQuery(con, query);
    int nc = sc.numberOfColumns();
    ArrayMap<String, Integer> rv = new ArrayMap<>();

    for(int i = 1; i <= nc; i++)
    {
      Column c = sc.column(i);
      rv.add(new Pair<>(c.name(), c.typeEnum()));
    }

    return rv;
  }

  /**
   * Verifica esistenza tabella nel db.
   * La ricerca del nome tabella è case insensitive.
   * @param con connessione al db
   * @param nomeTabella nome della tabella
   * @return vero se esiste
   * @throws Exception
   */
  public static boolean existTable(Connection con, String nomeTabella)
     throws Exception
  {
    Boolean rv = scanTabelleColonne(con, nomeTabella, null,
       (dbCon, nomeSchema1, nomeTabella1, nomeColonna1) -> Boolean.TRUE);
    return rv == null ? false : rv;
  }

  /**
   * Verifica esistenza tabella nel db.
   * La ricerca del nome tabella è case sensitive.
   * @param con connessione al db
   * @param nomeTabella nome della tabella
   * @return vero se esiste
   * @throws Exception
   */
  public static boolean existTableExact(Connection con, String nomeTabella)
     throws Exception
  {
    try ( ResultSet rs = con.getMetaData().getTables(null, null, null, TABLES_FILTER))
    {
      while(rs.next())
      {
        if(nomeTabella.equals(rs.getString("TABLE_NAME")))
          return true;
      }
    }
    return false;
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
  public static <T> T scanTabelleColonne(Connection con, String nomeTabella, String nomeColonna, QueryBuilder.ScanColumn<T> sfun)
     throws Exception
  {
    QueryBuilder qb = getQueryBuilder();
    return qb.scanTabelleColonne(con, nomeTabella, nomeColonna, sfun);
  }

  public static Integer getTipoColonna(Connection con,
     String nomeSchema, String nomeTabella, String nomeColonna)
     throws SQLException
  {
    try ( ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), nomeSchema, nomeTabella, null))
    {
      while(rs.next())
      {
        String cn = rs.getString("COLUMN_NAME");
        if(StringOper.isEquNocase(cn, nomeColonna))
          return rs.getInt("DATA_TYPE");
      }
    }
    return 0;
  }

  public static ArrayMap<String, Integer> getTipiColonne(Connection con,
     String nomeSchema, String nomeTabella, String nomeColonna)
     throws SQLException
  {
    ArrayMap<String, Integer> rv = new ArrayMap<>();

    try ( ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), nomeSchema, nomeTabella, null))
    {
      while(rs.next())
      {
        String cn = rs.getString("COLUMN_NAME");
        int tipo = rs.getInt("DATA_TYPE");
        rv.add(new Pair<>(cn, tipo));
      }
    }

    return rv;
  }

  /**
   * Verifica una colonna per tipo numerico.
   *
   * @param con connessione al db
   * @param nomeTabella nome della tabella (eventualmente con schema)
   * @param nomeColonna nome della colonna
   * @return vero se la colonna è numerica
   * @throws Exception
   */
  public static boolean isNumeric(Connection con, String nomeTabella, String nomeColonna)
     throws Exception
  {
    int tipo = getTipoColonna(con, nomeTabella, nomeColonna);
    return isNumeric(tipo);
  }

  /**
   * Verifica per tipo numerico.
   *
   * @param sqlType tipo sql (Types)
   * @return vero se la colonna è numerica
   */
  public static boolean isNumeric(int sqlType)
  {
    switch(sqlType)
    {
      case Types.BIT:
      case Types.TINYINT:
      case Types.SMALLINT:
      case Types.INTEGER:
      case Types.BIGINT:
      case Types.FLOAT:
      case Types.REAL:
      case Types.DOUBLE:
      case Types.NUMERIC:
      case Types.DECIMAL:
        return true;
    }

    return false;
  }

  public static boolean isString(int sqlType)
  {
    switch(sqlType)
    {
      case Types.LONGNVARCHAR:
      case Types.LONGVARCHAR:
      case Types.NCHAR:
      case Types.NVARCHAR:
      case Types.VARCHAR:
      case Types.CLOB:
        return true;
    }
    return false;
  }

  public static boolean isDate(int sqlType)
  {
    switch(sqlType)
    {
      case Types.DATE:
      case Types.TIME:
      case Types.TIME_WITH_TIMEZONE:
      case Types.TIMESTAMP:
      case Types.TIMESTAMP_WITH_TIMEZONE:
        return true;
    }
    return false;
  }

  public static Schema schemaQuery(Connection con, String sSQL)
     throws Exception
  {
    try ( QueryDataSet qds = new QueryDataSet(con, sSQL))
    {
      return qds.schema();
    }
  }

  public static Schema schemaTable(Connection con, String nomeTabella)
     throws Exception
  {
    try ( TableDataSet tds = new TableDataSet(con, nomeTabella))
    {
      return tds.schema();
    }
  }

  public static java.sql.Date cvtDate(java.util.Date d)
  {
    return new java.sql.Date(d.getTime());
  }

  public static java.sql.Timestamp cvtTimestamp(java.util.Date d)
  {
    if(d == null)
      return null;

    return new java.sql.Timestamp(d.getTime());
  }

  public static java.sql.Timestamp cvtTimestampNotNull(java.util.Date d)
  {
    if(d == null)
      return currentTimestamp();

    return new java.sql.Timestamp(d.getTime());
  }

  public static java.sql.Timestamp currentTimestamp()
  {
    return new java.sql.Timestamp(System.currentTimeMillis());
  }

  /**
   * Costruzione di statement SQL.
   *
   * @param tableName nome tabella
   * @param tipiCampi mappa dei tipi di campi
   * @param valori mappa dei valori convertiti in stringa
   * @return istruzione SQL
   */
  public static String createInsertStatement(String tableName,
     ArrayMap<String, Integer> tipiCampi, Map<String, String> valori)
  {
    StringBuilder sb1 = new StringBuilder(512);
    StringBuilder sb2 = new StringBuilder(512);

    for(Map.Entry<String, String> entry : valori.entrySet())
    {
      String key = StringOper.okStr(entry.getKey());
      String value = StringOper.okStr(entry.getValue());

      Integer tipo = tipiCampi.getIgnoreCase(key);
      if(tipo == null)
      {
        continue;
      }

      sb1.append(",").append(key);

      if(DbUtils.isNumeric(tipo))
      {
        sb2.append(",").append(value);
      }
      else
      {
        sb2.append(",'").append(value).append("'");
      }
    }

    if(sb1.length() == 0 || sb2.length() == 0)
    {
      return null;
    }

    String sSQL
       = "INSERT INTO " + tableName + "(" + sb1.toString().substring(1) + ")"
       + " VALUES(" + sb2.toString().substring(1) + ")";

    return sSQL;
  }

  /**
   * Costruzione di statement SQL.
   *
   * @param tableName nome tabella
   * @param tipiCampi mappa dei tipi di campi
   * @param valoriUpdate valori da aggiornare convertiti in stringa
   * @param valoriSelect valori di selezione convertiti in stringa
   * @return
   */
  public static String createUpdateStatement(String tableName,
     ArrayMap<String, Integer> tipiCampi, Map<String, String> valoriUpdate, Map<String, String> valoriSelect)
  {
    StringBuilder sb1 = new StringBuilder(512);
    StringBuilder sb2 = new StringBuilder(512);

    if(valoriUpdate != null && !valoriUpdate.isEmpty())
    {
      mapToString(valoriUpdate, tipiCampi, sb1, ",");
    }

    if(valoriSelect != null && !valoriSelect.isEmpty())
    {
      mapToString(valoriSelect, tipiCampi, sb2, ") AND (");
    }

    if(sb1.length() == 0)
    {
      return null;
    }

    String sSQL
       = "UPDATE " + tableName
       + " SET " + sb1.toString().substring(1);

    if(sb2.length() > 0)
    {
      sSQL += " WHERE " + sb2.toString().substring(6) + ")";
    }

    return sSQL;
  }

  private static void mapToString(Map<String, String> valori,
     ArrayMap<String, Integer> tipiCampi, StringBuilder sb, String sep)
  {
    for(Map.Entry<String, String> entry : valori.entrySet())
    {
      String key = StringOper.okStr(entry.getKey());
      String value = StringOper.okStr(entry.getValue());

      Integer tipo = tipiCampi.getIgnoreCase(key);
      if(tipo == null)
      {
        continue;
      }

      sb.append(sep).append(key);

      if(DbUtils.isNumeric(tipo))
      {
        sb.append("=").append(value);
      }
      else
      {
        sb.append("='").append(value).append("'");
      }
    }
  }

  /**
   * Ritorna le colonne della chiave primaria di una tabella. La ricerca del nome tabella è case insensitive.
   *
   * @param con connessione al db
   * @param nomeTabella nome della tabella (eventualmente con schema)
   * @return int {@code =>} SQL type from java.sql.Types 0=non trovato
   * @throws Exception
   */
  public static ArrayMap<String, Integer> getPrimaryKeys(Connection con, String nomeTabella)
     throws Exception
  {
    return scanTabelleColonne(con, nomeTabella, null, DbUtils::getPrimaryKeys);
  }

  public static ArrayMap<String, Integer> getPrimaryKeys(Connection con,
     String nomeSchema, String nomeTabella, String nomeColonna)
     throws SQLException
  {
    ArrayMap<String, Integer> rv = new ArrayMap<>();

    try ( ResultSet rs = con.getMetaData().getPrimaryKeys(con.getCatalog(), nomeSchema, nomeTabella))
    {
      while(rs.next())
      {
        String cn = rs.getString("COLUMN_NAME");
        int seq = rs.getInt("KEY_SEQ");
        rv.add(new Pair<>(cn, seq));
      }
    }

    return rv;
  }

  /**
   * Esegue una query ritornando l'array di interi del primo campo.
   * Di solito utilizzata per estrarre le chiavi di una tabella secondaria.
   * ES: SELECT idcollegata FROM tabellaCollegata WHERE idmaster=100
   * @param con connessione al db
   * @param sSQL query per il risultato
   * @return array di interi del primo campo della query
   * @throws Exception
   */
  public static int[] queryForID(Connection con, String sSQL)
     throws Exception
  {
    return queryForID(con, 1, sSQL);
  }

  /**
   * Esegue una query ritornando l'array di interi del primo campo.
   * Di solito utilizzata per estrarre le chiavi di una tabella secondaria.
   * ES: SELECT idcollegata FROM tabellaCollegata WHERE idmaster=100
   * @param con connessione al db
   * @param numField numero di campo da estrarre (1=primo campo)
   * @param sSQL query per il risultato
   * @return array di interi del primo campo della query
   * @throws Exception
   */
  public static int[] queryForID(Connection con, int numField, String sSQL)
     throws Exception
  {
    List<Record> lsRecs = executeQuery(sSQL, con);
    return lsRecs.stream()
       .mapToInt(LEU.rethrowFunctionInt((r) -> r.getValue(numField).asInt()))
       .filter((i) -> i != 0)
       .sorted().distinct().toArray();
  }

  /**
   * Esegue una query ritornando l'array di interi del primo campo.
   * Di solito utilizzata per estrarre le chiavi di una tabella secondaria.
   * ES: SELECT idcollegata FROM tabellaCollegata WHERE idmaster=100
   * @param numField numero di campo da estrarre (1=primo campo)
   * @param rs ResultSet appositamente creato
   * @return array di interi del primo campo della query
   * @throws Exception
   */
  public static int[] queryForID(int numField, ResultSet rs)
     throws Exception
  {
    ArrayList<Integer> rv = new ArrayList<>(128);
    while(rs.next())
      rv.add(rs.getInt(numField));

    return rv.stream()
       .mapToInt((i) -> i)
       .filter((i) -> i != 0)
       .sorted().distinct().toArray();
  }

  /**
   * Returns numberOfResults records in a QueryDataSet as a List
   * of Record objects. Starting at record start. Used for
   * functionality like util.LargeSelect.
   *
   * @param qds The <code>QueryDataSet</code> to extract results
   * from.
   * @param start The index from which to start retrieving
   * <code>Record</code> objects from the data set.
   * @param numberOfResults The number of results to return (or
   * <code> -1</code> for all results).
   * @return A <code>List</code> of <code>Record</code> objects.
   * @exception Exception
   */
  public static List<Record> getSelectResults(QueryDataSet qds, int start, int numberOfResults)
     throws Exception
  {
    List<Record> results = null;

    if(numberOfResults < 0)
    {
      results = new ArrayList<>();
      qds.fetchRecords();
    }
    else
    {
      results = new ArrayList<>(numberOfResults);
      qds.fetchRecords(start, numberOfResults);
    }

    int startRecord = 0;

    // Offset the correct number of records
    if(start > 0 && numberOfResults <= 0)
    {
      startRecord = start;
    }

    // Return a List of Record objects.
    for(int i = startRecord; i < qds.size(); i++)
    {
      Record rec = qds.getRecord(i);
      results.add(rec);
    }

    return results;
  }

  /**
   * Returna all records of a query.
   * @param qds The <code>QueryDataSet</code> to extract results
   * from.
   * @return the records
   * @throws Exception
   */
  public static List<Record> getSelectResults(QueryDataSet qds)
     throws Exception
  {
    return getSelectResults(qds, 0, -1);
  }

  public static List<Record> executeQuery(String queryString)
     throws Exception
  {
    return PeerTransactAgent.executeReturnReadonly((con) -> executeQuery(queryString, con));
  }

  public static List<Record> executeQuery(String queryString, String dbName)
     throws Exception
  {
    return executeQuery(queryString, 0, -1, dbName);
  }

  /**
   * Method for performing a SELECT.
   *
   * @param queryString A String with the sql statement to execute.
   * @param start The first row to return.
   * @param numberOfResults The number of rows to return.
   * @param dbName The database to connect to.
   * @return List of Record objects.
   * @throws TorqueException Any exceptions caught during processing will be
   * rethrown wrapped into a TorqueException.
   */
  public static List<Record> executeQuery(String queryString, int start, int numberOfResults, String dbName)
     throws Exception
  {
    Connection con = null;
    List results = null;
    try
    {
      con = Torque.getConnection(dbName);
      // execute the query
      results = executeQuery(
         queryString,
         start,
         numberOfResults,
         con);
    }
    finally
    {
      Torque.closeConnection(con);
    }
    return results;
  }

  public static List<Record> executeQuery(String queryString, Connection con)
     throws Exception
  {
    return executeQuery(queryString, 0, -1, con);
  }

  /**
   * Method for performing a SELECT. Returns all results.
   *
   * @param queryString A String with the sql statement to execute.
   * @param start The first row to return.
   * @param numberOfResults The number of rows to return.
   * @param con A Connection.
   * @return List of Record objects.
   * @throws Exception
   */
  public static List<Record> executeQuery(String queryString, int start, int numberOfResults, Connection con)
     throws Exception
  {
    QueryDataSet qds = null;
    List results = Collections.EMPTY_LIST;

    try
    {
      // execute the query
      long startTime = System.currentTimeMillis();
      qds = new QueryDataSet(con, queryString);
      if(log.isDebugEnabled())
      {
        log.debug("Elapsed time="
           + (System.currentTimeMillis() - startTime) + " ms");
      }
      results = getSelectResults(qds, start, numberOfResults);
    }
    finally
    {
      if(qds != null)
        qds.close();
    }

    return results;
  }

  /**
   * Method to create an SQL query for actual execution based on values in a
   * Criteria.
   *
   * @param criteria A Criteria.
   * @return the SQL query for actual execution
   * @exception TorqueException Trouble creating the query string.
   */
  public static String createQueryString(Criteria criteria)
     throws TorqueException
  {
    Query query = SqlBuilder.buildQuery(criteria);
    return query.toString();
  }

  /**
   * Returns all results.
   *
   * @param criteria A Criteria.
   * @param con A Connection.
   * @return List of Record objects.
   * @throws TorqueException Any exceptions caught during processing will be
   * rethrown wrapped into a TorqueException.
   */
  public static List<Record> doSelect(Criteria criteria, Connection con)
     throws Exception
  {
    return BasePeer.doSelect(criteria, new VillageRecordMapper(), con);
  }

  public static void doInsert(String fullTableName, ColumnValues insertValues)
     throws TorqueException
  {
    Connection connection = null;
    try
    {
      connection = Transaction.begin();
      doInsert(fullTableName, insertValues, connection);
      Transaction.commit(connection);
      connection = null;
    }
    finally
    {
      if(connection != null)
      {
        Transaction.safeRollback(connection);
      }
    }
  }

  /**
   * Inserts a record into a database table.
   * <p>
   * If the primary key is included in Criteria, then that value will
   * be used to insert the row.
   * <p>
   * Otherwise, if the primary key can be generated automatically,
   * the generated key will be used for the insert and will be returned.
   * <p>
   * If no value is given for the primary key is defined and it cannot
   * be generated automatically or the table has no primary key,
   * the values will be inserted as specified and null will be returned.
   *
   * @param fullTableName name of the table
   * @param insertValues Contains the values to insert, not null.
   * @param connection the connection to use for the insert, not null.
   *
   * @return the primary key of the inserted row (if the table
   * has a primary key) or null (if the table does not have
   * a primary key).
   *
   * @throws TorqueException if a database error occurs.
   */
  public static int doInsert(String fullTableName, ColumnValues insertValues, Connection connection)
     throws TorqueException
  {
    if(insertValues == null)
    {
      throw new TorqueException("insertValues is null");
    }
    if(connection == null)
    {
      throw new TorqueException("connection is null");
    }

    List<String> columnNames = new ArrayList<>();
    List<JdbcTypedValue> replacementObjects = new ArrayList<>();
    for(Map.Entry<org.apache.torque.Column, JdbcTypedValue> columnValue : insertValues.entrySet())
    {
      org.apache.torque.Column column = columnValue.getKey();
      columnNames.add(column.getColumnName());
      JdbcTypedValue value = columnValue.getValue();
      replacementObjects.add(value);
    }

    StringBuilder query = new StringBuilder("INSERT INTO ")
       .append(fullTableName)
       .append("(")
       .append(StringUtils.join(columnNames, ","))
       .append(") VALUES (");

    for(int i = 0; i < columnNames.size(); ++i)
    {
      if(i != 0)
        query.append(",");
      query.append("?");
    }
    query.append(")");

    try ( PreparedStatement ps = connection.prepareStatement(query.toString()))
    {
      populatePreparedStatement(replacementObjects, ps, 1);

      long startTime = System.currentTimeMillis();
      int rv = ps.executeUpdate();
      long queryEndTime = System.currentTimeMillis();
      log.trace("insert took " + (queryEndTime - startTime) + " milliseconds");

      return rv;
    }
    catch(SQLException e)
    {
      throw ExceptionMapper.getInstance().toTorqueException(e);
    }
  }

  /**
   * Executes an update against the database.The rows to be updated
   * are selected using <code>criteria</code> and updated using the values
   * in <code>updateValues</code>.
   *
   * @param criteria selects which rows of which table should be updated.
   * @param updateValues Which columns to update with which values, not null.
   * @param connection the database connection to use, not null.
   *
   * @return the number of affected rows.
   *
   * @throws TorqueException if updating fails.
   */
  public static int doUpdate(
     Criteria criteria,
     ColumnValues updateValues,
     Connection connection)
     throws TorqueException
  {
    Query query = SqlBuilder.buildQuery(criteria);
    query.setType(Query.Type.UPDATE);
    query.getSelectClause().clear();

    List<JdbcTypedValue> replacementObjects = new ArrayList<>();
    for(Map.Entry<org.apache.torque.Column, JdbcTypedValue> updateValue : updateValues.entrySet())
    {
      org.apache.torque.Column column = updateValue.getKey();
      query.getSelectClause().add(column.getColumnName());
      replacementObjects.add(updateValue.getValue());
    }

    try ( PreparedStatement ps = connection.prepareStatement(query.toString()))
    {
      int position = populatePreparedStatement(replacementObjects, ps, 1);

      for(Object value : query.getPreparedStatementReplacements())
      {
        ps.setObject(position++, value);
      }

      long startTime = System.currentTimeMillis();
      int affectedRows = ps.executeUpdate();
      long queryEndTime = System.currentTimeMillis();
      log.trace("update took " + (queryEndTime - startTime) + " milliseconds");

      return affectedRows;
    }
    catch(SQLException e)
    {
      throw ExceptionMapper.getInstance().toTorqueException(e);
    }
  }

  /**
   * Inserisce i valori nel PreparedStatement.
   * @param replacementObjects valori da inserire
   * @param ps statement da popolare
   * @param position indice iniziale
   * @return indice finale
   * @throws SQLException
   */
  public static int populatePreparedStatement(List<JdbcTypedValue> replacementObjects, final PreparedStatement ps, int position)
     throws SQLException
  {
    for(JdbcTypedValue rep : replacementObjects)
    {
      Object value = rep.getValue();

      if(value != null)
      {
        if(rep.getJdbcType() != Types.BLOB && rep.getJdbcType() != Types.CLOB)
        {
          ps.setObject(position, value, rep.getJdbcType());
        }
        else
        {
          ps.setObject(position, value);
        }
      }
      else
      {
        ps.setNull(position, rep.getJdbcType());
      }

      position++;
    }

    return position;
  }

  public static int executeStatement(String sSQL)
     throws TorqueException
  {
    return BasePeer.executeStatement(sSQL);
  }

  public static int executeStatement(String sSQL, Connection con)
     throws TorqueException
  {
    return BasePeer.executeStatement(sSQL, con);
  }

  /**
   * Esegue uno script SQL.
   * Ogni query viene riconosciuta dal terminatore ';'.
   * @param con connessione al db
   * @param r reader da cui leggere lo script
   * @param ignoreErrors se vero log degli errori senza interruzione
   * @return numero di query eseguite
   * @throws Exception
   */
  public static int executeSqlScript(Connection con, Reader r, boolean ignoreErrors)
     throws Exception
  {
    int c, count = 0;
    StringBuilder sb = new StringBuilder(128);

    do
    {
      c = r.read();

      if(c == ';' || c == -1)
      {
        if(sb.length() != 0)
        {
          String sSQL = sb.toString().trim();

          if(!sSQL.isEmpty())
          {
            try ( PreparedStatement ps = con.prepareStatement(sSQL))
            {
              count += ps.executeUpdate();
            }
            catch(Exception ex)
            {
              if(ignoreErrors)
              {
                log.error("Execute script SQL error:", ex);
              }
              else
                throw ex;
            }
            sb = new StringBuilder(128);
          }
        }
      }
      else
      {
        sb.append((char) c);
      }
    }
    while(c != -1);

    return count;
  }

  public static String costruisciSQLzero(Connection con, String nomeTabella)
     throws Exception
  {
    return scanTabelleColonne(con, nomeTabella, null, (conp, nomeSchemap, nomeTabellap, __ignorami) ->
    {
      int nsize = NESSUNO_INDEFINITO.length() - 2;
      StringBuilder sb1 = new StringBuilder(1024);
      StringBuilder sb2 = new StringBuilder(1024);

      try ( ResultSet rs = con.getMetaData().getColumns(conp.getCatalog(), nomeSchemap, nomeTabellap, null))
      {
        for(int i = 0; rs.next(); i++)
        {
          String cn = rs.getString("COLUMN_NAME");
          int tipo = rs.getInt("DATA_TYPE");
          int size = rs.getInt("COLUMN_SIZE");
          int tn = rs.getInt("NULLABLE");

          if(i > 0)
          {
            sb1.append(',');
            sb2.append(',');
          }

          sb1.append(cn);

          switch(tipo)
          {
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
              if("stato_rec".equalsIgnoreCase(cn))
                sb2.append("10");
              else
                sb2.append("0");
              break;

            case Types.TIMESTAMP:
              if(tn == ResultSetMetaData.columnNoNulls)
                sb2.append("current_timestamp");
              else
                sb2.append("NULL");
              break;

            case Types.BOOLEAN:
              sb2.append("false");
              break;

            default:
              if(tn == ResultSetMetaData.columnNoNulls)
              {
                if(size > nsize)
                  sb2.append(NESSUNO_INDEFINITO);
                else
                  sb2.append("'0'");
              }
              else
                sb2.append("NULL");
              break;
          }
        }
      }

      if(sb1.length() == 0)
        return null;

      if(nomeSchemap != null)
        return "INSERT INTO " + nomeSchemap + "." + nomeTabellap + " (" + sb1.toString() + ") VALUES (" + sb2.toString() + ")";

      return "INSERT INTO " + nomeTabellap + " (" + sb1.toString() + ") VALUES (" + sb2.toString() + ")";
    });
  }

  public static Record findRecord(Collection<Record> lsRecs, int id, String columnName)
     throws Exception
  {
    for(Record r : lsRecs)
    {
      if(id == r.getValue(columnName).asInt())
        return r;
    }
    return null;
  }

  public static Record findRecord(Collection<Record> lsRecs, String id, String columnName)
     throws Exception
  {
    final String ids = StringOper.okStrNull(id);
    if(ids == null)
      return null;

    for(Record r : lsRecs)
    {
      if(ids.equals(StringOper.okStr(r.getValue(columnName).asString())))
        return r;
    }

    return null;
  }
}
