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
import java.util.regex.Pattern;
import org.rigel5.HtmlUtils;
import org.rigel5.exceptions.MissingColumnException;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * Informazioni per i pulsanti custom nelle liste.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class CustomButtonInfo implements Cloneable
{
  private String url;
  private String javascript;
  private String className;
  private String icon;
  private final Hashtable htparam = new Hashtable();
  private final Hashtable runtimeParameters = new Hashtable();
  private int popup;
  private String text;
  private String confirm;
  private String html;
  private boolean lineEdit;
  private CustomButtonRuntimeInterface cbri = null;
  private final List<CustomButtonInfo> innerButtons = new ArrayList<>();

  public CustomButtonInfo()
  {
  }

  public CustomButtonInfo(String text, String javascript)
  {
    this.javascript = javascript;
    this.text = text;
  }

  @Override
  public Object clone()
     throws CloneNotSupportedException
  {
    CustomButtonInfo cb = (CustomButtonInfo) super.clone();
    cb.innerButtons.clear();
    cb.innerButtons.addAll(innerButtons);
    return cb;
  }

  public void addParam(String paramName, String paramValue)
  {
    htparam.put(paramName, paramValue);
  }

  public void addRuntimeParam(String paramName, Object paramValue)
  {
    runtimeParameters.put(paramName, paramValue);
  }

  public Map getRuntimeParameters()
  {
    return runtimeParameters;
  }

  /**
   * Restituisce url per i bottoni custom di testata.
   * @param ptm table model di riferimento
   * @return url di chiamata
   * @throws java.lang.Exception
   */
  public String makeUrlTestata(RigelTableModel ptm)
     throws Exception
  {
    String param = url;

    Enumeration enumKeys = htparam.keys();
    while(enumKeys.hasMoreElements())
    {
      String nome = (String) enumKeys.nextElement();
      String valore = (String) htparam.get(nome);

      if(valore.contains("@") || valore.contains("#"))
        if((valore = ptm.getValueMacroInside(0, 0, valore, false, false)) == null)
          return null;

      param = HtmlUtils.mergeUrl(param, nome, valore);
    }

    return param;
  }

  /**
   * Per una determinata riga (record del database) produce le informazioni
   * per consentire l'aggancio a funzione custom.
   * @param ptm table model di riferimento
   * @param row indice del record
   * @return la stringa con l'url completa
   * @throws Exception
   */
  public String makeUrlRiga(RigelTableModel ptm, int row)
     throws Exception
  {
    String param = url;

    Enumeration enumKeys = htparam.keys();
    while(enumKeys.hasMoreElements())
    {
      String nome = (String) enumKeys.nextElement();
      String valore = (String) htparam.get(nome);

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

      param = HtmlUtils.mergeUrl(param, nome, valore);
    }

    return param;
  }

  /**
   * Ritorna script di selezione.
   * Per una determinata riga (record del database) produce le informazioni
   * per consentire l'aggancio a funzione custom.
   * Eventuali macro nella forma #caption o @nome vengono risolte.
   * @param row indice del record
   * @param ptm table model di riferimento
   * @return la stringa con il codice javascript
   * @throws Exception
   */
  public String makeJavascript(RigelTableModel ptm, int row)
     throws Exception
  {
    if("#".equals(javascript))
      return javascript;

    // se javascript non contiene macro, lo ritorna secco
    if(!findMacro.matcher(javascript).find())
      return javascript;

    return ptm.getValueMacroInside(row, 0, javascript, false, true);
  }

  public static final Pattern findMacro = Pattern.compile("[\\#\\@]");

  /**
   * Ritorna messaggio di conferma.
   * Per una determinata riga (record del database) produce
   * l'eventuale messaggio di conferma per l'utente.
   * Eventuali macro nella forma #caption o @nome vengono risolte.
   * @param ptm table model di riferimento
   * @param row indice del record
   * @param col colonna del bottone
   * @return la stringa con il messaggio di conferma per l'utente
   * @throws Exception
   */
  public String makeConfirmMessage(RigelTableModel ptm, int row, int col)
     throws Exception
  {
    return ptm.getValueMacroInside(row, col, confirm, false, true);
  }

  /**
   * Ritorna html alternativo.
   * Un custom button può contenere dell'html da utilizzare al posto
   * dell'icona.
   * Eventuali macro nella forma #caption o @nome vengono risolte.
   * @param ptm table model di riferimento
   * @param row indice del record
   * @param col colonna del bottone
   * @return la stringa con il codice html
   * @throws Exception
   */
  public String makeHtmlCustom(RigelTableModel ptm, int row, int col)
     throws Exception
  {
    return ptm.getValueMacroInside(row, col, html, false, true);
  }

  public String getUrl()
  {
    return url;
  }

  public void setUrl(String url)
  {
    this.url = url;
  }

  public String getJavascript()
  {
    return javascript;
  }

  public void setJavascript(String javascript)
  {
    this.javascript = javascript;
  }

  public boolean haveJavascript()
  {
    return this.javascript != null;
  }

  public void setClassName(String className)
  {
    this.className = className;
  }

  public String getClassName()
  {
    return className;
  }

  public boolean haveClassName()
  {
    return this.className != null;
  }

  public void setIcon(String icon)
  {
    this.icon = icon;
  }

  public String getIcon()
  {
    return icon;
  }

  public Hashtable getParam()
  {
    return htparam;
  }

  public void setPopup(int popup)
  {
    this.popup = popup;
  }

  public int getPopup()
  {
    return popup;
  }

  public void setText(String text)
  {
    this.text = text;
  }

  public String getText()
  {
    return text;
  }

  public String getConfirm()
  {
    return confirm;
  }

  public void setConfirm(String confirm)
  {
    this.confirm = confirm;
  }

  public boolean haveConfirm()
  {
    return this.confirm != null;
  }

  public CustomButtonRuntimeInterface getCbri()
  {
    return cbri;
  }

  public void setCbri(CustomButtonRuntimeInterface cbri)
  {
    this.cbri = cbri;
  }

  public boolean haveInnerButtons()
  {
    return !innerButtons.isEmpty();
  }

  public void addInnerButton(CustomButtonInfo innerBut)
  {
    innerButtons.add(innerBut);
  }

  public Iterator<CustomButtonInfo> innerButtonsIterator()
  {
    return innerButtons.iterator();
  }

  public String getHtml()
  {
    return html;
  }

  public void setHtml(String html)
  {
    this.html = html;
  }

  public boolean isLineEdit()
  {
    return lineEdit;
  }

  public void setLineEdit(boolean lineEdit)
  {
    this.lineEdit = lineEdit;
  }
}
