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
package org.rigel5.table.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.apache.commons.logging.*;
import org.rigel5.table.BuilderRicercaGenerica;
import org.rigel5.table.MascheraRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.RigelHtmlPage;

/**
 * Costruttore di una maschera di ricerca per le liste
 * che utilizzano gli oggetti OM di torque (peer).
 * Questa versione e' progettata per swing e di fatto
 * crea una finestra JFrame per la selezione del filtro.
 *
 * @author Nicola De Nisco
 */
public class SwingMascheraRicercaGenerica extends JPanel implements MascheraRicercaGenerica
{
  /** Logging */
  private static Log log = LogFactory.getLog(SwingMascheraRicercaGenerica.class);
  JPanel jPanelComando = new JPanel();
  JButton butOk = new JButton();
  JButton butPulisci = new JButton();
  JButton butAnnulla = new JButton();
  JPanel jPanelMessaggio = new JPanel();
  JLabel jLabel1 = new JLabel();
  JPanel jPanelSelezione = new JPanel();
  GridLayout gridLayout1 = new GridLayout();
  ArrayList<MascheraFiltroListner> listners = new ArrayList<MascheraFiltroListner>();
  BuilderRicercaGenerica brg = null;
  public Object retCrit = null;
  protected RigelTableModel ptm = null;
  JLabel la[] = null;
  JComboBox cb[] = null;
  JTextField tf[] = null;

  public SwingMascheraRicercaGenerica(BuilderRicercaGenerica brg, RigelTableModel rtm)
  {
    try
    {
      this.ptm = rtm;
      this.brg = brg;
      jbInit();
    }
    catch(Exception e)
    {
      log.error("RIGEL:", e);
    }
  }

  private void jbInit()
     throws Exception
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
    butPulisci.setText("Pulisci filtro");
    butPulisci.addActionListener(new java.awt.event.ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        butPulisci_actionPerformed(e);
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
    setLayout(new BorderLayout());
    jLabel1.setText("Selezionare i parametri di ricerca");
    jPanelSelezione.setLayout(gridLayout1);
    gridLayout1.setColumns(3);
    gridLayout1.setHgap(10);
    gridLayout1.setVgap(3);
    add(jPanelComando, BorderLayout.SOUTH);
    jPanelComando.add(butOk, null);
    jPanelComando.add(butPulisci, null);
    jPanelComando.add(butAnnulla, null);
    add(jPanelMessaggio, BorderLayout.NORTH);
    jPanelMessaggio.add(jLabel1, null);
    add(jPanelSelezione, BorderLayout.CENTER);

    cbInit();
  }

  public void addListner(MascheraFiltroListner listner)
  {
    listners.add(listner);
  }

  private void cbInit()
  {
    la = new JLabel[ptm.getColumnCount()];
    cb = new JComboBox[ptm.getColumnCount()];
    tf = new JTextField[ptm.getColumnCount()];

    gridLayout1.setRows(ptm.getColumnCount());
    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = ptm.getColumn(i);

      JLabel l = new JLabel(cd.getCaption());
      JComboBox c = new JComboBox(brg.getTipiConfronto());
      JTextField t = new JTextField();

      la[i] = l;
      cb[i] = c;
      tf[i] = t;

      jPanelSelezione.add(l, null);
      jPanelSelezione.add(c, null);
      jPanelSelezione.add(t, null);
    }
  }

  private Object buildCriteria()
     throws Exception
  {
    for(int i = 0; i < ptm.getColumnCount(); i++)
    {
      RigelColumnDescriptor cd = ptm.getColumn(i);

      cd.setFiltroTipo(cb[i].getSelectedIndex());
      cd.setFiltroValore(tf[i].getText());
    }

    return brg.buildCriteria();
  }

  void butOk_actionPerformed(ActionEvent e)
  {
    try
    {
      retCrit = buildCriteria();
      for(MascheraFiltroListner ml : listners)
      {
        ml.filtroCambiato(retCrit);
        ml.butOkPressed();
      }
    }
    catch(Exception ex)
    {
      log.error("RIGEL:", ex);
    }
  }

  void butPulisci_actionPerformed(ActionEvent e)
  {
    try
    {
      retCrit = buildCriteria();
      for(MascheraFiltroListner ml : listners)
      {
        ml.filtroCambiato(retCrit);
        ml.butPulisciPressed();
      }
    }
    catch(Exception ex)
    {
      log.error("RIGEL:", ex);
    }
  }

  void butAnnulla_actionPerformed(ActionEvent e)
  {
    for(MascheraFiltroListner ml : listners)
    {
      ml.butAnnullaPressed();
    }
  }

  @Override
  public Object buildCriteriaSafe(Map params)
     throws Exception
  {
    return null;
  }

  @Override
  public void buildHtmlRicerca(String nomeForm, RigelHtmlPage page)
     throws Exception
  {
  }

  @Override
  public void buildHtmlRicercaSemplice(String nomeForm, int sizeFld, RigelHtmlPage page)
     throws Exception
  {
  }
}
