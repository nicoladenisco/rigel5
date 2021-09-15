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
package org.rigel5.table.html.wrapper;

import java.util.*;
import org.commonlib5.utils.Pair;
import org.jdom2.Element;

/**
 * Informazioni per il master/detail di due forms/liste/liste-edit.
 *
 * @author Nicola De Nisco
 */
public class MasterDetailInfo implements Iterable<String>
{
  /**
   * Indica una relazione di tipo master (generalmente una form).
   */
  public static final int ROLE_MASTER = 1;

  /**
   * Indica una relazione di tipo detail (generalmente una lista-edit).
   */
  public static final int ROLE_DETAIL = 2;

  /**
   * Il ruolo di questa istanza (vedi ROLE_...)
   */
  protected int role = 0;

  /**
   * La lista per l'editing dei dati details.
   */
  protected String editList = null;

  /**
   * In alcuni casi puo' essere utilizzata una lista di sola visualizzazione
   * del dettaglio (ad esempio permessi insufficienti).
   */
  protected String viewList = null;

  /**
   * Accumulo dei parametri di link fra master e detail.
   * Nella Pair viene memorizzato il nome del campo link (first) ed un eventuale valore di default (second).
   */
  protected HashMap<String, Pair<String, String>> linkParam = new HashMap<>();

  /**
   * Riferimento all'oggeto XML originario che
   * ha creato questo MasterDetailInfo.
   */
  protected Element eleXml = null;

  /**
   * Aggiunge un parametro di link fra master e details.
   * @param nomeParam nome del parametro
   * @param valueParam valore del parametro secondo la sintassi di link.
   */
  public void addLinkParam(String nomeParam, String valueParam)
  {
    linkParam.put(nomeParam, new Pair<String, String>(valueParam, null));
  }

  /**
   * Aggiunge un parametro di link fra master e details.
   * @param nomeParam nome del parametro
   * @param valueParam valore del parametro secondo la sintassi di link.
   */
  public void addLinkParam(String nomeParam, String valueParam, String defaultValue)
  {
    linkParam.put(nomeParam, new Pair<String, String>(valueParam, defaultValue));
  }

  public int getRole()
  {
    return role;
  }

  public void setRole(int role)
  {
    this.role = role;
  }

  public String getEditList()
  {
    return editList;
  }

  public void setEditList(String editList)
  {
    this.editList = editList;
  }

  public String getViewList()
  {
    return viewList;
  }

  public void setViewList(String viewList)
  {
    this.viewList = viewList;
  }

  public Pair<String, String> getParameterInfo(String key)
  {
    return linkParam.get(key);
  }

  public String getParameter(String key)
  {
    return linkParam.get(key).first;
  }

  public String getDefaultValue(String key)
  {
    return linkParam.get(key).second;
  }

  public Element getEleXml()
  {
    return eleXml;
  }

  public void setEleXml(Element eleXml)
  {
    this.eleXml = eleXml;
  }

  @Override
  public Iterator<String> iterator()
  {
    return linkParam.keySet().iterator();
  }
}

