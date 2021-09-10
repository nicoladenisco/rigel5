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
package org.rigel2.table.swing;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.commonlib.utils.StringOper;
import org.rigel2.SetupHolder;
import org.rigel2.table.ForeignDataHolder;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;

/**
 * Renderizzatore specializzato per Rigel.
 * I valori che appaiono nelle celle della JTable vengono
 * convertiti in stringa dalle apposite funzioni di Rigel.
 *
 * @author Nicola De Nisco
 */
public class RigelTableCellRender extends DefaultTableCellRenderer
{
  public static Color defaultBackgroundColor = Color.white;
  public static final int FOREIGN_PREFETCH_LIMIT = 1000;
  protected TableCellRenderer realRender = null;

  public RigelTableCellRender(TableCellRenderer realRender)
  {
    this.realRender = realRender;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
     boolean isSelected, boolean hasFocus, int row, int column)
  {
    try
    {
      // estrae riferimento al nostro datamodel
      RigelTableModel ptm = (RigelTableModel) table.getModel();
      RigelColumnDescriptor cd = ptm.getColumn(column);

      value = cd.formatValue(value);

      if(cd.getForeignMode() != RigelColumnDescriptor.DISP_FLD_ONLY)
      {
        String val = StringOper.okStr(value);
        if(cd.getForeignValuesCount(row, column, ptm) < FOREIGN_PREFETCH_LIMIT)
        {
          List<ForeignDataHolder> lFdh = cd.getForeignValues(ptm, SetupHolder.getRi18n());
          for(ForeignDataHolder fdh : lFdh)
          {
            if(StringOper.isEqu(val, fdh.codice))
              value = fdh.descrizione;
          }
        }
        else
        {
          ForeignDataHolder fdh = cd.findHTableForeign(val, ptm, SetupHolder.getRi18n());
          value = fdh == null ? null : fdh.descrizione;
        }
      }

      Component c = null;

      try
      {
        c = realRender.getTableCellRendererComponent(table, value,
           isSelected, hasFocus, row, column);
      }
      catch(Exception e)
      {
        c = super.getTableCellRendererComponent(table, value,
           isSelected, hasFocus, row, column);
      }

      if(!hasFocus && !isSelected)
      {
        Color color = getCellColor(ptm, row, column);
        if(color != null)
          c.setBackground(color);
      }

      return c;
    }
    catch(Exception ex)
    {
      // fa riferimento alla classe base
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
  }

  public Color getCellColor(RigelTableModel ptm, int row, int column)
  {
    Color c = ptm.getRowColor(row);
    if(c != null)
      return c;

    c = ptm.getColumnColor(column);
    if(c != null)
      return c;

    return null;
  }
}
