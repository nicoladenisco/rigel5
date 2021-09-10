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
package org.rigel2.table.peer.html;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.rigel2.glue.pager.PeerTablePagerEditApp;
import org.rigel2.table.html.AbstractHtmlTablePager;
import org.rigel2.table.html.hEditTable;

/**
 * Wrapper specializzato per tabelle con edit.
 *
 * @author Nicola De Nisco
 */
public class PeerWrapperEditHtml extends HtmlPeerWrapperBase
{
  public static final String defTableStm = "TABLE WIDTH=100% BORDER=1";

  public PeerWrapperEditHtml()
     throws Exception
  {
    ptm = new org.rigel2.table.peer.html.PeerTableModel();
  }

  public void init()
     throws Exception
  {
    ((org.rigel2.table.peer.html.PeerTableModel) (ptm)).init(objectClass.newInstance());
    ((org.rigel2.table.peer.html.PeerTableModel) (ptm)).attach(tbl);

    tbl.setTableStatement(tableStatement == null ? defTableStm : tableStatement);
    tbl.setHeaderStatement("TR class=\"rigel_table_header_row\"");
    tbl.setColheadStatement("TD class=\"rigel_table_header_cell\"");
    tbl.setModel(ptm);
    tbl.setNosize(nosize);

    if(pager != null)
      ((AbstractHtmlTablePager) pager).setHTable(tbl);
  }

  @Override
  public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception
  {
    if(tbl instanceof hEditTable)
      ((hEditTable) (tbl)).salvaDati(params, pager.start, pager.limit);
  }

  @Override
  public String getHtmlForm(Map params, HttpSession sessione)
     throws Exception
  {
    // carica i parametri dalla map
    if(haveParametri() && (populateParametri(params) || params.get("reload") != null))
    {
      if(pager instanceof PeerTablePagerEditApp)
        ((PeerTablePagerEditApp) pager).reloadAllRecords();
    }

    return ((AbstractHtmlTablePager) pager).getHtml(params, sessione);
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

  public void rebind(List newRows)
     throws Exception
  {
    ((org.rigel2.table.peer.html.PeerTableModel) (ptm)).rebind(newRows);
  }
}
