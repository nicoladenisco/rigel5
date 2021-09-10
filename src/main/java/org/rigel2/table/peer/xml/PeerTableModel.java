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
package org.rigel2.table.peer.xml;

import javax.swing.table.*;
import org.rigel2.RigelI18nInterface;
import org.rigel2.table.MascheraRicercaGenerica;
import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.peer.PeerAbstractTableModel;
import org.rigel2.table.xml.xTable;

/**
 * <p>
 * Title: Rigel</p>
 * <p>
 * Description: PeerTableModel specializzato per la produzione di XML</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerTableModel extends PeerAbstractTableModel
{
  protected xTable tblAttach = null;
  protected boolean suppEsclRicerca = false;

  public PeerTableModel()
  {
  }

  public void attach(xTable tbl)
  {
    tblAttach = tbl;
    reAttach();
  }

  @Override
  public void reAttach()
  {
    dtcmdl = new DefaultTableColumnModel();
    for(int i = 0; i < vColumn.size(); i++)
    {
      RigelColumnDescriptor cd = (RigelColumnDescriptor) vColumn.get(i);
      if(cd.isVisible() && !(suppEsclRicerca && cd.isEscludiRicerca()))
        dtcmdl.addColumn(cd);
    }

    tblAttach.setModel(this);
    tblAttach.setColumnModel(dtcmdl);
  }

  @Override
  public MascheraRicercaGenerica getMascheraRG(RigelI18nInterface i18n)
     throws Exception
  {
    return null;
  }

  public void setSuppEsclRicerca(boolean suppEsclRicerca)
  {
    this.suppEsclRicerca = suppEsclRicerca;
  }

  public boolean isSuppEsclRicerca()
  {
    return suppEsclRicerca;
  }
}
