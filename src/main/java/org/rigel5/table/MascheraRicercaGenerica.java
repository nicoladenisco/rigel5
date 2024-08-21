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
package org.rigel5.table;

import java.util.Map;
import org.rigel5.RigelI18nInterface;
import org.rigel5.table.html.RigelHtmlPage;

/**
 * Una interfaccia per le funzioni generiche di ricerca.
 *
 * @author Nicola De Nisco
 */
public interface MascheraRicercaGenerica
{
  /**
   * Inizializza con i dati utili.
   * @param brg
   * @param rtm
   * @param i18n
   * @throws Exception
   */
  public void init(BuilderRicercaGenerica brg, RigelTableModel rtm, RigelI18nInterface i18n)
     throws Exception;

  /**
   * Costruisce l'oggetto filtro.
   * Viene genericamente chiamata alla POST
   * del form con i parametri di ricerca per
   * costruire appunto il filtro opportuno in
   * base ai patametri impostati.
   * @param params
   * @return
   * @throws Exception
   */
  public Object buildCriteriaSafe(Map params)
     throws Exception;

  /**
   * Ritorna l'HTML completo della maschera per l'impostazione
   * dei parametri di filtro e di ordinamento.
   * @param nomeForm the value of nomeForm
   * @param page the value of page
   * @throws Exception
   */
  public void buildHtmlRicerca(String nomeForm, RigelHtmlPage page)
     throws Exception;

  /**
   * Ritorna l'HTML completo della maschera di ricerca semplice
   * ovvero dei campi principali di ricerca che appaiono in testa
   * alla lista.
   * @param nomeForm the value of nomeForm
   * @param sizeFld dimensioni dei campi per l'input
   * @param haveFilter vero se il filtro è attivo
   * @param page the value of page
   * @throws Exception
   */
  public void buildHtmlRicercaSemplice(String nomeForm, int sizeFld, boolean haveFilter, RigelHtmlPage page)
     throws Exception;
}
