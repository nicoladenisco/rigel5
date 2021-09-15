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
package org.rigel5.forms.swing;

import java.util.*;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 * Combobox model adatto all'uso dei peer di Torque.
 * 
 * @author Nicola De Nisco
 */
public class PeerAbstractComboModel implements ComboBoxModel
{
  protected Vector vBuf = null;

  public PeerAbstractComboModel()
  {
  }

  @Override
  public void setSelectedItem(Object anItem)
  {
  }

  @Override
  public Object getSelectedItem()
  {
    return null;
  }

  @Override
  public int getSize()
  {
    return vBuf.size();
  }

  @Override
  public Object getElementAt(int index)
  {
    return vBuf.get(index);
  }

  @Override
  public void addListDataListener(ListDataListener l)
  {
  }

  @Override
  public void removeListDataListener(ListDataListener l)
  {
  }
}
