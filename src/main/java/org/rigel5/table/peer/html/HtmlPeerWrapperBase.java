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
package org.rigel5.table.peer.html;

import java.lang.reflect.*;
import java.sql.Connection;
import java.util.*;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.map.TableMap;
import org.apache.torque.sql.OrderBy;
import org.apache.torque.util.UniqueList;
import org.commonlib5.utils.Pair;
import org.commonlib5.utils.StringOper;
import org.rigel5.db.torque.CriteriaRigel;
import org.rigel5.db.torque.PeerTransactAgent;
import org.rigel5.db.torque.TableMapHelper;
import org.rigel5.exceptions.InvalidObjectException;
import org.rigel5.exceptions.MissingColumnException;
import org.rigel5.exceptions.MissingParameterException;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.html.wrapper.HtmlWrapperBase;
import org.rigel5.table.html.wrapper.ParametroListe;
import org.rigel5.table.peer.PeerAbstractTableModel;

/**
 * Classe base di tutti i Wrapper che utilizzano
 * oggetti di Torque.
 *
 * @author Nicola De Nisco
 */
abstract public class HtmlPeerWrapperBase extends HtmlWrapperBase
{
  protected Class objectClass;
  protected Class peerClass;
  protected TableMapHelper tmap;
  private Method doSelectM, getStatoRecM;

  /**
   * Imposta la classe dell'oggetto Peer (manipolatore).
   * @param peerClass
   * @throws Exception
   */
  public void setPeerClass(Class peerClass)
     throws Exception
  {
    if(peerClass == null)
      throw new InvalidObjectException("L'oggetto peer non esiste. Verificare la classpath.");

    this.peerClass = peerClass;

    // recupera il TableMap dall'oggetto peer
    Method getTableMapM = peerClass.getMethod("getTableMap");
    TableMap tm = (TableMap) getTableMapM.invoke(null);
    tmap = new TableMapHelper(tm);
    nomeTabella = tm.getName();

    // recupera metodo doSelect
    doSelectM = peerClass.getMethod("doSelect", Criteria.class, Connection.class);
  }

  public Class getPeerClass()
  {
    return peerClass;
  }

  /**
   * Imposta la classe dell'oggetto dati.
   * @param objectClass
   * @throws Exception
   */
  public void setObjectClass(Class objectClass)
     throws Exception
  {
    if(objectClass == null)
      throw new InvalidObjectException("L'oggetto dati non esiste. Verificare la classpath.");

    this.objectClass = objectClass;

    // recupera metodo per lettura stato_rec (se esiste)
    try
    {
      getStatoRecM = objectClass.getMethod("getStatoRec");
    }
    catch(NoSuchMethodException ei)
    {
      getStatoRecM = null;
    }
  }

  public Class getObjectClass()
  {
    return objectClass;
  }

  public TableMapHelper getTmap()
  {
    return tmap;
  }

  /**
   * Produce il criterio di selezione del record sottoposto a modifica
   * in base ai parametri passati. Viene utilizzata da jsform.jsp per
   * individuare il record da sottoporre ad editing.
   * @param param i parametri della richiesta HTTP
   * @return il criterio di selezione del record
   * @throws Exception
   */
  public Criteria makeCriteriaEditRiga(Map param)
     throws Exception
  {
    boolean isNew = param.containsKey("new");

    Criteria c = new Criteria();
    Enumeration enumParam = edInfo.getEnumParamsKey();
    while(enumParam.hasMoreElements())
    {
      String nomec = StringOper.okStr(enumParam.nextElement());
      String campo = StringOper.okStr(edInfo.getParam(nomec));

      if(nomec.isEmpty() || campo.isEmpty())
        continue;

      String valore = StringOper.okStr(param.get(nomec));
      if(valore == null)
        valore = StringOper.okStr(param.get(nomec.toLowerCase()));

      if(!isNew && valore == null)
        throw new MissingParameterException("Parametro " + nomec + " non specificato nella richiesta!");

      Pair<ColumnMap, Object> colCampo;
      RigelColumnDescriptor cd;

      switch(campo.charAt(0))
      {
        case '#':
          // aggancio dinamico a nome di colonna
          if((cd = ptm.getColumn(campo.substring(1))) == null)
            throw new MissingColumnException("Colonna " + campo + " non trovata fra le colonne della lista!");

          if((colCampo = tmap.getCampoAndParseValue(cd.getName(), valore)) == null)
            throw new MissingColumnException("Colonna " + campo + " non trovata in tabella!");

          c.and(colCampo.first, colCampo.second);
          break;

        case '@':
          // aggancio dinamico a nome campo
          if((colCampo = tmap.getCampoAndParseValue(campo.substring(1), valore)) == null)
            throw new MissingColumnException("Colonna " + campo + " non trovata in tabella!");

          c.and(colCampo.first, colCampo.second);
          break;

        default:
          break;
      }
    }

    return c;
  }

