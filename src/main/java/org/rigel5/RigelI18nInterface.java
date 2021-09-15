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

import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.hTable;

/**
 * Interfaccia di un gestore messaggi di internazionalizzazione.
 * La caption delle tabelle, messaggi generati da rigel e in genere
 * stringhe destianate alla visualizzazione dell'utente, vengono
 * sottoposti a questa interfaccia per una eventuale traduzione
 * in una stringa localizzata.
 *
 * @author Nicola De Nisco
 */
public interface RigelI18nInterface
{
  /**
   * Localizza nome campo di tabella.
   * Restituisce il nome del campo tabella nella locale corrente.
   * @param table tabella di riferimento
   * @param model modello dati di riferimento
   * @param column colonna di riferimento
   * @param numCol numero di colonna
   * @param caption la stringa che verrà usata come default
   * @return la stringa localizzata
   */
  public String localizeTableCaption(hTable table,
     RigelTableModel model, RigelColumnDescriptor column, int numCol, String caption);

  /**
   * Ritorna la caption localizzata per il pulsante 'Cerca' nella ricerca semplice.
   * @return caption pulsante
   */
  public String getCaptionButtonCerca();

  /**
   * Ritorna la caption localizzata per il pulsante 'Pulisci' nella ricerca semplice.
   * @return caption pulsante
   */
  public String getCaptionButtonPulisci();

  /**
   * Ritorna messaggio localizzato.
   * Usa il messaggio origine come chiave
   * per cercare il messaggio nella traduzione attiva.
   * @param defaultMessage messaggio chiave
   * @return corrispondente localizzato o il messaggio chiave se non trovato
   */
  public String resolveGenericMessage(String defaultMessage);

  /**
   * Ritorna messaggio localizzato.
   * Usa il messaggio origine come chiave
   * per cercare il messaggio nella traduzione attiva.
   * @param defaultMessage messaggio chiave
   * @return corrispondente localizzato o il messaggio chiave se non trovato
   */
  public String msg(String defaultMessage);

  /**
   * Ritorna messaggio localizzato.
   * Usa il messaggio origine come chiave
   * per cercare il messaggio nella traduzione attiva.
   * @param defaultMessage messaggio chiave
   * @param args argomenti di formattazione
   * @return corrispondente localizzato o il messaggio chiave se non trovato
   */
  public String msg(String defaultMessage, Object... args);
}
