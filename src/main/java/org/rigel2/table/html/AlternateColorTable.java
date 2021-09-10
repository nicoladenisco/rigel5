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
package org.rigel2.table.html;

/**
 * <p>
 * Title: Tabella HTML per visualizzione a colori alternati.</p>
 * <p>
 * Description: Questa versione di hTable consente di visualizzare
 * un risultato con colori alternati per le varie righe, in modo
 * da aumentare la leggibilita' della tabella visualizzata.</p>
 * <p>
 * I colori di default delle due righe sono due tonalita' di grigio
 * differenti, ma possono essere impostate con i metodi
 * <b>setEvenRowStatement() setOddRowStatement()</b>.
 * </p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class AlternateColorTable extends hTable
{
  private String evenRowStatement = "TR BGCOLOR=\"#F0F0F0\"";
  private String oddRowStatement = "TR BGCOLOR=\"#CCCCCC\"";

  public AlternateColorTable()
  {
    super();
    setShowHeader(true);
    setHeaderStatement("TR BGCOLOR=\"#00FFFF\"");
  }

  public void setEvenRowStatement(String newEvenRowStatement)
  {
    evenRowStatement = newEvenRowStatement;
  }

  public String getEvenRowStatement()
  {
    return evenRowStatement;
  }

  public void setOddRowStatement(String newOddRowStatement)
  {
    oddRowStatement = newOddRowStatement;
  }

  public String getOddRowStatement()
  {
    return oddRowStatement;
  }

  public String doRowStatement(int row)
  {
    return ((row & 1) == 0) ? evenRowStatement : oddRowStatement;
  }

  @Override
  public void doRow(int row)
     throws Exception
  {
    setRowStatement(doRowStatement(row));
    super.doRow(row);
  }
}
