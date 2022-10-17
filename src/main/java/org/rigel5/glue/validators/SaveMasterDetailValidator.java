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
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.jdom2.Element;
import org.rigel5.RigelI18nInterface;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.hEditTable;

/**
 * Validatore per master-detail.
 * Questo validatore viene usato per il master/detail
 * e viene invocato dopo il salvataggio del master e
 * di tutti i detail.
 * Consente di effettuare eventuali modifiche agli
 * oggetti sia master che detail dopo il salvataggio.
 *
 * @author Nicola De Nisco
 */
public interface SaveMasterDetailValidator
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
   * Valida il salvataggio.
   * @param obj oggetto master
   * @param tableModelMaster table model del master
   * @param tableMaster tabella del master
   * @param rowMaster riga di riferimento
   * @param detail lista di oggetti detail
   * @param tableModelDetail table model dei detail
   * @param tableDetail tabella del detail
   * @param session sessione HTTP
   * @param param mappa dei parametri del form
   * @param i18n
   * @param dbCon connessione al database (può essere null)
   * @param custom eventuali dati ulteriori
   * @return true indica successo
   * @throws Exception
   */
  public boolean validate(Object obj,
     RigelTableModel tableModelMaster, hEditTable tableMaster, int rowMaster,
     List detail, RigelTableModel tableModelDetail, hEditTable tableDetail,
     HttpSession session, Map param,
     RigelI18nInterface i18n, Connection dbCon, Map custom)
     throws Exception;
}
