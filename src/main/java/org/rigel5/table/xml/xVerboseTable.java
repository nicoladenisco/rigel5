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

import org.rigel5.table.RigelColumnDescriptor;

/**
 * Generatore di XML per tabella.
 * 
 * @author Nicola De Nisco
 */
public class xVerboseTable extends xTable
{
  public xVerboseTable()
  {
    showHeader = false;
  }

  @Override
  public String doCell(int row, int col) throws Exception
  {
    RigelColumnDescriptor cd;
    if((cd = getCD(col)) == null)
      return "";

    if(!cd.isVisible())
      return "";

    String fldName = cd.getCaption();
    String fldInfo = doFormCellHeader(row, col);
    String fldData = doFormCellValue(row, col, cd).trim();

    return
        "<" + fldName + doAlign(row, col) + doColor(row, col) + ">\r\n"+
        "<info>\r\n"+fldInfo+"</info>\r\n"+
        fldData+"\r\n"+
        "</" + fldName +">\r\n";
  }
}


