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
package org.rigel5.table.sql.html;

import javax.swing.table.*;
import org.rigel5.RigelI18nInterface;
import org.rigel5.db.sql.QueryBuilder;
import org.rigel5.table.MascheraRicercaGenerica;
import org.rigel5.table.html.HtmlMascheraRicercaGenerica;
import org.rigel5.table.html.hTable;
import org.rigel5.table.sql.SqlAbstractTableModel;
import org.rigel5.table.sql.SqlBuilderRicercaGenerica;
import org.rigel5.table.sql.SqlColumnDescriptor;

/**
 * <p>
 * Title: fonte dati SQL.</p>
 * <p>
 * Description: Questo table model ha lo scopo di fornire
 * dati a partire da una query libera eseguita sul database.</p>
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlTableModel extends SqlAbstractTableModel
{
  protected hTable tblAttach = null;

  public void attach(hTable tbl)
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
      SqlColumnDescriptor cd = (SqlColumnDescriptor) vColumn.get(i);

      /* NOTA IMPORTANTE:
       la versione HTML aggiunge tutte le colonne per
       poter gestiore eventuali campi di input invisibili comunque
       associati alle colonne invisibili;
       i controlli di visibilita' sono eseguiti nella hTable
       if(cd.isVisible())
       dtcmdl.addColumn(cd);
       */
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
  public void rebind(QueryBuilder qb)
     throws Exception
  {
    super.rebind(qb);
    reAttach();
  }

  @Override
  public void rebind(String _select, String _from, String _where, String _orderby)
     throws Exception
  {
    super.rebind(_select, _from, _where, _orderby);
    reAttach();
  }

  public hTable getTblAttach()
  {
    return tblAttach;
  }

  @Override
  public MascheraRicercaGenerica getMascheraRG(RigelI18nInterface i18n)
     throws Exception
  {
    String nometab = query.getVista();
    return new HtmlMascheraRicercaGenerica(new SqlBuilderRicercaGenerica(this, nometab), this, i18n);
  }
}
