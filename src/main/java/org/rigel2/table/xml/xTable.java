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
package org.rigel2.table.xml;

import java.awt.Color;
import java.io.Writer;
import java.util.Date;
import javax.swing.table.*;
import org.apache.commons.logging.*;
import org.commonlib.utils.StringOper;
import org.rigel2.*;
import org.rigel2.table.ForeignDataHolder;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;

/**
 * Generatore di XML per tabella.
 *
 * @author Nicola De Nisco
 */
public class xTable
{
  /** Logging */
  private static Log log = LogFactory.getLog(xTable.class);
  //
  protected TableModel tableModel;
  protected TableColumnModel columnModel;
  protected String tableStatement;
  protected String rowStatement;
  protected String colStatement;
  protected String headerStatement;
  protected boolean showHeader = true;
  protected String colheadStatement;
  protected float normWidth[];
  protected String imgEditData = null;
  protected String imgEditForeign = null;
  protected boolean showFieldsName = false;
  /** array per la raccolta occupazione di colonne */
  protected int[] arColSizes = null;
  protected int tipoColSize = 0;
  public static final int COL_SIZE_MAX = 0;
  public static final int COL_SIZE_AVG = 1;

  public xTable()
  {
    tableStatement = "records";
    headerStatement = "fields";
    rowStatement = "record";
    colheadStatement = "fld";
    colStatement = "df";
  }

  public void setModel(TableModel newTableModel)
  {
    tableModel = newTableModel;
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

  /**
   * Tipo di calcolo per il col size.
   * Una delle costanti COL_SIZE_...
   * @return tipo corrente
   */
  public int getTipoColSize()
  {
    return tipoColSize;
  }

  /**
   * Tipo di calcolo per il col size.
   * Una delle costanti COL_SIZE_...
   * @param tipoColSize tipo da impostare
   */
  public void setTipoColSize(int tipoColSize)
  {
    this.tipoColSize = tipoColSize;
  }

  /**
   * Restituisce il contenuto della tabella completa
   * @param out output della tabella
   * @throws java.lang.Exception
   */
  public void doXml(Writer out)
     throws Exception
  {
    doXml(out, 0, tableModel.getRowCount());
  }

  /**
   * Restituisce il contenuto della tabella completa
   * @param out
   * @param numRec
   * @param rStart
   * @throws java.lang.Exception
   */
  public void doXml(Writer out, int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return;

    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    normalizeCols();
    out.write("<" + tableStatement + ">\r\n");

    if(showHeader)
      doHeader(out);

    doRows(out, rStart, numRec);

    out.write("</" + tableStatement + ">\r\n");
  }

  /**
   * Produce l'header della tabella
   * @param out
   * @throws java.lang.Exception
   */
  public void doHeader(Writer out)
     throws Exception
  {
    out.write("<" + headerStatement + ">\r\n");
    out.write(preHeader());

    for(int i = 0; i < columnModel.getColumnCount(); i++)
    {
      out.write(doCellHeader(i));
    }

    out.write(postHeader());
    out.write("</" + headerStatement + ">\r\n");
  }

  public String doCellHeader(int col)
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    if(!cd.isVisible())
      return "";

    return "<" + colheadStatement + " name=\"" + cd.getCaption() + "\"" + " WIDTH=\"" + normWidth[col] + "\" " + doAlign(-1, col) + ">\n"
       + doFormCellHeader(0, col)
       + "</" + colheadStatement + ">\r\n";
  }

