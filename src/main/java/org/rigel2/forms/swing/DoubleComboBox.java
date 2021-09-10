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
import java.util.List;
import java.util.*;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rigel2.table.ForeignDataHolder;

/**
 * Combo box a doppia visualizzazione.
 * 
 * @author Nicola De Nisco
 */
public class DoubleComboBox extends JComboBox
{
  /** Logging */
  private static Log log = LogFactory.getLog(DoubleComboBox.class);
  protected String keys[] = null;
  protected int totalWidth = 0;
  protected int keyWidth = 0;
  protected int padding = 4;
  protected int drawMode = 0;
  protected Map<String, String> htList = null;

  class StyledComboBoxUI extends BasicComboBoxUI
  {
    @Override
    protected ComboPopup createPopup()
    {
      BasicComboPopup cbp = new BasicComboPopup(comboBox)
      {
        @Override
        protected Rectangle computePopupBounds(int px, int py, int pw, int ph)
        {
          return super.computePopupBounds(
             px, py, Math.max(comboBox.getPreferredSize().width, pw), ph);
        }
      };
      cbp.getAccessibleContext().setAccessibleParent(comboBox);
      return cbp;
    }
  }

  /**
   * Costruttore vuoto
   */
  public DoubleComboBox()
  {
    setUI(new StyledComboBoxUI());
  }

  public void init(Map<String, String> ht, boolean sort)
     throws Exception
  {
    htList = ht;
    keys = new String[ht.size()];
    keyWidth = totalWidth = 0;

    int i = 0;
    FontMetrics ft = getFontMetrics(getFont());
    Iterator<Map.Entry<String, String>> itr = ht.entrySet().iterator();
    while(itr.hasNext())
    {
      Map.Entry<String, String> entry = itr.next();
      String key = entry.getKey();
      String val = entry.getValue();
      keys[i++] = key;

      keyWidth = Math.max(keyWidth, ft.stringWidth(key) + padding + padding);
      totalWidth = Math.max(totalWidth, keyWidth + ft.stringWidth(val) + padding + padding);
    }

    if(sort)
      Arrays.sort(keys);

    removeAllItems();
    for(i = 0; i < keys.length; i++)
      addItem(keys[i]);

    ComboBoxRenderer render = new ComboBoxRenderer();
    render.setPreferredSize(new Dimension(totalWidth + 10, 20));
    setRenderer(render);
    setMaximumRowCount(10);
    computPreferedSize();
  }

  public void init(List<ForeignDataHolder> fdList, boolean sort)
     throws Exception
  {
    htList = new HashMap<String, String>();
    keys = new String[fdList.size()];
    keyWidth = totalWidth = 0;

    int i = 0;
    FontMetrics ft = getFontMetrics(getFont());
    Iterator<ForeignDataHolder> itr = fdList.iterator();
    while(itr.hasNext())
    {
      ForeignDataHolder entry = itr.next();
      String key = entry.codice;
      String val = entry.descrizione;
      keys[i++] = key;
      htList.put(key, val);

      keyWidth = Math.max(keyWidth, ft.stringWidth(key) + padding + padding);
      totalWidth = Math.max(totalWidth, keyWidth + ft.stringWidth(val) + padding + padding);
    }

    if(sort)
      Arrays.sort(keys);

    removeAllItems();
    for(i = 0; i < keys.length; i++)
      addItem(keys[i]);

    ComboBoxRenderer render = new ComboBoxRenderer();
    render.setPreferredSize(new Dimension(totalWidth + 10, 20));
    setRenderer(render);
    setMaximumRowCount(10);
    computPreferedSize();
  }

  protected void computPreferedSize()
  {
    Dimension d = getPreferredSize();

    switch(drawMode)
    {
      case 0:
        d.width = totalWidth;
        break;
      case 1:
        d.width = totalWidth - keyWidth;
        break;
    }

    setPreferredSize(d);
  }

  private class ComboBoxRenderer extends JLabel implements ListCellRenderer
  {
    private int idx = 0;
    private String objRender = null;

    public ComboBoxRenderer()
    {
      setOpaque(true);
      setHorizontalAlignment(CENTER);
      setVerticalAlignment(CENTER);
    }

    @Override
    public void paint(Graphics g)
    {
      switch(drawMode)
      {
        case 0:
          paintTutto(g);
          return;
        case 1:
          paintDescrizione(g);
          return;
      }
    }

    public void paintDescrizione(Graphics g)
    {
      Dimension d = getSize();
      g.setFont(getFont());
      FontMetrics ft = g.getFontMetrics();
      int xpos = 0;
      int ypos = ((d.height - ft.getHeight()) / 2) + ft.getAscent();

      g.setColor(getBackground());
      g.fillRect(0, 0, d.width, d.height);

      if(objRender != null)
      {
        String valRender = (String) htList.get(objRender);
        if(valRender != null)
        {
          g.setColor(getForeground());
          g.drawString(valRender, xpos + padding, ypos);
        }
      }
    }

    public void paintTutto(Graphics g)
    {
      Dimension d = getSize();
      g.setFont(getFont());
      FontMetrics ft = g.getFontMetrics();
      int xpos = 0;
      int ypos = ((d.height - ft.getHeight()) / 2) + ft.getAscent();

      g.setColor(getBackground());
      g.fillRect(0, 0, d.width, d.height);

      if(objRender != null)
      {
        String valRender = (String) htList.get(objRender);
        g.setColor(getForeground());
        g.drawString(objRender, xpos + padding, ypos);
        xpos += keyWidth;
        g.setColor(Color.gray);
        g.drawLine(xpos, 0, xpos, getSize().height);
        if(valRender != null)
        {
          g.setColor(getForeground());
          g.drawString(valRender, xpos + padding, ypos);
        }
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
        //setBackground(Color.white);
        //setForeground(Color.black);
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }

      idx = index;
      objRender = (String) value;

      this.setText("dummy");
      return this;
    }
  }

  public String getText()
  {
    try
    {
      if(keys == null)
        return null;

      int idx = getSelectedIndex();
      return keys[idx];
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
      if(keys == null || val == null)
        return;

      for(int i = 0; i < keys.length; i++)
      {
        if(val.equals(keys[i]))
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

  public void setPadding(int newPadding)
  {
    padding = newPadding;
  }

  public int getPadding()
  {
    return padding;
  }

  public int getDrawMode()
  {
    return drawMode;
  }

  public void setDrawMode(int drawMode)
  {
    this.drawMode = drawMode;
  }
}