  /**
   * Ritorna i campi del database delle foreign columns.
   * Possono essere utilizzati insieme al nome tabella per
   * recuperare informazioni dal db.
   * @return
   * @throws Exception
   */
  public String getForeignDbFields()
     throws Exception
  {
    String rv = "";
    if(foInfo == null)
      return rv;

    Enumeration enumParam = foInfo.getForeignColumnsKeys();
    while(enumParam.hasMoreElements())
    {
      String nomec = (String) enumParam.nextElement();
      String colonna = foInfo.getParam(nomec);

      RigelColumnDescriptor cd = ptm.getColumn(colonna);
      if(cd == null)
        throw new MissingColumnException("Foreign-server: colonna " + colonna + " (" + nomec + ") non presente!");

      String nomeCampo = tmap.getNomeCampo(cd.getName());
      rv += "," + nomeCampo;
    }
    return rv.substring(1);
  }

  /**
   * Restituisce un Criteria di selezione quando questo wrapper
   * fa capo ad una tabella di detail collegata a un rispettivo master.
   * Questa funzione utilizza una Map di parametri di link
   * eventualmente prodotta da makeMapMasterDetail() sulla
   * tabella master.
   * @param param parametri di link fra master e detail
   * @return
   * @throws java.lang.Exception
   */
  public Criteria makeCriteriaMasterDetail(Map param)
     throws Exception
  {
    if(mdInfo == null)
      return null;

    Criteria c = new Criteria();
    Iterator<String> itrPar = mdInfo.iterator();
    while(itrPar.hasNext())
    {
      String nomec = itrPar.next();
      Pair<String, String> info = mdInfo.getParameterInfo(nomec);
      String campo = info.first;

      // recupera parametro utilizzando eventualmente il default
      String valore = StringOper.okStr(param.get(nomec), info.second);
      if(valore == null)
        throw new MissingParameterException("Parametro " + nomec + " non specificato nella richiesta!");

      String columnName = null;
      if(campo.startsWith("#"))
      {
        // aggancio dinamico a valore di colonna
        RigelColumnDescriptor cd = ptm.getColumn(campo.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + campo + " non trovata!");

        columnName = cd.getName();
      }
      else if(campo.startsWith("@"))
      {
        // aggancio dinamico a valore di campo tabella
        columnName = campo.substring(1);
      }

      if(columnName != null)
      {
        Pair<ColumnMap, Object> cv = tmap.getCampoAndParseValue(columnName, valore);
        if(cv != null)
          c.and(cv.first, cv.second);
      }
    }

    return c;
  }

