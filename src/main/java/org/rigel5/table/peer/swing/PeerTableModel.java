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
package org.rigel5.table.peer.swing;

import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rigel5.RigelI18nInterface;
import org.rigel5.SetupHolder;
import org.rigel5.forms.swing.DoubleComboBox;
import org.rigel5.table.MascheraRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.peer.PeerAbstractTableModel;
import org.rigel5.table.peer.PeerBuilderRicercaGenerica;
import org.rigel5.table.peer.PeerColumnDescriptor;
import org.rigel5.table.swing.ForeignColumnCellEditor;
import org.rigel5.table.swing.RigelTableCellEditor;
import org.rigel5.table.swing.RigelTableCellRender;
import org.rigel5.table.swing.SwingMascheraRicercaGenerica;

/**
 * <p>
 * Title: Rigel</p>
 * <p>
 * Description: PeerTableModel specializzato per la gestione di una JTable (Swing)</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerTableModel extends PeerAbstractTableModel
{
  /** Logging */
  private static Log log = LogFactory.getLog(PeerTableModel.class);
  protected JTable tblAttach = null;

  public PeerTableModel()
  {
  }

  public void attachAndInit(JTable tbl, List objList)
     throws Exception
  {
    tblAttach = tbl;
    init(objList);
    tbl.setModel(this);
    reAttach();
  }

  public void attachAndInit(JTable tbl, Class objClass)
     throws Exception
  {
    tblAttach = tbl;
    init(objClass);
    tbl.setModel(this);
    reAttach();
  }

  @Override
  public void reAttach()
  {
    dtcmdl = new DefaultTableColumnModel();
    for(int i = 0; i < vColumn.size(); i++)
    {
      PeerColumnDescriptor cd = (PeerColumnDescriptor) vColumn.get(i);

      if(cd.isVisible())
      {
        // imposta renderizzatore
        cd.setCellRenderer(new RigelTableCellRender(tblAttach.getDefaultRenderer(cd.getValClass())));

        // se è attiva la modalità foreign genera il combo box per la selezione
        if(cd.getForeignMode() != RigelColumnDescriptor.DISP_FLD_ONLY)
        {
          if(cd.getComboExtraWhere() != null)
          {
            // nel caso extra where il set di dati foreign
            // può cambiare per ogni record potendo essere influenzato
            // da eventuali macro presenti nella clausola extra where
            cd.setCellEditor(new ForeignColumnCellEditor(new DoubleComboBox()));
          }
          else
          {
            // in tutti gli altri casi il set di dati foreign
            // è lo stesso per tutti i record e quindi
            // risparmiamo tempo e memoria utilizzando lo stesso
            // set di dati per tutti i record
            cd.setCellEditor(getComboEditor(cd));
          }
        }
        else
        {
          // imposta editor
          cd.setCellEditor(new RigelTableCellEditor(tblAttach.getDefaultEditor(cd.getValClass())));
        }

        dtcmdl.addColumn(cd);

        // carica nelle dimensioni colonna il valore corrispettivo in size
        int width = cd.getSize() * 100;
        cd.setWidth(width);
        cd.setPreferredWidth(width);
      }
    }

    tblAttach.setModel(this);
    tblAttach.setColumnModel(dtcmdl);
    fireTableDataChanged();
  }

  @Override
  public void setColumnVisible(int numcol, boolean visible)
  {
    super.setColumnVisible(numcol, visible);
    reAttach();
  }

  @Override
  public boolean toggleColumnVisible(int numcol)
  {
    boolean rv = super.toggleColumnVisible(numcol);
    reAttach();
    return rv;
  }

  @Override
  public MascheraRicercaGenerica getMascheraRG(RigelI18nInterface i18n)
     throws Exception
  {
    return new SwingMascheraRicercaGenerica(new PeerBuilderRicercaGenerica(this, map), this);
  }

  /**
   * Quando è attiva la modalità foreign su una colonna genera un
   * combo box con i valori della tabella collegata per l'editing.
   * @param cd
   * @return
   */
  protected TableCellEditor getComboEditor(PeerColumnDescriptor cd)
  {
    try
    {
      if(query == null)
        query = makeQueryBuilder();

      List foreignData = cd.getForeignValues(this, SetupHolder.getRi18n());

      switch(cd.getForeignMode())
      {
        case RigelColumnDescriptor.DISP_FLD_ONLY:
          // nessun collegamento master-detail
          return null;
        case RigelColumnDescriptor.DISP_FLD_EDIT:
        // collegamento master-detail in edit senza descrizione
        case RigelColumnDescriptor.DISP_DESCR_EDIT:
          // collegamento master-detail in edit con descrizione
          DoubleComboBox cb = new DoubleComboBox();
          cb.init(foreignData, false);
          return new DefaultCellEditor(cb);
//
//          return new DefaultCellEditor(
//             new JComboBox(new DefaultComboBoxModel(foreignData.toArray())));
//
        default:
          // collegamento master-detail di sola visualizzazione
          return null;
      }
    }
    catch(Exception ex)
    {
      log.error("Errore nella creazione dell'editor (combobox) per la tabella.", ex);
      return null;
    }
  }
}
