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
import javax.swing.table.TableModel;
import org.jdom2.Element;
import org.rigel5.RigelI18nInterface;

/**
 * Interfaccia di un generatore custom di HTML per l'edit di una colonna.
 *
 * @author Nicola De Nisco
 */
public interface CustomColumnEdit
{
  /**
   * Inizializzazione del plugin.
   * @param eleXML sezione XML quando letto da liste XML.
   * @throws Exception
   */
  public void init(Element eleXML)
     throws Exception;

  /**
   * Ritorna vero se questo plugin genera codice HTML custom per l'editing.
   * @return
   */
  public default boolean haveCustomHtml()
  {
    return true;
  }

  /**
   * Genera l'HTML necessario all'editing della colonna/riga indicata.
   * @param cd colonna di riferimento
   * @param model gestore dei dati della tabella
   * @param row riga richiesta
   * @param col colonna richiesta
   * @param cellText valore formattato del campo in questione
   * @param cellHtml html default per edit campo
   * @param nomeCampo nome del campo di default
   * @param i18n internazionalizzatore
   * @throws Exception
   * @return HTML del campo
   */
  public String getHtmlEdit(RigelColumnDescriptor cd, TableModel model,
     int row, int col, String cellText, String cellHtml, String nomeCampo, RigelI18nInterface i18n)
     throws Exception;

  /**
   * Ritorna vero se questo plugin implementa una logica speciale di parsing.
   * @return
   */
  public default boolean haveCustomParser()
  {
    return false;
  }

  /**
   * Parsing del campo.
   * @param cd colonna di riferimento
   * @param model gestore dei dati della tabella
   * @param row riga richiesta
   * @param col colonna richiesta
   * @param formattedValue valore formattato del campo in questione
   * @param nomeCampo nome del campo di default
   * @param oldValue valore precedente
   * @param params tutti i parametri della richiesta HTML
   * @param i18n internazionalizzatore per eventuali messaggi d'errore
   * @throws Exception
   * @return il valore richiesto opportunamente convertito nel tipo richiesto
   */
  public default Object parseValue(RigelColumnDescriptor cd, TableModel model,
     int row, int col, String formattedValue, String nomeCampo, String oldValue, Map params, RigelI18nInterface i18n)
     throws Exception
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Ritorna vero se questo plugin aggiunge codice HTML custom per l'editing.
   * @return
   */
  public default boolean haveAddHtml()
  {
    return false;
  }

  /**
   * Aggiunge l'HTML necessario all'editing della colonna/riga indicata.
   * La funzione può aggiungere HTML a quello già generato per default da rigel.
   * @param cd colonna di riferimento
   * @param model gestore dei dati della tabella
   * @param row riga richiesta
   * @param col colonna richiesta
   * @param formattedValue valore formattato del campo in questione
   * @param nomeCampo nome del campo di default
   * @param rigelHtml l'HTML generato per default da rigel
   * @param i18n internazionalizzatore
   * @throws Exception
   * @return HTML del campo
   */
  public default String addHtmlEdit(RigelColumnDescriptor cd, TableModel model,
     int row, int col, String formattedValue, String nomeCampo, String rigelHtml,
     RigelI18nInterface i18n)
     throws Exception
  {
    return rigelHtml;
  }
}
