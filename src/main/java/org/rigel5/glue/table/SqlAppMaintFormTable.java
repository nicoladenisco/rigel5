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
package org.rigel5.glue.table;

import com.workingdogs.village.Record;
import com.workingdogs.village.TableDataSet;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.rigel5.db.sql.FiltroData;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.db.torque.PeerTransactAgent;
import org.rigel5.glue.RecordObjectSaver;
import org.rigel5.glue.validators.Validator;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.FormTable;
import org.rigel5.table.html.RigelHtmlPage;
import org.rigel5.table.html.hEditTable;
import org.rigel5.table.peer.html.PeerTableModel;
import org.rigel5.table.sql.html.SqlTableModel;
import org.rigel5.table.sql.html.SqlWrapperFormHtml;

/**
 * Tabella per la visualizzazione di forms.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlAppMaintFormTable extends FormTable
{
  protected SqlWrapperFormHtml wf;
  protected Record objInEdit;
  protected RecordObjectSaver ros;

  /**
   * Costruttore protetto solo per classi derivate.
   * La classe derivata userà il metodo init() per l'inizializzazione.
   */
  public SqlAppMaintFormTable()
  {
    super(null);
  }

  public void init(String sID, SqlWrapperFormHtml wf, RecordObjectSaver ros)
     throws Exception
  {
    this.id = sID;
    this.wf = wf;
    this.ros = ros;

    // imposta numero colonne
    if(wf.getNumColonne() != 0)
      setColonne(wf.getNumColonne());

    String tableName = wf.getTM().getQuery().getDeleteFrom();
    PeerTransactAgent.executeReadonly((con) -> ros.init(tableName, con));
  }

  public void setUserInfo(int idUser, boolean isAdmin)
     throws Exception
  {
    ros.setUserInfo(idUser, isAdmin);
  }

  public Record findElementoEdit(HttpSession session, Map param)
     throws Exception
  {
    String newType = (String) param.get("new");

    if(newType != null && newType.equals("1"))
      return null;

    if(newType != null && newType.equals("2") && objInEdit != null)
      return objInEdit;

    // produce i parametri di selezione
    FiltroData c = wf.makeCriteriaEditRiga(param);

    List<Record> v = getRecords(c);
    return v == null || v.isEmpty() ? null : v.get(0);
  }

  public Record findElemento(HttpSession session, Map param)
     throws Exception
  {
    // produce i parametri di selezione
    FiltroData c = wf.makeCriteriaEditRiga(param);

    List<Record> v = getRecords(c);
    return v == null || v.isEmpty() ? null : v.get(0);
  }

  public List<Record> getRecords(FiltroData c)
     throws Exception
  {
    SqlTableModel tm = (SqlTableModel) getTM();
    QueryBuilder qb = tm.getQuery();
    String tableName = qb.getDeleteFrom();

    return PeerTransactAgent.executeReturnReadonly((con) ->
    {
      try ( TableDataSet td = new TableDataSet(con, tableName))
      {
        td.where(qb.makeFiltroWhere(c));
        return td.fetchAllRecords();
      }
    });
  }

  /**
   * Recupera l'oggetto in editing salvando i dati del form
   * ed eventualmente salvando il database.
   * @param session
   * @param param
   * @param saveDB
   * @param saveTmp
   * @param custom
   * @throws Exception
   */
  public void aggiornaDati(HttpSession session, Map param, boolean saveDB, boolean saveTmp, Map custom)
     throws Exception
  {
    synchronized(wf)
    {
      // estrae, aggiorna e visualizza oggetto
      objInEdit = findElementoEdit(session, param);
      if(objInEdit == null)
        objInEdit = newObject(session, param);

      Validator.StopParse stopParsing = new Validator.StopParse();
      if(Validator.preParseValidate(wf.getEleXml(), objInEdit, wf.getPtm(), (hEditTable) wf.getTbl(), 0,
         session, param, i18n, custom, stopParsing))
      {
        if(!stopParsing.stopParsing)
        {
          ((PeerTableModel) (getModel())).rebind(objInEdit);
          salvaDati(param);
        }

        // se richiesto salvataggio salva l'oggetto sul database
        if(saveDB)
          saveObject(objInEdit, wf.getPtm(), (hEditTable) wf.getTbl(), 0,
             session, param, custom);

        // se l'oggetto e' nuovo modifica il valore
        // del parametro new per segnalare oggetto creato e in cache
        if(objInEdit.toBeSavedWithInsert())
          param.put("new", "2");
      }
    }
  }

  /**
   * Produce HTML per l'editing del record corrente.
   * @param param
   * @param session
   * @param forceNew
   * @return
   * @throws Exception
   */
  public String getHtml(HttpSession session, Map param, boolean forceNew)
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtml(session, param, forceNew, page);

    // assembla tutte le componenti tranne i JAVASCRIPT_PART che sono molto specifici
    return wf.getUim().formatHtmlForm(page);
  }

  public void getHtml(HttpSession session, Map param, boolean forceNew, RigelHtmlPage page)
     throws Exception
  {
    synchronized(wf)
    {
      objInEdit = forceNew ? null : findElementoEdit(session, param);

      // ...altrimenti e' un oggetto nuovo e lo crea di conseguenza
      if(objInEdit == null)
      {
        objInEdit = newObject(session, param);
      }

      // collega l'oggetto al table model
      ((PeerTableModel) (getModel())).rebind(objInEdit);

      // estrae l'HTML per tutto il form
      doHtmlUnico(page);

      // crea campi hidden con i parametri di richiesta
      if(objInEdit.toBeSavedWithInsert())
        html.append("<input type=\"hidden\" name=\"new\" value=\"2\">\r\n");
      else
        html.append(wf.makeHiddenEditParametri(0));
    }
  }

  protected Record newObject(HttpSession sessione, Map param)
     throws Exception
  {
    Record newObj = null;
    String dupType = (String) param.get("dup");

    if(dupType != null && dupType.equals("1"))
    {
      // oggetto duplicato di uno gia' esistente
      Record parentObj = findElemento(sessione, param);
      if(parentObj != null)
      {
        newObj = new Record(parentObj);
        // pulisce dal nuovo oggetto tutti i campi controllati dal saver
        ros.clearNewObject(newObj);
        return newObj;
      }
    }

    // oggetto completamente nuovo
    SqlTableModel tm = (SqlTableModel) getTM();
    QueryBuilder qb = tm.getQuery();
    String tableName = qb.getDeleteFrom();

    newObj = PeerTransactAgent.executeReturnReadonly((con) ->
    {
      try ( TableDataSet td = new TableDataSet(con, tableName))
      {
        return td.addRecord();
      }
    });
    caricaDefaultsNuovoOggetto(newObj, param, wf.getNome());

    return newObj;
  }

  protected synchronized void saveObject(final Record obj,
     final RigelTableModel tableModel, final hEditTable table, final int row,
     final HttpSession session, final Map param, final Map custom)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent()
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        if(Validator.postParseValidate(wf.getEleXml(), obj, tableModel, table, row,
           session, param, i18n, dbCon, custom))
        {
          // avvia salvataggio con apposito gestore
          if(obj.needsToBeSaved())
            ros.salva(obj, dbCon, 0);

          return true; // transazione confermata
        }
        return false; // rollback della transazione
      }
    };

    ta.runNow();
  }

  public boolean isNewObject()
  {
    return objInEdit == null || objInEdit.toBeSavedWithInsert();
  }

  public Record getLastObjectInEdit()
  {
    return objInEdit;
  }
}
