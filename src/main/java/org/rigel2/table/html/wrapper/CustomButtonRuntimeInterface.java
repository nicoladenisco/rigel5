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
package org.rigel2.table.html.wrapper;

import org.rigel2.RigelI18nInterface;
import org.rigel2.table.RigelTableModel;
import org.rigel2.table.WrapperBase;

/**
 * Interfaccia da implementare per le classi avanzate di gestione
 * dei custom buttons.
 *
 * @author Nicola De Nisco
 */
public interface CustomButtonRuntimeInterface
{
  /**
   * Ritorna vero se il bottone è visibile per la riga corrente.
   * @return visibile
   */
  public boolean isVisible();

  /**
   * Ritorna vero se il bottone è abilitato per la riga corrente.
   * @return link sul bottone attivo
   */
  public boolean isEnabled();

  /**
   * Imposta dati per pulsante di testata.
   * Viene chiamata alla visualizzazione della lista.
   * L'oggetto può ritornare un CustomButtonInfo modificato rispetto
   * a quello orginale che verrà usato per la costruzione delle informazioni.
   * ATTENZIONE: non modificare l'oggetto info in ingresso.
   * @param info informazioni sul custom button originarie
   * @param lso wrapper con le informazioni sulla lista
   * @return le info per la costruzione del custom button
   * @throws Exception
   */
  public CustomButtonInfo setHeaderData(CustomButtonInfo info, WrapperBase lso)
     throws Exception;

  /**
   * Imposta i dati di riga.
   * Viene chiamata per ogni riga della tabella per impostare la
   * riga corrente e consentire un reperimento dei dati.
   * L'oggetto può ritornare un CustomButtonInfo modificato rispetto
   * a quello orginale che verrà usato per la costruzione delle informazioni.
   * ATTENZIONE: non modificare l'oggetto info in ingresso.
   * @param info informazioni sul custom button originarie
   * @param model table model della visualizzazione corrente
   * @param i18n interfaccia multilingua
   * @param row riga corrente
   * @param col colonna corrente
   * @throws Exception
   * @return the org.rigel2.table.html.wrapper.CustomButtonInfo
   */
  public CustomButtonInfo setRowData(CustomButtonInfo info,
     RigelTableModel model, RigelI18nInterface i18n, int row, int col)
     throws Exception;
}
