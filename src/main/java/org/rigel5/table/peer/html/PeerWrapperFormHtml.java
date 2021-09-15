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

import java.util.Map;
import javax.servlet.http.HttpSession;
import org.apache.torque.om.Persistent;
import org.rigel5.RigelUIManager;
import org.rigel5.exceptions.InvalidObjectException;
import org.rigel5.table.html.FormTable;
import org.rigel5.table.html.RigelHtmlPage;

/**
 * Wrapper specializzato per form HTML.
 *
 * @author Nicola De Nisco
 */
public class PeerWrapperFormHtml extends HtmlPeerWrapperBase
{
  protected PeerWrapperEditHtml detail = null;
  public static final String defTableStm = "TABLE WIDTH=100% BORDER=1";

  public PeerWrapperFormHtml()
     throws Exception
  {
    ptm = new org.rigel5.table.peer.html.PeerTableModel();
  }

  @Override
  public void setUim(RigelUIManager uim)
  {
    super.setUim(uim);
    if(detail != null)
      detail.setUim(uim);
  }

  public void init()
     throws Exception
  {
    if(tbl instanceof FormTable)
      ((FormTable) tbl).setColonne(numColonne);

    ((org.rigel5.table.peer.html.PeerTableModel) (ptm)).init(objectClass.newInstance());
    ((org.rigel5.table.peer.html.PeerTableModel) (ptm)).attach(tbl);
    tbl.setTableStatement(tableStatement == null ? defTableStm : tableStatement);
    tbl.setModel(ptm);
    tbl.setNosize(nosize);
  }

  public void init(Persistent obj)
     throws Exception
  {
    if(tbl instanceof FormTable)
      ((FormTable) tbl).setColonne(numColonne);

    if(!objectClass.isInstance(obj))
      throw new Exception("The object is not an instance of " + objectClass.getName());

    ((org.rigel5.table.peer.html.PeerTableModel) (ptm)).init(obj);
    ((org.rigel5.table.peer.html.PeerTableModel) (ptm)).attach(tbl);
    tbl.setTableStatement(tableStatement == null ? defTableStm : tableStatement);
    tbl.setModel(ptm);
    tbl.setNosize(nosize);
  }

  public void rebind(Object obj)
     throws Exception
  {
    ((org.rigel5.table.peer.html.PeerTableModel) (ptm)).rebind(obj);
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
      return uim.formatHtmlForm(page);
    }

    throw new InvalidObjectException("La tabella associata DEVE essere una FormTable.");
  }

  public PeerWrapperEditHtml getDetail()
  {
    return detail;
  }

  public void setDetail(PeerWrapperEditHtml detail)
  {
    this.detail = detail;
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
