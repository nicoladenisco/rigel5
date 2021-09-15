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

import java.sql.Connection;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.apache.torque.om.*;
import org.jdom2.Element;
import org.rigel5.RigelI18nInterface;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.hEditTable;

/**
 * Intefaccia di definizione di un validatore di postparsing.
 * I validatori sono specificati in lista.xml per effettuare
 * delle operazioni non standard prima o dopo il parsing di un record
 * durante l'edit dello stesso.
 * Un oggetto di questo tipo viene istanziato dopo il parsing
 * del form di edit di un record, consentendo di variare i dati
 * contenuti nell'oggeto obj prima che sia salvato sul database.
 * Se il metodo validate restituisce false l'oggetto non sara'
 * salvato sul database.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public interface PostParseValidator
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
   * Validazione di un risultato letto dal form.
   *
   * @param obj peer di Torque con i dati già letti dal form
   * @param tableModel the value of tableModel
   * @param table la tabella di visualizzazione
   * @param row the value of row
   * @param session sessione HTTP
   * @param param mappa dei parametri del form
   * @param i18n internazionalizzazione stringhe
   * @param dbCon connessione al database (può essere null)
   * @param custom eventuali dati ulteriori
   * @return vero se il contenuto dell'oggetto è valido e si può proseguire con il salvataggio
   * @throws Exception
   */
  public boolean validate(Persistent obj,
     RigelTableModel tableModel, hEditTable table, int row, HttpSession session, Map param,
     RigelI18nInterface i18n, Connection dbCon, Object custom)
     throws Exception;
}
