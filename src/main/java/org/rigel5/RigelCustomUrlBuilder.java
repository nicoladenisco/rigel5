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

import java.util.Map;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.wrapper.CustomButtonInfo;

/**
 * Definizione di un generatore di url da implementare
 * in progetti ospiti.
 * In vari punti di rigel occorre generare delle url
 * di link. Una classe che implementi questa interfaccia
 * occorre per collegare all'applicazione ospite la generazione
 * di queste URL. L'implemetazione va registrata nel
 * SetupHolder con la funzione setUrlBuilder().
 *
 * @author Nicola De Nisco
 */
public interface RigelCustomUrlBuilder
{
  /**
   * Ritorna la url da utilizzare per alla pressione dell'icona di
   * ricerca nella funzione foreing edit.
   * Generalmente fa aprire una popup con all'interno la maschera
   * per selezionare il valore del campo.
   * @param popup vero se la maschera e' in popup
   * @param cd
   * @param fldName
   * @return
   * @throws Exception
   */
  public String buildUrlForeginList(boolean popup,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, int col)
     throws Exception;

  /**
   * Ritorna la url da utilizzare per alla pressione dell'icona di
   * visualizzazione nella funzione foreing edit.
   * Generalmente fa aprire una popup con all'interno la maschera
   * per visualizzare il valore del campo.
   * @param popup vero se la maschera e' in popup
   * @param cd
   * @param fldName
   * @return
   * @throws Exception
   */
  public String buildUrlForeginForm(boolean popup,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, int col)
     throws Exception;

  /**
   * Ritorna la url da utilizzare per la funzione di selezione del record.
   * Generalmente questa è presente in una maschera popup che
   * verrà chiusa e il valore selezionato sarà passato al chimante.
   * @param popup vero se la maschera e' in popup
   * @param inputUrl parametri già pronti per l'editing
   * @param tableModel gestore della tabella
   * @param cd colonna corrente
   * @param fldName nome del campo
   * @param row riga di riferimento
   * @param jlc eventuale parametro di ritorno
   * @return la uri corretta (normalmente la chiamata ad una funzione javascript)
   * @throws Exception
   */
  public String buildUrlSelezionaRecord(boolean popup, String inputUrl,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, Map<String, String> extraParams)
     throws Exception;

  /**
   * Ritorna la url da utilizzare per la funzione di modifica del record.
   * Generalmente fa aprire una popup con all'interno la maschera
   * per modificare il record corrente.
   * @param popup vero se la maschera e' in popup
   * @param inputUrl parametri già pronti per l'editing
   * @param tableModel gestore della tabella
   * @param cd colonna corrente
   * @param fldName nome del campo
   * @param row riga di riferimento
   * @param jlc eventuale parametro di ritorno
   * @return la uri corretta (normalmente la chiamata ad una funzione javascript)
   * @throws Exception
   */
  public String buildUrlEditRecord(boolean popup, String inputUrl,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, Map<String, String> extraParams)
     throws Exception;

  /**
   * Ritorna la url da utilizzare per la funzione nuovo record.
   * Generalmente fa aprire una popup con all'interno la maschera
   * per modificare il record corrente.
   * @param popup vero se la maschera e' in popup
   * @param inputUrl parametri già pronti per l'editing
   * @param tableModel gestore della tabella
   * @param cd colonna corrente
   * @param fldName nome del campo
   * @param jlc eventuale parametro di ritorno
   * @return la uri corretta (normalmente la chiamata ad una funzione javascript)
   * @throws Exception
   */
  public String buildUrlNewRecord(boolean popup, String inputUrl,
     RigelTableModel tableModel, Map<String, String> extraParams)
     throws Exception;

  /**
   * Ritorna la url da utilizzare per la funzione di line link
   * quando questa è associata alla selezione del record.
   * Generalmente questa è presente in una maschera popup che
   * verrà chiusa e il valore selezionato sarà passato al chimante.
   * @param popup vero se la maschera e' in popup
   * @param inputUrl parametri già pronti per l'editing
   * @param tableModel gestore della tabella
   * @param cd colonna corrente
   * @param fldName nome del campo
   * @param row riga di riferimento
   * @param jlc eventuale parametro di ritorno
   * @return la uri corretta (normalmente la chiamata ad una funzione javascript)
   * @throws Exception
   */
  public String buildUrlLineSelezione(boolean popup, String inputUrl,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, Map<String, String> extraParams)
     throws Exception;