  public String doFormCellHeader(int row, int col)
  {
    RigelColumnDescriptor cd = getCD(col);

    String sOut = "    <name>" + cd.getCaption() + "</name>\n";

    switch(cd.getDataType())
    {
      case RigelColumnDescriptor.PDT_BOOLEAN:
        sOut += "    <type>BOOLEAN</type>\n";
        break;
      case RigelColumnDescriptor.PDT_DATE:
        sOut += "    <type>DATE</type>\n";
        break;
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
        sOut += "    <type>DATETIME</type>\n";
        break;
      case RigelColumnDescriptor.PDT_TIME:
        sOut += "    <type>TIME</type>\n";
        break;
      case RigelColumnDescriptor.PDT_INTEGER:
      case RigelColumnDescriptor.PDT_FLOAT:
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
      case RigelColumnDescriptor.PDT_NUMBERKEY:
        sOut += "    <type>NUMBER</type>\n";
        break;
      default:
      case RigelColumnDescriptor.PDT_STRINGKEY:
      case RigelColumnDescriptor.PDT_STRING:
        sOut += "    <type>STRING</type>\n";
        sOut += "    <size>" + cd.getSize() + "</size>\n";
        break;
    }

    return sOut;
  }

  public void doRows(Writer out)
     throws Exception
  {
    doRows(out, 0, tableModel.getRowCount());
  }

  public void doRows(Writer out, int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return;

    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    if(arColSizes == null)
      arColSizes = new int[tableModel.getColumnCount()];

    for(int i = 0; i < numRec; i++)
      doRow(out, rStart++);

    if(tipoColSize == COL_SIZE_AVG && arColSizes != null)
    {
      for(int i = 0; i < arColSizes.length; i++)
      {
        arColSizes[i] /= numRec;
      }
    }
  }

  public void doRow(Writer out, int row)
     throws Exception
  {
    out.write("<" + rowStatement + ">\r\n");
    out.write(preValues(row));

    for(int i = 0; i < columnModel.getColumnCount(); i++)
    {
      out.write(doCell(row, i));
    }

    out.write(postValues(row));
    out.write("</" + rowStatement + ">\r\n");
  }

  public String doColor(int row, int col)
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

  public String doCell(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    if(!cd.isVisible())
      return "";

    String fldData = doFormCellValue(row, col, cd);
    return doCellOutput(row, col, cd, fldData);
  }

  protected String doCellOutput(int row, int col, RigelColumnDescriptor cd, String fldData)
  {
    if(showFieldsName)
    {
      String tag = fmtTag(cd.getCaption());
      return fldData.equals("<val/>") ? "<" + tag + "/>\r\n"
                : "<" + tag + doAlign(row, col) + doColor(row, col) + ">"
         + fldData
         + "</" + tag + ">\r\n";
    }
    else
    {
      return fldData.equals("<val/>")
                ? "<" + colStatement + " name=\"" + cd.getCaption() + "\"/>\r\n"
                : "<" + colStatement + " name=\"" + cd.getCaption() + "\"" + doAlign(row, col) + doColor(row, col) + ">"
         + fldData
         + "</" + colStatement + ">\r\n";
    }
  }

  public String fmtId(String id)
  {
    if(id == null || id.trim().length() == 0)
      return "<id/>";

    return "<id><![CDATA[" + id + "]]></id>";
  }

  public String fmtVal(String val)
  {
    if(val == null || val.trim().length() == 0)
      return "<val/>";

    return "<val><![CDATA[" + val + "]]></val>";
  }

  /**
   * Rende una stringa compatibile per diventare
   * un tag xml (tutti gli spazi vengono sostiutiti con '-')
   *
   * @param s stringa da convertire
   * @return stringa convertita
   */
  public String fmtTag(String s)
  {
    return s.replace(' ', '-');
  }

  public String elaboraFixedText(RigelColumnDescriptor cd, String strCella)
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

  public String doFormCellValue(int row, int col, RigelColumnDescriptor cd)
     throws Exception
  {
    String val = StringOper.okStrNull(formatCell(row, col, tableModel.getValueAt(row, cd.getModelIndex())));
    if(val == null)
      return "<val/>";

    if(cd.getForeignMode() != RigelColumnDescriptor.DISP_FLD_ONLY)
    {
      if((val = StringOper.okStrNull(getForeignData(cd, val))) == null)
        return "<val/>";
    }
    else
    {
      if((val = elaboraFixedText(cd, val)) == null)
        return "<val/>";
    }

    // rimuove eventuali tag spuri
    val = val.replaceAll("</.+>", "");
    val = val.replaceAll("<.+>", "");

    // accumula lunghezze dati
    updateColSize(row, col, val);

    return fmtVal(val);
  }

