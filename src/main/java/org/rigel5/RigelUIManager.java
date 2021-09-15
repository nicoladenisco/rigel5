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

import javax.servlet.http.HttpSession;
import org.rigel5.table.html.AbstractHtmlTablePager;
import org.rigel5.table.html.RigelHtmlPage;

/**
 * Funzioni per la formattazione da implementare ad hoc
 * nelle applicazioni che utilizzano Rigel.
 * Esiste una implementazione di default di questa interfaccia
 * DefaultUIManager ma probabilmente altre applicazioni
 * vorranno utilizzare un proprio look and feel per la
 * formattazione finale dei contenuti.
 * Una istanza di classe che implementi questa interfaccia
 * va registrata nel SetupHolder con il metodo setUiManager().
 *
 * @author Nicola De Nisco
 */
public interface RigelUIManager
{
  /**
   * Formatta lista in caso di visualizzazione semplice.
   * Utilizzata quando non esiste campi per la ricerca semplice.
   * @param filtro tipo di visualizzazione
   * @param page the value of page
   * @throws Exception
   * @return the java.lang.String
   */
  public String formatHtmlLista(int filtro, RigelHtmlPage page)
     throws Exception;

  /**
   * Formatta form in caso di visualizzazione semplice.
   * @param page the value of page
   * @throws Exception
   * @return the java.lang.String
   */
  public String formatHtmlForm(RigelHtmlPage page)
     throws Exception;

  /**
   * Formatta lista quando è attiva la ricerca semplice.
   * Oltre al contenuto della tabella e la barra di navigazione
   * viene prodotto anche il form per la ricerca semplice.
   * @param filtro
   * @param page the value of page
   * @throws Exception
   * @return the java.lang.String
   */
  public String formatSimpleSearch(int filtro, RigelHtmlPage page)
     throws Exception;

  /**
   * Formatta lista adattando ad un palmare.
   * Come per formatSimpleSearch ma dovrebbe produrre una versione
   * più ridotta per adattarla alla visualizzazione su un palmare.
   * @param filtro
   * @param page the value of page
   * @throws Exception
   * @return the java.lang.String
   */
  public String formatSimpleSearchPalmare(int filtro, RigelHtmlPage page)
     throws Exception;

  /**
   * Ritorna l'HTML della barra inferiore di navigazione.
   * Questa funzione deve costruire la barra inferiore di navigazione
   * da accoppiare alla tabella visualizzata. I parametri sono sufficienti
   * per costruire questa barra che consenta di scorrere le pagine.
   * @param pagCurr numero di pagina corrente
   * @param numPagine numero totale delle pagine in base al filtro impostato
   * @param limit numero di record visualizzati in una pagina
   * @param tp il pager che sta chiedendo la paginazione
   * @param sessione the value of sessione
   * @param page the value of page
   * @throws Exception
   */
  public void addHtmlNavRecord(int pagCurr, int numPagine, int limit, AbstractHtmlTablePager tp, HttpSession sessione, RigelHtmlPage page)
     throws Exception;
}
