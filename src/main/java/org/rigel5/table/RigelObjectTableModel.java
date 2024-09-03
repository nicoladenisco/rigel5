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
package org.rigel5.table;

import java.util.*;

/**
 * <p>
 * Title: Rigel</p>
 * <p>
 * Description: TableModel specializzato per la manipolazione
 * di oggetti (vettori di oggetti)</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class RigelObjectTableModel extends RigelTableModel
{
  private ArrayList vBuf = null;

  @Override
  public boolean isInitalized()
  {
    return vBuf != null;
  }

  public boolean isEmpty()
  {
    return vBuf == null || vBuf.isEmpty();
  }

  public void rebind(Object bean)
  {
    clear();
    addObject(bean);
  }

  public void rebind(List newRows)
  {
    rebindInternal(new ArrayList(newRows));

    int lastRow = vBuf == null ? 0 : vBuf.size();
    fireTableRowsInserted(0, lastRow);
  }

  private void rebindInternal(ArrayList newRows)
  {
    vBuf = newRows;
    rowColor = new java.awt.Color[getRowCount()];
    markDeleted = new boolean[getRowCount()];
  }

  public void addObject(Object obj)
  {
    if(vBuf == null)
    {
      vBuf = new ArrayList();
    }

    int firstRow = vBuf.size();
    int lastRow = firstRow + 1;
    vBuf.add(obj);
    rebindInternal(vBuf);

    fireTableRowsInserted(firstRow, lastRow);
  }

  public void addObjects(ArrayList vobj)
  {
    if(vBuf == null)
    {
      vBuf = new ArrayList();
    }

    int firstRow = vBuf.size();
    int lastRow = firstRow + vobj.size();
    vBuf.addAll(vobj);
    rebindInternal(vBuf);

    fireTableRowsInserted(firstRow, lastRow);
  }

  public void clear()
  {
    int lastRow = vBuf == null ? 0 : vBuf.size();
    vBuf = null;
    rowColor = null;
    markDeleted = null;

    fireTableRowsDeleted(0, lastRow);
  }

  /**
   * Cancella dal buffer le righe marcate da cancellare
   * @return Vettore righe cancellate
   */
  public ArrayList removeDeleted()
  {
    ArrayList vToDel = getDeleted();
    if(!vToDel.isEmpty())
    {
      vBuf.removeAll(vToDel);
      rebindInternal(vBuf);
    }
    return vToDel;
  }

  /**
   * Ritorna un vettore con le righe marcate da cancellare
   * @return Vettore righe cancellate
   */
  public ArrayList getDeleted()
  {
    ArrayList vToDel = new ArrayList();

    if(vBuf == null || vBuf.isEmpty())
      return vToDel;

    for(int i = 0; i < vBuf.size(); i++)
    {
      if(markDeleted[i])
      {
        vToDel.add(vBuf.get(i));
        fireTableRowsDeleted(i, i + 1);
      }
    }
    return vToDel;
  }

  @Override
  public int getRowCount()
  {
    return vBuf == null ? 0 : vBuf.size();
  }

  @Override
  public Object getRowRecord(int row)
  {
    return vBuf == null || vBuf.isEmpty() ? null : vBuf.get(row);
  }

  public List getVBuf()
  {
    return vBuf;
  }
}
