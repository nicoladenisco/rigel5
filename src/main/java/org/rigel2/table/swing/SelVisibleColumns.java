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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.apache.commons.logging.*;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;

/**
 * Maschera per la selezione delle colonne visibili/invisibili.
 *
 * @author Nicola De Nisco
 */
public class SelVisibleColumns extends JFrame
{
  /** Logging */
  private static Log log = LogFactory.getLog(SelVisibleColumns.class);
  JPanel jPanelSelezione = new JPanel();
  JPanel jPanelComando = new JPanel();
  JButton butOk = new JButton();
  JButton butAnnulla = new JButton();
  RigelTableModel ptm = null;
  JCheckBox[] colVis = null;
  GridLayout gridLayout1 = new GridLayout();
  JPanel jPanelMessaggio = new JPanel();
  JLabel jLabel1 = new JLabel();
  JButton butApplica = new JButton();

  public static void showDialog(RigelTableModel Ptm)
  {
    SelVisibleColumns svDiag = new SelVisibleColumns(Ptm);
    svDiag.show();
  }

  public static void showDialog(RigelTableModel Ptm, String title)
  {
    SelVisibleColumns svDiag = new SelVisibleColumns(Ptm);
    svDiag.setTitle(title);
    svDiag.show();
  }

  public SelVisibleColumns(RigelTableModel Ptm)
  {
    ptm = Ptm;

    try {
      jbInit();
    }
    catch(Exception e) {
      log.error("RIGEL:", e);
    }
  }

  private void jbInit() throws Exception
  {
    butOk.setText("Ok");
    butOk.addActionListener(new java.awt.event.ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        butOk_actionPerformed(e);
      }
    });
    butAnnulla.setText("Annulla");
    butAnnulla.addActionListener(new java.awt.event.ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        butAnnulla_actionPerformed(e);
      }
    });
    jPanelSelezione.setLayout(gridLayout1);
    gridLayout1.setRows(10);
    gridLayout1.setColumns(1);
    jLabel1.setText("Selezionare le colonne da visualizzare");
    butApplica.setToolTipText("");
    butApplica.setText("Applica");
    butApplica.addActionListener(new java.awt.event.ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        butApplica_actionPerformed(e);
      }
    });
    this.getContentPane().add(jPanelSelezione, BorderLayout.CENTER);
    this.getContentPane().add(jPanelComando, BorderLayout.SOUTH);
    jPanelComando.add(butOk, null);
    jPanelComando.add(butApplica, null);
    jPanelComando.add(butAnnulla, null);
    this.getContentPane().add(jPanelMessaggio, BorderLayout.NORTH);
    jPanelMessaggio.add(jLabel1, null);

    cbInit();
    pack();
  }

  private void cbInit()
  {
    gridLayout1.setRows(ptm.getColumnCount());
    colVis = new JCheckBox[ptm.getColumnCount()];

    for(int i = 0; i < ptm.getColumnCount(); i++) {
      RigelColumnDescriptor cd = ptm.getColumn(i);

      JCheckBox jcb = new JCheckBox();
      colVis[i] = jcb;
      jcb.setText(cd.getCaption());
      jcb.setSelected(cd.isVisible());
      jPanelSelezione.add(jcb, null);
    }
  }

  void butOk_actionPerformed(ActionEvent e)
  {
    butApplica_actionPerformed(e);
    butAnnulla_actionPerformed(e);
  }

  void butAnnulla_actionPerformed(ActionEvent e)
  {
    setVisible(false);
    dispose();
  }

  void butApplica_actionPerformed(ActionEvent e)
  {
    for(int i = 0; i < ptm.getColumnCount(); i++) {
      RigelColumnDescriptor cd = ptm.getColumn(i);
      JCheckBox jcb = colVis[i];
      cd.setVisible(jcb.isSelected());
    }
    ptm.reAttach();
  }
}

