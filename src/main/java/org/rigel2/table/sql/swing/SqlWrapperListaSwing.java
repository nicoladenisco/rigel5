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
package org.rigel2.table.sql.swing;

import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.swing.JTable;
import org.rigel2.table.AbstractTablePager;
import org.rigel2.table.RigelTableModel;

/**
 * Wrapper delle liste per interfaccia swing.
 *
 * @author Nicola De Nisco
 */
public class SqlWrapperListaSwing extends SwingSqlWrapperBase
{
  public SqlWrapperListaSwing()
  {
    ptm = new org.rigel2.table.sql.swing.SqlTableModel();
  }

  public org.rigel2.table.sql.swing.SqlTableModel getTM()
  {
    return ((org.rigel2.table.sql.swing.SqlTableModel) (ptm));
  }

  public void init(JTable tbl)
     throws Exception
  {
    setPager(new AbstractTablePager()
    {
      @Override
      public RigelTableModel getRigelTableModel()
      {
        return ptm;
      }
    });

    getTM().attachAndInit(tbl, ssp.getSelect(), ssp.getFrom(), ssp.getWhere(), getOrderby());
    getTM().getQuery().setDeleteFrom(ssp.getDeleteFrom());
  }

  @Override
  public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception
  {
  }
}
