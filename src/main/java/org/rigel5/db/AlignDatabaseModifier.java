/*
 * Copyright (C) 2025 Nicola De Nisco
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
package org.rigel5.db;

/**
 * Adapter per eseguire modifiche al db da codice.
 * La macro ... di AbstractAlignDatabase richiede come parametro
 * il nome completo di una classe che implementa questa interfaccia.
 * Consente di inserire nel flusso di aggiornamento db (updpilot.xml)
 * delle operazioni eseguite da codice.
 *
 * @author Nicola De Nisco
 */
public interface AlignDatabaseModifier
{
  public void apply(AbstractAlignDatabase parent, String[] params)
     throws Exception;
}
