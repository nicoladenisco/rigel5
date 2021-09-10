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
package org.rigel2.table.sql.html;

import java.util.*;
import javax.servlet.http.*;
import org.rigel2.RigelUIManager;
import org.rigel2.SetupHolder;
import org.rigel2.exceptions.InvalidObjectException;
import org.rigel2.table.html.FormTable;
import org.rigel2.table.html.RigelHtmlPage;

/**
 * Wrapper per i form HTML.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlWrapperFormHtml extends HtmlSqlWrapperBase
{
  public static final String defTableStm = "TABLE WIDTH=100% BORDER=1";

  public SqlWrapperFormHtml()
     throws Exception
  {
    ptm = new org.rigel2.table.sql.html.SqlTableModel();
  }

  private org.rigel2.table.sql.html.SqlTableModel getTM()
  {
    return ((org.rigel2.table.sql.html.SqlTableModel) (ptm));
  }

  public void init()
     throws Exception
  {
    getTM().init(ssp.getSelect(), ssp.getFrom(), ssp.getWhere(), getOrderby(), false);
    getTM().getQuery().setDeleteFrom(ssp.getDeleteFrom());
    getTM().attach(tbl);

    tbl.setTableStatement(tableStatement == null ? defTableStm : tableStatement);
    tbl.setModel(ptm);
    tbl.setNosize(nosize);
  }

  @Override
  public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception
  {
    if(tbl instanceof FormTable)
      ((FormTable) (tbl)).salvaDati(params);
  }

  @Override
  public String getHtmlForm(Map params, HttpSession sessione)
     throws Exception
  {
    if(tbl instanceof FormTable)
    {
      RigelHtmlPage page = new RigelHtmlPage();
      ((FormTable) (tbl)).doHtmlUnico(page);

      // assembla tutte le componenti tranne i JAVASCRIPT_PART che sono molto specifici
      RigelUIManager uim = SetupHolder.getUiManager();
      return uim.formatHtmlForm(page);
    }

    throw new InvalidObjectException("La tabella associata DEVE essere una FormTable.");
  }

  @Override
  public String getHtmlLista(Map params, HttpSession sessione)
     throws Exception
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String getHtmlListaPalmare(Map params, HttpSession sessione)
     throws Exception
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
