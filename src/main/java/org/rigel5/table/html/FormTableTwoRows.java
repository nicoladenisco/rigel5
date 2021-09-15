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

import java.util.ArrayList;
import org.rigel5.HtmlUtils;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Gestore di form.
 * I form vengono presentati con label e campi sovrapposti.
 * @author Nicola De Nisco
 */
public class FormTableTwoRows extends FormTable
{
  public FormTableTwoRows(String sId)
  {
    super(sId);
  }

  @Override
  public synchronized String doFormRecord(int row)
     throws Exception
  {
    StringBuilder sOut = new StringBuilder(8192);
    int colo = colonne;
    int wd = 100 / colonne;

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

      String align = doAlign(row, i);
      String color = doColor(row, i);
      String style = doStyle(row, i);
      String inner = doInnerCell(row, i);
      String std = "";

      String size = nosize ? "" : "WIDTH=\"" + wd + "%\" ";

      if(cd.getHtmlSpan() <= 1 || colonne == 1)
        std = "<" + colStatement + " " + size
           + align + " " + color + " " + style + ">";
      else
        std = "<" + colheadStatement
           + (cd.getHtmlSpan() == 0 ? "" : " COLSPAN=" + ((cd.getHtmlSpan() * 2) - 1)) + " "
           + align + " " + color + " " + style + ">";

      sOut.append(std);

      if(showHeader)
      {
        String header = doFormCellHeader(row, i);
        ArrayList<String> arStyles = new ArrayList<String>();
        arStyles.add("rigel_form_header_cell");
        if(cd.isTestfornull() || cd.isTestforzero())
          arStyles.add("rigel_form_header_notnull");

        sOut.append(HtmlUtils.addSpanClasses(arStyles, header)).append("<br>");
      }

      ArrayList<String> arStyles = new ArrayList<String>();
      arStyles.add("rigel_form_field_cell");
      if(cd.isTestfornull() || cd.isTestforzero())
        arStyles.add("rigel_form_field_notnull");

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
  @Override
  protected String chiudiUltimaRiga(int colo)
     throws Exception
  {
    return "<td colspan=\"" + colo + "\">&nbsp</td>";
  }
}
