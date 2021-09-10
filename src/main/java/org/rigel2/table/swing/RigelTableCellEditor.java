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

import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import org.commonlib.utils.StringOper;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;

/**
 * Editor speciale per una tabella swing.
 * I valori vengono parsati opportunamente dalle
 * funzioni di Rigel, in accordo con le impostazioni
 * delle colonne.
 *
 * @author Nicola De Nisco
 */
public class RigelTableCellEditor extends AbstractCellEditor
    implements TableCellEditor
{
  protected TableCellEditor realEditor = null;
  protected Component editComponent = null;
  protected RigelTableModel ptm = null;
  protected RigelColumnDescriptor cd = null;

  public RigelTableCellEditor(TableCellEditor realEditor)
  {
    this.realEditor = realEditor;
  }

  @Override
  public Object getCellEditorValue()
  {
    Object value = null;
    if(editComponent instanceof JTextComponent)
    {
      value = ((JTextComponent)editComponent).getText();
    }
    else
    if(editComponent instanceof JCheckBox)
    {
      value = ((JCheckBox)editComponent).isSelected() ? "1" : "0";
    }
    else
    if(editComponent instanceof JComboBox)
    {
      value = ((JComboBox)editComponent).getSelectedItem();
    }

    // effetta il parsing corretto della stringa contenente il valore modificato
    return cd.parseValue(StringOper.okStr(value));
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value,
     boolean isSelected, int row, int column)
  {
    try
    {
      // estrae riferimento al nostro datamodel
      ptm = (RigelTableModel) table.getModel();
      cd = ptm.getColumn(column);

      // porta il valore precedente nel formato stabilito dai formattatori
      value = cd.formatValue(value);

      editComponent = realEditor.getTableCellEditorComponent(table, value, isSelected, row, column);

      return editComponent;
    }
    catch(Exception ex)
    {
      return null;
    }
  }
}
