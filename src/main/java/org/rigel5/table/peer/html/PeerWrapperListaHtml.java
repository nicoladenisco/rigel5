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

import java.util.*;
import javax.servlet.http.HttpSession;
import org.rigel5.exceptions.MissingColumnException;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.html.AbstractHtmlTablePager;
import org.rigel5.table.html.AbstractHtmlTablePagerFilter;
import org.rigel5.table.html.CommonPager;

/**
 * Wrapper specializzato per tabelle di visualizzazione HTML.
 *
 * @author Nicola De Nisco
 */
public class PeerWrapperListaHtml extends HtmlPeerWrapperBase
{
  public static final String defTableStm = "TABLE WIDTH=100% BORDER=0 CELLSPACING=0";

  public PeerWrapperListaHtml()
     throws Exception
  {
    ptm = new org.rigel5.table.peer.html.PeerTableModel();
  }

  public void init()
     throws Exception
  {
    ((org.rigel5.table.peer.html.PeerTableModel) (ptm)).init(objectClass.newInstance());
    ((org.rigel5.table.peer.html.PeerTableModel) (ptm)).attach(tbl);

    tbl.setTableStatement(tableStatement == null ? defTableStm : tableStatement);
    tbl.setHeaderStatement("TR class=\"rigel_table_header_row\"");
    tbl.setColheadStatement("TD class=\"rigel_table_header_cell\"");
    tbl.setNosize(nosize);
    tbl.setModel(ptm);

    ((AbstractHtmlTablePager) pager).setHTable(tbl);

    if(pager instanceof AbstractHtmlTablePagerFilter)
      ((AbstractHtmlTablePagerFilter) (pager)).setIdPager("PeerWrapperListaHtml:" + nome);

    // verifica se il pager ha dei ricerca-semplice impostati
    boolean haveSimpleSearch = false;
    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      if(ptm.getColumn(i).getRicercaSemplice() > 0)
      {
        haveSimpleSearch = true;
        break;
      }
    }

    // se non ci sono ricerca-semplice allora tenta di forzare
    // come tale quelle colonne marcate come foreign server
    if(!haveSimpleSearch && foInfo != null)
    {
      Enumeration enumCol = foInfo.getForeignColumnsKeys();
      while(enumCol.hasMoreElements())
      {
        String nomecmp = (String) enumCol.nextElement();
        String colonna = foInfo.getParam(nomecmp);

        RigelColumnDescriptor cd = ptm.getColumn(colonna);
        if(cd == null)
          throw new MissingColumnException("Foreign-server: colonna " + colonna + " (" + nomecmp + ") non presente!");

        cd.setRicercaSemplice(1);
      }
    }
  }

  /**
   * La pager non e' un form per cui questa funzione e' vuota.
   * @param params
   * @param sessione
   * @throws Exception
   */
  @Override
  public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception
  {
  }

  /**
   * Produce l'HTML della pager ordinaria.
   * @param params
   * @param sessione
   * @return
   * @throws Exception
   */
  @Override
  public String getHtmlForm(Map params, HttpSession sessione)
     throws Exception
  {
    if(pager instanceof AbstractHtmlTablePagerFilter)
      return ((AbstractHtmlTablePagerFilter) (pager)).getHtml(params, sessione);

    return ((AbstractHtmlTablePager) pager).getHtml(params, sessione);
  }

  /**
   * Produce l'HTML della pager ordinaria.
   * Introduce una riga con la ricerca semplice.
   * @param params
   * @param sessione
   * @return
   * @throws Exception
   */
  @Override
  public String getHtmlLista(Map params, HttpSession sessione)
     throws Exception
  {
    if(pager instanceof CommonPager)
      return ((CommonPager) (pager)).getHtmlSimpleSearch(params, sessione);

    return ((AbstractHtmlTablePager) pager).getHtml(params, sessione);
  }

  /**
   * Produce l'HTML della pager ordinaria.
   * L'HTML prodotto e' piu' compatto, adatto alla visualizzazione
   * su un palmare o comunque una device con schermo ridotto.
   * @param params
   * @param sessione
   * @return
   * @throws Exception
   */
  @Override
  public String getHtmlListaPalmare(Map params, HttpSession sessione)
     throws Exception
  {
    if(pager instanceof CommonPager)
      return ((CommonPager) (pager)).getHtmlSimpleSearchPalmare(params, sessione);

    return ((AbstractHtmlTablePager) pager).getHtml(params, sessione);
  }
}
