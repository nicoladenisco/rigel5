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
package org.rigel5.table.peer;

import com.workingdogs.village.*;
import java.beans.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.*;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.ForeignKeyMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.Persistent;
import org.apache.torque.om.StringKey;
import org.commonlib5.utils.StringOper;
import org.rigel5.SetupHolder;
import org.rigel5.db.DbUtils;
import org.rigel5.db.torque.PeerTransactAgent;
import org.rigel5.db.torque.TableMapHelper;
import org.rigel5.exceptions.MissingColumnException;
import org.rigel5.exceptions.MissingParameterException;
import org.rigel5.glue.PeerObjectSaver;
import org.rigel5.table.FiltroListe;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelObjectTableModel;
import org.rigel5.table.RigelTableModel;

/**
 * TableModel specializzato per la manipolazione di array di oggetti Torque.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class PeerAbstractTableModel extends RigelObjectTableModel
{
  protected Class beanClass = null;
  protected Class beanPeerClass = null;
  protected PropertyDescriptor[] props = null;
  protected Method doDeleteM = null;
  protected TableMap map;
  protected TableMapHelper maph;
  protected Criteria defaultOrderCriteria = null;
  /** Logging */
  private static Log log = LogFactory.getLog(PeerAbstractTableModel.class);

  public int addColumn(String Caption, String Name, int Size)
  {
    return addColumn(new PeerColumnDescriptor(Caption, Name, Size));
  }

  public int addColumn(String Caption, String Name, int Size, boolean Editable)
  {
    return addColumn(new PeerColumnDescriptor(Caption, Name, Size, Editable));
  }

  public int addColumn(String Caption, String Name, int Size,
     boolean Editable, boolean Visible)
  {
    return addColumn(new PeerColumnDescriptor(Caption, Name, Size, Editable, Visible));
  }

  public int addColumn(String Caption, String Name, int Size, int width, boolean Editable)
  {
    PeerColumnDescriptor cd = new PeerColumnDescriptor(Caption, Name, Size, Editable);
    cd.setWidth(width);
    return addColumn(cd);
  }

  public int addColumn(String Caption, String Name, int Size, int width,
     boolean Editable, boolean Visible)
  {
    PeerColumnDescriptor cd = new PeerColumnDescriptor(Caption, Name, Size, Editable, Visible);
    cd.setWidth(width);
    return addColumn(cd);
  }

  public int addColumn(String Name, int Pos, int Lun)
  {
    return addColumn(new PeerColumnDescriptor(Name, Pos, Lun));
  }

  public void init(String peerClassName)
     throws Exception
  {
    init(Class.forName(peerClassName));
  }

  public void init(Object bean)
     throws Exception
  {
    init(Arrays.asList(bean));
  }

  public void init(List objList)
     throws Exception
  {
    if(objList.isEmpty())
      return;

    if(!(objList.get(0) instanceof Persistent))
      throw new Exception("il vettore deve contenere oggetti om Torque (figli di Persistent)");

    init(objList.get(0).getClass());

    super.rebind(objList);
  }

  public void init(Class peerClass)
     throws Exception
  {
    if(beanClass != null && beanClass.equals(peerClass))
      return;

    beanClass = peerClass;

    if(!Persistent.class.isAssignableFrom(peerClass))
      throw new Exception("Sono ammessi solo oggetti om Torque (figli di Persistent)");

    if(getColumnCount() == 0)
      throw new Exception("nessuna colonna caricata: binding non possibile");

    if(query == null)
      query = makeQueryBuilder();

    //Introspector.flushCaches();
    props = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();

    // cerca il peer
    String nomePeer = beanClass.getName() + "Peer";
    beanPeerClass = Class.forName(nomePeer);

    // recupera il TableMap dall'oggetto peer
    Method getTableMapM = beanPeerClass.getMethod("getTableMap");
    map = (TableMap) getTableMapM.invoke(null);
    maph = new TableMapHelper(map);

    //dumpProps();
    ArrayList vNotFound = new ArrayList();

    for(int j = 0; j < getColumnCount(); j++)
    {
      PeerColumnDescriptor cd = (PeerColumnDescriptor) getColumn(j);
      cd.setModelIndex(j);

      if(cd.isAggregatoSql())
        throw new Exception("Le funzioni di aggregazione SQL non possono essere utilizzate con i peer.");

      if(cd.isCalcolato())
        continue;

      ColumnMap cmap = null;
      PropertyDescriptor prop = null;
      if((prop = findPeerProperty(cd.getName())) != null)
      {
        cd.setName(prop.getName()); // corregge il case
        cd.setPropDescr(prop);

        if((cmap = maph.getCampo(prop.getName())) != null)
        {
          cd.setPrimaryKey(cmap.isPrimaryKey());
          cd.setCmap(cmap);
        }
        else
          log.debug("ColumMap non trovata per la colonna " + cd.getCaption() + " " + cd.getName());
      }
      else
      {
        vNotFound.add(cd);
        continue;
      }

      if(cd.isForeignAuto())
      {
        // pulizia di sicurezza
        cd.setForeignTabella(null);
        cd.setForeignCampoLink(null);

        if(cmap == null)
        {
          log.error("Foreign auto non possibile per colonna " + cd.getName() + "; colonna non trovata nella tablemap.");
        }
        else
        {
          if(!impostaForeignAuto(cmap, cd))
          {
          }
        }

        // controllo finale ed eventuale disattivazione del foreign mode
        if(cd.getForeignCampoDisplay() == null)
        {
          // disattiva modalità foreign per la colonna
          cd.setForeignMode(RigelColumnDescriptor.DISP_FLD_ONLY);
          log.error("Foreign auto non possible per colonna " + cd.getName()
             + "; foreign mode disattivato.");
        }
      }
    }

    // riporta colonne non trovate
    if(!vNotFound.isEmpty())
    {
      String colList = "";
      for(Iterator itNf = vNotFound.iterator(); itNf.hasNext();)
      {
        RigelColumnDescriptor cd = (RigelColumnDescriptor) itNf.next();
        colList += "," + cd.getName();
      }

      throw new MissingColumnException("Colonne " + colList.substring(1) + " non trovate negli oggetti passati!");
    }

    // costruisce un criteria per l'ordinamento di default usando le chiavi primarie
    String tableName = maph.getNomeTabella();
    defaultOrderCriteria = new Criteria();
    Iterator itrPk = maph.getPrimaryKeys();
    while(itrPk.hasNext())
    {
      ColumnMap cm = (ColumnMap) itrPk.next();
      defaultOrderCriteria.addAscendingOrderByColumn(cm);
    }
  }

  protected boolean impostaForeignAuto(ColumnMap cmap, PeerColumnDescriptor cd)
     throws Exception
  {
    String localColName = cmap.getColumnName();
    String foreignColName, foreignTableName;

    ForeignKeyMap fk = maph.findForeignKeyByColumnName(localColName);
    if(fk == null || fk.getColumns().size() != 1)
    {
      // disattiva modalità foreign per la colonna
      cd.setForeignMode(RigelColumnDescriptor.DISP_FLD_ONLY);
      log.error("Foreign auto non possibile per colonna " + cd.getName()
         + "; il campo " + localColName + " non trovato nel databasemap oppure chiave composta (non gestibile).");
      return false;
    }
    else
    {
      ForeignKeyMap.ColumnPair fcp = fk.getColumns().get(0);
      foreignTableName = fk.getForeignTableName();
      foreignColName = fcp.getForeign().getColumnName();

      TableMap tmRelated = fk.getForeignTable();
      if(tmRelated == null)
      {
        // disattiva modalità foreign per la colonna
        cd.setForeignMode(RigelColumnDescriptor.DISP_FLD_ONLY);
        log.error("Foreign auto non possibile per colonna " + cd.getName()
           + "; tabella collegata " + foreignTableName + " non trovata nel databasemap.");
        return false;
      }
      else
      {
        // imposta nome tabella completo di eventuale schema
        foreignTableName = tmRelated.getFullyQualifiedTableName();

        if(cd.getForeignCampoDisplay() == null)
        {
          // campo di visualizzazione non specificato: cerca fra quelli
          // caricati nel setup holder nell'ordine indicato (vedi autoForeingColumns)
          TableMapHelper tmhRelated = new TableMapHelper(tmRelated);
          ColumnMap cmRelView = findAutoForeignColumn(tmhRelated);
          if(cmRelView == null)
          {
            // disattiva modalità foreign per la colonna
            cd.setForeignMode(RigelColumnDescriptor.DISP_FLD_ONLY);
            log.error("Colonna descrittiva non trovata su tabella foreign " + tmRelated.getName()
               + ". Verificare tabella o settaggio in autoForeignColumns in SetupHolder.");
            return false;
          }
          else
          {
            cd.setForeignTabella(foreignTableName);
            cd.setForeignCampoLink(foreignColName);
            cd.setForeignCampoDisplay(cmRelView.getColumnName());
            log.info("Foreign auto attivato per colonna " + cd.getName());
            return true;
          }
        }
        else
        {
          // il display è specificato quindi valido
          cd.setForeignTabella(foreignTableName);
          cd.setForeignCampoLink(foreignColName);
          log.info("Foreign auto attivato per colonna " + cd.getName());
          return true;
        }
      }
    }
  }

  /**
   * Ritorna la colonna di visualizzazione sulla tabella
   * collegata, cercando i possibili nomi di colonna specificati
   * nel SetupHolder (getAutoForeingColumns).
   * @param tmh
   * @return
   */
  protected ColumnMap findAutoForeignColumn(TableMapHelper tmh)
  {
    String[] sfc = SetupHolder.getAutoForeingColumns();
    if(sfc == null)
      return null;

    ColumnMap mp = null;
    for(int j = 0; j < sfc.length; j++)
    {
      if((mp = tmh.getCampo(StringOper.okStr(sfc[j]))) != null)
        return mp;
    }

    return null;
  }

  /**
   * Ritorna il PropertyDescriptor relativo alla colonna
   * indicata. La ricerca NON e' case sensitive.
   * @param colName nome del campo della colonna
   * @return descrittore altrimenti null se non trovato
   */
  public PropertyDescriptor findPeerProperty(String colName)
  {
    for(int i = 0; i < props.length; i++)
    {
      if(props[i].getName().equalsIgnoreCase(colName))
        return props[i];
    }
    return null;
  }

  @Override
  public void initFrom(RigelTableModel rtm)
     throws Exception
  {
    if(rtm instanceof PeerAbstractTableModel)
    {
      initFrom(rtm, (PeerAbstractTableModel) rtm);
    }
    else
    {
      initFrom(rtm, null);
    }
  }

  public void initFrom(RigelTableModel rtmColonne, PeerAbstractTableModel ptmQuery)
     throws Exception
  {
    super.initFrom(rtmColonne);

    if(ptmQuery != null)
    {
      beanClass = ptmQuery.beanClass;
      beanPeerClass = ptmQuery.beanPeerClass;
      props = ptmQuery.props;
      map = ptmQuery.map;
      maph = ptmQuery.maph;
      query = ptmQuery.query;
    }
  }

  public void autoInit(String peerClassName)
     throws Exception
  {
    Persistent objModello = (Persistent) (Class.forName(peerClassName).newInstance());
    autoInit(objModello);
  }

  public void autoInit(Object obj)
     throws Exception
  {
    autoInit(Arrays.asList(obj));
  }

  public void autoInit(List objList)
     throws Exception
  {
    super.rebind(objList);

    //if(getColumnCount() != 0)
    //  throw new Exception("Colonne gia' bindate: nessuna colonna deve essere presente.");
    if(query == null)
      query = makeQueryBuilder();

    beanClass = objList.get(0).getClass();
    props = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();

    // cerca il peer
    String nomePeer = beanClass.getName() + "Peer";
    beanPeerClass = Class.forName(nomePeer);

    // recupera il TableMap dall'oggetto peer
    Method getTableMapM = beanPeerClass.getMethod("getTableMap");
    map = (TableMap) getTableMapM.invoke(null);
    maph = new TableMapHelper(map);

    ColumnMap[] cmaps = map.getColumns();

    Arrays.sort(cmaps, (obj1, obj2) -> StringOper.compare(obj1.getColumnName(), obj2.getColumnName()));

    // costruisce eventuale mappa sostituiva dei nomi
    Map<String, String> nameMap = aiGetNameMap(map.getName());

    for(int i = 0; i < cmaps.length; i++)
      vColumn.add(aiBuildCD(i, cmaps[i], nameMap));

    init(objList);
  }

  public PeerColumnDescriptor aiBuildCD(int col, ColumnMap cmap, Map<String, String> nameMap)
     throws Exception
  {
    String colname = StringUtils.remove(cmap.getColumnName(), '_');
    String caption = cmap.getColumnName();
    if(nameMap != null)
      if((caption = (String) (nameMap.get(colname))) == null)
        caption = cmap.getColumnName();

    PeerColumnDescriptor cd = new PeerColumnDescriptor(caption, colname, 100, true);

    PropertyDescriptor prop = null;
    if((prop = findPeerProperty(colname)) != null)
    {
      cd.setName(prop.getName()); // corregge il case
      cd.setPropDescr(prop);
    }

    cd.setPrimaryKey(cmap.isPrimaryKey());
    cd.setCmap(cmap);

    String localColName = cmap.getColumnName();
    String foreignColName, foreignTableName;

    ForeignKeyMap fk = maph.findForeignKeyByColumnName(localColName);
    if(fk != null && fk.getColumns().size() == 1)
    {
      ForeignKeyMap.ColumnPair fcp = fk.getColumns().get(0);
      foreignTableName = fk.getForeignTableName();
      foreignColName = fcp.getForeign().getColumnName();
      TableMap tmRelated = fk.getForeignTable();

      aiForceLoading(foreignTableName);
      if(tmRelated != null)
      {
        TableMapHelper tmhRelated = new TableMapHelper(tmRelated);
        ColumnMap cmRelView = findAutoForeignColumn(tmhRelated);
        if(cmRelView != null)
        {
          cd.AttivaForeignMode(PeerColumnDescriptor.DISP_FLD_DESCR,
             foreignTableName, foreignColName,
             cmRelView.getColumnName());

          // ATTENZIONE: per le elaborazioni successive
          // viene considerata la colonna della tabella collegata
          cmap = cmRelView;
        }
        else
        {
          log.info("Colonna collegata " + foreignTableName + ".DESCRIZIONE(o RAG_SOC) non trovata!");
        }
      }
      else
      {
        log.info("Tabella collegata " + foreignTableName + " non trovata!");
      }
    }

    int width = 10;
    int htmlAlign = PeerColumnDescriptor.HTML_ALIGN_DEFAULT;
    int dataType = PeerColumnDescriptor.checkForType(cmap.getType().getClass().getName());

    switch(dataType)
    {
      case PeerColumnDescriptor.PDT_BOOLEAN:
        width = 2;
        break;
      case PeerColumnDescriptor.PDT_INTEGER:
      case PeerColumnDescriptor.PDT_NUMBERKEY:
        htmlAlign = PeerColumnDescriptor.HTML_ALIGN_CENTER;
        width = 5;
        break;
      case PeerColumnDescriptor.PDT_FLOAT:
        htmlAlign = PeerColumnDescriptor.HTML_ALIGN_RIGHT;
        width = 6;
        break;
      case PeerColumnDescriptor.PDT_DOUBLE:
      case PeerColumnDescriptor.PDT_MONEY:
        htmlAlign = PeerColumnDescriptor.HTML_ALIGN_RIGHT;
        width = 8;
        break;
      case PeerColumnDescriptor.PDT_DATE:
      case PeerColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case PeerColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case PeerColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case PeerColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case PeerColumnDescriptor.PDT_TIMESTAMP:
      case PeerColumnDescriptor.PDT_TIME:
        htmlAlign = PeerColumnDescriptor.HTML_ALIGN_CENTER;
        width = 10;
        break;
      case PeerColumnDescriptor.PDT_STRING:
      case PeerColumnDescriptor.PDT_STRINGKEY:
        htmlAlign = PeerColumnDescriptor.HTML_ALIGN_LEFT;
        width = cmap.getSize();
        break;
    }

    cd.setDataType(dataType);
    cd.setSize(width);
    cd.setHtmlAlign(htmlAlign);
    cd.setModelIndex(col);

    return cd;
  }

  /**
   * Mappa sostitutiva dei nomi colonna.
   * In una classe derivata questa funzione può restituire
   * una mappa di nomi alternativi per le colonne.
   * L'implemntazione di default restituisce null.
   * @param nomeTabella nome della tabella di cui sotruire la mappa
   * @return mappa dei nomi colonna da sostituire
   * @throws Exception
   */
  public Map<String, String> aiGetNameMap(String nomeTabella)
     throws Exception
  {
    return null;
  }

  /**
   * Forza il caricamento delle map e dei builder per la tabella indicata.
   * Assume che i peer siano tutti nella stessa path (it.xxx.yyy....)
   * dell'oggetto o dell'array passato ad autoInit.
   * @param nomeTabella nome della tabella (NO UNDERSCORE NEL NOME)
   */
  public void aiForceLoading(String nomeTabella)
  {
    try
    {
      String beanName = beanClass.getName();
      int p = beanName.lastIndexOf(".");
      if(p == -1)
        throw new Exception("Nome oggetti non corretto [" + beanName + "]");

      String nomePeer = beanName.substring(0, p) + "."
         + removeUnderScoresAndDots(nomeTabella.toLowerCase())
         + "Peer";

      Method m = Class.forName(nomePeer).getMethod("getMapBuilder");
      m.invoke(null);
    }
    catch(Exception ex)
    {
      log.error("Mancato precaricamento tableMap per la tabella " + nomeTabella, ex);
    }
  }

  /**
   * Remove Underscores from a string and replaces first
   * Letters with Capitals. foo_bar becomes FooBar
   */
  public static String removeUnderScoresAndDots(String data)
  {
    StringBuilder out = new StringBuilder();
    String[] arStr = data.split("_|\\.");
    for(int i = 0; i < arStr.length; i++)
    {
      String element = arStr[i];
      out.append(StringUtils.capitalize(element));
    }
    return out.toString();
  }

  public void saveData()
     throws Exception
  {
    saveData(0, getRowCount(), true, null);
  }

  public void saveData(PeerObjectSaver pos)
     throws Exception
  {
    saveData(0, getRowCount(), true, pos);
  }

  public void saveData(int start, int numrec, boolean delSelected)
     throws Exception
  {
    saveData(start, numrec, delSelected, null);
  }

  public synchronized void saveData(final int start, final int numrec,
     final boolean delSelected, final PeerObjectSaver pos)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent()
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        saveData(start, numrec, dbCon, delSelected, pos);
        return true;
      }
    };

    ta.runNow();
  }

  /**
   * Da ridefinire in classi derivate: cancella l'oggetto indicato.
   * A seconda della necessità la cancellazione può essere logica
   * o fisica del record.
   * Il record cancellato viene comunque salvato se l'oggetto è stato
   * modificato. Per bloccarne il salvataggio usare obj.setModified(false).
   * @param obj oggetto da cancellare
   * @param con connessione di riferimento al db
   * @throws Exception
   */
  public void deleteObject(Persistent obj, Connection con)
     throws Exception
  {
  }

  public void saveData(int start, int numrec, Connection dbCon, boolean delSelected)
     throws Exception
  {
    saveData(start, numrec, dbCon, delSelected, null);
  }

  public void saveData(int start, int numrec, Connection dbCon,
     boolean delSelected, PeerObjectSaver pos)
     throws Exception
  {
    Vector toDel = new Vector();
    while(numrec-- > 0 && start < getRowCount())
    {
      Persistent obj = (Persistent) (getRowRecord(start));
      if(delSelected && isRowDeleted(start))
      {
        deleteObject(obj, dbCon);
        toDel.add(obj);
      }

      if(pos == null)
      {
        if(obj.isModified())
          obj.save(dbCon);
      }
      else
      {
        pos.salva(obj, dbCon, isRowDeleted(start) ? 10 : 0);
      }

      start++;
    }

    if(delSelected && !toDel.isEmpty())
    {
      getVBuf().removeAll(toDel);
      rebind(getVBuf());
    }
  }

  public void setTableMap(TableMap m)
  {
    map = m;
  }

  public TableMap getTableMap()
  {
    return map;
  }

  /**
   * Recupera il numero totale dei records nella tabella
   * indicata; utilizza il map builder passato per identificare
   * il nome della tabella e quindi richiede al db il conteggio
   * dei records.
   * @return numero di records
   * @throws java.lang.Exception
   */
  @Override
  public long getTotalRecords()
     throws Exception
  {
    if(totalRecords == -1)
    {
      String nomeTabella = getTableMap().getName();

      if(nomeTabella == null)
        throw new Exception("Oggetto map builder non conforme!");

      List v = DbUtils.executeQuery("SELECT COUNT(*) AS NUMRECORDS FROM " + nomeTabella);
      if(!v.isEmpty())
        totalRecords = ((Record) (v.get(0))).getValue(1).asLong();
    }

    return totalRecords;
  }

  /**
   * Ritorna il conteggio totale dei records secondo il filtro impostato.
   * @param fl
   * @return numero di records
   * @throws java.lang.Exception
   */
  @Override
  public long getTotalRecords(FiltroListe fl)
     throws Exception
  {
    Criteria cSelezione = (Criteria) (fl.getOggFiltro());
    if(cSelezione == null)
      return getTotalRecords();

    cSelezione = (Criteria) cSelezione.clone();
    cSelezione.setOffset(fl.getOffset());
    cSelezione.setLimit(fl.getLimit());
    cSelezione.setIgnoreCase(fl.isIgnoreCase());

    return getTotalRecords(cSelezione);
  }

  /**
   * Ritorna il conteggio totale dei records secondo il filtro impostato.
   * @param cSelezione
   * @return numero di record
   * @throws java.lang.Exception
   */
  public long getTotalRecords(Criteria cSelezione)
     throws Exception
  {
    if(cSelezione == null)
      return getTotalRecords();

    cSelezione.setIgnoreCase(true);
    String critSql = DbUtils.createQueryString(cSelezione);

    int pos = critSql.indexOf(" FROM ");
    if(pos == -1)
      return getTotalRecords();

    int posOrder = critSql.indexOf(" ORDER BY ");
    if(posOrder != -1)
      critSql = critSql.substring(0, posOrder);

    String sSQL = "SELECT COUNT(*) AS NUMRECORDS " + critSql.substring(pos);

//   * RIMOSSO CODICE CON CACHE (02/02/2012)
//   * ECCESSIVAMENTE POCO USER FRIENDLY (LA PAGINAZIONE NON SI AGGIORNA)
    //log.debug("sSQL="+sSQL);
    List v = DbUtils.executeQuery(sSQL);
    if(!v.isEmpty())
      totalRecordsFilter = ((Record) (v.get(0))).getValue(1).asLong();

    return totalRecordsFilter;
  }

  @Override
  public boolean isNewRecord(int row)
  {
    return super.isInitalized() ? ((Persistent) (getRowRecord(row))).isNew() : false;
  }

  @Override
  public boolean isCellEditable(int row, int col)
  {
    if(((Persistent) (getRowRecord(row))).isNew())
      return getColumn(col).isEditable();

    return getColumn(col).isEditable() && !getColumn(col).isPrimaryKey();
  }

  /**
   * Ritorna una stringa di identificazione univoca del record,
   * costruendola in base alla chiave primaria.
   * @param row il record
   * @return la stringa univoca del record
   */
  @Override
  public String createQueryKey(int row)
     throws Exception
  {
    return ((Persistent) (getRowRecord(row))).getPrimaryKey().toString();
  }

  /**
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
    if(!StringOper.isOkStr(sKey))
      throw new MissingParameterException("Selettore record da cancellare non definito. Operazione non possibile.");

    if(doDeleteM == null)
    {
      // recupera il metodo per la cancellazione
      doDeleteM = beanPeerClass.getMethod("doDelete", ObjectKey.class);
    }

    doDeleteM.invoke(null, new StringKey(sKey));
    fireTableDataChanged();

    // reset numero totale record
    clearTotalRecords();
    return 1;
  }

  /**
   * Ripete l'operazione di attach sulla tabella.
   * Viene chiamata all'interno della rebind o comunque
   * quando cambia la disposizione delle colonne o il numero
   * di records contenuto in questo tablemodel.
   */
  @Override
  abstract public void reAttach();

  /**
   * Collega un nuovo oggetto a questo tablemodel
   * distruggendo il set di oggetti precente.
   * Se bean è null il tablemodel rimane vuoto (isInitialized() ritornerà false).
   * Viene eseguita anche una reAttach() per reinizializzare
   * la tabella collegata.
   * @param bean nuovo oggetto da gestire
   */
  @Override
  public void rebind(Object bean)
  {
    clear();
    if(bean != null)
    {
      if(!(bean instanceof Persistent))
        throw new RuntimeException("Rebint to object not descending by 'Persistent'.");

      super.rebind(bean);
      reAttach();
    }

    fireTableDataChanged();
  }

  /**
   * Collega una nuova lista di oggetti a questo tablemodel
   * distruggendo il set di oggetti precente.
   * Se newRow è null oppure vuoto il tablemodel
   * rimane vuoto (isInitialized() ritornerà false).
   * Viene eseguita anche una reAttach() per reinizializzare
   * la tabella collegata.
   * @param newRows nuova lista di oggetti da gestire
   */
  @Override
  public void rebind(List newRows)
  {
    clear();
    if(newRows != null && !newRows.isEmpty())
    {
      if(!(newRows.get(0) instanceof Persistent))
        throw new RuntimeException("Rebint to object not descending by 'Persistent'.");

      super.rebind(newRows);
      reAttach();
    }

    fireTableDataChanged();
  }

  public void dumpProps()
  {
    for(int i = 0; i < props.length; i++)
    {
      PropertyDescriptor pd = props[i];
      try
      {
        log.debug(
           i + ". " + pd.getName() + " "
           + (pd.getReadMethod() != null ? pd.getReadMethod().getName() + " "
           + pd.getReadMethod().getParameterTypes().length + " "
           + pd.getReadMethod() : "INDEFINITO"));
      }
      catch(Exception ex)
      {
        log.debug(ex.getMessage());
      }
    }
  }

  public Class getBeanClass()
  {
    return beanClass;
  }

  public Class getBeanPeerClass()
  {
    return beanPeerClass;
  }

  public Criteria getDefaultOrderCriteria()
  {
    return defaultOrderCriteria;
  }

  public void setDefaultOrderCriteria(Criteria defaultOrderCriteria)
  {
    this.defaultOrderCriteria = defaultOrderCriteria;
  }
}
