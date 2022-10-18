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
package org.rigel5.table.sql;

import com.workingdogs.village.Record;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Descrittore di colonna specializzato per l'uso di SqlAbstractTableModel.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlColumnDescriptor extends RigelColumnDescriptor
{
  private int cIndex;

  public SqlColumnDescriptor()
  {
    super();
  }

  public SqlColumnDescriptor(String Caption, String Name, int Size)
  {
    super();
    setHeaderValue(Caption);
    setSize(Size);
    setNomeCalc(Name);
  }

  public SqlColumnDescriptor(String Caption, String Name, int Size, boolean Editable)
  {
    super();
    setHeaderValue(Caption);
    setSize(Size);
    editable = Editable;
    setNomeCalc(Name);
  }

  public SqlColumnDescriptor(String Caption, String Name, int Size,
     boolean Editable, boolean Visible)
  {
    super();
    setHeaderValue(Caption);
    setSize(Size);
    editable = Editable;
    visible = Visible;
    setNomeCalc(Name);
  }

  public SqlColumnDescriptor(String Name, int Pos, int Lun)
  {
    super();
    setHeaderValue(Name);
    asciiPos = Pos;
    asciiLun = Lun;
    setNomeCalc(Name);
  }

  @Override
  public void setValue(Object bean, Object value)
     throws java.lang.Exception
  {
    Record r = (Record) bean;
    r.getValue(cIndex).setValue(value);
    r.markValueDirty(cIndex);
  }

  @Override
  public Object getValue(Object bean)
     throws java.lang.Exception
  {
    return isCalcolato() || bean == null ? null : ((Record) (bean)).getValue(cIndex).getValue();
  }

  public void setCIndex(int cIndex)
  {
    this.cIndex = cIndex;
  }

  public int getCIndex()
  {
    return cIndex;
  }
}