  protected void updateColSize(int row, int col, String val)
  {
    if(arColSizes == null || arColSizes.length < col)
      return;

    int vrif = val.length();
    switch(tipoColSize)
    {
      default:
      case COL_SIZE_MAX:
        if(val.contains(" "))
          vrif /= 2;
        if(arColSizes[col] < vrif)
          arColSizes[col] = vrif;
        break;
      case COL_SIZE_AVG:
        arColSizes[col] += vrif;
        break;
    }
  }

  /**
   * Produce la stringa rappresentazione del dato.
   * Se la colonna ha un formattatore esplicito lo usa.
   * @param value
   * @return
   * @throws java.lang.Exception
   */
  public String formatCell(int row, int col, Object value)
     throws Exception
  {
    if(value == null)
      return "";

    String sOut;
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) != null)
    {
      sOut = cd.formatValue(value);
    }
    else
    {
      if(value instanceof Date)
      {
        sOut = SetupHolder.getDateFormat().format(value);
      }
      else
      {
        sOut = value.toString();
      }
    }

    return sOut;
  }

  protected String getForeignData(int row, int col)
  {
    try
    {
      RigelColumnDescriptor cd = getCD(col);
      if(cd != null)
      {
        Object val = tableModel.getValueAt(row, col);
        if(val != null)
          return getForeignData(cd, val.toString());
      }
    }
    catch(Exception ex)
    {
      log.error("RIGEL:", ex);
    }
    return "";
  }

  protected String getForeignData(RigelColumnDescriptor cd, String val)
     throws Exception
  {
    if(tableModel instanceof RigelTableModel)
    {
      ForeignDataHolder fd = cd.findHTableForeign(val, (RigelTableModel) (tableModel), SetupHolder.getRi18n());

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
    return descrizione;
  }

  protected String formatForeign(String codice, String descrizione)
  {
    return codice + " " + descrizione;
  }

  /**
   * Estrae le dimensioni normalizzate a 100 delle colonne
   */
  public void normalizeCols()
  {
    int maxc = 0;
    int ncol = columnModel.getColumnCount();
    normWidth = new float[ncol];

    for(int i = 0; i < ncol; i++)
    {
      TableColumn tc = columnModel.getColumn(i);
      maxc += tc.getWidth();
    }
    for(int i = 0; i < ncol; i++)
    {
      TableColumn tc = columnModel.getColumn(i);
      normWidth[i] = (float) ((tc.getWidth() * getNormalizeRef()) / maxc);
    }
  }

  public float getNormalizeRef()
  {
    return 100.0f;
  }

  /**
   * Genera un nome paragrafo a partire dal nome campo
   * @param row
   * @param col
   * @return
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
   * @param col
   * @return
   */
  public RigelColumnDescriptor getCD(int col)
  {
    TableColumn tc = columnModel.getColumn(col);
    return (tc instanceof RigelColumnDescriptor) ? ((RigelColumnDescriptor) (tc)) : null;
  }

  public String preHeader()
  {
    return "";
  }

  public String postHeader()
  {
    return "";
  }

  public String preValues(int row)
  {
    return "";
  }

  public String postValues(int row)
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

  public void setShowFieldsName(boolean showFieldsName)
  {
    this.showFieldsName = showFieldsName;
  }

  public boolean isShowFieldsName()
  {
    return showFieldsName;
  }

  public int[] getArColSizes()
  {
    return arColSizes;
  }

  public void clearColSizes()
  {
    arColSizes = null;
  }
}
