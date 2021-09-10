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
package org.rigel2.table.peer.swing;

import java.awt.*;
import javax.swing.*;
import org.apache.commons.logging.*;
import org.rigel2.RigelI18nInterface;
import org.rigel2.forms.swing.*;
import org.rigel2.table.*;
import org.rigel2.table.peer.*;

/**
 * <p>
 * Title: Rigel</p>
 * <p>
 * Description: Gestore di un form bindato con campi presenti nei peer.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerFormModel extends PeerAbstractTableModel
{
  /** Logging */
  private static Log log = LogFactory.getLog(PeerFormModel.class);
  protected JComponent compAttach = null;
  protected JComponent[] editors = null;
  private boolean passiveMode = false;

  public PeerFormModel()
  {
  }

  public PeerFormModel(boolean passive)
  {
    passiveMode = passive;
  }

  /**
   * Collega questo gestore dati al componente che ricevera'
   * le label e i controlli di edit
   */
  public void attach(JComponent comp)
  {
    compAttach = comp;
    reAttach();
  }

  /**
   * Usato per la modalita' passiva: solo binding dei dati
   */
  public void attachPassive()
  {
    passiveMode = true;
    compAttach = null;
    reAttach();
  }

  /**
   * Collega i controlli e i dati
   */
  @Override
  public void reAttach()
  {
    if(passiveMode)
      reAttachPassive();
    else
      reAttachActive();
  }

  /**
   * Ripete l'operazione di attach al pannello:
   * rigenera le label e i controlli di editing per i campi visibili
   */
  protected void reAttachActive()
  {
    compAttach.removeAll();
    editors = new JComponent[vColumn.size()];

    for(int i = 0; i < vColumn.size(); i++)
    {
      PeerColumnDescriptor cd = (PeerColumnDescriptor) vColumn.get(i);

      if(cd.isVisible())
      {
        JLabel lbl = createLabel(cd);
        JComponent ced = cd.getFormControl();
        if(ced == null)
          ced = createEditor(cd);
        editors[i] = ced;
        addControls(i, cd, lbl, ced);
      }
    }
  }

  /**
   * Usata in modalita' passiva: l'aspetto grafico e' stato gia'
   * fissato, le uniche operazioni da fare sono il binding dei dati
   */
  protected void reAttachPassive()
  {
    editors = new JComponent[vColumn.size()];

    for(int i = 0; i < vColumn.size(); i++)
    {
      PeerColumnDescriptor cd = (PeerColumnDescriptor) vColumn.get(i);

      if(cd.isVisible())
      {
        JComponent ced = cd.getFormControl();
        if(ced == null)
          cd.setVisible(false);
        else
          editors[i] = ced;
      }
    }
  }

  public int addColumn(String Caption, String Name, int Size,
     JComponent editCtrl, boolean Editable)
  {
    PeerColumnDescriptor cd = new PeerColumnDescriptor(Caption, Name, Size, Editable);
    cd.setFormControl(editCtrl);
    return addColumn(cd);
  }

  public int addColumn(String Caption, String Name, int Size,
     JComponent editCtrl, boolean Editable, boolean Visible)
  {
    PeerColumnDescriptor cd = new PeerColumnDescriptor(Caption, Name, Size, Editable, Visible);
    cd.setFormControl(editCtrl);
    return addColumn(cd);
  }

  /**
   * Inserisce la label e il controllo di edit sul pannello ospite
   */
  public void addControls(int col, PeerColumnDescriptor cd, JLabel lbl, JComponent ced)
  {
    compAttach.add(lbl);
    compAttach.add(ced);
  }

  /**
   * Collega a questo gestore un nuovo vettore di elementi
   */
  @Override
  public void rebind(java.util.List newRows)
  {
    super.rebind(newRows);
    reAttach();
  }

  /**
   * Imposta lo stato di visibilita' della colonna
   * (esegue una reAttach() per rigenerare i controlli di conseguenza)
   */
  @Override
  public void setColumnVisible(int numcol, boolean visible)
  {
    super.setColumnVisible(numcol, visible);
    reAttach();
  }

  /**
   * Imposta lo stato di visibilita' della colonna
   * (esegue una reAttach() per rigenerare i controlli di conseguenza)
   */
  @Override
  public boolean toggleColumnVisible(int numcol)
  {
    boolean rv = super.toggleColumnVisible(numcol);
    reAttach();
    return rv;
  }

  /**
   * Crea la label con l'indicazione del nome del campo
   */
  public JLabel createLabel(PeerColumnDescriptor cd)
  {
    JLabel lbl = new JLabel();
    lbl.setText(cd.getCaption());
    lbl.setPreferredSize(new Dimension(cd.getWidth(), 21));
    return lbl;
  }

  /**
   * Crea il controllo di editing per il campo
   */
  public JComponent createEditor(PeerColumnDescriptor cd)
  {
    AdvancedTextField atf = new AdvancedTextField();
    atf.setPreferredSize(new Dimension(cd.getWidth(), 21));
    atf.setReadOnly(!cd.isEditable());
    return atf;
  }

  /**
   * Visualizza i dati di un record portandoli nei controlli di edit
   */
  public void showRecord(int numrec)
  {
    for(int i = 0; i < vColumn.size(); i++)
    {
      PeerColumnDescriptor cd = (PeerColumnDescriptor) vColumn.get(i);

      if(cd.isVisible())
      {
        setControlData(editors[i], formatCell(numrec, i, getValueAt(numrec, i)));
      }
    }
  }

  /**
   * Salva i dati di un record leggendoli dai controlli di edit
   */
  public void saveRecord(int numrec)
  {
    for(int i = 0; i < vColumn.size(); i++)
    {
      PeerColumnDescriptor cd = (PeerColumnDescriptor) vColumn.get(i);

      if(cd.isVisible() && cd.isEditable())
      {
        String fldSVal = null;
        Object fldOVal = null;

        try
        {

          if((fldSVal = getControlData(editors[i])) != null)
            if((fldOVal = parseCell(numrec, i, fldSVal)) != null)
              setValueAt(fldOVal, numrec, i);

        }
        catch(Exception ex)
        {
          JOptionPane.showMessageDialog(null,
             "Campo='" + cd.getCaption() + "' valore='" + fldSVal + "'\n" + ex.getMessage(), "Errore memorizzazione dati", JOptionPane.ERROR_MESSAGE);
          log.error("RIGEL:", ex);
        }
      }
    }
  }

  /**
   * Imposta la visualizzazione di un dato nel controllo indicato
   */
  public void setControlData(JComponent jc, String val)
  {
    if(jc instanceof JTextField)
      ((JTextField) (jc)).setText(val);
    else if(jc instanceof AdvancedComboBox)
      ((AdvancedComboBox) (jc)).setText(val);
    else if(jc instanceof DoubleComboBox)
      ((DoubleComboBox) (jc)).setText(val);
  }

  /**
   * Acquisisce il dato dal controllo indicato
   */
  public String getControlData(JComponent jc)
  {
    if(jc instanceof DoubleComboBox)
    {
      return ((DoubleComboBox) (jc)).getText();
    }

    if(jc instanceof AdvancedComboBox)
    {
      return ((AdvancedComboBox) (jc)).getText();
    }

    if(jc instanceof AdvancedTextField)
    {
      return ((AdvancedTextField) (jc)).isModified() ? ((AdvancedTextField) (jc)).getText() : null;
    }

    if(jc instanceof JTextField)
    {
      return ((JTextField) (jc)).getText();
    }

    return null;
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

  /**
   * Parsing del contenuto di un campo di edit.
   * Se la colonna ha un formattatore esplicito lo usa.
   * Utilizza il servizio di formattazione della data.
   */
  public Object parseCell(int row, int col, String value)
     throws Exception
  {
    RigelColumnDescriptor cd = getColumn(col);
    return cd.parseValue(value);
  }

  public void setPassiveMode(boolean newPassiveMode)
  {
    passiveMode = newPassiveMode;
  }

  public boolean isPassiveMode()
  {
    return passiveMode;
  }

  @Override
  public MascheraRicercaGenerica getMascheraRG(RigelI18nInterface i18n)
     throws Exception
  {
    return null;
  }
}
