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
package org.rigel5.db.torque;

import java.io.IOException;
import java.sql.Connection;
import java.util.Hashtable;
import org.apache.commons.logging.*;
import org.apache.torque.*;
import org.rigel5.db.ConnectionProducer;

/**
 * <p>
 * Title: Gestore connessioni SQL per applicazioni Turbine.</p>
 * <p>
 * Description: Produce un oggetto <b>java.sql.Connection</b> da utilizzare
 * delle classi Sql... del fremework.</p>
 * <p>
 * Questa implementazione e' specializzata per lavorare
 * all'interno di una applicazione basata su Turbine.</p>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class TurbineConnectionProducer extends ConnectionProducer
{
  /** Logging */
  private static final Log log = LogFactory.getLog(TurbineConnectionProducer.class);
  private static final Object semaforo = new Object();
  private static final Hashtable htThread = new Hashtable();

  @Override
  public ConnectionHolder getConnectionHolder()
     throws Exception
  {
    return new TurbineConnectionHolder();
  }

  public class TurbineConnectionHolder implements ConnectionHolder
  {
    private Connection con = null;

    @Override
    public Connection getConnection()
       throws Exception
    {
      if(con == null)
      {
        con = Torque.getConnection();
      }

      return con;
    }

    @Override
    public void releaseConnection()
       throws Exception
    {
      if(con != null)
      {
        Torque.closeConnection(con);
        con = null;
      }
    }

    /**
     * chiamata dal garbage collection: chiude la connessione se aperta
     * @throws Throwable
     */
    @Override
    public void finalize()
       throws Throwable
    {
      super.finalize();
      try
      {
        releaseConnection();
      }
      catch(Exception ex)
      {
        log.error("Chiudendo la connessione:", ex);
      }
    }

    @Override
    public void close()
       throws IOException
    {
      try
      {
        releaseConnection();
      }
      catch(Exception ex)
      {
        throw new IOException(ex);
      }
    }
  }

  /**
   * Ritorna la connessione al database di default.
   * @return l'oggetto connessione
   * @throws Exception
   */
  public Connection getConnection()
     throws Exception
  {
    synchronized(semaforo)
    {
      String thName = Thread.currentThread().getName();
      Connection dbCon = (Connection) htThread.get(thName);
      if(dbCon != null)
        return dbCon;

      dbCon = Torque.getConnection();
      supportTransaction(dbCon);
      htThread.put(thName, dbCon);

      log.debug("Allocata nuova connessione." + dbCon + "(" + htThread.size() + ")");
      return dbCon;
    }
  }

  /**
   * Rilascia una connessione ottenuta con getConnection()
   * @param con connessione da rilasciare
   * @throws Exception
   */
  public void releaseConnection(Connection con)
     throws Exception
  {
    synchronized(semaforo)
    {
      String thName = Thread.currentThread().getName();
      Connection dbCon = (Connection) htThread.get(thName);
      if(dbCon == null)
        throw new Exception("Il thread " + thName + " ha gia' rilasciato la connessione.");

      if(con != dbCon)
        throw new Exception("Errore nel rilascio connessione: non e' la stessa");

      Torque.closeConnection(dbCon);
      htThread.remove(thName);

      log.debug("Rilasciata connessione." + dbCon + " " + con);
    }
  }

  /**
   * Verifica se il db di default supporta le transazioni.
   * Il risultato viene recuperato solo una volta e salvato in una
   * cache.
   * Viene testata la connessione di default.
   * @return
   */
  @Override
  protected boolean supportTransaction()
  {
    if(transactionSupported == null)
    {
      Connection dbCon = null;
      try
      {
        dbCon = Torque.getConnection();
        transactionSupported = supportTransaction(dbCon);
      }
      catch(Exception ex)
      {
        log.error("", ex);
        transactionSupported = false;
      }

      if(dbCon != null)
        Torque.closeConnection(dbCon);
    }

    return transactionSupported;
  }
}
