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
package org.rigel5;

import java.util.List;
import org.rigel5.table.ForeignDataHolder;

/**
 * Implementazione vuota di un cache manager.
 * Non viene memorizzato alcun dato, per cui rigel ripeterà
 * gli accessi al db ogni volta che necessita.
 * Questo è il cache manager di default.
 * <code>
 *  SetupHolder.setCacheManager(new NullCacheManager());
 * </code>
 *
 * @author Nicola De Nisco
 */
public class NullCacheManager implements RigelCacheManager
{
  @Override
  public List<ForeignDataHolder> getForeignDataList(String chiave)
  {
    return null;
  }

  @Override
  public void putForeignDataList(String chiave, List<ForeignDataHolder> ls)
  {
  }

  @Override
  public List<ForeignDataHolder> getDataComboColonnaAttached(String chiave)
  {
    return null;
  }

  @Override
  public void putDataComboColonnaAttached(String chiave, List<ForeignDataHolder> ls)
  {
  }

  @Override
  public List<ForeignDataHolder> getDataComboColonnaSelf(String chiave)
  {
    return null;
  }

  @Override
  public void putDataComboColonnaSelf(String chiave, List<ForeignDataHolder> ls)
  {
  }

  @Override
  public Long getRecordCount(String chiave)
  {
    return null;
  }

  @Override
  public void putRecordCount(String chiave, long value)
  {
  }

  @Override
  public Object getGenericCachedData(String chiave)
  {
    return null;
  }

  @Override
  public void putGenericCachedData(String chiave, Object data)
  {
  }

  @Override
  public void purgeTabella(String nomeTabella)
  {
  }
}
