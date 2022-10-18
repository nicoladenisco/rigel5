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
package org.rigel5.table.sql;

import com.workingdogs.village.Column;
import com.workingdogs.village.Record;
import com.workingdogs.village.Schema;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.om.NumberKey;
import org.apache.torque.om.StringKey;
import org.commonlib5.utils.ArrayMap;
import org.commonlib5.utils.StringOper;
import org.rigel5.SetupHolder;
import org.rigel5.db.DbUtils;
import org.rigel5.db.sql.FiltroData;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.exceptions.InvalidObjectException;
import org.rigel5.exceptions.MissingColumnException;
import org.rigel5.exceptions.MissingParameterException;
import org.rigel5.exceptions.MissingPrimaryKeyException;
import org.rigel5.table.FiltroListe;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelObjectTableModel;
import org.rigel5.table.RigelTableModel;

/**
 * <p>
 * Title: fonte dati per SQL libero.</p>
 * <p>
 * Description: Questo modello di tabella viene utilizzato
 * per SQL libero, non legato ai Peer.</p>
 * <p>
 * <b>ATTENZIONE:</b> questa versione funzione con PostgreSQL.
 * In futuro verra' elaborata una versione piu' compatibile con
 * altri database.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class SqlAbstractTableModel extends RigelObjectTableModel
{
  /** Logging */
  private static Log log = LogFactory.getLog(SqlAbstractTableModel.class);
  private boolean fetchAllField = false, initialized = false;
  public static final String ORACLETIMESTAMP = "oracle.sql.TIMESTAMP";

  public SqlAbstractTableModel()
  {
  }

  public int addColumn(String Caption, String Name, int tipo, int Size)
     throws Exception
  {
    SqlColumnDescriptor cd = new SqlColumnDescriptor(Caption, Name, Size);
    addColumn(cd);
    cd.setValClass(retObjTipoClass(tipo));
    cd.setDataType(tipo);
    return getColumnCount() - 1;
  }

  public int addColumn(String Caption, String Name, int tipo, int Size, boolean Editable)
     throws Exception
  {
    SqlColumnDescriptor cd = new SqlColumnDescriptor(Caption, Name, Size, Editable);
    addColumn(cd);
    cd.setValClass(retObjTipoClass(tipo));
    cd.setDataType(tipo);
    return getColumnCount() - 1;
  }

  public int addColumn(String Caption, String Name, int tipo, int Size,
     boolean Editable, boolean Visible)
     throws Exception
  {
    SqlColumnDescriptor cd = new SqlColumnDescriptor(Caption, Name, Size, Editable, Visible);
    addColumn(cd);
    cd.setValClass(retObjTipoClass(tipo));
    cd.setDataType(tipo);
    return getColumnCount() - 1;
  }

  public int addColumn(String Caption, String Name, int tipo, int Size, int width, boolean Editable)
     throws Exception
  {
    SqlColumnDescriptor cd = new SqlColumnDescriptor(Caption, Name, Size, Editable);
    cd.setWidth(width);
    addColumn(cd);
    cd.setValClass(retObjTipoClass(tipo));
    cd.setDataType(tipo);
    return getColumnCount() - 1;
  }

  public int addColumn(String Caption, String Name, int tipo, int Size, int width,
     boolean Editable, boolean Visible)
     throws Exception
  {
    SqlColumnDescriptor cd = new SqlColumnDescriptor(Caption, Name, Size, Editable, Visible);
    cd.setWidth(width);
    addColumn(cd);
    cd.setValClass(retObjTipoClass(tipo));
    cd.setDataType(tipo);
    return getColumnCount() - 1;
  }

  public int addColumn(String Name, int Pos, int Lun)
  {
    return addColumn(new SqlColumnDescriptor(Name, Pos, Lun));
  }

  protected void makeAutoSelect()
     throws Exception
  {
    String select = "";
    for(int j = 0; j < getColumnCount(); j++)
    {
      SqlColumnDescriptor cd = (SqlColumnDescriptor) getColumn(j);

      if(!cd.isCalcolato())
        select += "," + cd.getName();
    }

    if(select.length() == 0)
      throw new Exception("Nessuna colonna valida collegata: init va chiamata dopo addColumn(...).");

    select = select.substring(1);
    query.setSelect(select);
  }

  public void init(String _from)
     throws Exception
  {
    init(null, _from, null, null, false);
  }

  public void init(String _from, String _where)
     throws Exception
  {
    init(null, _from, _where, null, false);
  }

  public void init(String _select, String _from, String _where, String _orderby, boolean fetchRecords)
     throws Exception
  {
    if(query == null)
      query = makeQueryBuilder();

    query.setSelect(_select);
    query.setFrom(_from);
    query.setWhere(_where);
    query.setOrderby(_orderby);
    init(fetchRecords);
  }

  public void init(QueryBuilder qb, boolean fetchRecords)
     throws Exception
  {
    query = qb;
    init(fetchRecords);
  }

  @Override
  public void initFrom(RigelTableModel rtm)
     throws Exception
  {
    if(rtm instanceof SqlAbstractTableModel)
      initFrom(rtm, (SqlAbstractTableModel) rtm);
    else
      initFrom(rtm, null);
  }

  public void initFrom(RigelTableModel rtmColonne, SqlAbstractTableModel stmQuery)
     throws Exception
  {
    super.initFrom(rtmColonne);

    if(stmQuery != null)
    {
      QueryBuilder qb = stmQuery.getQuery();
      if(qb != null)
        init(qb, false);
    }
  }

  public void init(boolean fetchRecords)
     throws Exception
  {
    initialized = false;

    // controlla che almeno la clausola from sia presente
    if(!query.haveFrom())
      throw new MissingParameterException("La clausola FROM non puo' essere vuota.");

    // costruisce la stringa di select se necessario
    if(!query.haveSelect())
      makeAutoSelect();

    // cerca di fornire un ordinamento di default attraverso le chiavi primarie
    if(!query.haveOrderby())
    {
      String orderby = "";
      for(int i = 0; i < vColumn.size(); i++)
      {
        RigelColumnDescriptor r = (RigelColumnDescriptor) vColumn.get(i);
        if(r.isPrimaryKey())
          orderby += "," + r.getName() + " ASC";
      }

      if(!orderby.isEmpty())
        query.setOrderby(orderby.substring(1));
    }

    fetchDataFromdb(fetchRecords);
    initialized = true;
  }

  protected void fetchDataFromdb(boolean fetchRecords)
     throws Exception
  {
    SetupHolder.getConProd().runConnection((con) ->
    {
      List<Record> lsRecs = query.executeQuery(con, fetchRecords);
      Schema qSchema = query.getSchema();
      fetchStructureFromQuery(qSchema);

      clear();

      if(fetchRecords)
        super.rebind(lsRecs);
    });
  }

  private void fetchStructureFromQuery(Schema qSchema)
     throws Exception
  {
    ArrayList<String> vNotFound = new ArrayList<>(16);

    for(int j = 0; j < getColumnCount(); j++)
    {
      boolean found = false;
      SqlColumnDescriptor cd = (SqlColumnDescriptor) getColumn(j);
      cd.setModelIndex(j);
      cd.setCIndex(j + 1); // default: posizione di colonna (valido se select=*)

      if(cd.isForeignAuto())
        throw new InvalidObjectException("SqlTableModel non supporta la funzionalita Foreign Auto Bind");

      if(!cd.isCalcolato() && !cd.isAggregatoSql())
      {
        for(int i = 1; i <= qSchema.numberOfColumns(); i++)
        {
          Column col = qSchema.column(i);
          if(col.name().equalsIgnoreCase(cd.getName()))
          {
            cd.setCIndex(i);
            cd.setName(col.name()); // per avere il case esatto

            // se non specificato esplicitamente ricava un default ragionevole in base al tipo di dato
            if(cd.getDataType() == RigelColumnDescriptor.PDT_UNDEFINED)
            {
              cd.setDataType(retTipoSql(col.typeEnum()));
              cd.setValClass(retObjTipoClass(cd.getDataType()));
            }

            found = true;
            break;
          }
        }

        if(!found)
          vNotFound.add(cd.getName());
      }
    }

    // riporta colonne non trovate
    if(!vNotFound.isEmpty())
    {
      String colList = StringOper.join(vNotFound.iterator(), ", ", "");
      throw new MissingColumnException("Colonne " + colList + " non trovate negli oggetti passati!");
    }
  }

  public void rebind()
     throws Exception
  {
    init(true);
  }

  public void rebind(QueryBuilder qb)
     throws Exception
  {
    init(qb, true);
  }

  public void rebind(String _select, String _from, String _where, String _orderby)
     throws Exception
  {
    init(_select, _from, _where, _orderby, true);
  }

  public void saveData()
     throws Exception
  {
    saveData(0, getRowCount(), true);
  }

  public void saveData(final int start, final int numrec, final boolean delSelected)
     throws Exception
  {
    throw new UnsupportedOperationException("Salvataggio non supportato.");
  }

  /**
   * Funzione di utilita' per la conversione degli oggetti.
   * ES: se e' un oggetto java.util.Date lo converte in java.sql.Date
   * @param o
   * @return
   */
  public static Object adjObjSql(Object o)
  {
    if(o instanceof java.util.Date)
      return new java.sql.Date(((java.util.Date) (o)).getTime());
    if(o instanceof NumberKey)
      return ((NumberKey) (o)).getBigDecimal().intValue();
    if(o instanceof StringKey)
      return ((StringKey) (o)).toString();
    return o;
  }

  /**
   * Ritorna una stringa di identificazione univoca del record,
   * costruendola in base alla chiave primaria.
   * @param row il record
   * @return la stringa univoca del record
   * @throws java.lang.Exception
   */
  @Override
  public String createQueryKey(int row)
     throws Exception
  {
    String sKey = "";
    Record r = ((Record) (getRowRecord(row)));
    for(int i = 0; i < getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = getColumn(i);
      if(!cd.isCalcolato() && cd.isPrimaryKey())
        sKey += "!" + cd.getValueAsString(r);
    }
    return sKey.length() == 0 ? null : sKey.substring(1);
  }

  /**
   * Cancellazione fisica (DELETE FROM ...) di un record dalla
   * tabella (vedi getDeleteFrom di QueryBuilder).
   * Cancella dal database il record corrispondente alla chiave
   * univoca di selezione ottenuta da createQueryKey()
   * @param sKey la chiave di selezione
   * @return il numero di record cancellati (di solito 1 oppure 0 se c'e' errore)
   * @throws java.lang.Exception
   */
  @Override
  public int deleteByQueryKey(String sKey)
     throws Exception
  {
    String sDel = "";
    for(int i = 0; i < getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = getColumn(i);
      if(!cd.isCalcolato() && cd.isPrimaryKey())
        sDel += " AND " + getColumn(i).getName() + "=?";
    }

    if(sDel.length() == 0)
      throw new MissingPrimaryKeyException("Nessuna chiave primaria definita. Operazione non possibile.");

    String sSQL = query.queryForDelete(sDel.substring(5));

    log.debug("sSQL=" + sSQL);

    int rv = SetupHolder.getConProd().functionConnection((con) ->
    {
      try ( PreparedStatement ps = con.prepareStatement(sSQL))
      {
        int fld = 1;
        StringTokenizer stok = new StringTokenizer(sKey, "!");
        while(stok.hasMoreTokens())
        {
          String tok = stok.nextToken();
          ps.setString(fld++, tok);
        }

        return ps.executeUpdate();
      }
    });

    // reset numero totale record
    clearTotalRecords();

    return rv;
  }

  /**
   * Esegue la cancellazione logica dalla tabella (vedi getDeleteFrom di QueryBuilder).
   * Lo stato_rec della tabella viene portato a 10 (cancellato)
   * ed eventuali campi id_user e ult_modif aggiornati coerentemente.
   * Cancella dal database il record corrispondente alla chiave
   * univoca di selezione ottenuta da createQueryKey().
   * @param sKey stringa con i valori di chiave primaria separati da '!'
   * @param idUser id dell'utente che effettua la cancellazione
   * @return numero di record cancellati (di norma 1) o 0 se nessun record corrisponde
   * @throws Exception
   */
  public int deleteLogicalByQueryKey(String sKey, int idUser)
     throws Exception
  {
    String tableName = query.getDeleteFrom();
    return deleteLogicalByQueryKey(sKey, tableName, idUser, 10);
  }

  /**
   * Esegue la cancellazione logica su una tabella qualsiasi.
   * Lo stato_rec della tabella viene portato al valore indicato
   * ed eventuali campi id_user e ult_modif aggiornati coerentemente.
   * Cancella dal database il record corrispondente alla chiave
   * univoca di selezione ottenuta da createQueryKey().
   * @param sKey stringa con i valori di chiave primaria separati da '!'
   * @param tableName nome della tabella da cui cancellare i records
   * @param idUser id dell'utente che effettua la cancellazione
   * @param statoRec valore del campo stato_rec (10 di norma vuol dire cancellato)
   * @return numero di record cancellati (di norma 1) o 0 se nessun record corrisponde
   * @throws Exception
   */
  public int deleteLogicalByQueryKey(String sKey, String tableName, int idUser, int statoRec)
     throws Exception
  {
    QueryBuilder qb = SetupHolder.getQueryBuilder();
    qb.setFrom(tableName);

    int j = 0, rv = 0;
    String sVals[] = StringOper.split(sKey, '!');
    FiltroData fd = new FiltroData();

    for(int i = 0; i < getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = getColumn(i);
      if(!cd.isCalcolato() && cd.isPrimaryKey())
      {
        if(j >= sVals.length)
          throw new MissingPrimaryKeyException("Chiave primaria non corrispondente. Operazione non possibile.");

        fd.addWhere(cd, SqlEnum.EQUAL, sVals[j++]);
      }
    }

    if(!fd.haveWhere())
      throw new MissingPrimaryKeyException("Nessuna chiave primaria definita. Operazione non possibile.");

    rv = SetupHolder.getConProd().functionConnection((con) ->
    {
      boolean haveStatoRec = false;

      ArrayMap<String, Integer> tipi = DbUtils.getTipiColonne(con, tableName);
      for(Map.Entry<String, Integer> entry : tipi.entrySet())
      {
        String colName = entry.getKey();
        Integer tipo = entry.getValue();

        if(StringOper.isEquNocase("stato_rec", colName))
        {
          fd.addUpdate(RigelColumnDescriptor.PDT_INTEGER, colName, statoRec);
          haveStatoRec = true;
        }

        if(StringOper.isEquNocase("id_user", colName))
          fd.addUpdate(RigelColumnDescriptor.PDT_INTEGER, colName, idUser);

        if(StringOper.isEquNocase("ult_modif", colName))
          fd.addUpdate(RigelColumnDescriptor.PDT_TIMESTAMP, colName, new java.util.Date());
      }

      if(!haveStatoRec)
        throw new MissingColumnException("Cancellazione logica non possibile su tabella " + tableName);

      String sSQL = qb.queryForUpdate(fd);
      log.info("Delete sSQL=" + sSQL);

      try ( Statement st = con.createStatement())
      {
        return st.executeUpdate(sSQL);
      }
    });

    // reset numero totale record
    clearTotalRecords();
    return rv;
  }

  /**
   * Recupera il numero totale di records per la query impostata.
   * La chiamata viene passata al QueryBuilder.
   * @return numero totale dei records.
   * @throws Exception
   */
  @Override
  public long getTotalRecords()
     throws Exception
  {
    return SetupHolder.getConProd().functionConnection((con) -> query.getTotalRecords(con));
  }

  /**
   * Recupera il numero totale di records per la query e per il filtro.
   * La chiamata viene passata al QueryBuilder.
   * @param fl filtro applicato
   * @return numero totale dei records.
   * @throws Exception
   */
  @Override
  public long getTotalRecords(FiltroListe fl)
     throws Exception
  {
    return SetupHolder.getConProd().functionConnection((con) -> query.getTotalRecords(con, fl));
  }

  @Override
  public boolean isInitalized()
  {
    return initialized;
  }

  @Override
  public boolean isNewRecord(int row)
  {
    return false;
  }

  @Override
  public boolean isCellEditable(int row, int col)
  {
    return getColumn(col).isEditable() && !getColumn(col).isPrimaryKey();
  }

  private static final String mdls = new String();
  private static final Boolean mdlb = false;
  private static final Integer mdli = 0;
  private static final Float mdlf = 0.0f;
  private static final Double mdld = 0.0;
  private static final java.util.Date mdla = new java.util.Date();

  public static Object retObjTipo(int tipo)
     throws Exception
  {
    switch(tipo)
    {
      default:
      case RigelColumnDescriptor.PDT_UNDEFINED:
        throw new Exception("Tipo PDT_UNDEFINED non gestibile");
      case RigelColumnDescriptor.PDT_STRING:
        return mdls;
      case RigelColumnDescriptor.PDT_BOOLEAN:
        return mdlb;
      case RigelColumnDescriptor.PDT_INTEGER:
        return mdli;
      case RigelColumnDescriptor.PDT_FLOAT:
        return mdlf;
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
        return mdld;
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
      case RigelColumnDescriptor.PDT_TIME:
        return mdla;
      case RigelColumnDescriptor.PDT_STRINGKEY:
        throw new Exception("Tipo PDT_STRINGKEY non gestibile");
      case RigelColumnDescriptor.PDT_NUMBERKEY:
        throw new Exception("Tipo PDT_NUMBERKEY non gestibile");
    }
  }

  public static Class retObjTipoClass(int tipo)
     throws Exception
  {
    return retObjTipo(tipo).getClass();
  }

  public static int retSqlTipo(int tipo)
  {
    switch(tipo)
    {
      default:
      case RigelColumnDescriptor.PDT_UNDEFINED:
        return Types.OTHER;
      case RigelColumnDescriptor.PDT_STRING:
        return Types.VARCHAR;
      case RigelColumnDescriptor.PDT_BOOLEAN:
        return Types.BOOLEAN;
      case RigelColumnDescriptor.PDT_INTEGER:
        return Types.INTEGER;
      case RigelColumnDescriptor.PDT_FLOAT:
        return Types.FLOAT;
      case RigelColumnDescriptor.PDT_DOUBLE:
        return Types.DOUBLE;
      case RigelColumnDescriptor.PDT_MONEY:
        return Types.NUMERIC;
      case RigelColumnDescriptor.PDT_DATE:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
      case RigelColumnDescriptor.PDT_TIME:
        return Types.DATE;
      case RigelColumnDescriptor.PDT_STRINGKEY:
        return Types.VARCHAR;
      case RigelColumnDescriptor.PDT_NUMBERKEY:
        return Types.INTEGER;
    }
  }

  public static int retTipoSql(int tipo)
  {
    switch(tipo)
    {
      default:
      case Types.VARCHAR:
        return RigelColumnDescriptor.PDT_STRING;

      case Types.BOOLEAN:
        return RigelColumnDescriptor.PDT_BOOLEAN;

      case Types.INTEGER:
        return RigelColumnDescriptor.PDT_INTEGER;

      case Types.FLOAT:
        return RigelColumnDescriptor.PDT_FLOAT;

      case Types.DOUBLE:
        return RigelColumnDescriptor.PDT_DOUBLE;

      case Types.NUMERIC:
        return RigelColumnDescriptor.PDT_MONEY;

      case Types.DATE:
        return RigelColumnDescriptor.PDT_DATE;
    }
  }

  public void setFetchAllField(boolean fetchAllField)
  {
    this.fetchAllField = fetchAllField;
  }

  public boolean isFetchAllField()
  {
    return fetchAllField;
  }
}
