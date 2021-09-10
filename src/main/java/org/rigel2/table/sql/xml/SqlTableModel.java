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
package org.rigel2.table.sql.xml;

import javax.swing.table.DefaultTableColumnModel;
import org.rigel2.RigelI18nInterface;
import org.rigel2.table.*;
import org.rigel2.table.sql.SqlAbstractTableModel;
import org.rigel2.table.xml.xTable;

/**
 * TableModel specializzato per la produzione di XML.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlTableModel extends SqlAbstractTableModel
{
  protected xTable tblAttach = null;
  protected boolean suppEsclRicerca = false;

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
  public void rebind()
     throws Exception
  {
    super.rebind();
    reAttach();
  }

  @Override
  public void rebind(String _select, String _from, String _where, String _orderby)
     throws Exception
  {
    super.rebind(_select, _from, _where, _orderby);
    reAttach();
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
