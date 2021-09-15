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
import org.apache.torque.om.Persistent;
import org.jdom2.Element;
import org.rigel5.RigelI18nInterface;
import org.rigel5.table.RigelTableModel;
import org.rigel5.table.html.hEditTable;

/**
 * Action da eseguire dopo la save di un form
 * Questa action viene eseguita dopo il salvataggio.
 * Consente di effettuare eventuali modifiche agli
 * oggetti sia master che detail dopo il salvataggio.
 *
 * @author Nicola De Nisco
 */
public interface PostSaveAction
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
   * @param tableModel table model
   * @param table tabella
   * @param row riga di riferimento
   * @param session sessione HTTP
   * @param param mappa dei parametri del form
   * @param i18n
   * @param custom eventuali dati ulteriori
   * @throws Exception
   * @return the boolean
   */
  public boolean action(Persistent obj,
     RigelTableModel tableModel, hEditTable table, int row, HttpSession session,
     Map param, RigelI18nInterface i18n, Object custom)
     throws Exception;
}
