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
package org.rigel5.db;

import java.io.Closeable;
import java.sql.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlib5.lambda.ConsumerThrowException;
import org.commonlib5.lambda.FunctionTrowException;

/**
 * <p>
 * Title: Gestore connessioni SQL.</p>
 * <p>
 * Description: Produce un oggetto <b>java.sql.Connection</b> da utilizzare
 * delle classi Sql... del fremework.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class ConnectionProducer
{
  protected Boolean transactionSupported = null;
  public static final String DB_YES_TRANS = "Il database supporta le transazioni.";
  public static final String DB_NO_TRANS = "Transazioni NON supportate da questo database.";
  private static Log log = LogFactory.getLog(ConnectionProducer.class);

  public interface ConnectionHolder extends Closeable
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
  abstract public ConnectionHolder getConnectionHolder()
     throws Exception;

  public void runConnection(ConsumerThrowException<Connection> fun)
     throws Exception
  {
    try (ConnectionHolder ch = getConnectionHolder())
    {
      Connection con = ch.getConnection();
      fun.accept(con);
    }
  }

  public <T> T functionConnection(FunctionTrowException<Connection, T> fun)
     throws Exception
  {
    try (ConnectionHolder ch = getConnectionHolder())
    {
      Connection con = ch.getConnection();
      return fun.apply(con);
    }
  }

  /**
   * Verifica se il db di default supporta le transazioni.
   * Il risultato viene recuperato solo una volta e salvato in una cache.
   * @param con
   * @return
   */
  public boolean supportTransaction(Connection con)
  {
    if(transactionSupported == null)
    {
      try
      {
        transactionSupported = con.getMetaData().supportsTransactions();
        log.info(transactionSupported ? DB_YES_TRANS : DB_NO_TRANS);
      }
      catch(Exception ex)
      {
        transactionSupported = false;
        log.error("Error checking transaction support:", ex);
      }
    }

    return transactionSupported;
  }

  abstract protected boolean supportTransaction();

  /**
   * Ritorna vero se il database supporta le transazioni.
   * @return
   */
  public boolean isTransactionSupported()
  {
    if(transactionSupported == null)
      supportTransaction();

    return transactionSupported;
  }

  public void setTransactionSupported(boolean transactionSupported)
  {
    this.transactionSupported = transactionSupported;
  }
}
