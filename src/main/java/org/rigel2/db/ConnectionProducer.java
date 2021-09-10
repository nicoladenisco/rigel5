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
import org.commonlib.lambda.ConsumerThrowException;
import org.commonlib.lambda.FunctionTrowException;

/**
 * <p>
 * Title: Gestore connessioni SQL.</p>
 * <p>
 * Description: Produce un oggetto <b>java.sql.Connection</b> da utilizzare
 * delle classi Sql... del fremework.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public interface ConnectionProducer
{
  public interface ConnectionHolder
  {
    /**
     * Ritorna la connessione al database di default.
     * @return l'oggetto connessione
     * @throws Exception
     */
    public Connection getConnection()
       throws Exception;

    /**
     * Rilascia una connessione ottenuta con getConnection()
     * @throws Exception
     */
    public void releaseConnection()
       throws Exception;
  }

  /**
   * Ritorna la connessione al database di default.
   * @return l'oggetto connessione
   * @throws Exception
   */
  public ConnectionHolder getConnectionHolder()
     throws Exception;

  /**
   * Ritorna vero se il database supporta le transazioni.
   * @return
   */
  public boolean isTransactionSupported();

  public default void runConnection(ConsumerThrowException<Connection> fun)
     throws Exception
  {
    ConnectionHolder ch = getConnectionHolder();

    try
    {
      Connection con = ch.getConnection();
      fun.accept(con);
    }
    finally
    {
      ch.releaseConnection();
    }
  }

  public default <T> T functionConnection(FunctionTrowException<Connection, T> fun)
     throws Exception
  {
    ConnectionHolder ch = getConnectionHolder();

    try
    {
      Connection con = ch.getConnection();
      return fun.apply(con);
    }
    finally
    {
      ch.releaseConnection();
    }
  }
}
