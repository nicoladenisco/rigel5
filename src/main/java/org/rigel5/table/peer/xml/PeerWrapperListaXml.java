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
package org.rigel5.table.peer.xml;

import java.io.Writer;
import java.util.*;
import javax.servlet.http.*;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.map.ColumnMap;
import org.apache.torque.sql.OrderBy;
import org.apache.torque.util.UniqueList;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.peer.PeerAbstractTableModel;
import org.rigel5.table.peer.html.HtmlPeerWrapperBase;
import org.rigel5.table.xml.xTable;

/**
 * Wrapper per la generazione di XML.
 *
 * @author Nicola De Nisco
 */
public class PeerWrapperListaXml extends HtmlPeerWrapperBase
{
  protected xTable xtbl;
  protected int limit = 0;

  public static final int RECORD_PER_PASSATA = 100;

  public PeerWrapperListaXml()
     throws Exception
  {
    ptm = new org.rigel5.table.peer.xml.PeerTableModel();
  }

  public void init()
     throws Exception
  {
    ((org.rigel5.table.peer.xml.PeerTableModel) ptm).attach(xtbl);

    // imposta numero per pagina
    if(getNumPerPage() != 0)
      limit = getNumPerPage();

    ((org.rigel5.table.peer.xml.PeerTableModel) ptm).init(getObjectClass().newInstance());
  }

  public void getXml(Writer out)
     throws java.lang.Exception
  {
    xtbl.setShowHeader(true); // abilita generazione header

    if(limit == 0)
    {
      // genera XML a blocchi di RECORD_PER_PASSATA records
      // questo e' necessario per grosse tabelle, dove una
      // unica getRecords impegnerebbe pesantemente la memoria
      int recStart = 0;
      boolean contFetch = true;

      xtbl.normalizeCols();
      out.write("<" + xtbl.getTableStatement() + ">\r\n");

      xtbl.doHeader(out);

      do
      {
        Criteria c = new Criteria();
        c.setOffset(recStart);
        c.setLimit(RECORD_PER_PASSATA);
        List data = getRecords(c);
        contFetch = !(data.size() < RECORD_PER_PASSATA);
        recStart += data.size();
        ((org.rigel5.table.peer.xml.PeerTableModel) ptm).rebind(data);
        xtbl.doRows(out);
      }
      while(contFetch);

      out.write("</" + xtbl.getTableStatement() + ">\r\n");
    }
    else
    {
      ((org.rigel5.table.peer.xml.PeerTableModel) ptm).rebind(getRecords(new Criteria()));
      xtbl.doXml(out);
    }
  }

  @Override
  public List getRecords(Criteria c)
     throws Exception
  {
    // attiva ordinametno di default se previsto
    for(String nomeCol : sortColumns)
    {
      boolean descOrder = false;
      if(nomeCol.startsWith("!"))
      {
        descOrder = true;
        nomeCol = nomeCol.substring(1);
      }

      RigelColumnDescriptor cd = ptm.getColumn(nomeCol);
      if(cd == null)
        throw new Exception("Colonna di sort " + nomeCol + " non trovata!");

      ColumnMap campo = tmap.getCampo(cd.getName());
      if(descOrder)
        c.addDescendingOrderByColumn(campo);
      else
        c.addAscendingOrderByColumn(campo);
    }

    // usa ordinamento di default per chiave primaria
    PeerAbstractTableModel pptm = (PeerAbstractTableModel) ptm;
    if((c.getOrderByColumns() == null || c.getOrderByColumns().isEmpty()) && pptm.getDefaultOrderCriteria() != null)
    {
      // copia le colonne di ordinamento dal criteria di default a quello corrente
      UniqueList<OrderBy> oc = pptm.getDefaultOrderCriteria().getOrderByColumns();
      for(OrderBy ob : oc)
      {
        if(ob.getOrder().equals(SqlEnum.ASC))
          c.addAscendingOrderByColumn(ob.getColumn());
        else
          c.addDescendingOrderByColumn(ob.getColumn());
      }
    }

    if(limit != 0)
      c.setLimit(limit);

    return doSelect(c);
  }

  public int getLimit()
  {
    return limit;
  }

  public void setLimit(int limit)
  {
    this.limit = limit;
  }

  public xTable getXtbl()
  {
    return xtbl;
  }

  public void setXtbl(xTable xtbl)
  {
    this.xtbl = xtbl;
  }

  @Override
  public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception
  {
    throw new UnsupportedOperationException("Method getHtml() not yet implemented.");
  }

  @Override
  public String getHtmlForm(Map params, HttpSession sessione)
     throws Exception
  {
    throw new UnsupportedOperationException("Method getHtml() not yet implemented.");
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
