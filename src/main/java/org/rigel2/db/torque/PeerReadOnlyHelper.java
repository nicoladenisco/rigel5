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
package org.rigel2.db.torque;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.apache.torque.Torque;

/**
 * Utilità per eseguire operazioni readonly senza usare lambda (PeerTransactAgent).
 * Pensata per essere usata in posti particolari (JSP per esempio).
 *
 * @author Nicola De Nisco
 */
public class PeerReadOnlyHelper implements Closeable
{
  Connection dbCon = null;
  DatabaseMetaData md = null;
  int readOnlyState = -1, isolationLevelState = -1;

  public Connection getReadOnlyConnection()
     throws Exception
  {
    dbCon = Torque.getConnection();
    md = dbCon.getMetaData();

    // memorizza stato e imposta connesione a read only
    readOnlyState = dbCon.isReadOnly() ? 1 : 0;
    dbCon.setReadOnly(true);

    // se supportato imposta letture senza transazione
    if(md.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED))
    {
      isolationLevelState = dbCon.getTransactionIsolation();
      dbCon.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }

    return dbCon;
  }

  public void putReadOnlyConnection()
     throws Exception
  {
    if(dbCon != null)
    {
      // riporta stato read only a valore precedente
      if(readOnlyState != -1)
        dbCon.setReadOnly(readOnlyState == 1);

      // se supportato riporta isolamento come precedente
      if(isolationLevelState != -1)
        dbCon.setTransactionIsolation(isolationLevelState);

      Torque.closeConnection(dbCon);
    }
  }

  @Override
  public void close()
     throws IOException
  {
    try
    {
      putReadOnlyConnection();
    }
    catch(IOException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw new IOException(e);
    }
  }
}
