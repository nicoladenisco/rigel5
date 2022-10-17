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
package org.rigel5.glue.validators;

import java.util.Map;
import javax.servlet.http.HttpSession;
import org.jdom2.Element;
import org.rigel5.RigelI18nInterface;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.hEditTable;

/**
 * Intefaccia di definizione di un validatore di preparsing.
 * I validatori sono specificati in lista.xml per effettuare
 * delle operazioni non standard prima o dopo il parsing di un record
 * durante l'edit dello stesso.
 * Un oggetto di questo tipo viene istanziato prima del parsing
 * del form di edit di un record, consentendo di variare i parametri
 * del form agendo sull'oggetto param passato al metodo validate.
 * Il validatore puo' arrestare il parsing e il salvataggio del record
 * tramite il metodo isStopParsing() facendogli restituire true.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public interface PreParseValidator
{
  /**
   * Inizializzazione del validator.
   * Viene chiamara alla creazione del validator; l'elemento XML
   * descrittore del validator viene passato per poter recuperare
   * eventuali parametri extra necessari a questo validator.
   * @param eleXML
   * @throws Exception
   */
  public void init(Element eleXML)
     throws Exception;

  /**
   * Pre-validazione di un risultato, prima che i dati vengano
   * copiati dal form all'oggetto specificato.Questa funzione consente di effettuare controlli sui dati
   * (ed eventualmente modificarli) prima che vengano scritti
   * nell'oggetto indicato.Tutti i parametri che verrano utilizzati
   * sono in param che può eventualmente essere modificata di
   * conseguenza.
   *
   * @param obj peer di Torque con i dati originali
   * @param tableModel the value of tableModel
   * @param table
   * @param row the value of row
   * @param session sessione HTTP
   * @param param mappa dei parametri del form
   * @param custom eventuali dati ulteriori
   * @param i18n
   * @return vero se si può proseguire, falso per abortire il salvataggio
   * @throws Exception
   */
  public boolean validate(Object obj,
     RigelTableModel tableModel, hEditTable table, int row,
     HttpSession session, Map param, RigelI18nInterface i18n, Map custom)
     throws Exception;

  /**
   * Ritorna vero se le ulteriori operazioni di parsing
   * devono essere annullate.
   * Questa funzione generalmente ritornerà sempre true,
   * ma possono crearsi situazioni in cui validate() carica
   * già i dati nell'oggetto e quindi sebbene ritorni true
   * per confermare i dati validi, non vogliamo che ulteriori
   * caricamenti vengano effettuati sull'oggetto; di conseguenza
   * questa funzione ritornerà false.
   * @return stop parsing
   */
  public boolean isStopParsing();
}
