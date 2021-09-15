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
package org.rigel5.table.sql;

import java.util.Map;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.html.wrapper.ParametroListe;

/**
 * Parametri di una select.
 *
 * @author Nicola De Nisco
 */
public class SqlSelectParam
{
  private String select;
  private String where;
  private String from;
  private String orderby;
  private String having;
  private String deleteFrom;
  private SqlGroupBy groupby;
  private String strGroupby;

  public void setSelect(String select)
  {
    this.select = select;
  }

  public String getSelect()
  {
    return select;
  }

  public void setFrom(String from)
  {
    this.from = from;
  }

  public String getFrom()
  {
    return from;
  }

  public void setWhere(String where)
  {
    this.where = where;
  }

  public String getWhere()
  {
    return where;
  }

  public void setHaving(String having)
  {
    this.having = having;
  }

  public String getHaving()
  {
    return having;
  }

  public void setOrderby(String orderby)
  {
    this.orderby = orderby;
  }

  public String getOrderby()
  {
    return orderby;
  }

  public void setDeleteFrom(String deleteFrom)
  {
    this.deleteFrom = deleteFrom;
  }

  public String getDeleteFrom()
  {
    return deleteFrom;
  }

  public void setGroupby(SqlGroupBy groupby)
  {
    this.groupby = groupby;
  }

  public SqlGroupBy getGroupby()
  {
    return groupby;
  }

  public String getStrGroupby()
  {
    return strGroupby;
  }

  public void setStrGroupby(String strGroupby)
  {
    this.strGroupby = strGroupby;
  }

  public String makeGroupBySelect()
  {
    if(groupby == null && strGroupby == null)
      return null;

    String rv = "";
    if(strGroupby != null)
      rv += strGroupby;

    if(groupby != null)
      for(RigelColumnDescriptor cd : groupby.colonne)
        rv += "," + cd.getName();

    if(rv.isEmpty())
      return null;

    if(rv.startsWith(","))
      rv = rv.substring(1);

    return rv;
  }

  public String makeGroupByGroupBy()
  {
    if(groupby == null && strGroupby == null)
      return null;

    String rv = "";
    if(strGroupby != null)
      rv += strGroupby;

    if(groupby != null)
      for(RigelColumnDescriptor cd : groupby.colonne)
        if(!cd.isAggregatoSql())
          rv += "," + cd.getName();

    if(rv.isEmpty())
      return null;

    if(rv.startsWith(","))
      rv = rv.substring(1);

    return rv;
  }

  public String getOrderbyGroupby()
  {
    return (groupby == null) ? null : groupby.orderby;
  }

  public String getHavingParametri()
  {
    if(groupby == null || !groupby.filtro.haveParametri())
      return null;

    String rv = "";
    for(ParametroListe pl : groupby.filtro.getParametri())
    {
      if(pl.getValore() != null)
        rv += " AND " + pl.getCampo() + pl.getOperazione() + pl.getValoreFmt();
    }

    return rv.length() == 0 ? null : rv.substring(5);
  }

  public void cumulaHavingParametri()
  {
    String hp = getHavingParametri();
    if(hp == null)
      return;

    if(having == null)
      having = hp;
    else
      having += " AND " + hp;
  }

  public void setParametroHaving(String nomeParam, String valParam)
  {
    groupby.filtro.setParametro(nomeParam, valParam);
  }

  public boolean populateParametriHaving(Map params)
  {
    if(groupby.filtro.haveParametri())
      return groupby.filtro.populateParametri(params);
    return false;
  }

  public void clearParametriHaving()
  {
    groupby.filtro.clearParametri();
  }
}
