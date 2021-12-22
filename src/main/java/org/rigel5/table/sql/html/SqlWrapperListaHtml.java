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
package org.rigel5.table.sql.html;

import java.util.*;
import javax.servlet.http.*;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.table.html.AbstractHtmlTablePager;
import org.rigel5.table.html.AbstractHtmlTablePagerFilter;
import org.rigel5.table.html.CommonPager;

/**
 * <p>
 * Title: wrapper per VIEW in HTML da liste SQL.</p>
 * <p>
 * Description: questo wrapper e' specializzato per coordinare
 * una hTable in congiunzione con un SqlTableModel e un SqlListaElem
 * per consentire la visualizzazione paginata di una query libera.</p>
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlWrapperListaHtml extends HtmlSqlWrapperBase
{
  public static final String defTableStm = "TABLE WIDTH=100% BORDER=0 CELLSPACING=0";

  public SqlWrapperListaHtml()
  {
    ptm = new org.rigel5.table.sql.html.SqlTableModel();
  }

  private org.rigel5.table.sql.html.SqlTableModel getTM()
  {
    return ((org.rigel5.table.sql.html.SqlTableModel) (ptm));
  }

  public void init(QueryBuilder qb)
     throws Exception
  {
    SqlTableModel tm = getTM();
    qb.setSelect(ssp.getSelect());
    qb.setFrom(ssp.getFrom());
    qb.setWhere(ssp.getWhere());
    qb.setOrderby(getOrderby());
    qb.setGroupby(ssp.makeGroupBySelect());
    qb.setHaving(ssp.getHaving());
    qb.setDeleteFrom(ssp.getDeleteFrom());

    tm.init(qb, false);
    tm.attach(tbl);

    tbl.setTableStatement(tableStatement == null ? defTableStm : tableStatement);
    tbl.setHeaderStatement("TR class=\"rigel_table_header_row\"");
    tbl.setColheadStatement("TD class=\"rigel_table_header_cell\"");
    tbl.setNosize(nosize);
    tbl.setModel(ptm);

    ((AbstractHtmlTablePager) pager).setHTable(tbl);

    if(pager instanceof AbstractHtmlTablePagerFilter && nome != null)
    {
      if(((AbstractHtmlTablePagerFilter) (pager)).getIdPager() == null)
        ((AbstractHtmlTablePagerFilter) (pager)).setIdPager("SqlWrapperListaHtml:" + nome);
    }
  }

  @Override
  public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception
  {
  }

  @Override
  public String getHtmlForm(Map params, HttpSession sessione)
     throws Exception
  {
    if(haveParametri())
      rebindQuery(params);

    return ((AbstractHtmlTablePager) pager).getHtml(params, sessione);
  }

  @Override
  public String getHtmlLista(Map params, HttpSession sessione)
     throws java.lang.Exception
  {
    if(haveParametri())
      rebindQuery(params);

    if(pager instanceof CommonPager)
      return ((CommonPager) (pager)).getHtmlSimpleSearch(params, sessione);

    return ((AbstractHtmlTablePager) pager).getHtml(params, sessione);
  }

  @Override
  public String getHtmlListaPalmare(Map params, HttpSession sessione)
     throws Exception
  {
    if(haveParametri())
      rebindQuery(params);

    if(pager instanceof CommonPager)
      return ((CommonPager) (pager)).getHtmlSimpleSearchPalmare(params, sessione);

    return ((AbstractHtmlTablePager) pager).getHtml(params, sessione);
  }

  private void rebindQuery(Map params)
     throws Exception
  {
    // carica i parametri dalla map
    populateParametri(params);

    SqlTableModel tm = getTM();
    tm.getQuery().setParametri(getFiltroParametri());
  }
}
