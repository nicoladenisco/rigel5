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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.om.Persistent;
import org.rigel5.db.torque.PeerTransactAgent;
import org.rigel5.glue.PeerObjectSaver;
import org.rigel5.glue.validators.Validator;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.FormTable;
import org.rigel5.table.html.RigelHtmlPage;
import org.rigel5.table.html.hEditTable;
import org.rigel5.table.peer.html.PeerTableModel;
import org.rigel5.table.peer.html.PeerWrapperFormHtml;

/**
 * Tabella per la visualizzazione di forms.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerAppMaintFormTable extends FormTable
{
  protected PeerObjectSaver pos = null;
  protected PeerWrapperFormHtml wf;
  protected Persistent objInEdit;

  /**
   * Costruttore protetto solo per classi derivate.
   * La classe derivata userà il metodo init() per l'inizializzazione.
   */
  public PeerAppMaintFormTable()
  {
    super(null);
  }

  public void init(String sID, PeerWrapperFormHtml wf, PeerObjectSaver pos)
     throws Exception
  {
    this.id = sID;
    this.wf = wf;

    // imposta numero colonne
    if(wf.getNumColonne() != 0)
      setColonne(wf.getNumColonne());

    this.pos = pos;
    pos.init(wf.getObjectClass(), wf.getPeerClass());
  }

  public void setUserInfo(int idUser, boolean isAdmin)
     throws Exception
  {
    pos.setUserInfo(idUser, isAdmin);
  }

  public Persistent findElementoEdit(HttpSession session, Map param)
     throws Exception
  {
    String newType = (String) param.get("new");

    if(newType != null && newType.equals("1"))
      return null;

    if(newType != null && newType.equals("2") && objInEdit != null)
      return objInEdit;

    // produce i parametri di selezione
    Criteria c = wf.makeCriteriaEditRiga(param);

    List v = getRecords(c);
    return v == null || v.isEmpty() ? null : (Persistent) (v.get(0));
  }

  public Persistent findElemento(HttpSession session, Map param)
     throws Exception
  {
    // produce i parametri di selezione
    Criteria c = wf.makeCriteriaEditRiga(param);

    List v = getRecords(c);
    return v == null || v.isEmpty() ? null : (Persistent) (v.get(0));
  }

  public List getRecords(Criteria c)
     throws Exception
  {
    return wf.doSelect(c);
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
  public void aggiornaDati(HttpSession session, Map param, boolean saveDB, boolean saveTmp, Object custom)
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
        if(objInEdit.isNew())
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
      if(objInEdit.isNew())
        html.append("<input type=\"hidden\" name=\"new\" value=\"2\">\r\n");
      else
        html.append(wf.makeHiddenEditParametri(0));
    }
  }

  protected Persistent newObject(HttpSession sessione, Map param)
     throws Exception
  {
    Persistent newObj = null;
    String dupType = (String) param.get("dup");

    if(dupType != null && dupType.equals("1"))
    {
      // oggetto duplicato di uno gia' esistente
      Persistent parentObj = findElemento(sessione, param);
      if(parentObj != null)
      {
        Method new_obj_m = wf.getObjectClass().getMethod("copy", boolean.class);
        newObj = (Persistent) new_obj_m.invoke(parentObj, false);
        if(newObj != null)
        {
          // pulisce dal nuovo oggetto tutti i campi controllati dal saver
          pos.clearNewObject(newObj);
          return newObj;
        }
      }
    }

    // oggetto completamente nuovo
    newObj = (Persistent) (wf.getObjectClass().newInstance());

    // carica eventuali valori di default per il nuovo oggetto
    PeerTableModel ptm = ((PeerTableModel) (getModel()));
    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = ptm.getColumn(i);

      if(cd.getDefVal() != null)
      {
        String sVal = cd.getDefVal();
        if(sVal.equals("@today"))
          sVal = cd.formatValue(new Date());
        cd.setValueAscii(newObj, sVal);
      }

      String key = cd.getDefValParam();
      if(key != null)
      {
        Object defVal = param.get(key);
        if(defVal == null)
          defVal = param.get(wf.getNome() + key);
        if(defVal != null)
          cd.setValueAscii(newObj, defVal.toString());
      }
    }

    return newObj;
  }

  protected synchronized void saveObject(final Persistent obj,
     final RigelTableModel tableModel, final hEditTable table, final int row,
     final HttpSession session, final Map param, final Object custom)
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
          if(obj.isModified())
            pos.salva(obj, dbCon, 0);

          return true; // transazione confermata
        }
        return false; // rollback della transazione
      }
    };

    ta.runNow();
  }

  public boolean isNewObject()
  {
    return objInEdit == null || objInEdit.isNew();
  }

  public Persistent getLastObjectInEdit()
  {
    return objInEdit;
  }
}
