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

import java.sql.Connection;

/**
 * Esecutore di operazioni su db con supporto di transazioni.
 * Vedi implementazione in SqlTransactAgent e PeerTransactAgent.
 * @author Nicola De Nisco
 * @version 1.0
 */
public interface TransactAgent
{
  /**
   * Ritorna vero se l'implementazione supporta le transazioni.
   * @return vero per transazione supportata dal db
   */
  public boolean isTransactionSupported();

  /**
   * Funzione da ridefinire in classi derivate:
   * eseguire le operazioni richieste; se viene
   * sollevata una eccezione viene effettuata una rollback
   * e quindi risollevata l'eccezione.
   * Se ritorna true viene effettuato un commit
   * @param dbCon connessione al db sotto transazione
   * @param transactionSupported vero se il db supporta le transazioni
   * @return true per commit false per rollback
   * @throws Exception
   */
  public abstract boolean run(Connection dbCon, boolean transactionSupported)
     throws Exception;

  /**
   * Esegue operazione in transazione.
   * @throws Exception
   */
  public void runNow()
     throws Exception;
}