  /**
   * Creazione dei parametri di link verso una tabella detail.
   * Questa funzione viene chiamata quando questo wrapper e' associato
   * ad una tabella master.La Map prodotta contiene i dati di link
   * verso la tabella detail.
   * Questa Map potra' essere usata da
   * makeCriteriaMasterDetail() per stabilire l'insieme dei record
   * sottoposti ad editing.
   * @param row il record in editing come master (in caso di form sempre 0).
   * @return mappa valori di link
   * @throws java.lang.Exception
   */
  public Map makeMapMasterDetail(int row)
     throws Exception
  {
    if(mdInfo == null)
      return null;

    Hashtable rv = new Hashtable();
    Iterator<String> itrPar = mdInfo.iterator();
    while(itrPar.hasNext())
    {
      String nomec = itrPar.next();
      String valore = mdInfo.getParameter(nomec);

      if(valore.startsWith("#"))
      {
        // aggancio dinamico a valore di colonna
        RigelColumnDescriptor cd = ptm.getColumn(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        valore = cd.getValueAsString(valObj);
      }
      else if(valore.startsWith("@"))
      {
        // aggancio dinamico a valore di campo tabella
        RigelColumnDescriptor cd = ptm.getColumnByName(valore.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + valore + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        valore = cd.getValueAsString(valObj);
      }

      if(valore == null)
        return null;

      rv.put(nomec, valore);
    }

    return rv;
  }

  /**
   * Quando è attivo un master/detail salva nelle righe di detail
   * i valori di link con il record master.
   * @param param
   * @param row riga di detail da aggiornare
   * @throws Exception
   */
  public void saveMasterDetailLink(Map param, int row)
     throws Exception
  {
    if(mdInfo == null)
      return;

    Iterator<String> itrPar = mdInfo.iterator();
    while(itrPar.hasNext())
    {
      String nomec = itrPar.next();
      Pair<String, String> info = mdInfo.getParameterInfo(nomec);
      String campo = info.first;

      // recupera parametro utilizzando eventualmente il default
      String valore = StringOper.okStr(param.get(nomec), info.second);
      if(valore == null)
        throw new MissingParameterException("Parametro " + nomec + " non specificato nella richiesta!");

      if(campo.startsWith("#"))
      {
        // aggancio dinamico a valore di colonna
        RigelColumnDescriptor cd = ptm.getColumn(campo.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + campo + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        cd.setValueAscii(valObj, valore);
      }
      else if(campo.startsWith("@"))
      {
        // aggancio dinamico a valore di campo tabella
        RigelColumnDescriptor cd = ptm.getColumnByName(campo.substring(1));
        if(cd == null)
          throw new MissingColumnException("Colonna " + campo + " non trovata!");

        Object valObj = ptm.getRowRecord(row);
        cd.setValueAscii(valObj, valore);
      }
    }
  }

  /**
   * Recupero dei records da sottoporre ad editing.
   * @return vettore di oggetti BaseObject (derivati da)
   * @throws Exception
   */
  public List getRecords()
     throws Exception
  {
    return getRecords(new Criteria());
  }

  /**
   * Recupero dei records da sottoporre ad editing.
   * @param c criteria specifico di selezione
   * @return vettore di oggetti BaseObject (derivati da)
   * @throws Exception
   */
  public List getRecords(Criteria c)
     throws Exception
  {
    // attiva ordinamento di default se previsto
    for(String nomeCol : sortColumns)
    {
      boolean descOrder = false;
      if(nomeCol.startsWith("!"))
      {
        descOrder = true;
        nomeCol = nomeCol.substring(1);
      }

      RigelColumnDescriptor cd = getPtm().getColumn(nomeCol);
      if(cd == null)
        throw new Exception("Colonna di sort " + nomeCol + " non trovata!");

      // cerca il nome della colonna fra i metodi del map builder
      ColumnMap campo = tmap.getCampo(cd.getName());
      if(descOrder)
        c.addDescendingOrderByColumn(campo);
      else
        c.addAscendingOrderByColumn(campo);
    }

    // usa ordinamento di default per chiave primaria
    PeerAbstractTableModel pptm = (PeerAbstractTableModel) ptm;
    if((c.getOrderByColumns() == null || c.getOrderByColumns().isEmpty()) && pptm.getDefaultOrderCriteria() != null)
    {
      // copia le colonne di ordinamento dal criteria di default a quello corrente
      UniqueList<OrderBy> oc = pptm.getDefaultOrderCriteria().getOrderByColumns();
      for(OrderBy ob : oc)
      {
        if(ob.getOrder().equals(SqlEnum.ASC))
          c.addAscendingOrderByColumn(ob.getColumn());
        else
          c.addDescendingOrderByColumn(ob.getColumn());
      }
    }

    // rimuove statorec se previsto
    if(getStatoRecM != null)
      CriteriaRigel.removeDeleted(c, nomeTabella);

    // attiva filtro se previsto
    for(ParametroListe pl : filtro.getParametri())
    {
      if(pl.getValore() != null)
      {
        String where = pl.getCampo() + pl.getOperazione() + pl.getValoreFmt();
        c.and(nomeTabella + "." + pl.getCampo(), (Object) where, SqlEnum.CUSTOM);
      }
    }

    return doSelect(c);
  }

  public List doSelect(Criteria c)
     throws Exception
  {
    return PeerTransactAgent.executeReturnReadonly((con) -> (List) doSelectM.invoke(null, c, con));
  }
}
