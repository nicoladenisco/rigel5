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

import java.text.Format;
import java.util.*;
import javax.swing.table.TableModel;
import org.rigel5.HtmlUtils;
import org.rigel5.RigelExtendedFormat;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * Gestore di form per input dati.
 * Generatore di input a tabella
 * lavora insieme ad un PeerTableModel per consentire
 * l'editing dei dati; questa versione e' stata pensata
 * per un singolo record da editare.
 * Per un editing tabellare di piu' records vedi hEditTable.
 * @author Nicola De Nisco
 * @version 1.0
 */
public class FormTable extends hEditTable
{
  protected int colonne = 1;
  protected int[][] matcol = null;
  protected int mr, mc, mrMax;
  protected boolean firstRow = true;
  protected boolean rowOpen = false;

  public FormTable(String sId)
  {
    super(sId);
  }

  @Override
  public void setModel(TableModel newTableModel)
  {
    tableModel = newTableModel;

    // NOTA: la versione della classe base imposta automaticamente nosize
    // quando le colonne sono numerose; questo comportamento gradito in hTable
    // è sgradito qui, in quanto distrugge l'estetica dei forms
    //
    // se il numero di colonne è grande disabilita l'emissione di size
    //if(SetupHolder.getNoSizeLimit() != 0
    //   && tableModel.getColumnCount() > SetupHolder.getNoSizeLimit())
    //  nosize = true;
  }

  public synchronized void setColonne(int newColonne)
  {
    colonne = newColonne;
    matcol = null;
  }

  public int getColonne()
  {
    return colonne;
  }

  @Override
  public synchronized void doHtml(int rStart, int numRec, RigelHtmlPage page)
     throws Exception
  {
    clearScriptTest();
    html.clear();
    javascript.clear();

    if(tableModel instanceof RigelTableModel)
      formName = ((RigelTableModel) (tableModel)).getFormName();

    if(rStart > tableModel.getRowCount())
      return;
    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    strartRow = rStart;
    numRows = numRec;
    numCols = tableModel.getColumnCount();

    html.append("<div id=\"rigel_formtable_").append(formName)
       .append("\" class=\"rigel_formtable\">\r\n<").append(tableStatement).append(">\r\n");

    html.append(doFormRows(rStart, numRec));

    html.append("</TABLE>\r\n</div>\r\n");

    if(!html.isEmpty())
      page.add(html);

    if(!javascript.isEmpty())
      page.add(javascript);

    if(!scriptTest.isEmpty())
      page.add(scriptTest);
  }

  public synchronized void doHtmlUnico(RigelHtmlPage page)
     throws Exception
  {
    doHtml(0, 1, page);
  }

  /**
   * Compatibilità versine precedente di rigel.
   * Restituisce html+javascript in una unica stringa.
   * Questa funzione è fornita solo per compatibiltà con la verione precedente.
   * Utilizzare i componenti di pagina.
   * @deprecated usare la versione con RigelHtmlPage
   * @return html comprensivo della pagina
   * @throws Exception
   */
  public String doHtmlUnico()
     throws Exception
  {
    RigelHtmlPage page = new RigelHtmlPage();
    doHtmlUnico(page);

    return page.getAllJavascript() + "\n" + page.getHtml("body");
  }

  public String doFormRows(int rStart, int numRec)
     throws Exception
  {
    if(rStart > tableModel.getRowCount())
      return "";
    if(rStart + numRec > tableModel.getRowCount())
      numRec = tableModel.getRowCount() - rStart;

    String sOut = "";
    while(numRec-- > 0)
      sOut += doFormRecord(rStart++);
    return sOut;
  }

  protected synchronized void costruisciMatrice()
     throws Exception
  {
    int colo = colonne, size = 0;
    int numColonne = tableModel.getColumnCount();
    matcol = new int[numColonne][colonne];
    mr = mc = 0;

    for(int i = 0; i < numColonne; i++)
    {
      RigelColumnDescriptor cd = getCD(i);
      if(cd == null || !cd.isVisible())
        continue;

      if(colo == colonne)
        // inizio riga
        mc = 0;

      size = Math.max(1, cd.getHtmlSpan());

      try
      {
        while(size-- > 0)
        {
          if(cd.isEditable())
            matcol[mr][mc] = i + 1;

          mc++;
          colo--;
        }
      }
      catch(Exception e)
      {
        throw new Exception(i18n.msg(
           "FormTable %s. Errore in parametro spam: la colonna %d: %s [%s] è fuori dalla tabella.",
           id, i, cd.getCaption(), cd.getName()));
      }

      if(colo == 0)
      {
        // fine riga
        colo = colonne;
        mr++;
      }
    }

    mrMax = mr;
  }

