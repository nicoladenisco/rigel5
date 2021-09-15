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
public class TurbineConnectionProducer implements ConnectionProducer
{
  private boolean transactionSupported = false, transactionChecked = false;
  public static final String DB_YES_TRANS = "Il database supporta le transazioni.";
  public static final String DB_NO_TRANS = "Transazioni NON supportate da questo database.";
  /** Logging */
  private static Log log = LogFactory.getLog(TurbineConnectionProducer.class);
  private static final Object semaforo = new Object();
  private static Hashtable htThread = new Hashtable();

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
   * Il risultato viene recuperato solo una volta e salvato in una cache.
   * @param con connessione da testare
   * @throws Exception
   */
  public boolean supportTransaction(Connection dbCon)
  {
    if(!transactionChecked)
    {
      try
      {
        transactionSupported = dbCon.getMetaData().supportsTransactions();
        log.info(transactionSupported ? DB_YES_TRANS : DB_NO_TRANS);
        log.debug(transactionSupported ? DB_YES_TRANS : DB_NO_TRANS);
      }
      catch(Exception ex)
      {
        transactionSupported = false;
        log.info(DB_NO_TRANS);
        log.debug(DB_NO_TRANS);
      }
      transactionChecked = true;
    }
    return transactionSupported;
  }

  /**
   * Verifica se il db di default supporta le transazioni.
   * Il risultato viene recuperato solo una volta e salvato in una cache.
   * Viene testata la connessione di default.
   * @throws Exception
   */
  public boolean supportTransaction()
  {
    if(!transactionChecked)
    {
      Connection dbCon = null;
      try
      {
        dbCon = Torque.getConnection();
        supportTransaction(dbCon);
      }
      catch(Exception ex)
      {
        if(dbCon != null)
          Torque.closeConnection(dbCon);
      }
      transactionChecked = true;
    }
    return transactionSupported;
  }

  /**
   * Ritorna vero se il database di default supporta le transazioni.
   */
  @Override
  public boolean isTransactionSupported()
  {
    return supportTransaction();
  }

  /**
   * Imposta esplicitamente lo stato di supporto delle transazioni senza
   * interrogare il database in merito: alcuni database (DBMAKER ver. 3.7) non
   * gradiscono che si indaghi sul suo stato di supporto delle transazioni;
   * con questo metodo si puo' impostare esplicitamente senza ricorrere
   * all'auto discovering.
   */
  public void setTransactionSupported(boolean transactionSupported)
  {
    this.transactionChecked = true;
    this.transactionSupported = transactionSupported;
  }
}
