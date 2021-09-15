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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.rigel5.table.ForeignDataHolder;

/**
 * Implementazione minimale di un cache manager.
 * Mantiene i dati in memoria per un tempo prefissato
 * (per default 30 minuti), abbassando la frequenza di
 * accesso al db.
 * <code>
 *  SetupHolder.setCacheManager(new SimpleCacheManager());
 * </code>
 *
 * @author Nicola De Nisco
 */
public class SimpleCacheManager implements RigelCacheManager
{
  /**
   * Segnaposto per oggetto nella cache.
   */
  public static class CacheHit
  {
    public long tCreated;
    public Object payload;

    public CacheHit(Object payload)
    {
      this.tCreated = System.currentTimeMillis();
      this.payload = payload;
    }
  }
  /**
   * Tempo di permanenza nella cache.
   * Default 30 minuti.
   */
  protected long expireMillis = 30 * 60 * 1000L;
  /**
   * Mappa della cache.
   * I dati vengono memorizzati qui.
   */
  protected HashMap<String, CacheHit> cache = new HashMap<String, CacheHit>();

  @Override
  public List<ForeignDataHolder> getForeignDataList(String chiave)
  {
    return (List<ForeignDataHolder>) getGenericCachedData("l1:" + chiave);
  }

  @Override
  public void putForeignDataList(String chiave, List<ForeignDataHolder> ls)
  {
    putGenericCachedData("l1:" + chiave, ls);
  }

  @Override
  public List<ForeignDataHolder> getDataComboColonnaAttached(String chiave)
  {
    return (List<ForeignDataHolder>) getGenericCachedData("l2:" + chiave);
  }

  @Override
  public void putDataComboColonnaAttached(String chiave, List<ForeignDataHolder> ls)
  {
    putGenericCachedData("l2:" + chiave, ls);
  }

  @Override
  public List<ForeignDataHolder> getDataComboColonnaSelf(String chiave)
  {
    return (List<ForeignDataHolder>) getGenericCachedData("l3:" + chiave);
  }

  @Override
  public void putDataComboColonnaSelf(String chiave, List<ForeignDataHolder> ls)
  {
    putGenericCachedData("l3:" + chiave, ls);
  }

  @Override
  public Long getRecordCount(String chiave)
  {
    return (Long) getGenericCachedData("l4:" + chiave);
  }

  @Override
  public void putRecordCount(String chiave, long value)
  {
    putGenericCachedData("l4:" + chiave, value);
  }

  @Override
  public synchronized Object getGenericCachedData(String chiave)
  {
    CacheHit rv;

    if((rv = cache.get(chiave)) == null)
      return null;

    if((System.currentTimeMillis() - rv.tCreated) > expireMillis)
    {
      cache.remove(chiave);
      return null;
    }

    return rv.payload;
  }

  @Override
  public synchronized void putGenericCachedData(String chiave, Object data)
  {
    cache.put(chiave, new CacheHit(data));
  }

  @Override
  public synchronized void purgeTabella(String nomeTabella)
  {
    ArrayList<String> toRemove = new ArrayList<>();
    String upNomeTab = " FROM " + nomeTabella.toUpperCase() + " ";

    for(Map.Entry<String, CacheHit> entrySet : cache.entrySet())
    {
      String key = entrySet.getKey();
      CacheHit value = entrySet.getValue();

      if(key.contains(upNomeTab))
        toRemove.add(key);
    }

    for(String s : toRemove)
      cache.remove(s);
  }

  /**
   * Rimuove tutte le entry della cache.
   */
  public synchronized void flushCache()
  {
    cache.clear();
  }

  /**
   * Rimuove tutti i dati scaduti.
   */
  public synchronized void purge()
  {
    for(Map.Entry<String, CacheHit> entry : cache.entrySet())
    {
      String chiave = entry.getKey();
      CacheHit data = entry.getValue();

      if((System.currentTimeMillis() - data.tCreated) > expireMillis)
        cache.remove(chiave);
    }
  }

  /**
   * Legge il timeout della cache.
   * @return millisecondi di permanenza oggetti
   */
  public long getExpireMillis()
  {
    return expireMillis;
  }

  /**
   * Imposta il timeout della cache.
   * @param expireMillis tempo i permanenza in millisecondi
   */
  public void setExpireMillis(long expireMillis)
  {
    this.expireMillis = expireMillis;
  }
}
