/*
 * Copyright (C) 2021 Nicola De Nisco
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

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import org.apache.torque.Torque;
import org.apache.torque.util.Transaction;

/**
 * Utilità per eseguire operazioni read/write senza usare lambda (PeerTransactAgent).
 * Pensata per essere usata in posti particolari (JSP per esempio).
 *
 * @author Nicola De Nisco
 */
public class PeerReadWriteTransactionHelper implements Closeable
{
  protected Connection dbCon = null;

  public Connection getConnection()
     throws Exception
  {
    if(dbCon == null)
      dbCon = Transaction.begin(Torque.getDefaultDB());

    return dbCon;
  }

  public void commit()
     throws Exception
  {
    if(dbCon != null)
    {
      Transaction.commit(dbCon);
      dbCon = null;
    }
  }

  public void rollback()
     throws Exception
  {
    if(dbCon != null)
    {
      Transaction.rollback(dbCon);
      dbCon = null;
    }
  }

  @Override
  public void close()
     throws IOException
  {
    if(dbCon != null)
    {
      Transaction.safeRollback(dbCon);
      dbCon = null;
    }
  }
}