  /**
   * Ritorna la url da utilizzare per la funzione di line link
   * quando questa è associata alla modifica del record.
   * Generalmente fa aprire una popup con all'interno la maschera
   * per modificare il record corrente. Nella maggior parte dei
   * casi ritorna la stessa url di buildUrlEditRecord.
   * @param popup vero se la maschera e' in popup
   * @param inputUrl parametri già pronti per l'editing
   * @param tableModel gestore della tabella
   * @param cd colonna corrente
   * @param fldName nome del campo
   * @param row riga di riferimento
   * @param jlc eventuale parametro di ritorno
   * @return la uri corretta (normalmente la chiamata ad una funzione javascript)
   * @throws Exception
   */
  public String buildUrlLineEdit(boolean popup, String inputUrl,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, Map<String, String> extraParams)
     throws Exception;

  /**
   * Ritorna la url da utilizzare per la cancellazione del record.
   * Generalmente richiede conferma all'utente e avvia la procedura di cancellazione.
   * @param popup vero se la maschera e' in popup
   * @param inputUrl parametri già pronti per l'editing
   * @param tableModel gestore della tabella
   * @param cd colonna corrente
   * @param fldName nome del campo
   * @param row riga di riferimento
   * @param jlc eventuale parametro di ritorno
   * @return la uri corretta (normalmente la chiamata ad una funzione javascript)
   * @throws Exception
   */
  public String buildUrlCancellaRecord(boolean popup, String inputUrl,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, Map<String, String> extraParams)
     throws Exception;

  /**
   * Ritorna la url a cui punta il bottone di testata.
   * Questa funzione viene usate nelle liste, dove non c'è un
   * record corrente.
   * @param popup vero se la maschera e' in popup
   * @param tableModel tableModel con i dati di tabella
   * @param cb descrittore bottone di testata
   * @return url di chiamata del bottone
   * @throws Exception
   */
  public String buildUrlHeaderButton(boolean popup, RigelTableModel tableModel, CustomButtonInfo cb)
     throws Exception;

  /**
   * Ritorna la url a cui punta il bottone di testata.
   * Questa funzione viene usate nei forms dove c'è un concetto di record corrente.
   * @param popup vero se la maschera e' in popup
   * @param tableModel tableModel con i dati di tabella
   * @param row numero di riga in costruzione
   * @param cb descrittore bottone di testata
   * @return url di chiamata del bottone
   * @throws Exception
   */
  public String buildUrlHeaderButton(boolean popup, RigelTableModel tableModel, int row, CustomButtonInfo cb)
     throws Exception;

  /**
   * Ritorna la url a cui punta il custom button.
   * @param tableModel tableModel con i dati di tabella
   * @param popup vero se la maschera e' in popup
   * @param cd descrittore della colonna ospite
   * @param fldName nome del campo
   * @param row numero di riga in costruzione
   * @param cb descrittore custom button
   * @return url di chiamata del custom button
   * @throws Exception
   */
  public String buildUrlCustomButton(boolean popup,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, CustomButtonInfo cb)
     throws Exception;

  /**
   * Ritorna la url dell'icona associata ad un custom button.
   * @param popup vero se la maschera e' in popup
   * @param cd
   * @param fldName
   * @param row
   * @return
   * @throws Exception
   */
  public String buildImageCustomButton(boolean popup,
     RigelTableModel tableModel, RigelColumnDescriptor cd,
     String fldName, int row, CustomButtonInfo cb)
     throws Exception;

  /**
   * Funzione generica che rende una url assoluta all'interno dell'applicazione.
   * @param popup vero se la maschera e' in popup
   * @param url
   * @return
   * @throws Exception
   */
  public String makeUrlAbsolute(boolean popup, String url)
     throws Exception;

}
