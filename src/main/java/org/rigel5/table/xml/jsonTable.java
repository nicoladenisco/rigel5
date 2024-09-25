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
package org.rigel5.table.xml;

import java.awt.Color;
import java.util.Date;
import javax.swing.table.*;
import org.apache.commons.logging.*;
import org.commonlib5.utils.StringOper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rigel5.SetupHolder;
import org.rigel5.table.ForeignDataHolder;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * Generatore di XML per tabella.
 *
 * @author Nicola De Nisco
 */
public class jsonTable
{
  /** Logging */
  private static Log log = LogFactory.getLog(jsonTable.class);
  //
  protected TableModel tableModel;
  protected TableColumnModel columnModel;
  protected boolean showHeader = true;
  protected float normWidth[];
  protected String imgEditData = null;
  protected String imgEditForeign = null;
  protected boolean showFieldsName = false;
  /** array per la raccolta occupazione di colonne */
  protected int[] arColSizes = null;

  public jsonTable()
  {
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

  /**
   * Restituisce il contenuto della tabella completa
   * @param out output della tabella
   * @throws java.lang.Exception
   */
  public void doJson(JSONObject out)
     throws Exception
  {
    doJson(out, 0, tableModel.getRowCount());
  }

  /**
   * Restituisce il contenuto della tabella completa
   * @param out
   * @param numRec
   * @param rStart
   * @throws java.lang.Exception
   */
  public void doJson(JSONObject out, int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return;

    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    normalizeCols();

    if(showHeader)
      doHeader(out);

    doRows(out, rStart, numRec);
  }

  /**
   * Produce l'header della tabella
   * @param out
   * @throws java.lang.Exception
   */
  public void doHeader(JSONObject out)
     throws Exception
  {
    JSONObject tmp;
    JSONArray header = new JSONArray();

    if((tmp = preHeader()) != null)
      header.put(tmp);

    for(int i = 0; i < columnModel.getColumnCount(); i++)
    {
      if((tmp = doCellHeader(i)) != null)
        header.put(tmp);
    }

    if((tmp = postHeader()) != null)
      header.put(tmp);

    out.put("header", header);
  }

  public JSONObject doCellHeader(int col)
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return null;

    if(!cd.isVisible())
      return null;

    JSONObject h = new JSONObject();
    h.put("name", cd.getCaption());
    h.put("width", normWidth[col]);
    h.put("align", doAlign(-1, col));

    switch(cd.getDataType())
    {
      case RigelColumnDescriptor.PDT_BOOLEAN:
        h.put("type", "BOOLEAN");
        break;
      case RigelColumnDescriptor.PDT_DATE:
        h.put("type", "DATE");
        break;
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPDATEONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPHOURONLY:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOSEC:
      case RigelColumnDescriptor.PDT_TIMESTAMP_CMPTOMIN:
      case RigelColumnDescriptor.PDT_TIMESTAMP:
        h.put("type", "DATETIME");
        break;
      case RigelColumnDescriptor.PDT_TIME:
        h.put("type", "TIME");
        break;
      case RigelColumnDescriptor.PDT_INTEGER:
      case RigelColumnDescriptor.PDT_FLOAT:
      case RigelColumnDescriptor.PDT_DOUBLE:
      case RigelColumnDescriptor.PDT_MONEY:
      case RigelColumnDescriptor.PDT_NUMBERKEY:
        h.put("type", "NUMBER");
        break;
      default:
      case RigelColumnDescriptor.PDT_STRINGKEY:
      case RigelColumnDescriptor.PDT_STRING:
        h.put("type", "STRING");
        h.put("size", cd.getSize());
        break;
    }

    return h;
  }

  public void doRows(JSONObject out)
     throws Exception
  {
    doRows(out, 0, tableModel.getRowCount());
  }

  public void doRows(JSONObject out, int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return;

    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    arColSizes = new int[tableModel.getColumnCount()];

    JSONArray data = new JSONArray();
    out.put("data", data);

    if(numRec > 0)
    {
      for(int i = 0; i < numRec; i++)
      {
        JSONArray dataRow = new JSONArray();
        data.put(dataRow);
        doRow(dataRow, rStart++);
      }

      for(int i = 0; i < arColSizes.length; i++)
      {
        arColSizes[i] /= numRec;
      }
    }

    out.put("normalized-size", new JSONArray(arColSizes));
  }

  public void doRow(JSONArray out, int row)
     throws Exception
  {
    JSONObject tmp;

    if((tmp = preValues(row)) != null)
      out.put(tmp);

    for(int col = 0; col < columnModel.getColumnCount(); col++)
    {
      if((tmp = doCell(row, col)) != null)
        out.put(tmp);
    }

    if((tmp = postValues(row)) != null)
      out.put(tmp);
  }

  public String doColor(int row, int col)
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    Color c = cd.getColor();
    if(c != null)
      return fmtColor(c);

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
        sAlign = "left";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_CENTER:
        sAlign = "center";
        break;
      case RigelColumnDescriptor.HTML_ALIGN_RIGHT:
        sAlign = "right";
        break;
    }

    return sAlign;
  }

  public JSONObject doCell(int row, int col)
     throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return null;

    if(!cd.isVisible())
      return null;

    String fldData = doFormatCellValue(row, col, cd);
    return doCellOutput(row, col, cd, fldData);
  }

  protected JSONObject doCellOutput(int row, int col, RigelColumnDescriptor cd, String fldData)
  {
    JSONObject c = new JSONObject();

    c.put("name", cd.getCaption());
    c.put("value", fldData);
    c.put("align", doAlign(row, col));
    c.put("color", doColor(row, col));

    return c;
  }

  public String fmtId(String id)
  {
    return StringOper.okStr(id);
  }

  public String fmtVal(String val)
  {
    return StringOper.okStr(val);
  }

  public String fmtColor(Color c)
  {
    //String s = Integer.toString(c.getRGB(), 16);
    //s = s.substring(s.length()-6, s.length());
    String s = (c.getRed() < 10 ? "0" : "") + Integer.toString(c.getRed(), 16)
       + (c.getGreen() < 10 ? "0" : "") + Integer.toString(c.getGreen(), 16)
       + (c.getBlue() < 10 ? "0" : "") + Integer.toString(c.getBlue(), 16);
    return s;
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

  public String doFormatCellValue(int row, int col, RigelColumnDescriptor cd)
     throws Exception
  {
    String val = StringOper.okStrNull(formatCell(row, col, tableModel.getValueAt(row, cd.getModelIndex())));
    if(val == null)
      return "";

    if(cd.getForeignMode() != RigelColumnDescriptor.DISP_FLD_ONLY)
    {
      if((val = StringOper.okStrNull(getForeignData(cd, val))) == null)
        return "";
    }
    else
    {
      if((val = elaboraFixedText(cd, val)) == null)
        return "";
    }

    return fmtVal(val);
  }

  /**
   * Produce la stringa rappresentazione del dato.
   * Se la colonna ha un formattatore esplicito lo usa.
   * @param row
   * @param col
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

  public JSONObject preHeader()
  {
    return null;
  }

  public JSONObject postHeader()
  {
    return null;
  }

  public JSONObject preValues(int row)
  {
    return null;
  }

  public JSONObject postValues(int row)
  {
    return null;
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
