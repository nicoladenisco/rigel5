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

import java.awt.*;
import java.text.Format;
import java.util.*;
import javax.swing.table.*;
import org.apache.commons.logging.*;
import org.commonlib5.utils.*;
import org.rigel5.RigelCustomUrlBuilder;
import org.rigel5.RigelExtendedFormat;
import org.rigel5.RigelI18nInterface;
import org.rigel5.SetupHolder;
import org.rigel5.table.*;

/**
 * <p>
 * Title: Rigel</p>
 * <p>
 * Description: Visualizzatore di tabella HTML.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class hTable
{
  /** Logging */
  private static Log log = LogFactory.getLog(hTable.class);
  protected TableModel tableModel;
  protected TableColumnModel columnModel;
  protected String tableStatement;
  protected String rowStatement;
  protected String colStatement;
  protected String headerStatement;
  protected boolean showHeader = true;
  protected String colheadStatement;
  protected int normWidth[];
  protected String imgEditData = null;
  protected String imgEditForeign = null;
  protected int strartRow = 0, numRows = 0, numCols = 0;
  protected String imgFormForeign = null;
  protected boolean nosize = false;
  protected boolean popup = false;
  protected boolean editPopup = false;
  protected String rowTip = null;
  protected String formName = "fo"; // aggiornato dal RigelTableModel
  protected RigelCustomUrlBuilder urlBuilder = null;
  protected RigelI18nInterface i18n = null;
  protected Map<String, String> extraParamsUrls = Collections.EMPTY_MAP;
  protected RigelHtmlPageComponent html = new RigelHtmlPageComponent(PageComponentType.HTML, "body");
  protected RigelHtmlPageComponent javascript = new RigelHtmlPageComponent(PageComponentType.JAVASCRIPT, "body");

  public hTable()
  {
    tableStatement = "TABLE WIDTH=\"100%\"";
    headerStatement = "TR";
    rowStatement = "TR";
    colheadStatement = "TD valign=\"middle\"";
    colStatement = "TD valign=\"middle\"";
    urlBuilder = SetupHolder.getUrlBuilder();
    i18n = SetupHolder.getRi18n();
  }

  public void setModel(TableModel newTableModel)
  {
    tableModel = newTableModel;

    // se il numero di colonne è grande disabilita l'emissione di size
    if(SetupHolder.getNoSizeLimit() != 0
       && tableModel.getColumnCount() > SetupHolder.getNoSizeLimit())
      nosize = true;
  }

  public TableModel getModel()
  {
    return tableModel;
  }

  public void setColumnModel(TableColumnModel newColumnModel)
  {
    columnModel = newColumnModel;
  }

  public TableColumnModel getColumnModel()
  {
    return columnModel;
  }

  public void setShowHeader(boolean newShowHeader)
  {
    showHeader = newShowHeader;
  }

  public boolean isShowHeader()
  {
    return showHeader;
  }

  public void setTableStatement(String newTableStatement)
  {
    tableStatement = newTableStatement;
  }

  public String getTableStatement()
  {
    return tableStatement;
  }

  public void setRowStatement(String newRowStatement)
  {
    rowStatement = newRowStatement;
  }

  public String getRowStatement()
  {
    return rowStatement;
  }

  public void setColStatement(String newColStatement)
  {
    colStatement = newColStatement;
  }

  public String getColStatement()
  {
    return colStatement;
  }

  public void setHeaderStatement(String newHeaderStatement)
  {
    headerStatement = newHeaderStatement;
  }

  public String getHeaderStatement()
  {
    return headerStatement;
  }

  public void setColheadStatement(String newColheadStatement)
  {
    colheadStatement = newColheadStatement;
  }

  public String getColheadStatement()
  {
    return colheadStatement;
  }

  public boolean isNosize()
  {
    return nosize;
  }

  public void setNosize(boolean nosize)
  {
    this.nosize = nosize;
  }

  public String getRowTip()
  {
    return rowTip;
  }

  public void setRowTip(String rowTip)
  {
    this.rowTip = rowTip;
  }

  public boolean isPopup()
  {
    return popup;
  }

  public void setPopup(boolean popup)
  {
    this.popup = popup;
  }

  public boolean isEditPopup()
  {
    return editPopup;
  }

  public void setEditPopup(boolean editPopup)
  {
    this.editPopup = editPopup;
  }

  public RigelCustomUrlBuilder getUrlBuilder()
  {
    return urlBuilder;
  }

  public void setUrlBuilder(RigelCustomUrlBuilder urlBuilder)
  {
    this.urlBuilder = urlBuilder;
  }

  public RigelI18nInterface getI18n()
  {
    return i18n;
  }

  public void setI18n(RigelI18nInterface i18n)
  {
    this.i18n = i18n;
  }

  /**
   * Compatibilità versine precedente di rigel.
   * Restituisce html+javascript in una unica stringa.
   * Questa funzione è fornita solo per compatibiltà con la verione precedente.
   * Utilizzare i componenti di pagina.
   * @deprecated
   * @return html comprensivo della pagina
   * @throws Exception
   */
  public String doHtml()
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    doHtml(page);

    return page.getAllJavascript() + "\n" + page.getHtml("body");
  }

  /**
   * Restituisce il contenuto della tabella completa
   * @param page
   * @throws Exception
   */
  public void doHtml(RigelHtmlPage page)
     throws Exception
  {
    doHtml(0, tableModel.getRowCount(), page);
  }

  /**
   * Restituisce il contenuto della tabella completa
   * @param rStart
   * @param numRec
   * @param page
   * @throws Exception
   */
  public synchronized void doHtml(int rStart, int numRec, RigelHtmlPage page)
     throws Exception
  {
    html.clear();
    javascript.clear();

    if(tableModel instanceof RigelTableModel)
      formName = ((RigelTableModel) (tableModel)).getFormName();

    if(rStart > tableModel.getRowCount())
      return;
    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    normalizeCols();
    html.append("<div class=\"rigel_htable\">\r\n<").append(tableStatement).append(">\r\n");

    if(showHeader)
      doHeader();

    doRows(rStart, numRec);

    html.append("</TABLE>\r\n</div>\r\n");

    if(!html.isEmpty())
      page.add(html);

    if(!javascript.isEmpty())
      page.add(javascript);
  }

  /**
   * Produce l'header della tabella
   * @throws java.lang.Exception
   */
  public void doHeader()
     throws Exception
  {
    html.append("<").append(headerStatement).append(">\r\n").append(preHeader());

    for(int i = 0; i < tableModel.getColumnCount(); i++)
    {
      html.append(doCellHeader(i));
    }

    html.append(postHeader()).append("</TR>\r\n");
  }

  protected String cellBegin(int row, int col)
     throws Exception
  {
    String align = doAlign(row, col);
    String color = doColor(row, col);
    String style = doStyle(row, col);

    if(row == -1)
      // header della tabella
      if(nosize)
        return "<" + colheadStatement + " "
           + align + " " + color + " " + style + ">";
      else
        return "<" + colheadStatement + " WIDTH=\"" + normWidth[col] + "%\""
           + align + " " + color + " " + style + ">";
    else // corpo della tabella
    if(nosize)
      return "<" + colStatement + " "
         + align + " " + color + " " + style + ">";
    else
      return "<" + colStatement + " WIDTH=\"" + normWidth[col] + "%\""
         + align + " " + color + " " + style + ">";
  }

  protected String cellEnd(int row, int col)
     throws Exception
  {
    return "</TD>\r\n";
  }

  public String doCellHeader(int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) != null)
      if(!cd.isVisible())
        return "";

    String rv = cellBegin(-1, col);
    rv += doFormCellHeader(0, col);
    rv += cellEnd(-1, col);

    return rv;
  }

  public String getColumnCaption(int col)
     throws Exception
  {
    String defCaption = columnModel.getColumn(col).getHeaderValue().toString();
    return i18n.localizeTableCaption(this, getTM(), getCD(col), col, defCaption);
  }

  public String doFormCellHeader(int row, int col)
     throws Exception
  {
    return getColumnCaption(col);
  }

  public void doRows()
     throws Exception
  {
    doRows(0, tableModel.getRowCount());
  }

  public void doRows(int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return;

    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    strartRow = rStart;
    numRows = numRec;
    numCols = tableModel.getColumnCount();

    while(numRec-- > 0)
      doRow(rStart++);
  }

  public void doRow(int row)
     throws Exception
  {
    String sRowStat = rowStatement;
    if(rowTip != null)
      sRowStat += " title='" + rowTip + "'";

    html.append("<").append(sRowStat).append(">\r\n").append(preValues(row));

    for(int i = 0; i < tableModel.getColumnCount(); i++)
      html.append(doCell(row, i));

    html.append(postValues(row)).append("</TR>\r\n");
  }

  public String doColor(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    Color c = cd.getColor();
    if(c != null)
    {
      //String s = Integer.toString(c.getRGB(), 16);
      //s = s.substring(s.length()-6, s.length());
      String s = (c.getRed() < 10 ? "0" : "") + Integer.toString(c.getRed(), 16)
         + (c.getGreen() < 10 ? "0" : "") + Integer.toString(c.getGreen(), 16)
         + (c.getBlue() < 10 ? "0" : "") + Integer.toString(c.getBlue(), 16);
      return " bgcolor=\"#" + s + "\"";
    }

    return "";
  }

  public String doAlign(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    String sAlign = "";

    switch(cd.getHtmlAlign())
    {
      case RigelColumnDescriptor.HTML_ALIGN_LEFT:
        sAlign = " align=\"left\"";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_CENTER:
        sAlign = " align=\"center\"";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_RIGHT:
        sAlign = " align=\"right\"";
        break;
    }

    return sAlign;
  }

  public String doStyle(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    return cd.getHtmlStyle() == null ? "" : " class=\"" + cd.getHtmlStyle() + "\"";
  }

  public String doCell(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) != null)
      if(!cd.isVisible())
        return "";

    String value = doFormCellValue(row, col);
    String text = elaboraFixedText(row, col, value);

    return cellBegin(row, col)
       + text
       + cellEnd(row, col);
  }

  public String elaboraFixedText(int row, int col, String strCella)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) != null)
    {
      int pos;
      String sfix = cd.getFixedText();
      if(sfix == null)
        return strCella;

      if((pos = sfix.indexOf("@@@")) != -1)
        return sfix.substring(0, pos) + strCella + sfix.substring(pos + 3);
      else
        return sfix;
    }
    return strCella;
  }

  /**
   * Formatta l'interno della cella.
   * @param row riga corrente
   * @param col colonna corrente
   * @return HTML
   * @throws Exception
   */
  public String doFormCellValue(int row, int col)
     throws Exception
  {
    String val = formatCell(row, col, tableModel.getValueAt(row, col));
    if(val == null || val.trim().length() == 0)
      val = " &nbsp;";

    RigelColumnDescriptor cd;
    if((cd = getCD(col)) != null)
    {
      if(cd.getForeignMode() != RigelColumnDescriptor.DISP_FLD_ONLY)
      {
        val = getForeignData(cd, val);
        if(val == null || val.trim().length() == 0)
          val = " &nbsp;";
      }

      if(cd.isHtmlPara())
        val = "<p id=\"" + getNomePara(row, col) + "\">" + val + "</p>";
    }

    return val;
  }

  /**
   * Produce la stringa rappresentazione del dato.
   * Se la colonna ha un formattatore esplicito lo usa.
   */
  public String formatCell(int row, int col, Object value)
     throws Exception
  {
    if(value == null)
      return "";

    String sOut;
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) != null)
      sOut = cd.formatValue(value);
    else if(value instanceof Date)
      sOut = SetupHolder.getDateFormat().format(value);
    else
      sOut = value.toString();

    if(sOut.startsWith("<html>") && sOut.endsWith("</html>"))
    {
      return sOut.substring(6, sOut.length() - 7);
    }

    return StringOper.CvtWEBstring(sOut);
  }

  protected String getForeignData(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd = getCD(col);
    if(cd != null)
    {
      Object val = tableModel.getValueAt(row, col);
      if(val != null)
        return getForeignData(cd, val.toString());
    }
    return "";
  }

  protected String getForeignData(RigelColumnDescriptor cd, String val)
     throws Exception
  {
    if(tableModel instanceof RigelTableModel)
    {
      ForeignDataHolder fd = cd.findHTableForeign(val, getTM(), i18n);

      if(fd == null)
      {
        // ritorna un foreign value di tipo INDEFINITO
        fd = new ForeignDataHolder();
        fd.codice = val;
        fd.alternateCodice = val;
        fd.descrizione = "INDEFINITO";
      }

      String sOut = "";
      switch(cd.getForeignMode())
      {
        case RigelColumnDescriptor.DISP_DESCR_ONLY:
        case RigelColumnDescriptor.DISP_DESCR_EDIT:
        case RigelColumnDescriptor.DISP_DESCR_EDIT_ALTERNATE:
          sOut = formatForeign(fd.descrizione);
          break;
        case RigelColumnDescriptor.DISP_FLD_DESCR:
          sOut = formatForeign(fd.codice, fd.descrizione);
          break;
        case RigelColumnDescriptor.DISP_FLD_DESCR_ALTERNATE:
          sOut = formatForeign(fd.alternateCodice, fd.descrizione);
          break;
      }
      return sOut;
    }

    return "";
  }

  protected String formatForeign(String descrizione)
  {
    return descrizione == null ? "" : descrizione.trim();
  }

  protected String formatForeign(String codice, String descrizione)
  {
    return codice.trim() + "-" + (descrizione == null ? "" : descrizione.trim());
  }

  /**
   * Normalizzazione colonne.Estrae le dimensioni normalizzate a 100 delle colonne.
   * Qui viene anche utilizzato l'interfaccia estesa dei formatter
   * per passare l'interfaccia di internazionalizzazione.
   * @throws java.lang.Exception
   */
  public void normalizeCols()
     throws Exception
  {
    int maxc = 0;
    normWidth = new int[tableModel.getColumnCount()];

    for(int i = 0; i < tableModel.getColumnCount(); i++)
    {
      TableColumn tc = columnModel.getColumn(i);
      maxc += tc.getWidth();
    }

    for(int i = 0; i < tableModel.getColumnCount(); i++)
    {
      TableColumn tc = columnModel.getColumn(i);
      normWidth[i] = (int) ((tc.getWidth() * getNormalizeRef()) / maxc);

      if(tc instanceof RigelColumnDescriptor)
      {
        Format formatter = ((RigelColumnDescriptor) tc).getFormatter();
        if(formatter != null && formatter instanceof RigelExtendedFormat)
        {
          ((RigelExtendedFormat) formatter).prepareToRender(i18n);
        }
      }
    }
  }

  public float getNormalizeRef()
  {
    return 100.0f;
  }

  /**
   * Genera un nome paragrafo a partire dal nome campo
   */
  public String getNomePara(int row, int col)
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    return cd.getName() + "_P_" + row;
  }

  /**
   * Ritorna un oggetto RigelColumnDescriptor se la colonna
   * indicata e' realmente una istanza di RigelColumnDescriptor;
   * altrimenti ritorna null.
   */
  public RigelColumnDescriptor getCD(int col)
  {
    TableColumn tc = columnModel.getColumn(col);
    return (tc instanceof RigelColumnDescriptor) ? ((RigelColumnDescriptor) (tc)) : null;
  }

  public RigelTableModel getTM()
  {
    return (tableModel instanceof RigelTableModel) ? (RigelTableModel) tableModel : null;
  }

  public String preHeader()
     throws Exception
  {
    return "";
  }

  public String postHeader()
     throws Exception
  {
    return "";
  }

  public String preValues(int row)
     throws Exception
  {
    return "";
  }

  public String postValues(int row)
     throws Exception
  {
    return "";
  }

  public void setImgEditData(String imgEditData)
  {
    this.imgEditData = imgEditData;
  }

  public String getImgEditData()
  {
    return imgEditData == null ? SetupHolder.getImgEditData() : imgEditData;
  }

  public void setImgEditForeign(String imgEditForeign)
  {
    this.imgEditForeign = imgEditForeign;
  }

  public String getImgEditForeign()
  {
    return imgEditForeign == null ? SetupHolder.getImgEditForeign() : imgEditForeign;
  }

  public void setImgFormForeign(String imgFormForeign)
  {
    this.imgFormForeign = imgFormForeign;
  }

  public String getImgFormForeign()
  {
    return imgFormForeign == null ? SetupHolder.getImgFormForeign() : imgFormForeign;
  }

  public Map<String, String> getExtraParamsUrls()
  {
    return extraParamsUrls;
  }

  public void setExtraParamsUrls(Map<String, String> extraParamsUrls)
  {
    this.extraParamsUrls = extraParamsUrls;
  }
}
