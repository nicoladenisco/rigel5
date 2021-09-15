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
package org.rigel5;

import javax.servlet.http.HttpSession;
import org.rigel5.table.html.AbstractHtmlTablePager;
import org.rigel5.table.html.AbstractHtmlTablePagerFilter;
import org.rigel5.table.html.PageComponentType;
import org.rigel5.table.html.RigelHtmlPage;
import org.rigel5.table.html.RigelHtmlPageComponent;

/**
 * Implementazione di default del RigelUIManager.
 * @author Nicola De Nisco
 */
public class DefaultUIManager implements RigelUIManager
{
  @Override
  public String formatHtmlLista(int filtro, RigelHtmlPage page)
     throws Exception
  {
    StringBuilder sb = new StringBuilder(4096);

    // prima emette tutti i commenti
    page.buildPart(sb, PageComponentType.COMMENT, "<!--", "-->");

    // quindi tutti i javascript
    page.buildPart(sb, PageComponentType.JAVASCRIPT, "<SCRIPT LANGUAGE=\"JavaScript\">", "</SCRIPT>");

    if(filtro == AbstractHtmlTablePagerFilter.FILTRO_MACHERA)
    {
      // html ricerca avanzata
      page.buildPart(sb, PageComponentType.HTML, "search", "", "");
    }
    else
    {
      // html ricerca semplice
      page.buildPart(sb, PageComponentType.HTML, "simplesearch", "", "");

      // html principale
      sb.append("<div class=\"rigel_body\">");
      page.buildPart(sb, PageComponentType.HTML, "body", "", "");
      sb.append("</div>");

      // html navigazione
      page.buildPart(sb, PageComponentType.HTML, "nav", "", "");
    }

    return sb.toString();
  }

  @Override
  public String formatHtmlForm(RigelHtmlPage page)
     throws Exception
  {
    StringBuilder sb = new StringBuilder(4096);

    // prima emette tutti i commenti
    page.buildPart(sb, PageComponentType.COMMENT, "<!--", "-->");

    // quindi tutti i javascript
    page.buildPart(sb, PageComponentType.JAVASCRIPT, "<SCRIPT LANGUAGE=\"JavaScript\">", "</SCRIPT>");

    // html principale
    sb.append("<div class=\"rigel_body\">");
    page.buildPart(sb, PageComponentType.HTML, "body", "", "");
    sb.append("</div>");

    return sb.toString();
  }

  @Override
  public String formatSimpleSearch(int filtro, RigelHtmlPage page)
     throws Exception
  {
    return formatHtmlLista(filtro, page);
  }

  @Override
  public String formatSimpleSearchPalmare(int filtro, RigelHtmlPage page)
     throws Exception
  {
    return formatHtmlLista(filtro, page);
  }

  /**
   * Restituisce la barra inferiore di navigazione.
   * Nella barra inferiore viene indicato sulla sinistra il navigatore
   * per numeri di pagina, al centro l'indicazione con la pagina corrente
   * e le pagine totali, sulla sinistra un navigatore del tipo precedente
   * - successivo.
   * @param pagCurr the value of pagCurr
   * @param numPagine the value of numPagine
   * @param limit the value of limit
   * @param tp the value of tp
   * @param sessione the value of sessione
   * @param page the value of page
   * @throws java.lang.Exception
   */
  @Override
  public void addHtmlNavRecord(int pagCurr, int numPagine, int limit, AbstractHtmlTablePager tp, HttpSession sessione, RigelHtmlPage page)
     throws Exception
  {
    RigelHtmlPageComponent html = new RigelHtmlPageComponent(PageComponentType.HTML, "nav");

    String sLeft = "";
    if(numPagine < 10)
    {
      for(int i = 0; i < numPagine; i++)
      {
        sLeft += getHtmlPiedeNav(tp, i, sessione);
        if(i < numPagine - 1)
          sLeft += "-";
      }
    }
    else
    {
      int middleLeft = pagCurr / 2;
      int middleRight = (numPagine + pagCurr) / 2;

      // prima pagina
      if(pagCurr > 1)
        sLeft += getHtmlPiedeNav(tp, 0, sessione) + "-";

      // tre pagine con quella corrente al centro
      if(pagCurr > 0)
        sLeft += getHtmlPiedeNav(tp, middleLeft, sessione) + "-";

      // tre pagine con quella corrente al centro
      sLeft += getHtmlPiedeNav(tp, pagCurr, sessione);

      // tre pagine con quella corrente al centro
      if(pagCurr < numPagine - 1)
        sLeft += "-" + getHtmlPiedeNav(tp, middleRight, sessione);

      // ultima pagina
      if(pagCurr < numPagine - 2)
        sLeft += "-" + getHtmlPiedeNav(tp, numPagine - 1, sessione);
    }

    String sCenter = "Pag. " + (pagCurr + 1) + " / " + numPagine;

    String sRight = "";
    sRight += pagCurr > 0
                 ? HtmlUtils.makeHrefNoenc(tp.getSelfUrl((pagCurr - 1) * limit, sessione), "Prec.", "rigel_navattivo")
                 : "<span class=\"rigel_navdisattivo\">Prec.</span>";
    sRight += " - ";
    sRight += pagCurr < (numPagine - 1)
                 ? HtmlUtils.makeHrefNoenc(tp.getSelfUrl((pagCurr + 1) * limit, sessione), "Succ.", "rigel_navattivo")
                 : "<span class=\"rigel_navdisattivo\">Succ.</span>";

    html.append(
       "<table width=100% border=0 cellspacing=0 cellpadding=1><TR BGCOLOR=\"navy\">\r\n"
       + "<TD width=33% class=\"rigel_page_number\" align=left>").append(sLeft).append("</td>\r\n"
       + "<TD width=33% class=\"rigel_page_number\" align=center>").append(sCenter).append("</td>\r\n"
       + "<TD width=33% class=\"rigel_page_number\" align=right>").append(sRight).append("</td>\r\n</tr></table>\r\n");

    page.add(html);
  }

  /**
   * Ritorna l'HTML per il piede della pagina.
   * @param tp the value of tp
   * @param pagina
   * @param sessione the value of sessione
   * @throws Exception
   * @return the java.lang.String
   */
  protected String getHtmlPiedeNav(AbstractHtmlTablePager tp, int pagina, HttpSession sessione)
     throws Exception
  {
    return HtmlUtils.makeHrefNoenc(tp.getSelfUrl(pagina * tp.limit, sessione),
       "" + (pagina + 1), "rigel_navattivo");
  }
}
