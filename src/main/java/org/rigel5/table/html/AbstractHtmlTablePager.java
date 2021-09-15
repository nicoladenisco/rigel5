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
package org.rigel5.table.html;

import java.util.*;
import javax.servlet.http.HttpSession;
import org.rigel5.HtmlUtils;
import org.rigel5.RigelI18nInterface;
import org.rigel5.RigelUIManager;
import org.rigel5.SetupHolder;
import org.rigel5.table.AbstractTablePager;
import org.rigel5.table.RigelTableModel;

/**
 * Paginatore per tabelle.
 * Implementazione standard di un paginatore per tabelle html.
 * Puo' essere utilizzato insieme a hTable, hEditTable, FormTable.
 * Consente di scorrere le informazioni per pagine con un controllo
 * avanti-indietro.
 * Attenzione: il paginatore mantiene dei dati al suo interno,
 * quindi non puo' essere utilizzato fra utenti diversi (sessioni diverse).
 * Occorre crearne uno per ogni nuova sessione.
 * @author Nicola De Nisco
 * @version 1.0
 */
public class AbstractHtmlTablePager extends AbstractTablePager
{
  public String sRstart = "rstart";
  protected String baseSelfUrl = null;
  protected hTable table = null;
  protected RigelUIManager uim = SetupHolder.getUiManager();

  public void setHTable(hTable newHtmlTable)
  {
    table = newHtmlTable;
    table.i18n = i18n;
  }

  public hTable getHTable()
  {
    return table;
  }

  public RigelUIManager getUim()
  {
    return uim;
  }

  public void setUim(RigelUIManager uim)
  {
    this.uim = uim;
  }

  @Override
  public void setI18n(RigelI18nInterface i18n)
  {
    super.setI18n(i18n);

    if(table != null)
      table.i18n = i18n;
  }

  /**
   * Ritorna l'HTML della tabella.
   * Viene utilizzata da getHtml(Map params).
   * @param page the value of page
   * @throws Exception
   */
  protected void getHtmlTable(RigelHtmlPage page)
     throws Exception
  {
    table.doHtml(start, limit, page);
  }

  /**
   * Funzione principale per la visualizzazione della pagina.
   * Dai parametri recupera il record di partenza.
   * @param params the value of params
   * @param sessione the value of sessione
   * @param page the value of page
   * @throws java.lang.Exception
   */
  public void getHtml(Map params, HttpSession sessione, RigelHtmlPage page)
     throws Exception
  {
    String sStart = (String) (params.get(sRstart));
    if(sStart != null && sStart.trim().length() > 0)
      start = Integer.parseInt(sStart.trim());

    long totalRecords = getTotalRecords();

    getHtmlTable(page);

    if(totalRecords > limit)
      getHtmlNavRecord(totalRecords, sessione, page);
  }

  /**
   * Restituisce la barra inferiore di navigazione.
   * Nella barra inferiore viene indicato sulla sinistra il navigatore
   * per numeri di pagina, al centro l'indicazione con la pagina corrente
   * e le pagine totali, sulla sinistra un navigatore del tipo precedente
   * - successivo.
   * @param numRecords the value of numRecords
   * @param sessione the value of sessione
   * @param page the value of page
   * @throws java.lang.Exception
   */
  public void getHtmlNavRecord(long numRecords, HttpSession sessione, RigelHtmlPage page)
     throws Exception
  {
    calcNumPagine(numRecords);
    pagCurr = (int) (start / limit);

    uim.addHtmlNavRecord(pagCurr, numPagine, limit, this, sessione, page);
  }

  /**
   * Ritorna URL alla pagina che contiene la tabella.
   * Da ridefinire nelle classi derivate: restituisce l'url della pagina
   * in cui viene inserita la tabella con la relativa indicazione del record
   * di start.
   * @param prefix
   * @param rStart
   * @return
   * @throws Exception
   */
  public String getSelfUrl(String prefix, int rStart)
     throws Exception
  {
    if(baseSelfUrl == null)
      throw new Exception(i18n.msg(
         "Non posso creare la selfUrl: baseSelfUrl deve essere impostata "
         + "oppure getSelfUrl() deve essere ridefinita."));

    return HtmlUtils.mergeUrl(baseSelfUrl, prefix, rStart);
  }

  public String getSelfUrl(int rStart, HttpSession sessione)
     throws Exception
  {
    return getSelfUrl(sRstart, rStart);
  }

  public String getSortUrl(String fldName)
  {
    return null;
  }

  public void setBaseSelfUrl(String baseSelfUrl)
  {
    this.baseSelfUrl = baseSelfUrl;
  }

  public String getBaseSelfUrl()
  {
    return baseSelfUrl;
  }

  @Override
  public RigelTableModel getRigelTableModel()
  {
    if(table.getModel() instanceof RigelTableModel)
    {
      return ((RigelTableModel) (table.getModel()));
    }
    return null;
  }

  /**
   * Ritorna l'html per la pagina.
   * @param params parametri della richiesta HTML
   * @param sessione sessione della richiesta
   * @return HTML completo
   * @throws Exception
   */
  public String getHtml(Map params, HttpSession sessione)
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtml(params, sessione, page);
    return uim.formatHtmlLista(0, page);
  }

  /**
   * Ritorna l'html per la pagina con inclusa la
   * ricerca semplice per i campi appositamente
   * etichettati.
   * @param params parametri della richiesta HTML
   * @param sessione sessione della richiesta
   * @return l'html da visualizzare
   * @throws java.lang.Exception
   */
  public String getHtmlSimpleSearch(Map params, HttpSession sessione)
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtml(params, sessione, page);
    return uim.formatSimpleSearch(0, page);
  }

  /**
   * Ritorna l'html per la pagina con inclusa la
   * ricerca semplice per i campi appositamente
   * etichettati.
   * @param params parametri della richiesta HTML
   * @param sessione sessione della richiesta
   * @return l'html da visualizzare
   * @throws java.lang.Exception
   */
  public String getHtmlSimpleSearchPalmare(Map params, HttpSession sessione)
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    getHtml(params, sessione, page);
    return uim.formatSimpleSearchPalmare(0, page);
  }

}
