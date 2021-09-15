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
package org.rigel5.table.peer;

import java.beans.*;
import java.lang.reflect.*;
import org.apache.commons.logging.*;
import org.apache.torque.map.ColumnMap;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Descrittore di colonna specializzato per l'inserimento in PeerAbstractTableModel.
 *
 * @author unascribed
 * @version 1.0
 */
public class PeerColumnDescriptor extends RigelColumnDescriptor
{
  /** Logging */
  private static Log log = LogFactory.getLog(PeerColumnDescriptor.class);
  private PropertyDescriptor prop;
  protected Method getter = null;
  protected Method setter = null;
  protected ColumnMap cmap = null;

  public PeerColumnDescriptor()
  {
    super();
  }

  public PeerColumnDescriptor(String Caption, String Name, int Size)
  {
    super();
    setHeaderValue(Caption);
    setSize(Size);
    setNomeCalc(Name);
  }

  public PeerColumnDescriptor(String Caption, String Name, int Size, boolean Editable)
  {
    super();
    setHeaderValue(Caption);
    setSize(Size);
    setEditable(Editable);
    setNomeCalc(Name);
  }

  public PeerColumnDescriptor(String Caption, String Name, int Size,
     boolean Editable, boolean Visible)
  {
    super();
    setHeaderValue(Caption);
    setSize(Size);
    setEditable(Editable);
    setVisible(Visible);
    setNomeCalc(Name);
  }

  public PeerColumnDescriptor(String Name, int Pos, int Lun)
  {
    super();
    setHeaderValue(Name);
    setAsciiPos(Pos);
    setAsciiLun(Lun);
    setNomeCalc(Name);
  }

  public void setPropDescr(PropertyDescriptor Prop)
     throws Exception
  {
    prop = Prop;
    if(prop != null)
    {
      if(prop instanceof IndexedPropertyDescriptor)
        throw new Exception(prop.getName()
           + " e' una proprieta' con indice(non supportata)");

      setValClass(prop.getPropertyType());

      getter = prop.getReadMethod();
      setter = prop.getWriteMethod();
      if(setter == null)
        setEditable(false);

      if(getter == null)
        log.debug("Warning: il getter della proprieta' " + getName() + " e' null (" + prop.toString() + ")");
    }
  }

  public PropertyDescriptor getPropDescr()
  {
    return prop;
  }

  @Override
  public Object getValue(Object bean)
     throws Exception
  {
    return isCalcolato() || getter == null ? null : getter.invoke(bean, (Object[]) null);
  }

  @Override
  public void setValue(Object bean, Object value)
     throws Exception
  {
    if(isCalcolato() || setter == null)
      return;

    setter.invoke(bean, value);
  }

  public ColumnMap getCmap()
  {
    return cmap;
  }

  public void setCmap(ColumnMap cmap)
  {
    this.cmap = cmap;
  }

  @Override
  public Object parseValue(String value)
  {
    // limitazione campi stringa alla lunghezza della struttura
    if(cmap != null && cmap.getSize() > 0)
    {
      switch(dataType)
      {
        case PDT_STRINGKEY:
        case PDT_STRING:
          if(value.length() > cmap.getSize())
            value = value.substring(0, cmap.getSize());
      }
    }

    return super.parseValue(value);
  }

  @Override
  public boolean isTestfornull()
  {
    // controlla che nella map di tabella il campo sia NOT NULL
    if(cmap != null && cmap.isNotNull())
      return true;

    return testfornull;
  }
}
