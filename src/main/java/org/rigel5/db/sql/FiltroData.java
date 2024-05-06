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
import org.commonlib5.utils.StringOper;
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

  public final ArrayList<updateInfo> vUpdate = new ArrayList<>();

  public FiltroData addUpdate(String nomecampo, Object val)
  {
    return addUpdate(0, nomecampo, val);
  }

  public FiltroData addUpdate(int type, String nomecampo, Object val)
  {
    updateInfo ui = new updateInfo();
    ui.type = type;
    ui.nomecampo = nomecampo;
    ui.val = val;
    vUpdate.add(ui);
    return this;
  }

  public FiltroData addUpdate(RigelColumnDescriptor cd, Object val)
  {
    return addUpdate(cd.getDataType(), cd.getName(), val);
  }

  public FiltroData addInsert(String nomecampo, Object val)
  {
    return addInsert(0, nomecampo, val);
  }

  public FiltroData addInsert(int type, String nomecampo, Object val)
  {
    return addUpdate(type, nomecampo, val);
  }

  public FiltroData addInsert(RigelColumnDescriptor cd, Object val)
  {
    return addUpdate(cd, val);
  }

  public final ArrayList<String> vSelect = new ArrayList<>();

  public FiltroData addSelect(String nomecampo)
  {
    vSelect.add(nomecampo);
    return this;
  }

  public FiltroData addSelect(RigelColumnDescriptor cd)
  {
    vSelect.add(cd.getName());
    return this;
  }

  public static class whereInfo
  {
    public int type;
    public String nomecampo;
    public SqlEnum criteria;
    public Object val;
  }

  public final ArrayList<whereInfo> vWhere = new ArrayList<>();

  public FiltroData addWhere(String nomecampo, SqlEnum criteria)
  {
    if(criteria.equals(SqlEnum.ISNULL) || criteria.equals(SqlEnum.ISNOTNULL))
      return addWhere(nomecampo, criteria, "");

    throw new RuntimeException("Valore non valido per criteria: deve essere ISNULL o ISNOTNULL");
  }

  public FiltroData addWhere(String nomecampo, SqlEnum criteria, Object val)
  {
    return addWhere(0, nomecampo, criteria, val);
  }

  public FiltroData addWhere(int type, String nomecampo, SqlEnum criteria, Object val)
  {
    if(val == null)
      throw new RuntimeException("Where component can not be empty.");

    whereInfo wi = new whereInfo();
    wi.type = type;
    wi.nomecampo = nomecampo;
    wi.criteria = criteria;
    wi.val = val;
    vWhere.add(wi);
    return this;
  }

  public FiltroData addWhere(RigelColumnDescriptor cd, SqlEnum criteria, Object val)
  {
    if(val == null)
      throw new RuntimeException("Where component can not be empty.");

    if(val instanceof String)
    {
      String sVal = StringOper.okStrNull(val);
      if(sVal == null)
        throw new RuntimeException("Where component can not be empty.");

      val = cd.parseValue(sVal);
    }

    return addWhere(cd.getDataType(), cd.getName(), criteria, val);
  }

  public static class betweenInfo
  {
    public int type;
    public String nomecampo;
    public Object val1;
    public Object val2;
  }

  public final ArrayList<betweenInfo> vBetween = new ArrayList<>();

  public FiltroData addBetween(String nomecampo, Object val1, Object val2)
  {
    return addBetween(0, nomecampo, val1, val2);
  }

  public FiltroData addBetween(int type, String nomecampo, Object val1, Object val2)
  {
    betweenInfo bi = new betweenInfo();
    bi.type = type;
    bi.nomecampo = nomecampo;
    bi.val1 = val1;
    bi.val2 = val2;
    vBetween.add(bi);
    return this;
  }

  public FiltroData addBetween(RigelColumnDescriptor cd, Object val1, Object val2)
  {
    return addBetween(cd.getDataType(), cd.getName(), val1, val2);
  }

  public static class orderbyInfo
  {
    public String nomecampo;
    public String dir;
  }

  public final ArrayList<orderbyInfo> vOrderby = new ArrayList<>();

  public FiltroData addOrderby(String nomecampo)
  {
    return addOrderby(nomecampo, null);
  }

  public FiltroData addOrderby(String nomecampo, String dir)
  {
    orderbyInfo oi = new orderbyInfo();
    oi.nomecampo = nomecampo;
    oi.dir = dir;
    vOrderby.add(oi);
    return this;
  }

  public FiltroData addOrderby(RigelColumnDescriptor cd, String dir)
  {
    return addOrderby(cd.getName(), dir);
  }

  public boolean haveWhere()
  {
    return !vWhere.isEmpty() || !vBetween.isEmpty() || !vFreeWhere.isEmpty();
  }

  public boolean haveOrderby()
  {
    return !vOrderby.isEmpty();
  }

  public boolean haveUpdate()
  {
    return !vUpdate.isEmpty();
  }

  public boolean haveSelect()
  {
    return !vSelect.isEmpty();
  }

  public boolean isEmpty()
  {
    return !(haveSelect() || haveUpdate() || haveWhere() || haveOrderby());
  }

  public final ArrayList<String> vFreeWhere = new ArrayList<>();

  public FiltroData addFreeWhere(String wherePart)
  {
    vFreeWhere.add(wherePart);
    return this;
  }
}
