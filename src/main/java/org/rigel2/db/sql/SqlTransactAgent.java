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
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rigel2.SetupHolder;
import org.rigel2.db.ConcurrentThreadTransaction;
import org.rigel2.db.TransactAgent;

/**
 * Gestore di transazioni.
 *
 * @author Nicola De Nisco
 */
abstract public class SqlTransactAgent implements TransactAgent
{
  private static Log log = LogFactory.getLog(SqlTransactAgent.class);

  public boolean executed = false, readonly = false;
  public HashMap<String, Object> tcontext = new HashMap<>();
  public Object[] tparams = null;
  public static ConcurrentHashMap<Long, Timestamp> thids = new ConcurrentHashMap<>();

  @FunctionalInterface
  public interface executor
  {
    public void execute(Connection con)
       throws Exception;
  }

  @FunctionalInterface
  public interface executorContext
  {
    public void execute(Connection con, Map<String, Object> context)
       throws Exception;
  }

  @FunctionalInterface
  public interface executorNoconn
  {
    public void execute()
       throws Exception;
  }

  @FunctionalInterface
  public interface executorContextNoconn
  {
    public void execute(Map<String, Object> context)
       throws Exception;
  }

  @FunctionalInterface
  public interface executorReturn<T>
  {
    public T execute(Connection con)
       throws Exception;
  }

  @FunctionalInterface
  public interface executorReturnNoconn<T>
  {
    public T execute()
       throws Exception;
  }

  public SqlTransactAgent()
  {
  }

  /**
   * Costruttore con possibiltà di esecuzione immediata.
   * @param execute se vero esegue immediatamente la transazione (chiama runNow())
   * @throws Exception
   */
  public SqlTransactAgent(boolean execute)
     throws Exception
  {
    if(execute)
      runNow();
  }

  /**
   * Costruttore con possibiltà di esecuzione immediata.
   * @param execute se vero esegue immediatamente la transazione (chiama runNow())
   * @param context mappa di chiave/volere per usi nella funzione ridefinita
   * @throws Exception
   */
  public SqlTransactAgent(boolean execute, Map<String, Object> context)
     throws Exception
  {
    this.tcontext.putAll(context);
    if(execute)
      runNow();
  }

  /**
   * Costruttore con possibiltà di esecuzione immediata.
   * @param execute se vero esegue immediatamente la transazione (chiama runNow())
   * @param params parametri; saranno disponibile all'interno della funzione ridefinita nell'array tparams
   * @throws Exception
   */
  public SqlTransactAgent(boolean execute, Object... params)
     throws Exception
  {
    this.tparams = params;
    if(execute)
      runNow();
  }

  @Override
  public boolean isTransactionSupported()
  {
    return SetupHolder.getConProd().isTransactionSupported();
  }

  @Override
  public void runNow()
     throws Exception
  {
    SetupHolder.getConProd().runConnection((dbCon) ->
    {
      if(isTransactionSupported())
        runTransaction(dbCon);
      else
        runSimple(dbCon);
    });
  }

  protected void runTransaction(Connection dbCon)
     throws Exception
  {
    boolean prevAutoCommitState = true;
    long thid = Thread.currentThread().getId();
    if(thids.contains(thid))
      throw new ConcurrentThreadTransaction(Thread.currentThread(), thids.get(thid));

    try
    {
      prevAutoCommitState = dbCon.getAutoCommit();
      dbCon.setAutoCommit(false);

      // esegue comandi all'interno della transazione
      thids.put(thid, new Timestamp(System.currentTimeMillis()));
      executed = readonly = false;
      executed = run(dbCon, true);
      thids.remove(thid);

      if(executed)
        dbCon.commit();
      else
        dbCon.rollback();
    }
    catch(Exception ex)
    {
      thids.remove(thid);
      executed = false;

      try
      {
        if(dbCon != null)
          dbCon.rollback();
      }
      catch(SQLException sQLException)
      {
        // questa eccezione viene ignorata
        // essendo piu' importante riportare
        // quella che ha abortito la transazione
        log.error("Fatal in rollback transaction.", sQLException);
      }

      throw ex;
    }
    finally
    {
      if(dbCon != null)
        dbCon.setAutoCommit(prevAutoCommitState);
    }
  }

  protected void runSimple(Connection dbCon)
     throws Exception
  {
    // esegue comandi senza transazione
    executed = readonly = false;
    executed = run(dbCon, false);
  }

  public void runReadOnly()
     throws Exception
  {
    SetupHolder.getConProd().runConnection((dbCon) -> runReadOnlyInternal(dbCon));
  }

  public void runReadOnlyInternal(Connection dbCon)
     throws Exception
  {
    DatabaseMetaData md = null;
    int readOnlyState = -1, isolationLevelState = -1;

    try
    {
      md = dbCon.getMetaData();

      // memorizza stato e imposta connesione a read only
      readOnlyState = dbCon.isReadOnly() ? 1 : 0;
      dbCon.setReadOnly(true);

      // se supportato imposta letture senza senza transazione
      if(md.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED))
      {
        isolationLevelState = dbCon.getTransactionIsolation();
        dbCon.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
      }

      // esegue comandi senza transazione
      executed = false;
      readonly = true;
      executed = run(dbCon, false);
    }
    finally
    {
      // riporta stato read only a valore precedente
      if(readOnlyState != -1)
        dbCon.setReadOnly(readOnlyState == 1);

      // se supportato riporta isolamento come precedente
      if(isolationLevelState != -1)
        dbCon.setTransactionIsolation(isolationLevelState);
    }
  }

  public static Map<String, Object> execute(executor exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(true, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        ((executor) tparams[0]).execute(dbCon);
        return true;
      }
    };

    return ta.tcontext;
  }

  public static Map<String, Object> execute(executorContext exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(true, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        ((executorContext) tparams[0]).execute(dbCon, tcontext);
        return true;
      }
    };

    return ta.tcontext;
  }

  public static Map<String, Object> execute(Connection con, executorNoconn exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(false, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        ((executorNoconn) tparams[0]).execute();
        return true;
      }
    };

    ta.runTransaction(con);
    return ta.tcontext;
  }

  public static Map<String, Object> execute(Connection con, executorContextNoconn exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(false, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        ((executorContextNoconn) tparams[0]).execute(tcontext);
        return true;
      }
    };

    ta.runTransaction(con);
    return ta.tcontext;
  }

  public static <T> T executeReturn(executorReturn<T> exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(true, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        tcontext.put("rv", ((executorReturn<T>) tparams[0]).execute(dbCon));
        return true;
      }
    };

    return (T) ta.tcontext.get("rv");
  }

  public static <T> T executeReturn(Connection con, executorReturnNoconn<T> exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(false, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        tcontext.put("rv", ((executorReturnNoconn<T>) tparams[0]).execute());
        return true;
      }
    };

    ta.runTransaction(con);
    return (T) ta.tcontext.get("rv");
  }

  public static Map<String, Object> executeReadonly(executor exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(false, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        ((executor) tparams[0]).execute(dbCon);
        return true;
      }
    };

    ta.runReadOnly();
    return ta.tcontext;
  }

  public static <T> T executeReturnReadonly(executorReturn<T> exec)
     throws Exception
  {
    SqlTransactAgent ta = new SqlTransactAgent(false, exec)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        tcontext.put("rv", ((executorReturn<T>) tparams[0]).execute(dbCon));
        return true;
      }
    };

    ta.runReadOnly();
    return (T) ta.tcontext.get("rv");
  }
}
