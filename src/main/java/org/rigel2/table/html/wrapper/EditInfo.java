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
package org.rigel2.table.html.wrapper;

import java.util.*;
import org.rigel2.HtmlUtils;

/**
 * Parametri per il collegamento liste-form.
 *
 * @author Nicola De Nisco
 */
public class EditInfo
{
  /**
   * Url del form per la modifica del record.
   * Utilizzato solo nelle liste.
   */
  protected String urlEditRiga = null;
  /**
   * Modalità di popup.
   * Se diverso da zero indica edit in una finestra
   * di popup. Il valore indica il tipo di finestra.
   */
  protected int popupMode = 0;
  /**
   * Parametri di input (se usato nel form) o di
   * output (se usato nella lista) per collegare lista e form.
   */
  protected Hashtable urlEditRigaParam = new Hashtable();

  void clearEditInfo()
  {
    urlEditRiga = null;
    urlEditRigaParam.clear();
  }

  public Hashtable getUrlEditRigaParam()
  {
    return urlEditRigaParam;
  }

  public void addParamEditRiga(String nomeParam, String valueParam)
  {
    urlEditRigaParam.put(nomeParam, valueParam);
  }

  public void setUrlEditRiga(String urlEditRiga)
  {
    this.urlEditRiga = urlEditRiga;
  }

  public String getUrlEditRiga()
  {
    return urlEditRiga;
  }

  public boolean haveEditRiga()
  {
    //return urlEditRiga != null && !urlEditRigaParam.isEmpty();
    return urlEditRiga != null;
  }

  public boolean isEditRigaJavascript()
  {
    return haveEditRiga() && HtmlUtils.isJavascriptBegin(urlEditRiga);
  }

  public boolean haveEditParam()
  {
    return !urlEditRigaParam.isEmpty();
  }

  public Enumeration getEnumParamsKey()
  {
    // l'enumeratore ritorna le chiavi in ordine alfabetico
    return new Enumeration()
    {
      List<String> lss = Collections.list(urlEditRigaParam.keys());
      Iterator itr = null;

      @Override
      public boolean hasMoreElements()
      {
        if(itr == null)
        {
          Collections.sort(lss);
          itr = lss.iterator();
        }

        return itr.hasNext();
      }

      @Override
      public Object nextElement()
      {
        return itr == null ? null : itr.next();
      }
    };
  }

  public String getParam(String key)
  {
    return (String) urlEditRigaParam.get(key);
  }

  public int getPopupMode()
  {
    return popupMode;
  }

  public void setPopupMode(int popupMode)
  {
    this.popupMode = popupMode;
  }
}
