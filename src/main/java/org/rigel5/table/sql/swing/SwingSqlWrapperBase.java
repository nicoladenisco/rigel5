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
package org.rigel5.table.sql.swing;

import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.wrapper.ParametroListe;
import org.rigel5.table.html.wrapper.SwingWrapperBase;
import org.rigel5.table.sql.SqlSelectParam;

/**
 * Classe base dei Wrapper per Swing costruiti su liste-sql o form-sql o liste-edit-sql.
 *
 * @author Nicola De Nisco
 */
abstract public class SwingSqlWrapperBase extends SwingWrapperBase
{
  public SqlSelectParam ssp = new SqlSelectParam();

  public void setSelect(String string)
  {
    ssp.setSelect(string);
  }

  public void setFrom(String string)
  {
    ssp.setFrom(string);
  }

  public void setWhere(String string)
  {
    ssp.setWhere(string);
  }

  public void setOrderby(String string)
  {
    ssp.setOrderby(string);
  }

  public void setHaving(String having)
  {
    ssp.setHaving(having);
  }

  public void setDeleteFrom(String deleteFrom)
  {
    ssp.setDeleteFrom(deleteFrom);
  }

  public String getOrderby()
  {
    return getOrderby(ptm);
  }

  public String getOrderby(RigelTableModel ptm)
  {
    if(ssp.getOrderby() == null && !sortColumns.isEmpty())
    {
      String orderby = "";
      for(String item : sortColumns)
      {
        RigelColumnDescriptor cd = ptm.getColumn(item);
        if(cd != null)
          orderby += "," + cd.getName();
      }

      orderby = orderby.length() == 0 ? null : orderby.substring(1);
      ssp.setOrderby(orderby);
    }
    return ssp.getOrderby();
  }

  public String getWhereParametri()
  {
    if(!filtro.haveParametri())
      return null;

    String rv = "";
    for(ParametroListe pl : filtro.getParametri())
    {
      if(pl.getValore() != null)
        rv += " AND " + pl.getCampo() + pl.getOperazione() + pl.getValoreFmt();
    }

    return rv.length() == 0 ? null : rv.substring(5);
  }

  public void cumulaWhereParametri()
  {
    String wp = getWhereParametri();
    if(wp == null)
      return;

    if(ssp.getWhere() == null)
      ssp.setWhere(wp);
    else
      ssp.setWhere(ssp.getWhere() + " AND " + wp);
  }
}
