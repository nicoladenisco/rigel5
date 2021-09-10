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
package org.rigel2;

import java.util.List;
import org.rigel2.table.ForeignDataHolder;

/**
 * Cache dei valori utilizzati da Rigel.
 * Rigel utilizza questa cache per memorizzare valori ripetitivi,
 * al fine di non affaticare inutilmente il database.
 * L'implementazione più semplice è una cache vuota (NullCacheManager).
 * Una istanza di questa classe viene inserita nel SeupHolder.
 *
 * @author Nicola De Nisco
 */
public interface RigelCacheManager
{
  /**
   * Ritorna i dati foreign memorizzati.
   * @param chiave chiave di ricerca
   * @return il dato passato da putForeignDataList o null se non presente
   */
  public List<ForeignDataHolder> getForeignDataList(String chiave);

  /**
   * Inserisce dati foreign nella cache.
   * @param chiave chiave di ricerca
   * @param ls il dato da memorizzare
   */
  public void putForeignDataList(String chiave, List<ForeignDataHolder> ls);

  /**
   * Ritorna i dati foreign memorizzati.
   * @param chiave chiave di ricerca
   * @return il dato passato da putDataComboColonnaAttached o null se non presente
   */
  public List<ForeignDataHolder> getDataComboColonnaAttached(String chiave);

  /**
   * Inserisce dati foreign nella cache.
   * @param chiave chiave di ricerca
   * @param ls il dato da memorizzare
   */
  public void putDataComboColonnaAttached(String chiave, List<ForeignDataHolder> ls);

  /**
   * Ritorna i dati foreign memorizzati.
   * @param chiave chiave di ricerca
   * @return il dato passato da putDataComboColonnaSelf o null se non presente
   */
  public List<ForeignDataHolder> getDataComboColonnaSelf(String chiave);

  /**
   * Inserisce dati foreign nella cache.
   * @param chiave chiave di ricerca
   * @param ls il dato da memorizzare
   */
  public void putDataComboColonnaSelf(String chiave, List<ForeignDataHolder> ls);

  /**
   * Ritorna un conteggio record salvato in cache.
   * @param chiave
   * @return
   */
  public Long getRecordCount(String chiave);

  /**
   * Salva un conteggio record in cache.
   * @param chiave
   * @param value
   */
  public void putRecordCount(String chiave, long value);

  /**
   * Recupero generico di dati dalla cache.
   * @param chiave chiave di ricerca
   * @return il dato passato da putGenericCacheData o null se non presente
   */
  public Object getGenericCachedData(String chiave);

  /**
   * Inserimento generico di dati nella cache.
   * @param chiave chiave di ricerca
   * @param data dati da memorizzare
   */
  public void putGenericCachedData(String chiave, Object data);

  /**
   * Rimuove dalla cache i dati che referenziano la tabella indicata.
   * @param nomeTabella nome della tabella aggiornata
   */
  public void purgeTabella(String nomeTabella);
}
