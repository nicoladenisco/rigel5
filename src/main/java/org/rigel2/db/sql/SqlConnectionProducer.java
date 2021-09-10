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
package org.rigel2.db.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rigel2.db.ConnectionProducer;

/**
 * Produttore di connessioni SQL.
 * Ogni istanza può produrre una sola connessione che ha il
 * ciclo di vita legato all'istanza. Quando l'istanza viene
 * rimossa (dal garbace collector) la connessione viene chiusa.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class SqlConnectionProducer implements ConnectionProducer
{
  /** Logging */
  private static Log log = LogFactory.getLog(SqlConnectionProducer.class);
  private String driver = null;
  private String url = null;
  private String user = null;
  private String password = null;
  private int cntCount = 0;
  private long creationTime = 0;
  private Connection con = null;
  public static final long CONN_TIME_EXPIRE = 10 * 60 * 1000; // 10 minuti

  public class SqlConnectionHolder implements ConnectionHolder
  {
    private Connection con = null;

    @Override
    public Connection getConnection()
       throws Exception
    {
      if(con == null)
        con = createConnection();

      return con;
    }

    @Override
    public void releaseConnection()
       throws Exception
    {
      if(con != null)
      {
        con.close();
        con = null;
      }
    }

    // chiamata dal garbage collection: chiude la connessione se aperta
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

  public SqlConnectionProducer()
  {
  }

  public SqlConnectionProducer(String driver, String url,
     String user, String password)
  {
    this.driver = driver;
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public SqlConnectionProducer(String driver, String url,
     String user, String password, boolean supTrans)
  {
    this.driver = driver;
    this.url = url;
    this.user = user;
    this.password = password;
    this.transactionChecked = true;
    this.transactionSupported = supTrans;
  }

  private Connection createConnection()
     throws Exception
  {
    Class.forName(driver);
    Connection icon = DriverManager.getConnection(url, user, password);
    creationTime = System.currentTimeMillis();
    supportTransaction(icon);
    return icon;
  }

  public Connection getConnection()
     throws Exception
  {
    if(con == null)
      con = createConnection();

    cntCount++;
    return con;
  }

  public void releaseConnection(Connection c)
     throws Exception
  {
    if(con != c)
      throw new Exception("Connessione non generata da questa classe!");

    cntCount--;
    if(cntCount == 0
       && (System.currentTimeMillis() - creationTime) > CONN_TIME_EXPIRE)
    {
      con.close();
      con = null;
    }
  }

  private boolean transactionSupported = false, transactionChecked = false;

  /**
   * Verifica se il db di default supporta le transazioni.
   * Il risultato viene recuperato solo una volta e salvato in una cache.
   * @param con
   * @return
   */
  public boolean supportTransaction(Connection con)
  {
    if(!transactionChecked)
    {
      try
      {
        transactionSupported = con.getMetaData().supportsTransactions();
      }
      catch(Exception ex)
      {
        transactionSupported = false;
      }
      transactionChecked = true;
    }
    return transactionSupported;
  }

  public void setUrl(String url)
  {
    this.url = url;
  }

  public String getUrl()
  {
    return url;
  }

  public void setUser(String user)
  {
    this.user = user;
  }

  public String getUser()
  {
    return user;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  public String getPassword()
  {
    return password;
  }

  public void setCntCount(int cntCount)
  {
    this.cntCount = cntCount;
  }

  public int getCntCount()
  {
    return cntCount;
  }

  public void setCreationTime(long creationTime)
  {
    this.creationTime = creationTime;
  }

  public long getCreationTime()
  {
    return creationTime;
  }

  public void setDriver(String driver)
  {
    this.driver = driver;
  }

  public String getDriver()
  {
    return driver;
  }

  @Override
  public boolean isTransactionSupported()
  {
    return transactionSupported;
  }

  public void setTransactionSupported(boolean transactionSupported)
  {
    this.transactionSupported = transactionSupported;
  }

  @Override
  public ConnectionHolder getConnectionHolder()
     throws Exception
  {
    return new SqlConnectionHolder();
  }
}
