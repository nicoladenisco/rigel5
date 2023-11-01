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

import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import com.workingdogs.village.Schema;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.sql.Query;
import org.apache.torque.sql.SqlBuilder;
import org.commonlib5.utils.MacroResolver;
import org.commonlib5.utils.StringOper;
import org.rigel5.RigelI18nInterface;
import org.rigel5.SetupHolder;
import org.rigel5.SqlUtils;
import org.rigel5.db.DbUtils;
import static org.rigel5.db.DbUtils.TABLES_FILTER;
import static org.rigel5.db.DbUtils.VIEWS_FILTER;
import org.rigel5.db.torque.CriteriaRigel;
import org.rigel5.table.FiltroListe;
import org.rigel5.table.ForeignDataHolder;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * <p>
 * Title: QueryBuilder</p>
 * <p>
 * Description: Custruttore di Query SQL.</p>
 * <p>
 * Questa classe viene specializzata per i database
 * supportati al fine di adattarsi alla sintassi specifica.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class QueryBuilder implements Closeable
{
  /** Logging */
  private static final Log log = LogFactory.getLog(QueryBuilder.class);
  public final static int MAX_RECORDS = 500;
  protected String select = "*";
  protected String from;
  protected String where;
  protected String orderby;
  protected String groupby;
  protected String having;
  protected int offset = 0;
  protected int limit = 0;
  protected FiltroData filtro, parametri;
  protected String deleteFrom;
  protected boolean ignoreCase = true;
  protected boolean useDistinct = false;
  protected boolean nativeLimit = true;
  protected boolean nativeOffset = true;
  protected SimpleDateFormat dfIso = new SimpleDateFormat("yyyy-MM-dd");
  protected SimpleDateFormat hhIso = new SimpleDateFormat("HH:mm:ss");
  protected SimpleDateFormat dsIso = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  protected SimpleDateFormat dmIso = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  protected static HashMap<String, Boolean> statoRecMap = new HashMap<String, Boolean>();
  protected QueryDataSet lastQuery;
  protected MacroResolver macroResolver;

  public String makeSQLstring()
     throws Exception
  {
    return makeSQLstring(true, true, true);
  }

  /**
   * Restituisce una query di coneggio a partire da una query generica.
   * @param genericQuery una query per selezionare record
   * @return una query di conteggio dei record selezionati
   */
  abstract public String getCountRecordsQuery(String genericQuery);

  /**
   * Restituisce una query di conteggio per il criteria specificato.
   * @param c criteria (Torque) per selezionare record
   * @return una query di conteggio dei record selezionati
   * @throws org.apache.torque.TorqueException
   */
  public String getCountRecordsQuery(Criteria c)
     throws TorqueException
  {
    Query q = SqlBuilder.buildQuery(c);
    String subSQL = q.toString();

    int idx = subSQL.indexOf(" FROM ");
    if(idx != -1)
      subSQL = "SELECT * " + subSQL.substring(idx);

    return getCountRecordsQuery(subSQL);
  }

  /**
   * Query di congeggio.
   * Restiuisce una query di conteggio per i parametri attuali di questo query builer con in più il filtro specificato.
   * @param fl filtro per records
   * @return una query di conteggio dei record selezionati
   * @throws java.lang.Exception
   */
  public String getTotalRecordsQueryAddFilter(FiltroData fl)
     throws Exception
  {
    String sSQL = makeSQLstringNoFiltro(false);

    sSQL = getCountRecordsQuery(sSQL);

    if(fl != null && fl.haveWhere())
      sSQL += " WHERE " + makeFiltroWhere(fl);

    return sSQL;
  }

  abstract public String adjCampo(int dataType, String campo);

  abstract public String adjValue(int dataType, Object val);

  public String adjCampoValue(int dataType, String campo, String compare, Object val)
  {
    return adjCampo(dataType, campo) + compare + adjValue(dataType, val);
  }

  abstract public String getVista()
     throws Exception;

  public String adjLike(String campo, Object val)
  {
    return adjCampo(RigelColumnDescriptor.PDT_STRING, campo)
       + " LIKE '%" + adjValue(RigelColumnDescriptor.PDT_STRING, val) + "%'";
  }

  public long getValueFromSequence(String sequenceName, Connection con)
     throws Exception
  {
    String sSQL = "SELECT nextval('" + sequenceName + "'::regclass)";
    List<Record> lsRecs = DbUtils.executeQuery(sSQL, con);
    return lsRecs.isEmpty() ? 0 : lsRecs.get(0).getValue(1).asLong();
  }

  public String queryForInsert(FiltroData fd)
     throws Exception
  {
    String fldNames = "";
    String fldValues = "";

    for(FiltroData.updateInfo ui : fd.vUpdate)
    {
      if(ui.val == null)
        continue;

      fldNames += "," + adjCampo(ui.type, ui.nomecampo);
      fldValues += "," + adjValue(ui.type, ui.val);
    }

    if(fldNames.length() == 0)
      return null;

    return queryForInsert(fldNames.substring(1), fldValues.substring(1));
  }

  public String queryForInsert(String fldNames, String fldValues)
     throws Exception
  {
    if(deleteFrom == null)
      deleteFrom = from;

    return "INSERT INTO " + deleteFrom + "(" + fldNames + ") VALUES (" + fldValues + ")";
  }

  public String queryForSelect()
     throws Exception
  {
    return makeSQLstring(true, true, true);
  }

  public String queryForSelect(FiltroData fd)
     throws Exception
  {
    setSelect(makeFiltroSelect(fd));
    setWhere(makeFiltroWhere(fd));
    setOrderby(makeFiltroOrderby(fd));
    return makeSQLstring(true, true, true);
  }

  public String queryForUpdate(FiltroData fd)
     throws Exception
  {
    String fldUpdates = makeFiltroUpdate(fd);
    String fldWhere = makeFiltroWhere(fd);

    return queryForUpdate(fldUpdates, fldWhere);
  }

  public String queryForUpdate(String fldUpdates)
     throws Exception
  {
    return queryForUpdate(fldUpdates, haveWhere() ? where : null);
  }

  public String queryForUpdate(String fldUpdates, String fldWhere)
     throws Exception
  {
    if(deleteFrom == null)
      deleteFrom = from;

    if(fldWhere == null)
      return "UPDATE " + deleteFrom + " SET " + fldUpdates;
    else
      return "UPDATE " + deleteFrom + " SET " + fldUpdates + " WHERE " + fldWhere;
  }

  public String queryForDelete()
     throws Exception
  {
    return queryForDelete(haveWhere() ? where : null);
  }

  public String queryForDelete(FiltroData fd)
     throws Exception
  {
    String fldWhere = makeFiltroWhere(fd);
    return queryForDelete(fldWhere);
  }

  public synchronized String queryForDelete(String fldWhere)
     throws Exception
  {
    if(deleteFrom == null)
      deleteFrom = from;

    if(fldWhere == null)
      return "DELETE FROM " + deleteFrom;
    else
      return "DELETE FROM " + deleteFrom + " WHERE " + fldWhere;
  }

  public synchronized String makeSQLstringNoFiltro(boolean useOrderby)
     throws Exception
  {
    String sSQL = null;

    if(useDistinct)
      sSQL = "SELECT DISTINCT " + select + " FROM " + from;
    else
      sSQL = "SELECT " + select + " FROM " + from;

    if(haveWhere())
      sSQL += " WHERE " + where;

    if(parametri != null && parametri.haveWhere())
      sSQL = SqlUtils.addWhere(sSQL, makeFiltroWhere(parametri));

    if(haveGroupby())
      sSQL += " GROUP BY " + groupby;

    if(haveHaving())
      sSQL += " HAVING " + having;

    if(useOrderby)
      // il filtro orby e' un filtro di default, ma se
      // l'utente ha selezionato un ordinamento diverso (a suo piacere)
      // ignoriamo orby e in seguito applicheremo quello dell'utente
      if(haveOrderby() && (filtro == null || !filtro.haveOrderby()))
        sSQL += " ORDER BY " + orderby;

    if(macroResolver != null)
      sSQL = macroResolver.resolveMacro(sSQL);

    return sSQL;
  }

  public String makeSQLstring(boolean useOrderby, boolean useLimit, boolean fetchRecord)
     throws Exception
  {
    if(!fetchRecord)
      useOrderby = useLimit = false;

    String sSQL = makeSQLstringNoFiltro(useOrderby);

    if(haveFilter())
    {
      // attiva subselect per il filtro
      sSQL = "SELECT * FROM (" + sSQL + ")";

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

  public synchronized String makeFiltroSelect(FiltroData fd)
  {
    String sel = "";

    for(String col : fd.vSelect)
    {
      if(col == null)
        continue;

      sel += "," + col;
    }

    return sel.length() == 0 ? null : sel.substring(1);
  }

  public synchronized String makeFiltroUpdate(FiltroData fd)
  {
    String upd = "";

    for(FiltroData.updateInfo ui : fd.vUpdate)
    {
      if(ui.val == null)
        continue;

      upd += "," + adjCampo(ui.type, ui.nomecampo) + "=" + adjValue(ui.type, ui.val);
    }

    return upd.length() == 0 ? null : upd.substring(1);
  }

  public synchronized String makeFiltroWhere(FiltroData fd)
  {
    StringBuilder whre = new StringBuilder();

    for(FiltroData.whereInfo wi : fd.vWhere)
    {
      if(SqlEnum.ISNULL.equals(wi.criteria))
        whre.append(" AND ").append(wi.nomecampo).append(" IS NULL");
      else if(SqlEnum.ISNOTNULL.equals(wi.criteria))
        whre.append(" AND ").append(wi.nomecampo).append(" IS NOT NULL");
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

  public synchronized String makeFiltroOrderby(FiltroData fd)
  {
    String orby = null;

    for(FiltroData.orderbyInfo oi : fd.vOrderby)
    {
      String ob = oi.nomecampo + " " + oi.dir;

      if(orby == null)
        orby = ob;
      else
        orby += "," + ob;
    }

    return orby;
  }

  public QueryDataSet buildQueryDataset(Connection con, boolean fetchRecords)
     throws Exception
  {
    String sSQL = makeSQLstring(true, fetchRecords, fetchRecords);
    log.debug("SQL=" + sSQL);
    return new QueryDataSet(con, sSQL);
  }

  public synchronized List<Record> executeQuery(Connection con, boolean fetchRecords)
     throws Exception
  {
    if(lastQuery != null)
      lastQuery.close();

    lastQuery = buildQueryDataset(con, fetchRecords);

    if(!fetchRecords)
      return Collections.EMPTY_LIST;

    // simula il parametro offset
    if(!nativeOffset)
      return DbUtils.getSelectResults(lastQuery, offset, limit);

    return DbUtils.getSelectResults(lastQuery);
  }

  public Schema getSchema()
  {
    return lastQuery == null ? null : lastQuery.schema();
  }

  @Override
  public void close()
     throws IOException
  {
    if(lastQuery != null)
    {
      lastQuery.close();
      lastQuery = null;
    }
  }

  /**
   * Ritorna il conteggio totale dei record di questa query.
   * RIMOSSO CODICE CON CACHE (02/02/2012)
   * ECCESSIVAMENTE POCO USER FRIENDLY (LA PAGINAZIONE NON SI AGGIORNA)
   * @param con connessione SQL
   * @param fl eventuale filtro applicato
   * @return numero di records
   * @throws Exception
   */
  public synchronized long getTotalRecords(Connection con, FiltroListe fl)
     throws Exception
  {
    if(fl == null || fl.getOggFiltro() == null)
      return getTotalRecords(con);

    long rv = -1;
    String sSQL = getTotalRecordsQueryAddFilter((FiltroData) (fl.getOggFiltro()));
    try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sSQL))
    {
      if(rs.next())
        rv = rs.getLong(1);
    }

    return rv;
  }

  /**
   * Ritorna il conteggio totale dei record di questa query.
   * RIMOSSO CODICE CON CACHE (02/02/2012)
   * ECCESSIVAMENTE POCO USER FRIENDLY (LA PAGINAZIONE NON SI AGGIORNA)
   * @param con connessione SQL
   * @return numero di records
   * @throws Exception
   */
  public synchronized long getTotalRecords(Connection con)
     throws Exception
  {
    long rv = -1;
    String sSQL = getTotalRecordsQueryAddFilter(null);
    try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sSQL))
    {
      if(rs.next())
        rv = rs.getLong(1);
    }

    return rv;
  }

  /**
   * Determina il numero di record restituiti dalla query indicata.
   * Viene usata per decidere se attivare la modalita combo.
   * Il risultato viene passato nella cache.
   * @param con connessione SQL
   * @param sSQLinner query di cui si vule conoscere il conteggio.
   * @return numero di records
   * @throws java.lang.Exception
   */
  public long getGenericQueryRecordCount(Connection con, String sSQLinner)
     throws Exception
  {
    String sSQL = QueryBuilder.this.getCountRecordsQuery(sSQLinner);

    Long rv;
    if((rv = SetupHolder.getCacheManager().getRecordCount(sSQL)) != null)
      return rv;

    long count;
    try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sSQL))
    {
      count = rs.next() ? rs.getLong(1) : 0;
    }

    SetupHolder.getCacheManager().putRecordCount(sSQL, count);
    return count;
  }

  /**
   * Recupera i dati esterni per tabelle collegate.
   * Il risultato viene passato nella cache.
   * Se la tabella foreign contiene il campo STATO_REC
   * vengono automaticamente eliminati i record cancellati.
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param i18n
   * @return lista dati esterni
   * @throws Exception
   */
  public List<ForeignDataHolder> getForeignDataList(int row, int col,
     RigelTableModel rtm, RigelColumnDescriptor cd, RigelI18nInterface i18n)
     throws Exception
  {
    List<ForeignDataHolder> rv;
    String sSQL = getQueryForeignDataList(row, col, rtm, cd, haveStatoRec(cd.getForeignTabella()));
    if(cd.isEnableCache() && (rv = SetupHolder.getCacheManager().getForeignDataList(sSQL)) != null)
      return rv;

    return SetupHolder.getConProd().functionConnection((con) -> getForeignDataList(con, sSQL, row, col, rtm, cd, i18n));
  }

  /**
   * Recupera i dati esterni per tabelle collegate.
   * Il risultato viene passato nella cache.
   * Recupera tutti i record, ignorando il valore di STATO_REC.
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param i18n
   * @return lista dati esterni
   * @throws Exception
   */
  public List<ForeignDataHolder> getForeignDataListAll(int row, int col,
     RigelTableModel rtm, RigelColumnDescriptor cd, RigelI18nInterface i18n)
     throws Exception
  {
    List<ForeignDataHolder> rv;
    String sSQL = getQueryForeignDataList(row, col, rtm, cd, false);
    if(cd.isEnableCache() && (rv = SetupHolder.getCacheManager().getForeignDataList(sSQL)) != null)
      return rv;

    return SetupHolder.getConProd().functionConnection((con) -> getForeignDataList(con, sSQL, row, col, rtm, cd, i18n));
  }

  /**
   * Recupera i dati esterni per tabelle collegate.
   * Il risultato viene passato nella cache.
   * @param con connessione al db
   * @param sSQL
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param i18n
   * @return lista dati esterni
   * @throws Exception
   */
  public List<ForeignDataHolder> getForeignDataList(Connection con, String sSQL,
     int row, int col, RigelTableModel rtm, RigelColumnDescriptor cd, RigelI18nInterface i18n)
     throws Exception
  {
    List<ForeignDataHolder> rv = null;

    if(sSQL == null)
      sSQL = getQueryForeignDataList(row, col, rtm, cd, haveStatoRec(cd.getForeignTabella()));

    if(cd.isEnableCache() && (rv = SetupHolder.getCacheManager().getForeignDataList(sSQL)) != null)
      return rv;

    rv = new ArrayList<>();
    try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sSQL))
    {
      int numCol = rs.getMetaData().getColumnCount();
      ForeignDataHolder zero = null;

      while(rs.next())
      {
        ForeignDataHolder fdh = new ForeignDataHolder();

        if(cd.isForeignAlternate())
        {
          fdh.codice = rs.getString(1);
          fdh.alternateCodice = rs.getString(2);
          fdh.descrizione = "";
          for(int i = 3; i <= numCol; i++)
            fdh.descrizione += rs.getString(i) + " ";
        }
        else
        {
          fdh.codice = rs.getString(1);
          fdh.descrizione = "";
          for(int i = 2; i <= numCol; i++)
            fdh.descrizione += rs.getString(i) + " ";
        }

        // cerca l'elemento '0' per metterlo da parte
        // in modo da inserirlo in cima alla lista comunque
        if(zero == null && fdh.codice.equals("0"))
        {
          zero = fdh;
          continue;
        }

        rv.add(fdh);
      }

      // se ha trovato l'elemento '0' lo inserisce
      // in cima alla lista, in modo che esca sempre per primo
      if(zero != null)
        rv.add(0, zero);

      // aggiunge eventuale zero se richiesto da setup
      if(zero == null && SetupHolder.isAutoComboAlwaysHaveZero())
      {
        zero = new ForeignDataHolder();
        zero.codice = "0";
        zero.alternateCodice = "0";
        zero.descrizione = i18n.msg("Nessuno/indefinito");
        rv.add(0, zero);
      }

      // inserisce il dato nella cache
      if(cd.isEnableCache())
        SetupHolder.getCacheManager().putForeignDataList(sSQL, rv);
    }

    return rv;
  }

  /**
   * Costuisce la query utilizzata da getForeignDataList() ed estimateForeignDataList().
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param haveStatoRec vero se la tabella collegata ha il campo STATO_REC.
   * @return la query SQL per ottenere la lista di dati esterni
   * @throws Exception
   */
  public String getQueryForeignDataList(int row, int col, RigelTableModel rtm,
     RigelColumnDescriptor cd, boolean haveStatoRec)
     throws Exception
  {
    String sSQL;

    if(cd.isForeignAlternate())
      sSQL
         = "SELECT DISTINCT " + cd.getForeignCampoLink() + "," + cd.getForeignCampoAlternateLink() + "," + cd.getForeignCampoDisplay()
         + " FROM " + cd.getForeignTabella()
         + " WHERE (" + cd.getForeignCampoLink() + " IS NOT NULL) AND (" + cd.getForeignCampoAlternateLink() + " IS NOT NULL)";
    else
      sSQL = "SELECT DISTINCT " + cd.getForeignCampoLink() + "," + cd.getForeignCampoDisplay()
         + " FROM " + cd.getForeignTabella()
         + " WHERE " + cd.getForeignCampoLink() + " IS NOT NULL";

    if(cd.getForeignCampoDisplay().indexOf(',') == -1)
      sSQL += " AND " + cd.getForeignCampoDisplay() + " IS NOT NULL";

    if(haveStatoRec)
      sSQL += " AND " + CriteriaRigel.filtro(cd.getForeignTabella());

    if(cd.getComboExtraWhere() != null)
      sSQL += " AND " + rtm.getValueMacroInside(row, col, cd.getComboExtraWhere(), true, false);

    if(cd.getForeignCampoDisplay().indexOf(',') == -1)
      sSQL += " ORDER BY " + cd.getForeignCampoDisplay();
    else
      sSQL += " ORDER BY " + cd.getForeignCampoLink();

    if(macroResolver != null)
      sSQL = macroResolver.resolveMacro(sSQL);

    log.debug("** getQueryForeignDataList: " + sSQL);
    return sSQL;
  }

  /**
   * Determina il numero di elemnti in foreign mode.
   * Viene usata per decidere se attivare la modalita combo.
   * Il risultato viene passato nella cache.
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @return
   * @throws Exception
   */
  public long estimateForeignDataList(int row, int col, RigelTableModel rtm, RigelColumnDescriptor cd)
     throws Exception
  {
    Long rv;
    String sSQL = getQueryForeignDataList(row, col, rtm, cd, haveStatoRec(cd.getForeignTabella()));
    if((rv = SetupHolder.getCacheManager().getRecordCount(sSQL)) != null)
      return rv;

    return SetupHolder.getConProd().functionConnection((con) ->
    {
      long rv1 = estimateForeignDataList(con, sSQL, row, col, rtm, cd);
      SetupHolder.getCacheManager().putRecordCount(sSQL, rv1);
      return rv1;
    });
  }

  /**
   * Determina il numero di elementi in foreign mode.
   * Viene usata per decidere se attivare la modalita combo.
   * Il risultato viene passato nella cache.
   * @param con connessione al db
   * @param sSQL
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @return numero di records
   * @throws Exception
   */
  public long estimateForeignDataList(Connection con, String sSQL,
     int row, int col, RigelTableModel rtm, RigelColumnDescriptor cd)
     throws Exception
  {
    if(sSQL == null)
      sSQL = getQueryForeignDataList(row, col, rtm, cd, haveStatoRec(cd.getForeignTabella()));

    return getGenericQueryRecordCount(con, sSQL);
  }

  /**
   * Recupera i dati esterni per edit di tabelle collegate con combo box.
   * Il risultato viene passato nella cache.
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param i18n
   * @return lista dati esterni
   * @throws Exception
   */
  public List<ForeignDataHolder> getDataComboColonnaAttached(int row, int col,
     RigelTableModel rtm, RigelColumnDescriptor cd, RigelI18nInterface i18n)
     throws Exception
  {
    List<ForeignDataHolder> rv;
    String sSQL = getQueryComboColonnaAttached(row, col, rtm, cd, haveStatoRec(cd.getComboRicercaTabella()));
    if(cd.isEnableCache() && (rv = SetupHolder.getCacheManager().getDataComboColonnaAttached(sSQL)) != null)
      return rv;

    return SetupHolder.getConProd().functionConnection((con) -> getDataComboColonnaAttached(con, sSQL, row, col, rtm, cd, i18n));
  }

  /**
   * Recupera i dati esterni per edit di tabelle collegate con combo box.
   * Il risultato viene passato nella cache.
   * @param con connessione al db
   * @param sSQL
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param i18n
   * @return lista dati esterni
   * @throws Exception
   */
  public List<ForeignDataHolder> getDataComboColonnaAttached(Connection con, String sSQL,
     int row, int col, RigelTableModel rtm, RigelColumnDescriptor cd, RigelI18nInterface i18n)
     throws Exception
  {
    List<ForeignDataHolder> rv = null;

    if(sSQL == null)
      sSQL = getQueryComboColonnaAttached(row, col, rtm, cd, haveStatoRec(cd.getComboRicercaTabella()));

    if(cd.isEnableCache() && (rv = SetupHolder.getCacheManager().getDataComboColonnaAttached(sSQL)) != null)
      return rv;

    rv = new ArrayList<>();
    try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sSQL))
    {
      int numCol = rs.getMetaData().getColumnCount();
      ForeignDataHolder zero = null;

      while(rs.next())
      {
        ForeignDataHolder fdh = new ForeignDataHolder();
        fdh.codice = rs.getString(1);
        fdh.descrizione = "";

        for(int i = 2; i <= numCol; i++)
          fdh.descrizione += rs.getString(i) + " ";

        if(fdh.codice.trim().length() == 0 || fdh.descrizione.trim().length() == 0)
          continue;

        // cerca l'elemento '0' per metterlo da parte
        // in modo da inserirlo in cima alla lista comunque
        if(zero == null && fdh.codice.equals("0"))
        {
          zero = fdh;
          continue;
        }

        rv.add(fdh);
      }

      // se ha trovato l'elemento '0' lo inserisce
      // in cima alla lista, in modo che esca sempre per primo
      if(zero != null)
        rv.add(0, zero);

      // aggiunge eventuale zero se richiesto da setup
      if(zero == null && SetupHolder.isAutoComboAlwaysHaveZero())
      {
        zero = new ForeignDataHolder();
        zero.codice = "0";
        zero.descrizione = i18n.msg("Nessuno/indefinito");
        rv.add(0, zero);
      }

      // inserisce il dato nella cache
      if(cd.isEnableCache())
        SetupHolder.getCacheManager().putDataComboColonnaAttached(sSQL, rv);
    }

    return rv;
  }

  /**
   * Costruisce la query utilizzata da getDataComboColonnaAttached().
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param haveStatoRec vero se la tabella collegata ha il campo STATO_REC.
   * @return la query SQL per ottenere la lista di dati esterni
   * @throws Exception
   */
  public String getQueryComboColonnaAttached(int row, int col, RigelTableModel rtm,
     RigelColumnDescriptor cd, boolean haveStatoRec)
     throws Exception
  {
    String sSQL = "SELECT DISTINCT " + cd.getComboRicercaCampoLink() + "," + cd.getComboRicercaCampoDisplay()
       + " FROM " + cd.getComboRicercaTabella()
       + " WHERE " + cd.getComboRicercaCampoLink() + " IS NOT NULL";

    if(cd.getComboRicercaCampoDisplay().indexOf(',') == -1)
      sSQL += " AND " + cd.getComboRicercaCampoDisplay() + " IS NOT NULL";

    if(haveStatoRec)
      sSQL += " AND " + CriteriaRigel.filtro(cd.getComboRicercaTabella());

    if(cd.getComboExtraWhere() != null)
      sSQL += " AND " + rtm.getValueMacroInside(row, col, cd.getComboExtraWhere(), true, false);

    if(cd.getComboRicercaCampoDisplay().indexOf(',') == -1)
      sSQL += " ORDER BY " + cd.getComboRicercaCampoDisplay();
    else
      sSQL += " ORDER BY " + cd.getComboRicercaCampoLink();

    if(macroResolver != null)
      sSQL = macroResolver.resolveMacro(sSQL);

    log.debug("** getQueryComboColonnaAttached: " + sSQL);
    return sSQL;
  }

  /**
   * Recupera tutti i valori possibili per il campo indicato.
   * Viene utilizzata per costruire un combo di ricerca con
   * i dati stessi del campo.
   * Il risultato viene passato nella cache.
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param nomeTabella nome della tabella
   * @param nomeCampo nome del campo
   * @return lista dati esterni
   * @throws Exception
   */
  public List<ForeignDataHolder> getDataComboColonnaSelf(int row, int col,
     RigelTableModel rtm, RigelColumnDescriptor cd,
     String nomeTabella, String nomeCampo)
     throws Exception
  {
    List<ForeignDataHolder> rv;
    String sSQL = getQueryComboColonnaSelf(row, col, rtm, cd, nomeTabella, nomeCampo);
    if(cd.isEnableCache() && (rv = SetupHolder.getCacheManager().getDataComboColonnaSelf(sSQL)) != null)
      return rv;

    return SetupHolder.getConProd().functionConnection((con) -> getDataComboColonnaSelf(con, sSQL, row, col, rtm, cd, nomeTabella, nomeCampo));
  }

  /**
   * Recupera tutti i valori possibili per il campo indicato.
   * Viene utilizzata per costruire un combo di ricerca con
   * i dati stessi del campo.
   * Il risultato viene passato nella cache.
   * @param con connessione al db
   * @param sSQL
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param nomeTabella nome della tabella
   * @param nomeCampo nome del campo
   * @return lista dati esterni
   * @throws Exception
   */
  public List<ForeignDataHolder> getDataComboColonnaSelf(Connection con, String sSQL,
     int row, int col, RigelTableModel rtm, RigelColumnDescriptor cd, String nomeTabella, String nomeCampo)
     throws Exception
  {
    List<ForeignDataHolder> rv = null;

    if(sSQL == null)
      sSQL = getQueryComboColonnaSelf(row, col, rtm, cd, nomeTabella, nomeCampo);

    if(cd.isEnableCache() && (rv = SetupHolder.getCacheManager().getDataComboColonnaSelf(sSQL)) != null)
      return rv;

    rv = new ArrayList<>();
    try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sSQL))
    {
      while(rs.next())
      {
        ForeignDataHolder fd = new ForeignDataHolder();
        fd.codice = rs.getString(1);
        fd.descrizione = rs.getString(1);
        rv.add(fd);
      }

      // inserisce il dato nella cache
      if(cd.isEnableCache())
        SetupHolder.getCacheManager().putDataComboColonnaSelf(sSQL, rv);
    }

    return rv;
  }

  /**
   * Costruisce la query utilizzata da getDataComboColonnaSelf().
   * @param row riga corrente
   * @param col colonna corrente
   * @param rtm tableModel con i dati principali
   * @param cd colonna chiave per il recupero
   * @param nomeTabella nome della tabella
   * @param nomeCampo nome del campo
   * @return la query SQL per ottenere la lista di valori possibili.
   * @throws Exception
   */
  public String getQueryComboColonnaSelf(int row, int col,
     RigelTableModel rtm, RigelColumnDescriptor cd,
     String nomeTabella, String nomeCampo)
     throws Exception
  {
    String sSQL = "SELECT DISTINCT " + nomeCampo
       + " FROM " + nomeTabella
       + " WHERE " + nomeCampo + " IS NOT NULL";

    if(cd.getComboExtraWhere() != null)
      sSQL += " AND " + rtm.getValueMacroInside(row, col, cd.getComboExtraWhere(), true, false);

    sSQL += " ORDER BY " + nomeCampo;

    if(macroResolver != null)
      sSQL = macroResolver.resolveMacro(sSQL);

    return sSQL;
  }

  /**
   * Controlla presenza del campo stato_rec nella tabella indicata.
   * Serve a determinare se una tabella supporta il concetto di
   * cancellazione logica.
   * @param nomeTabella tabella da verificare
   * @throws Exception
   * @return vero se la tabella ha il campo STATO_REC
   */
  public boolean haveStatoRec(String nomeTabella)
     throws Exception
  {
    return StatoRecCache.getInstance().haveStatoRec(nomeTabella);
  }

  /**
   * Controlla per eccezione non fatale.
   * <b>L'implementazione dipende fortemente dal db, deve quindi
   * essere implementata nelle classi derivate. Qui viene sempre
   * risollevata l'eccezione.</b>
   * Se l'eccezione si riferisce ad una violazione di chiave
   * esterna, violazione di indice, ecc.. in sostanza un'eccezione
   * che non sia causata da un malfunzionamento del db o della
   * connessione, restituisce una stringa di errore.
   * Diversamente risolleva l'eccezione.
   * @param ex eccezione da controllare
   * @param i18n the value of i18n
   * @throws SQLException
   * @return the java.lang.String
   */
  public String formatNonFatalError(SQLException ex, RigelI18nInterface i18n)
     throws SQLException
  {
    throw ex;
  }

  /**
   * Aggiunge offset e limit ad una query preesistente.
   * @param sSQL stringa della query
   * @param offset prima riga da prelevare
   * @param limit numero di righe da prelevare
   * @return
   */
  abstract public String addNativeOffsetToQuery(String sSQL, long offset, long limit);

  /**
   * Aggiunge una limitazione ad un record della query.
   * @param sSQL stringa della query
   * @return
   */
  abstract public String limitQueryToOne(String sSQL);

  /**
   * Disattiva foreign keys per la tabella indicata.
   * @param nomeTabella nome della tabella
   * @return vero se l'operazione ha avuto successo
   */
  abstract public boolean disableForeignKeys(String nomeTabella);

  /**
   * Riattiva foreign keys per la tabella indicata.
   * @param nomeTabella nome della tabella
   * @return vero se l'operazione ha avuto successo
   */
  abstract public boolean enableForeignKeys(String nomeTabella);

  @FunctionalInterface
  public interface ScanColumn<T>
  {
    public T scan(Connection con, String nomeSchema, String nomeTabella, String nomeColonna)
       throws Exception;
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
  public <T> T scanTabelleColonne(Connection con, String nomeTabella, String nomeColonna, ScanColumn<T> sfun)
     throws Exception
  {
    String nomeSchema = null;
    int pos = nomeTabella.indexOf('.');
    if(pos != -1)
    {
      nomeSchema = nomeTabella.substring(0, pos);
      nomeTabella = nomeTabella.substring(pos + 1);
    }

    try (ResultSet rSet = con.getMetaData().getTables(con.getCatalog(), null, null, TABLES_FILTER))
    {
      while(rSet.next())
      {
        if(rSet.getString("TABLE_TYPE").equals("TABLE"))
        {
          String schema = rSet.getString("TABLE_SCHEM");
          String tableName = rSet.getString("TABLE_NAME");

          if(!isSchemaPublic(schema) && StringOper.isOkStr(nomeSchema))
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

    return null;
  }

  /**
   * Ritorna vero se lo schema è lo schema di default.
   * In Postgres o Mysql si chiama public, in MSSql si chiama dbo, ecc.
   * @param nomeSchema nome da testare
   * @return vero se è lo schema di default del db
   */
  public boolean isSchemaPublic(String nomeSchema)
  {
    return nomeSchema == null || StringOper.isEquNocaseAny(nomeSchema, "public", "dbo");
  }

  /**
   * Lista delle viste di un database.
   * @param con connessione al db
   * @return lista di tutte le viste presenti (schema di default)
   * @throws Exception
   */
  public List<String> getAllViews(Connection con)
     throws Exception
  {
    DatabaseMetaData databaseMetaData = con.getMetaData();
    ArrayList<String> viewNames = new ArrayList<String>();
    try (ResultSet rSet = databaseMetaData.getTables(con.getCatalog(), null, null, VIEWS_FILTER))
    {
      while(rSet.next())
      {
        if(rSet.getString("TABLE_TYPE").equals("VIEW"))
        {
          String schema = rSet.getString("TABLE_SCHEM");
          String tableName = rSet.getString("TABLE_NAME");
          if(!isSchemaPublic(schema))
            viewNames.add(schema + "." + tableName);
          else
            viewNames.add(tableName);
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
  public List<String> getAllTables(Connection con)
     throws Exception
  {
    DatabaseMetaData databaseMetaData = con.getMetaData();
    ArrayList<String> tableNames = new ArrayList<String>();
    try (ResultSet rSet = databaseMetaData.getTables(con.getCatalog(), null, null, TABLES_FILTER))
    {
      while(rSet.next())
      {
        if(rSet.getString("TABLE_TYPE").equals("TABLE"))
        {
          String schema = rSet.getString("TABLE_SCHEM");
          String tableName = rSet.getString("TABLE_NAME");
          if(!isSchemaPublic(schema))
            tableNames.add(schema + "." + tableName);
          else
            tableNames.add(tableName);
        }
      }
    }

    return tableNames;
  }

  /**
   * Restituisce un identificativo della transazione corrente.
   * In caso di non implementazione restituisce null.
   * @param con connessione al db
   * @return stringa rappresentazione dell ID della transazione
   * @throws Exception
   */
  public String getTransactionID(Connection con)
     throws Exception
  {
    return null;
  }

  public boolean haveSelect()
  {
    return !(select == null || select.trim().length() == 0);
  }

  public boolean haveFrom()
  {
    return !(from == null || from.trim().length() == 0);
  }

  public boolean haveWhere()
  {
    return !(where == null || where.trim().length() == 0);
  }

  public boolean haveOrderby()
  {
    return !(orderby == null || orderby.trim().length() == 0);
  }

  public boolean haveGroupby()
  {
    return !(groupby == null || groupby.trim().length() == 0);
  }

  public boolean haveHaving()
  {
    return !(having == null || having.trim().length() == 0);
  }

  public boolean haveFilter()
  {
    return filtro != null && (filtro.haveWhere() || filtro.haveOrderby());
  }

  public boolean haveLimit()
  {
    return limit != 0;
  }

  public String getFrom()
  {
    return from;
  }

  public String getGroupby()
  {
    return groupby;
  }

  public String getHaving()
  {
    return having;
  }

  public int getLimit()
  {
    return limit;
  }

  public int getOffset()
  {
    return offset;
  }

  public String getOrderby()
  {
    return orderby;
  }

  public String getSelect()
  {
    return select;
  }

  public String getWhere()
  {
    return where;
  }

  public void setFrom(String from)
  {
    this.from = from;
  }

  public void addFrom(String from)
  {
    this.from += from;
  }

  public void setGroupby(String groupby)
  {
    this.groupby = groupby;
  }

  public void addGroupby(String groupby)
  {
    this.groupby += groupby;
  }

  public void setHaving(String having)
  {
    this.having = having;
  }

  public void addHaving(String having)
  {
    this.having += having;
  }

  public void setLimit(int limit)
  {
    //this.limit = Math.min(MAX_RECORDS, limit);
    this.limit = limit;
  }

  public void setOffset(int offset)
  {
    this.offset = offset;
  }

  public void setOrderby(String orderby)
  {
    this.orderby = orderby;
  }

  public void addOrderby(String orderby)
  {
    this.orderby += orderby;
  }

  public void setSelect(String select)
  {
    this.select = select;
  }

  public void addSelect(String select)
  {
    this.select += select;
  }

  public void setWhere(String where)
  {
    this.where = where;
  }

  public void addWhere(String where)
  {
    this.where += where;
  }

  public FiltroData getFiltro()
  {
    return filtro;
  }

  public void setFiltro(FiltroData filtro)
  {
    this.filtro = filtro;
  }

  public FiltroData getParametri()
  {
    return parametri;
  }

  public void setParametri(FiltroData extra)
  {
    this.parametri = extra;
  }

  public String getTablename()
  {
    return deleteFrom;
  }

  public void setTablename(String tablename)
  {
    this.deleteFrom = tablename;
  }

  public String getDeleteFrom()
  {
    return deleteFrom;
  }

  public void setDeleteFrom(String deleteFrom)
  {
    this.deleteFrom = deleteFrom;
  }

  public boolean isIgnoreCase()
  {
    return ignoreCase;
  }

  public void setIgnoreCase(boolean ignoreCase)
  {
    this.ignoreCase = ignoreCase;
  }

  public boolean isUseDistinct()
  {
    return useDistinct;
  }

  public void setUseDistinct(boolean useDistinct)
  {
    this.useDistinct = useDistinct;
  }

  public boolean isNativeLimit()
  {
    return nativeLimit;
  }

  public void setNativeLimit(boolean nativeLimit)
  {
    this.nativeLimit = nativeLimit;
  }

  public boolean isNativeOffset()
  {
    return nativeOffset;
  }

  public void setNativeOffset(boolean nativeOffset)
  {
    this.nativeOffset = nativeOffset;
  }

  public MacroResolver getMacroResolver()
  {
    return macroResolver;
  }

  public void setMacroResolver(MacroResolver macroResolver)
  {
    this.macroResolver = macroResolver;
  }
}
