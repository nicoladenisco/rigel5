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
package org.rigel2.table.sql.xml;

import java.io.Writer;
import java.util.*;
import javax.servlet.http.*;
import javax.swing.table.TableColumnModel;
import org.commonlib.utils.StringOper;
import org.rigel2.SetupHolder;
import org.rigel2.db.sql.QueryBuilder;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;
import org.rigel2.table.sql.SqlGroupBy;
import org.rigel2.table.sql.html.HtmlSqlWrapperBase;
import org.rigel2.table.xml.*;

/**
 * Title: wrapper per XML da liste SQL.
 * Description: questo wrapper e' specializzato per coordinare
 * una hTable in congiunzione con un SqlTableModel
 * per consentire la produzione di XML.
 * E' possibile eseguire dei raggruppamenti ricavando
 * delle sezioni master/detail.
 * Lo scopo principale e' realizzare stampe in PDF.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlWrapperListaXml extends HtmlSqlWrapperBase
{
  protected xTable xtbl = null;
  protected xTable tblGroup = null;
  protected RigelTableModel ptmGroup = null;
  protected QueryBuilder query = null;
  protected int limit = 0;
  protected boolean fixedCols = false;
  protected boolean uniqueQuery = false;
  public static final int RECORD_PER_PASSATA = 100;

  public SqlWrapperListaXml()
     throws Exception
  {
    ptm = new SqlTableModel();
    ptmGroup = new SqlTableModel();
  }

  public void init()
     throws Exception
  {
    if(xtbl == null)
      xtbl = new xTable();

    if(ssp.getGroupby() == null)
    {
      ((SqlTableModel) (ptm)).init(ssp.getSelect(), ssp.getFrom(), ssp.getWhere(), getOrderby(), false);
      ((SqlTableModel) (ptm)).attach(xtbl);
      ptmGroup = null;
    }
    else
    {
      ((SqlTableModel) (ptm)).init(ssp.getSelect(), ssp.getFrom(), ssp.getWhere(), null, false);
      ((SqlTableModel) (ptm)).attach(xtbl);

      if(tblGroup == null)
        tblGroup = new xTable();

      // raggruppamento: il table model ptmGroup e' quello per il ciclo
      // esterno e quindi ha la clausola group by
      QueryBuilder qb = ((SqlTableModel) (ptmGroup)).makeQueryBuilder();
      qb.setGroupby(ssp.makeGroupByGroupBy());

      ((SqlTableModel) (ptmGroup)).setQuery(qb);
      ((SqlTableModel) (ptmGroup)).init(
         null,
         ssp.getFrom(), ssp.getWhere(), ssp.getOrderbyGroupby(),
         false);
      ((SqlTableModel) (ptmGroup)).attach(tblGroup);
      tblGroup.normalizeCols();
      //tblGroup.setShowFieldsName(true);

      //log.debug("gs="+makeGroupBySelect());
      //log.debug("gb="+makeGroupByGroupBy());
    }
  }

  public String makeGroupByWhere(int row)
     throws Exception
  {
    SqlGroupBy groupby = ssp.getGroupby();

    if(groupby == null)
      return null;

    if(query == null)
      query = SetupHolder.getQueryBuilder();

    String rv = "";
    for(RigelColumnDescriptor cd : groupby.colonne)
    {
      if(!cd.isAggregatoSql())
      {
        Object valObj = ptmGroup.getRowRecord(row);
        String valore = cd.getValueAsString(valObj);
        rv += " AND " + cd.getName() + "=" + query.adjValue(cd.getDataType(), valore);
      }
    }
    return rv.length() == 0 ? null : rv.substring(5);
  }

  public RigelTableModel getPtmGroup()
  {
    return ptmGroup;
  }

  public void setPtmGroup(RigelTableModel ptmGroup)
  {
    this.ptmGroup = ptmGroup;
  }

  public void getXml(Writer out)
     throws java.lang.Exception
  {
    // predispone per recupero di tutti i records
    QueryBuilder qb1 = ((SqlTableModel) (ptm)).getQuery();
    qb1.setLimit(0);

    if(ssp.getGroupby() == null)
    {
      String where = StringOper.okStrNull(ssp.getWhere());
      if(where == null)
        where = getWhereParametri();
      else
        where += " AND " + getWhereParametri();

      qb1.setWhere(where);
      doXmlParts(xtbl, ((SqlTableModel) (ptm)), out);
    }
    else
    {
      QueryBuilder qb2 = ((SqlTableModel) (ptmGroup)).getQuery();

      String where = ssp.getWhere();
      if(where == null)
        where = getWhereParametri();
      else
        where += " AND " + getWhereParametri();

      qb2.setWhere(where);

      ssp.cumulaHavingParametri();
      qb2.setHaving(ssp.getHaving());

      // predispone per prelevare tutti i records
      qb2.setLimit(0);

      // reinizializza e carica i records
      ((SqlTableModel) (ptmGroup)).rebind();

      tblGroup.setTableStatement("head");
      tblGroup.setHeaderStatement("hfld");
      tblGroup.setRowStatement("hrec");
      String rv = "";

      for(int row = 0; row < ptmGroup.getRowCount(); row++)
      {
        // completa bind per il dettaglio
        String wheredet = ssp.getWhere();
        if(wheredet == null)
          wheredet = makeGroupByWhere(row);
        else
          wheredet += " AND " + makeGroupByWhere(row);

        //log.debug("gw="+wheredet);
        //log.debug("gw="+makeGroupByWhere(row));
        ((SqlTableModel) (ptm)).rebind(ssp.getSelect(), ssp.getFrom(), wheredet, getOrderby());

        // se il dettaglio e' vuoto salta tutto il blocco
        if(ptm.getRowCount() == 0)
          continue;

        // emette xml per il master
        out.write("<group>\r\n");
        out.write("<head>\r\n");
        tblGroup.doHeader(out);
        tblGroup.doRow(out, row);
        out.write("</head>\r\n");

        // emette xml per il dettaglio
        xtbl.doXml(out);
        out.write("</group>\r\n");
      }
    }
  }

  /**
   * Produce XML richiedendo RECORD_PER_PASSATA records al database
   * in una serie di passate successive. Minimizza quindi l'impatto di memoria.
   */
  protected void doXmlParts(xTable xtbl, SqlTableModel stm, Writer out)
     throws Exception
  {
    xtbl.normalizeCols();
    xtbl.clearColSizes();

    out.write("<" + xtbl.getTableStatement() + ">\r\n");
    xtbl.doHeader(out);

    if(uniqueQuery)
    {
      // una query unica per tutti i dati:
      // utile nel caso di tabelle ridefinite
      // con calcolo dei totali o simili
      stm.rebind();
      xtbl.doRows(out);
    }
    else
    {
      // genera XML a blocchi di RECORD_PER_PASSATA records
      // questo e' necessario per grosse tabelle, dove una
      // unica getRecords impegnerebbe pesantemente la memoria
      int rStart = 0;
      do
      {
        stm.getQuery().setOffset(rStart);
        stm.getQuery().setLimit(RECORD_PER_PASSATA);

        stm.rebind();
        xtbl.doRows(out);
        rStart += RECORD_PER_PASSATA;
      }
      while(stm.getRowCount() == RECORD_PER_PASSATA);
    }

    // emette le dimensioni di colonna
    if(fixedCols || xtbl.getArColSizes() == null)
      getFixedSizes(xtbl.getColumnModel(), out);
    else
      getColumnSizes(xtbl.getArColSizes(), xtbl.getColumnModel(), out);

    out.write("</" + xtbl.getTableStatement() + ">\r\n");
  }

  protected void getColumnSizes(int[] arSizes, TableColumnModel tm, Writer out)
     throws Exception
  {
    double total = 0;
    if(arSizes != null)
    {
      out.write("<column-sizes>\r\n");
      for(int i = 0; i < tm.getColumnCount(); i++)
        total += arSizes[i];

      for(int i = 0; i < tm.getColumnCount(); i++)
      {
        RigelColumnDescriptor cd = (RigelColumnDescriptor) tm.getColumn(i);
        float size = (float) (arSizes[i] / total);
        out.write("<column name=\"" + cd.getCaption() + "\" size=\"" + size + "\"/>\r\n");
      }
      out.write("</column-sizes>\r\n");
    }
  }

  protected void getFixedSizes(TableColumnModel tm, Writer out)
     throws Exception
  {
    double total = 0;
    out.write("<column-sizes>\r\n");
    for(int i = 0; i < tm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) tm.getColumn(i);
      total += cd.getSize();
    }

    for(int i = 0; i < tm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) tm.getColumn(i);
      float size = (float) (cd.getSize() / total);
      out.write("<column name=\"" + cd.getCaption() + "\" size=\"" + size + "\"/>\r\n");
    }
    out.write("</column-sizes>\r\n");
  }

  public xTable getXtbl()
  {
    return xtbl;
  }

  public void setXtbl(xTable xtbl)
  {
    this.xtbl = xtbl;
  }

  public xTable getTblGroup()
  {
    return tblGroup;
  }

  public void setTblGroup(xTable tblGroup)
  {
    this.tblGroup = tblGroup;
  }

  public boolean isFixedCols()
  {
    return fixedCols;
  }

  public void setFixedCols(boolean fixedCols)
  {
    this.fixedCols = fixedCols;
  }

  public boolean isUniqueQuery()
  {
    return uniqueQuery;
  }

  public void setUniqueQuery(boolean uniqueQuery)
  {
    this.uniqueQuery = uniqueQuery;
  }

  @Override
  public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String getHtmlForm(Map params, HttpSession sessione)
     throws Exception
  {
    throw new UnsupportedOperationException("Not supported yet.");
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
