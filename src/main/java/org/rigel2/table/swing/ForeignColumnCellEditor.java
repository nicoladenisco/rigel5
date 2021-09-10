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
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlib.utils.StringOper;
import org.rigel2.SetupHolder;
import org.rigel2.forms.swing.DoubleComboBox;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.peer.swing.PeerTableModel;

/**
 * Editor di colonna specializzato per la modalità foreign.
 * Viene usato solo quando una colonna ha una clausola extraWhere.
 * In tal coso il set di dati foreign può cambiare per ogni record
 * a causa delle macro utilizzate in extraWhere.
 * Questo editor esegue la query dei possibili valori foreign
 * ogni volta che deve far apparire il combo di popup.
 *
 * @author Nicola De Nisco
 */
public class ForeignColumnCellEditor extends DefaultCellEditor
{
  /** Logging */
  private static Log log = LogFactory.getLog(ForeignColumnCellEditor.class);

  protected Object keyValue;
  protected DoubleComboBox cb = null;

  public ForeignColumnCellEditor(DoubleComboBox cb)
  {
    super(cb);
    this.cb = cb;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table,
     Object value,
     boolean isSelected,
     int row,
     int col)
  {
    keyValue = value;

    // estrae riferimento al nostro datamodel
    PeerTableModel ptm = (PeerTableModel) table.getModel();
    RigelColumnDescriptor cd = ptm.getColumn(col);

    try
    {
      // recupera la lista di valori foreign validi per questo record e questa colonna
      List localForeignValues = ptm.getQuery().getForeignDataList(row, col, ptm, cd, SetupHolder.getRi18n());
      cb.init(localForeignValues, false);
      cb.setText(StringOper.okStr(value));
    }
    catch(Exception ex)
    {
      log.error("Errore acquisendo la lista di valori foreign.", ex);
    }

    return cb;
  }
}
