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
package org.rigel5.glue.pager;

import java.sql.Connection;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.om.Persistent;
import org.commonlib5.utils.StringOper;
import org.rigel5.HtmlUtils;
import org.rigel5.db.torque.PeerTransactAgent;
import org.rigel5.glue.PeerObjectSaver;
import org.rigel5.glue.validators.Validator;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.AbstractHtmlTablePager;
import org.rigel5.table.html.hEditTable;
import org.rigel5.table.peer.html.PeerTableModel;
import org.rigel5.table.peer.html.PeerWrapperEditHtml;

/**
 * Paginatore per liste di oggetti Peer
 * specializzato per le funzioni di edit.
 * Viene utilizzato dagli editor di form e di tabelle.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerTablePagerEditApp extends AbstractHtmlTablePager
{
  public PeerObjectSaver pos = null;
  private PeerWrapperEditHtml wl;
  public Map mdLinkParams;

  /**
   * Costruisce il paginatore.
   */
  public PeerTablePagerEditApp()
  {
  }

  /**
   * Inizializza paginatore.
   * @param wl wrapper di riferimento
   * @param type lista da visualizzare
   * @param pos gestore di salvataggio dati
   * @throws Exception
   */
  public void init(PeerWrapperEditHtml wl, String type, PeerObjectSaver pos)
     throws Exception
  {
    this.wl = wl;
    this.pos = pos;

    // imposta numero per pagina
    if(wl.getNumPerPage() != 0)
      limit = wl.getNumPerPage();

    pos.init(wl.getObjectClass(), wl.getPeerClass());
  }

  /**
   * Forza un ricaricamento di tutto il set di records dal database.
   * @throws Exception
   */
  public void reloadAllRecords()
     throws Exception
  {
    rebindAllRecords();
  }

  /**
   * Ricollega l'insieme di record.
   * L'operazione e' sottoposta a cache in sessione:
   * se non ci sono state variazioni sull'insieme di record
   * non vengono ripetute le query al db.
   * @throws Exception
   */
  public void rebindAllRecords()
     throws Exception
  {
    // costruisce un criteria vuoto per tutti i records
    Criteria cLink = new Criteria();
    List vRecords = wl.getRecords(cLink);

    // rimappa i record del detail
    PeerTableModel ptm = ((PeerTableModel) (wl.getPtm()));
    ptm.rebind(vRecords);
  }

  /**
   * Caricamento dati master-detail.
   * Quando questa lista è utilizzata come detail
   * di un form, qui vengono caricati i record da
   * modificare relativi al master.
   * @param linkParams parametri di collegamento fra master e detail
   * @throws Exception
   */
  public void rebindMasterDetail(Map linkParams)
     throws Exception
  {
    mdLinkParams = linkParams;

    // costruisce il corrispondente criteria di selezione sul detail
    Criteria cLink = wl.makeCriteriaMasterDetail(linkParams);
    List vRecords = wl.getRecords(cLink);

    // rimappa i record del detail
    PeerTableModel ptm = ((PeerTableModel) (wl.getPtm()));
    ptm.rebind(vRecords);
  }

  @Override
  public long getTotalRecords()
     throws Exception
  {
    return wl.getPtm().getRowCount();
  }

  /**
   * Aggiornamento e salvataggio.
   * Viene chiamata dopo la post del form che contiene
   * i dati della tabella da salvare nel corrispettivo
   * vettore di oggetti.
   * @param session sessione di riferimento
   * @param param mappa dei parametri della post
   * @param nuovo flag per la creazione di un nuovo record
   * @param saveDB flag per salvataggio su db
   * @param custom eventuali dati custom da sottoporre ai validatori
   * @throws Exception
   */
  public void aggiornaDati(HttpSession session, Map param,
     boolean nuovo, boolean saveDB, Object custom)
     throws Exception
  {
    aggiornaDati(session, param, nuovo, saveDB, custom, mdLinkParams);
  }

  /**
   * Aggiornamento e salvataggio.
   * Viene chiamata dopo la post del form che contiene
   * i dati della tabella da salvare nel corrispettivo
   * vettore di oggetti.
   * Questa versione viene usata durante il salvataggio
   * di un master-detail (qui siamo nel detail).
   * @param session sessione di riferimento
   * @param param mappa dei parametri della post
   * @param nuovo flag per la creazione di un nuovo record
   * @param saveDB flag per salvataggio su db
   * @param custom eventuali dati custom da sottoporre ai validatori
   * @param linkParams parametri di collegamento master-detail
   * @throws Exception
   */
  public void aggiornaDati(HttpSession session, Map param,
     boolean nuovo, boolean saveDB, Object custom, Map linkParams)
     throws Exception
  {
    PeerTableModel ptm = ((PeerTableModel) (wl.getPtm()));
    long numRec = ptm.getRowCount();

    hEditTable tbl = (hEditTable) wl.getTbl();
    Validator.StopParse stopParsing = new Validator.StopParse();
    for(int i = 0, row = start; i < limit && row < numRec; i++, row++)
    {
      Persistent objInEdit = (Persistent) ptm.getRowRecord(row);
      if(Validator.preParseValidate(wl.getEleXml(), objInEdit, ptm, tbl, row,
         session, param, i18n, custom, stopParsing))
      {
        if(!stopParsing.stopParsing)
          tbl.salvaDatiRiga(row, param);
      }
      else
        saveDB = false;
    }

    if(nuovo)
    {
      int numNuovi = StringOper.parse(param.get("number_new_objects"), 1);
      while(numNuovi-- > 0)
        ptm.addObject(newObject(param, session));
    }

    if(linkParams != null)
      for(int row = 0; row < ptm.getRowCount(); row++)
        wl.saveMasterDetailLink(linkParams, row);

    if(saveDB)
      saveData(session, param, custom);
  }

  /**
   * Crea opportunamente un nuovo oggetto da aggiungere al vettore di edit.
   * @param session session di riferimento
   * @param param mappa dei parametri della post
   * @return nuovo oggetto derivato da Persistent
   * @throws Exception
   */
  protected Persistent newObject(Map param, HttpSession session)
     throws Exception
  {
    Persistent newObj = (Persistent) (wl.getObjectClass().newInstance());

    // carica eventuali valori di default per il nuovo oggetto
    PeerTableModel ptm = ((PeerTableModel) (wl.getPtm()));
    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = ptm.getColumn(i);
      String key = cd.getDefValParam();
      if(key != null)
      {
        Object defVal = param.get(key);
        if(defVal == null)
          defVal = param.get(wl.getNome() + key);
        if(defVal != null)
          cd.setValueAscii(newObj, defVal.toString());
      }
    }

    return newObj;
  }

  /**
   * Salva tutti i dati sul db.
   * Viene salvato tutto il blocco di record nel db
   * in una unica transazione.
   * @param session sessione di riferimento
   * @param param mappa dei parametri della post
   * @param custom eventuali dati custom da sottoporre ai validatori
   * @throws Exception
   */
  protected synchronized void saveData(final HttpSession session,
     final Map param, final Object custom)
     throws Exception
  {
    final PeerTableModel ptm = ((PeerTableModel) (wl.getPtm()));

    PeerTransactAgent ta = new PeerTransactAgent()
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        for(int i = 0; i < ptm.getRowCount(); i++)
        {
          int statoRec = ptm.isRowDeleted(i) ? 10 : 0;
          saveObject(dbCon, (Persistent) ptm.getRowRecord(i), ptm, i,
             session, param, custom, statoRec);
        }

        return true;
      }
    };

    ta.runNow();
    ptm.removeDeleted();
  }

  /**
   * Salva un singolo oggetto sul db.
   * @param dbCon connessione sql
   * @param obj oggetto da salvare
   * @param tableModel table model di riferimento
   * @param row riga corrispondente all'oggetto da salvare
   * @param session sessione di riferimento
   * @param param mappa dei parametri della post
   * @param custom eventuali dati custom da sottoporre ai validatori
   * @param statoRec valore di stato rec da impostare ove previsto
   * @throws Exception
   */
  protected void saveObject(Connection dbCon, Persistent obj,
     RigelTableModel tableModel, int row,
     HttpSession session, Map param, Object custom, int statoRec)
     throws Exception
  {
    if(!Validator.postParseValidate(wl.getEleXml(), obj, tableModel, (hEditTable) wl.getTbl(), row,
       session, param, i18n, dbCon, custom))
      return;

    // avvia salvataggio con apposito gestore
    if(obj.isModified() || statoRec != 0)
      pos.salva(obj, dbCon, statoRec);
  }

  @Override
  public String getSelfUrl(int rStart, HttpSession sessione)
     throws Exception
  {
    String rv = super.getSelfUrl(rStart, sessione);

    if(mdLinkParams != null)
      rv = HtmlUtils.mergeUrl(rv, mdLinkParams);

    return rv;
  }
}
