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

import java.text.Format;

/**
 * Estensione format.
 * Usato principalmente per l'internazionalizzazione.
 *
 * @author Nicola De Nisco
 */
abstract public class RigelExtendedFormat extends Format
{
  /**
   * Prepara eventuali messaggi usando l'iternazionalizzatore.
   * @param i18n
   * @throws Exception
   */
  abstract public void prepareToRender(RigelI18nInterface i18n)
     throws Exception;
}
