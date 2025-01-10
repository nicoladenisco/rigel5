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

import java.text.Format;
import java.text.ParsePosition;
import org.rigel5.table.RigelTableModel;

/**
 * Estensione format.
 * Consente di formattare campi speciali.
 *
 * @author Nicola De Nisco
 */
abstract public class RigelExtendedFormat extends Format
{
  /**
   * Prepara eventuali messaggi usando l'iternazionalizzatore.
   * @param i18n interfaccia per internazionalizzazione
   * @throws Exception
   */
  public void prepareToRender(RigelI18nInterface i18n)
     throws Exception
  {
  }

  /**
   * Notifica record da formattare.
   * Viene chiamata prima di iniziare la renderizzazione del record.
   * Il formatter può estrarre dati dal record utili
   * alla renderizzazione del campo specifico da formattare.
   * @param tm table model per estrazione record
   * @param row riga di riferimento
   * @param col colonna di riferimento
   * @throws Exception
   */
  public void prepareFormatRecord(RigelTableModel tm, int row, int col)
     throws Exception
  {
  }

  @Override
  public Object parseObject(String source, ParsePosition pos)
  {
    pos.setIndex(source.length());
    return source;
  }
}
