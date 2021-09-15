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
package org.rigel5.db.sql;

import java.util.*;
import org.apache.torque.criteria.SqlEnum;
import org.rigel5.table.RigelColumnDescriptor;

/**
 * Dati filtraggio SQL.
 * Memorizza l'impostazione di un filtro
 * per l'uso di SqlAbstractTableModel. Viene creato da SqlBuilderRicercaGenerica
 * sulla base della selezione dell'utente.
 * @author Nicola De Nisco
 * @version 1.0
 */
public class FiltroData
{
  public static class updateInfo
  {
    public int type;
    public String nomecampo;
    public Object val;
  }

  public Vector vUpdate = new Vector(10, 10);

  public void addUpdate(int type, String nomecampo, Object val)
  {
    updateInfo ui = new updateInfo();
    ui.type = type;
    ui.nomecampo = nomecampo;
    ui.val = val;
    vUpdate.add(ui);
  }

  public void addUpdate(RigelColumnDescriptor cd, Object val)
  {
    addUpdate(cd.getDataType(), cd.getName(), val);
  }

  public void addInsert(int type, String nomecampo, Object val)
  {
    addUpdate(type, nomecampo, val);
  }

  public void addInsert(RigelColumnDescriptor cd, Object val)
  {
    addUpdate(cd, val);
  }

  public Vector vSelect = new Vector(10, 10);

  public void addSelect(String nomecampo)
  {
    vSelect.add(nomecampo);
  }

  public void addSelect(RigelColumnDescriptor cd)
  {
    vSelect.add(cd.getName());
  }

  public static class whereInfo
  {
    public int type;
    public String nomecampo;
    public SqlEnum criteria;
    public Object val;
  }

  public Vector vWhere = new Vector(10, 10);

  public void addWhere(int type, String nomecampo, SqlEnum criteria, Object val)
  {
    whereInfo wi = new whereInfo();
    wi.type = type;
    wi.nomecampo = nomecampo;
    wi.criteria = criteria;
    wi.val = val;
    vWhere.add(wi);
  }

  public void addWhere(RigelColumnDescriptor cd, SqlEnum criteria, Object val)
  {
    addWhere(cd.getDataType(), cd.getName(), criteria, val);
  }

  public static class betweenInfo
  {
    public int type;
    public String nomecampo;
    public Object val1;
    public Object val2;
  }

  public Vector vBetween = new Vector(10, 10);

  public void addBetween(int type, String nomecampo, Object val1, Object val2)
  {
    betweenInfo bi = new betweenInfo();
    bi.type = type;
    bi.nomecampo = nomecampo;
    bi.val1 = val1;
    bi.val2 = val2;
    vBetween.add(bi);
  }

  public void addBetween(RigelColumnDescriptor cd, Object val1, Object val2)
  {
    addBetween(cd.getDataType(), cd.getName(), val1, val2);
  }

  public static class orderbyInfo
  {
    public String nomecampo;
    public String dir;
  }

  public Vector vOrderby = new Vector(10, 10);

  public void addOrderby(String nomecampo, String dir)
  {
    orderbyInfo oi = new orderbyInfo();
    oi.nomecampo = nomecampo;
    oi.dir = dir;
    vOrderby.add(oi);
  }

  public void addOrderby(RigelColumnDescriptor cd, String dir)
  {
    addOrderby(cd.getName(), dir);
  }

  public boolean haveWhere()
  {
    return !vWhere.isEmpty() || !vBetween.isEmpty();
  }

  public boolean haveOrderby()
  {
    return !vOrderby.isEmpty();
  }
}
