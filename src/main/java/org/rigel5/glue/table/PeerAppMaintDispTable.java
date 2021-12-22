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

import java.util.*;
import javax.servlet.http.*;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.om.Persistent;
import org.rigel5.table.html.DispTable;
import org.rigel5.table.html.FormTable;
import org.rigel5.table.peer.html.PeerTableModel;
import org.rigel5.table.peer.html.PeerWrapperFormHtml;

/**
 * Versione specializzata per applicazione di una generica DispTable.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerAppMaintDispTable extends DispTable
{
  private PeerWrapperFormHtml wf;
  private Persistent objInEdit = null;

  /**
   * Costruttore protetto solo per classi derivate.
   * La classe derivata userà il metodo init() per l'inizializzazione.
   */
  public PeerAppMaintDispTable()
  {
    super(null);
  }

  public void init(String sID, PeerWrapperFormHtml wf)
     throws Exception
  {
    this.id = sID;
    this.wf = wf;

    // imposta numero colonne
    if(wf.getNumColonne() != 0)
      setColonne(wf.getNumColonne());
  }

  public Persistent findElementoEdit(Map param)
     throws Exception
  {
    if(param.get("new") != null)
      return null;

    Criteria c = wf.makeCriteriaEditRiga(param);
    List v = getRecords(c);
    objInEdit = v == null ? null : (Persistent) (v.get(0));
    return objInEdit;
  }

  public List getRecords(Criteria c)
     throws Exception
  {
    return wf.doSelect(c);
  }

  /**
   * Produce HTML per l'editing del record corrente.
   * @param param
   * @param sessione
   * @return
   * @throws Exception
   */
  public String getHtml(Map param, HttpSession sessione)
     throws Exception
  {
    // estrae, aggiorna e visualizza oggetto
    Persistent objEdit = findElementoEdit(param);
    if(objEdit == null)
      objEdit = newObject(param, sessione);

    ((PeerTableModel) (getModel())).rebind(objEdit);

    return wf.getHtmlForm(param, sessione);
  }

  protected Persistent newObject(Map param, HttpSession sessione)
     throws Exception
  {
    Persistent newObj = (Persistent) (wf.getObjectClass().newInstance());
    caricaDefaultsNuovoOggetto(newObj, param, wf.getNome());
    return newObj;
  }

  public Persistent getObjInEdit()
  {
    return objInEdit;
  }

  public static String getHtmlDisp(PeerWrapperFormHtml pw, Map param, HttpSession sessione)
     throws Exception
  {
    FormTable table = (FormTable) pw.getTbl();
    synchronized(pw)
    {
      // inserisce nuova tabella in sola visualizzazione
      PeerAppMaintDispTable pd = new PeerAppMaintDispTable();
      pd.init("AA", pw);
      pw.setTbl(pd);
      pw.init();

      // genera l'html di sola visualizzazione
      String sHtml = pd.getHtml(param, sessione);

      // ripristina la vecchia tabella
      pw.setTbl(table);
      pw.init();
      return sHtml;
    }
  }
}
