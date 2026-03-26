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
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rigel5.db.ConcurrentThreadTransaction;
import org.rigel5.db.TransactAgent;

/**
 * <p>
 * Title: Gestore di transazioni per applicazioni Turbine.</p>
 * <p>
 * Description:
 * Classe base per gestire operazioni su db con transazioni.
 * Ereditare da questa classe e ridefinire run.</p>
 * <p>
 * Tutte le operazioni eseguite nella funzione run ridefinita,
 * utilizzando l'oggetto DBConnection fornito, vengono eseguite
 * all'interno di una transazione sul db.
 * </p>
 * <p>
 * Se una qualsiasi eccezione viene sollevata si ottiene un
 * rollback della transazione in corso.<br>
 * Se la funzione run restituisce true la transazione viene confermata
 * e una commit viene eseguita.
 * </p>
 * <pre>
 * <code>
 *
 * esempio:
 * ...
 * ...
 *  PeerTransactAgent ta = new PeerTransactAgent()
 *  {
 *    public boolean run(DBConnection dbCon, boolean transactionSupported)
 *       throws Exception
 *     {
 *        PeerTableModel ptm = (PeerTableModel)(wl.getPtm());
 *        int numEle = ptm.getRowCount();
 *
 *        for(int i=0 ; i&lt;numEle ; i++)
 *        {
 *          BaseObject obj = (BaseObject)(ptm.getRowRecord(i));
 *          if(ptm.isRowDeleted(i)) {
 *            obj.delete(dbCon);
 *          } else if(obj.isModified())
 *            obj.save(dbCon);
 *        }
 *        ptm.removeDeleted();
 *
 *        // conferma la transazione ed esce
 *        return true;
 *     }
 *   };
 *
 *   ta.runNow();
 * ...
 * ...
 *
 * </code>
 * </pre>
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class PeerTransactAgent implements TransactAgent
{
  private static final Log log = LogFactory.getLog(PeerTransactAgent.class);

  public boolean executed = false, readonly = false;
  public HashMap<String, Object> tcontext = new HashMap<>();
  public Object[] tparams = null;
  public static ConcurrentHashMap<Long, Timestamp> thids = new ConcurrentHashMap<>();
  private static Boolean usaTransazione;

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
  public interface executorReturn<T>
  {
    public T execute(Connection con)
       throws Exception;
  }

  public PeerTransactAgent()
  {
  }

  /**
   * Costruttore con possibiltà di esecuzione immediata.
   * @param execute se vero esegue immediatamente la transazione (chiama runNow())
   * @throws Exception
   */
  public PeerTransactAgent(boolean execute)
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
  public PeerTransactAgent(boolean execute, Map<String, Object> context)
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
  public PeerTransactAgent(boolean execute, Object... params)
     throws Exception
  {
    this.tparams = params;
    if(execute)
      runNow();
  }

  @Override
  public boolean isTransactionSupported()
  {
    if(usaTransazione == null)
    {
      synchronized(thids)
      {
        if(usaTransazione == null)
        {
          try(PeerReadWriteHelper p = new PeerReadWriteHelper())
          {
            p.getConnection();
            usaTransazione = p.getDatabaseMetaData().supportsTransactions();
          }
          catch(Exception e)
          {
            throw new RuntimeException(e);
          }
        }
      }
    }

    return usaTransazione;
  }

  @Override
  public void runNow()
     throws Exception
  {
    if(isTransactionSupported())
      runTransaction();
    else
      runSimple();
  }

  public void runTransaction()
     throws Exception
  {
    long thid = Thread.currentThread().getId();
    if(thids.contains(thid))
      throw new ConcurrentThreadTransaction(Thread.currentThread(), thids.get(thid));

    try(PeerReadWriteTransactionHelper p = new PeerReadWriteTransactionHelper())
    {
      Connection con = p.getConnection();

      // esegue comandi all'interno della transazione
      thids.put(thid, new Timestamp(System.currentTimeMillis()));
      executed = readonly = false;
      executed = run(con, true);

      if(executed)
        p.commit();
      else
        p.rollback();
    }
    finally
    {
      thids.remove(thid);
    }
  }

  public void runSimple()
     throws Exception
  {
    try(PeerReadWriteHelper p = new PeerReadWriteHelper())
    {
      executed = readonly = false;
      executed = run(p.getConnection(), false);
    }
  }

  public void runReadOnly()
     throws Exception
  {
    try(PeerReadOnlyHelper p = new PeerReadOnlyHelper())
    {
      executed = readonly = false;
      executed = run(p.getReadOnlyConnection(), false);
    }
  }

  public static Map<String, Object> execute(executor exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(true, exec)
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

  public static Map<String, Object> execute(boolean useTransaction, executor exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(true, exec)
    {
      @Override
      public boolean isTransactionSupported()
      {
        return useTransaction && super.isTransactionSupported();
      }

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
    PeerTransactAgent ta = new PeerTransactAgent(true, exec)
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

  public static Map<String, Object> execute(boolean useTransaction, executorContext exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(true, exec)
    {
      @Override
      public boolean isTransactionSupported()
      {
        return useTransaction && super.isTransactionSupported();
      }

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

  public static Map<String, Object> execute(Map<String, Object> data, executorContext exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(true, exec, data)
    {
      @Override
      public boolean run(Connection dbCon, boolean transactionSupported)
         throws Exception
      {
        tcontext.putAll((Map<String, Object>) tparams[1]);
        ((executorContext) tparams[0]).execute(dbCon, tcontext);
        return true;
      }
    };

    return ta.tcontext;
  }

  public static <T> T executeReturn(executorReturn<T> exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(true, exec)
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

  public static <T> T executeReturn(boolean useTransaction, executorReturn<T> exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(true, exec)
    {
      @Override
      public boolean isTransactionSupported()
      {
        return useTransaction && super.isTransactionSupported();
      }

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

  public static Map<String, Object> executeReadonly(executor exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(false, exec)
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

  public static Map<String, Object> eR(executor exec)
     throws Exception
  {
    return executeReadonly(exec);
  }

  public static <T> T executeReturnReadonly(executorReturn<T> exec)
     throws Exception
  {
    PeerTransactAgent ta = new PeerTransactAgent(false, exec)
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

  public static <T> T eRR(executorReturn<T> exec)
     throws Exception
  {
    return executeReturnReadonly(exec);
  }
}
