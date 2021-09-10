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
package org.rigel2;

import org.rigel2.table.RigelColumnDescriptor;
import org.rigel2.table.RigelTableModel;
import org.rigel2.table.html.hTable;

/**
 * Implementazione di default di RigelI18nInterface.
 * Tutti i messaggi sono passanti, ovvero nessuna modifica di locale viene introdotta.
 *
 * @author Nicola De Nisco
 */
public class DefaultRigelI18nImplementation implements RigelI18nInterface
{
  //
  // caption bottoni ricerca semplice
  private String captionButtonCerca = "Cerca";
  private String captionButtonPulisci = "Pulisci";

  @Override
  public String localizeTableCaption(hTable table,
     RigelTableModel model, RigelColumnDescriptor column, int numCol, String caption)
  {
    return caption;
  }

  @Override
  public String getCaptionButtonCerca()
  {
    return captionButtonCerca;
  }

  public void setCaptionButtonCerca(String captionButtonCerca)
  {
    this.captionButtonCerca = captionButtonCerca;
  }

  @Override
  public String getCaptionButtonPulisci()
  {
    return captionButtonPulisci;
  }

  public void setCaptionButtonPulisci(String captionButtonPulisci)
  {
    this.captionButtonPulisci = captionButtonPulisci;
  }

  @Override
  public String resolveGenericMessage(String defaultMessage)
  {
    return defaultMessage;
  }

  @Override
  public String msg(String defaultMessage)
  {
    return defaultMessage;
  }

  @Override
  public String msg(String defaultMessage, Object... args)
  {
    return String.format(defaultMessage, args);
  }
}
