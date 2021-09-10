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
package org.rigel2.db;

import java.sql.Timestamp;

/**
 * Eccezione sollevata quando uno stesso thread richiede più transazioni contemporneamente.
 *
 * @author Nicola De Nisco
 */
public class ConcurrentThreadTransaction extends Exception
{
  private Thread thError;

  public ConcurrentThreadTransaction(long thid, String nome, Timestamp t)
  {
    super("Thread with ID: " + thid + " NAME:" + nome + " have already an active transaction started at " + t + ".");
  }

  public ConcurrentThreadTransaction(Thread thError, Timestamp t)
  {
    this(thError.getId(), thError.getName(), t);
    this.thError = thError;
  }

  public ConcurrentThreadTransaction(String message)
  {
    super(message);
  }

  public Thread getThError()
  {
    return thError;
  }
}
