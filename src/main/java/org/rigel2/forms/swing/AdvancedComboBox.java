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
package org.rigel2.forms.swing;

import java.awt.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import org.apache.commons.logging.*;
import org.rigel2.table.peer.PeerColumnDescriptor;

/**
 * Implementazione di un combo box con piu' campi visualizzati
 * partendo da un array di oggetti om (vedi it.italsystems.newstar.om.*).
 * I campi vengono visualizzati separati ed e' possibile agganciare un
 * editing sul primo: setText e getText agiscono cercando o riportando
 * i valori del primo campo.
 */
public class AdvancedComboBox extends JComboBox
{
  /** Logging */
  private static Log log = LogFactory.getLog(AdvancedComboBox.class);
  protected Class beanClass = null;
  protected PropertyDescriptor[] props = null;
  protected Vector vColumn = new Vector(10, 10);
  protected int totalWidth = 0;
  private int padding = 4;

  /**
   * Costruttore vuoto
   */
  public AdvancedComboBox()
  {
  }

  /**
   * Costruttore per vettore pronto e campo codice + campi visibile.
   */
  public AdvancedComboBox(Vector data, String fldKey, String[] fldVis)
     throws Exception
  {
    super(data);
    vColumn.add(new PeerColumnDescriptor(fldKey, fldKey, 10, false));
    for(int i = 0; i < fldVis.length; i++)
    {
      vColumn.add(new PeerColumnDescriptor(fldVis[i], fldVis[i], 10, false));
    }
    initInternal(data);
  }

  public AdvancedComboBox(Vector data, String fldKey, int sizeKey,
     String fldVis1, int size1)
     throws Exception
  {
    super(data);
    vColumn.add(new PeerColumnDescriptor(fldKey, fldKey, sizeKey, false));
    vColumn.add(new PeerColumnDescriptor(fldVis1, fldVis1, size1, false));
    initInternal(data);
  }

  public AdvancedComboBox(Vector data, String fldKey, int sizeKey,
     String fldVis1, int size1, String fldVis2, int size2)
     throws Exception
  {
    super(data);
    vColumn.add(new PeerColumnDescriptor(fldKey, fldKey, sizeKey, false));
    vColumn.add(new PeerColumnDescriptor(fldVis1, fldVis1, size1, false));
    vColumn.add(new PeerColumnDescriptor(fldVis2, fldVis2, size2, false));
    initInternal(data);
  }

  public void init(Vector objList)
     throws Exception
  {
    initInternal(objList);
    setModel(new DefaultComboBoxModel(objList));
  }

  protected void initInternal(Vector objList)
     throws Exception
  {
    if(objList.isEmpty())
      return;

    totalWidth = 0;
    beanClass = objList.get(0).getClass();
    props = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();

    for(int j = 0; j < getColumnCount(); j++)
    {
      boolean found = false;
      PeerColumnDescriptor cd = getColumn(j);
      cd.setModelIndex(j);
      for(int i = 0; i < props.length; i++)
      {
        if(props[i].getName().equalsIgnoreCase(cd.getName()))
        {
          cd.setPropDescr(props[i]);
          found = true;
          break;
        }
      }
      if(!found)
        throw new Exception("Colonna " + cd.getName() + " non trovata negli oggetti passati!");
      totalWidth += cd.getWidth() + padding;
    }

    ComboBoxRenderer render = new ComboBoxRenderer();
    render.setPreferredSize(new Dimension(totalWidth + 10, 20));
    setRenderer(render);
    setMaximumRowCount(10);
  }

  private class ComboBoxRenderer extends JLabel implements ListCellRenderer
  {
    private int idx = 0;
    private Object objRender = null;

    public ComboBoxRenderer()
    {
      setOpaque(true);
      setHorizontalAlignment(CENTER);
      setVerticalAlignment(CENTER);
    }

    @Override
    public void paint(Graphics g)
    {
      g.setFont(getFont());
      FontMetrics ft = g.getFontMetrics();
      int ypos = ((getSize().height - ft.getHeight()) / 2) + ft.getAscent();

      g.setColor(getBackground());
      g.fillRect(0, 0, getSize().width, getSize().height);
      g.setColor(getForeground());

      int xpos = 0;

      for(int i = 0; i < getColumnCount(); i++)
      {
        PeerColumnDescriptor cd = getColumn(i);
        try
        {
          g.drawString(formatCell(idx, i, cd.getValue(objRender)), xpos + padding, ypos);
        }
        catch(Exception ex)
        {
          log.debug("idx=" + idx);
          log.error("RIGEL:", ex);
        }

        if(i > 0)
        {
          g.setColor(Color.gray);
          g.drawLine(xpos, 0, xpos, getSize().height);
        }
        xpos += cd.getWidth();
      }
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
       int index, boolean isSelected, boolean cellHasFocus)
    {
      if(isSelected)
      {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      }
      else
      {
        setBackground(Color.white);
        setForeground(Color.black);
        //setBackground(list.getBackground());
        //setForeground(list.getForeground());
      }

      idx = index;
      objRender = value;

      this.setText("dummy");
      return this;
    }
  }

  public void addColumn(String fld, int size)
  {
    vColumn.add(new PeerColumnDescriptor(fld, fld, size, false));
  }

  public PeerColumnDescriptor getColumn(int col)
  {
    return (PeerColumnDescriptor) vColumn.get(col);
  }

  public int getColumnCount()
  {
    return vColumn.size();
  }

  public String getText()
  {
    try
    {
      int idx = getSelectedIndex();
      PeerColumnDescriptor cd = getColumn(0);
      return formatCell(idx, 0, cd.getValue(getModel().getElementAt(idx)));
    }
    catch(Exception ex)
    {
      log.error("RIGEL:", ex);
    }
    return null;
  }

  public void setText(String val)
  {
    try
    {
      PeerColumnDescriptor cd = getColumn(0);
      int numRow = getModel().getSize();
      for(int i = 0; i < numRow; i++)
      {
        Object valObj = cd.getValue(getModel().getElementAt(i));
        if(val.equals(formatCell(i, 0, valObj)))
        {
          setSelectedIndex(i);
          return;
        }
      }
      setSelectedIndex(0);
    }
    catch(Exception ex)
    {
      log.error("RIGEL:", ex);
    }
  }

  /**
   * Produce la stringa rappresentazione del dato.
   * Se la colonna ha un formattatore esplicito lo usa.
   * Utilizza il servizio di formattazione della data.
   */
  public String formatCell(int row, int col, Object value)
  {
    return getColumn(col).formatValue(value);
  }

  public void setPadding(int newPadding)
  {
    padding = newPadding;
  }

  public int getPadding()
  {
    return padding;
  }
}