  public synchronized String doFormRecord(int row)
     throws Exception
  {
    StringBuilder sOut = new StringBuilder(8192);
    int colo = colonne;
    int wh, wd;

    if(colonne == 1)
    {
      wh = 30;
      wd = 70;
    }
    else
    {
      wh = 30 / colonne;
      wd = 70 / colonne;
    }

    firstRow = true;
    rowOpen = false;
    int numColonne = tableModel.getColumnCount();

    if(matcol == null)
      costruisciMatrice();

    // emette la matrice nell'html come commento
    // molto utile in caso di debugging
    sOut.append("<!--\r\nFormTableMatrix:\r\n");
    for(int r = 0; r < mrMax; r++)
    {
      for(int c = 0; c < colonne; c++)
      {
        sOut.append(String.format("%4d", matcol[r][c]));
      }
      sOut.append("\r\n");
    }
    sOut.append("-->\r\n");

    // notifica tutti i formatter estesi con il record corrente
    for(int i = 0; i < tableModel.getColumnCount(); i++)
    {
      RigelColumnDescriptor rcd = getCD(i);
      Format rf = rcd.getFormatter();
      if(rf != null && rf instanceof RigelExtendedFormat)
        ((RigelExtendedFormat) rf).prepareFormatRecord(getTM(), row, i);
    }

    mr = mc = 0;
    for(int i = 0; i < numColonne; i++)
    {
      RigelColumnDescriptor cd = getCD(i);
      if(cd == null || !cd.isVisible())
        continue;

      if(colo == colonne)
      {
        // inizio riga
        sOut.append("<").append(rowStatement).append(">\r\n");
        rowOpen = true;
        mc = 0;
      }

      String cellText = doCellText(row, i, tableModel.getValueAt(row, i));
      String cellHtml = doCellHtml(row, i, cellText);

      String align = doAlign(row, i);
      String color = doColor(row, i);
      String style = doStyle(row, i);
      String inner, std = "";

      if(isColumnEditable(row, i) || cd.isHiddenEdit())
      {
        inner = doInnerCell(row, i, cellText, cellHtml);
      }
      else
      {
        inner = elaboraFixedText(row, i, cellHtml);
      }

      if(showHeader)
      {
        String header = doFormCellHeader(row, i);
        ArrayList<String> arStyles = new ArrayList<>();
        arStyles.add("rigel_form_header_cell");
        if(cd.isTestfornull() || cd.isTestforzero())
          arStyles.add("rigel_form_header_notnull");

        String size = nosize ? "" : "WIDTH=\"" + wh + "%\" ";

        std = "<" + colheadStatement + " " + size
           + align + " " + color + " " + style + ">";

        sOut.append(std);
        sOut.append(HtmlUtils.addSpanClasses(arStyles, header));
        sOut.append("</TD>\r\n");
      }

      ArrayList<String> arStyles = new ArrayList<>();
      arStyles.add("rigel_form_field_cell");
      if(cd.isTestfornull() || cd.isTestforzero())
        arStyles.add("rigel_form_field_notnull");

      String size = nosize ? "" : "WIDTH=\"" + wd + "%\" ";

      if(cd.getHtmlSpan() <= 1 || colonne == 1)
        std = "<" + colStatement + " " + size
           + align + " " + color + " " + style + ">";
      else
        std = "<" + colheadStatement
           + (cd.getHtmlSpan() == 0 ? "" : " COLSPAN=" + ((cd.getHtmlSpan() * 2) - 1)) + " "
           + align + " " + color + " " + style + ">";

      sOut.append(std);
      sOut.append(HtmlUtils.addSpanClasses(arStyles, inner));
      sOut.append("</TD>\r\n");

      if(cd.getHtmlSpan() > 0)
      {
        colo -= cd.getHtmlSpan();
        mc += cd.getHtmlSpan();
      }
      else
      {
        colo--;
        mc++;
      }

      if(colo == 0)
      {
        // fine riga
        sOut.append("</TR>\r\n");
        colo = colonne;
        firstRow = false;
        rowOpen = false;
        mr++;
      }
    }

    // chiude ultima riga se necessario
    if(rowOpen)
    {
      sOut.append(chiudiUltimaRiga(colo));
      sOut.append("</TR>\r\n");
    }

    return sOut.toString();
  }

  /**
   * Recupero colonne non riempite.
   * Produce l'HTML per l'ultima cella in caso di
   * tabella non riempita completamente.
   * @param colo numero di colonne da recuperare
   * @return HTML colonne
   * @throws Exception
   */
  protected String chiudiUltimaRiga(int colo)
     throws Exception
  {
    if(showHeader)
      colo *= 2;

    return "<td colspan=\"" + colo + "\">&nbsp</td>";
  }

  public void salvaDatiUno(Map params)
     throws Exception
  {
    salvaDatiRiga(0, params);
  }

  @Override
  protected String moveKey(int row, int col)
     throws Exception
  {
    // in caso di una sola riga non può esserci navigazione fra righe
    if(mrMax <= 1)
      return "";

    try
    {
      int rowu = mr, rowd = mr, realColumnUp, realColumnDw;

      do
      {
        rowu = --rowu % mrMax;
        if(rowu == -1)
          rowu = mrMax - 1;
        realColumnUp = matcol[rowu][mc];
      }
      while(realColumnUp == 0 && rowu != mr);

      do
      {
        rowd = ++rowd % mrMax;
        realColumnDw = matcol[rowd][mc];
      }
      while(realColumnDw == 0 && rowd != mr);

      return "onKeyDown=\"return moveKey(document." + formName + "." + getNomeCampo(row, realColumnUp - 1)
         + ", document." + formName + "." + getNomeCampo(row, realColumnDw - 1) + ", event);\" ";
    }
    catch(Exception e)
    {
      System.out.println("Errore: " + row + " " + col);
      return "";
    }
  }
}
