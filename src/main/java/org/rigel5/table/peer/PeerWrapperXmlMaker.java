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

import java.util.*;
import org.commonlib5.utils.StringOper;
import org.jdom2.*;
import org.rigel5.exceptions.InvalidObjectException;
import org.rigel5.table.RigelBaseWrapperXmlMaker;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.WrapperBase;
import org.rigel5.table.peer.html.HtmlPeerWrapperBase;
import org.rigel5.table.peer.html.PeerWrapperEditHtml;
import org.rigel5.table.peer.html.PeerWrapperFormHtml;
import org.rigel5.table.peer.html.PeerWrapperListaHtml;
import org.rigel5.table.peer.xml.PeerWrapperListaXml;

/**
 * Costruisce gli oggetti wrapper a partire dall'XML.
 *
 * @author Nicola De Nisco
 */
public class PeerWrapperXmlMaker extends RigelBaseWrapperXmlMaker
{
  protected TorqueObjectManager tObjMan = new TorqueObjectManager();

  /**
   * Recupera l'oggetto TorqueObjectManager associato
   * a questo generatore di wrapper.
   *
   * @return the value of tObjMan
   */
  public TorqueObjectManager getTObjMan()
  {
    return tObjMan;
  }

  /**
   * Imposta l'oggetto TorqueObjectManager associato
   * a questo generatore di wrapper.
   *
   * @param tObjMan new value of tObjMan
   */
  public void setTObjMan(TorqueObjectManager tObjMan)
  {
    this.tObjMan = tObjMan;
  }

  /////////////////////////////////////////////////////////////////////////
  //
  /**
   * Popola un wrapper con i dati a lui relativi.
   * @param nomeLista
   * @param lista
   * @param wb
   * @param edit
   * @param removeCaratt
   * @return
   * @throws Exception
   */
  @Override
  public WrapperBase parseWrapper(String nomeLista, Element lista, WrapperBase wb,
     boolean edit, boolean removeCaratt)
     throws Exception
  {
    HtmlPeerWrapperBase wl = (HtmlPeerWrapperBase) (super.parseWrapper(nomeLista, lista, wb, edit, removeCaratt));
    Element e = null;

    if((e = lista.getChild("peer")) != null)
    {
      String peerName = e.getTextTrim();
      Class cp = tObjMan.buildPeer(peerName);
      if(cp == null)
        throw new InvalidObjectException("La classe " + peerName
           + " non esiste. Ho cercato in " + StringOper.join(tObjMan.getBasePeerArray(), ','));

      wl.setPeerClass(cp);
    }

    if((e = lista.getChild("object")) != null)
    {
      String objName = e.getTextTrim();
      Class cp = tObjMan.buildObject(objName);
      if(cp == null)
        throw new InvalidObjectException("La classe " + objName
           + " non esiste. Ho cercato in " + StringOper.join(tObjMan.getBaseObjectArray(), ','));

      wl.setObjectClass(cp);
    }

    String OrderBy;
    if((OrderBy = lista.getChildTextTrim("orderby")) != null)
    {
      StringTokenizer tks = new StringTokenizer(OrderBy, ",");
      while(tks.hasMoreTokens())
      {
        wl.addSortColumn(tks.nextToken());
      }
    }

    List<Element> colonne = lista.getChildren("colonna");
    for(Element item : colonne)
    {
      RigelColumnDescriptor cd;
      String nomeColonna = item.getAttributeValue("nome");

      if(wl.isCustomColumnsEnabled())
      {
        cd = parseColumn(nomeLista, item, edit, buildColumn(nomeLista, nomeColonna, item, PeerColumnDescriptor.class));
      }
      else
      {
        cd = parseColumn(nomeLista, item, edit, new PeerColumnDescriptor());
      }

      if(removeCaratt && cd.isCaratteristiche())
        continue;

      addColumn(item, wl.getPtm(), cd);
    }

    // rimuove colonne in liste derivate
    removeColumns(lista, wl.getPtm());

    return wl;
  }

  public PeerWrapperListaHtml getLista(String nomeLista, PeerWrapperListaHtml wr)
     throws Exception
  {
    Element lista = getElementLista(XML_LISTE_PEER, nomeLista);
    String extend = getExtends(lista);
    if(extend != null)
      wr = getLista(extend, wr);

    return (PeerWrapperListaHtml) (parseWrapper(nomeLista, lista, wr, false, false));
  }

  public PeerWrapperEditHtml getListaEdit(String nomeLista, PeerWrapperEditHtml wr)
     throws Exception
  {
    Element lista = getElementLista(XML_EDIT_PEER, nomeLista);
    String extend = getExtends(lista);
    if(extend != null)
      wr = getListaEdit(extend, wr);

    return (PeerWrapperEditHtml) (parseWrapper(nomeLista, lista, wr, true, false));
  }

  public PeerWrapperFormHtml getForm(String nomeForm, PeerWrapperFormHtml wr)
     throws Exception
  {
    Element form = getElementLista(XML_FORMS_PEER, nomeForm);
    String extend = getExtends(form);
    if(extend != null)
      wr = getForm(extend, wr);

    String sNumColon;
    if((sNumColon = form.getChildTextTrim("numcolonne")) != null)
      wr.setNumColonne(Integer.parseInt(sNumColon));

    return (PeerWrapperFormHtml) (parseWrapper(nomeForm, form, wr, true, false));
  }

  /////////////////////////////////////////////////////////////////////////
  //
  public PeerWrapperListaHtml getLista(String nomeLista)
     throws Exception
  {
    return getLista(nomeLista, new PeerWrapperListaHtml());
  }

  public PeerWrapperEditHtml getListaEdit(String nomeLista)
     throws Exception
  {
    return getListaEdit(nomeLista, new PeerWrapperEditHtml());
  }

  public PeerWrapperFormHtml getForm(String nomeForm)
     throws Exception
  {
    return getForm(nomeForm, new PeerWrapperFormHtml());
  }

  public PeerWrapperListaXml getListaXml(String nomeLista, boolean removeCaratt)
     throws Exception
  {
    Element lista = getElementLista(XML_XML_PEER, nomeLista);
    return (PeerWrapperListaXml) (parseWrapper(nomeLista, lista, new PeerWrapperListaXml(), false, removeCaratt));
  }

  /**
   * Costruisce una lista XML a partire da una lista HTML rimuovendo
   * eventualmente le colonne di selezione e/o di funzioni.
   * @param nomeLista
   * @return
   * @throws Exception
   */
  public PeerWrapperListaXml getListaXmlFromListe(String nomeLista, boolean removeCaratt)
     throws Exception
  {
    Element lista = getElementLista(XML_LISTE_PEER, nomeLista);
    return (PeerWrapperListaXml) (parseWrapper(nomeLista, lista, new PeerWrapperListaXml(), false, removeCaratt));
  }
}
