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
package org.rigel5.glue;

import com.workingdogs.village.Record;
import java.sql.Connection;

/**
 * Salvataggio degli oggetti Record.
 *
 * @author Nicola De Nisco
 */
public interface RecordObjectSaver
{
  /**
   * Inizilizza con un tabella del db.
   * @param tableName nome della tabella
   * @param con connessione al db (sola lettura)
   * @throws Exception
   */
  public void init(String tableName, Connection con)
     throws Exception;

  /**
   * Imposta le credenziali dell'utente.
   * @param idUser
   * @param isAdmin
   * @throws Exception
   * @deprecated usare le funzioni salva con utente specificato: sono rientranti
   */
  public void setUserInfo(int idUser, boolean isAdmin)
     throws Exception;

  /**
   * Salva un oggetto sul database applicando una serie di controlli
   * se stiamo modificando un record gia' esistente:
   * lo stato_rec deve essere 0, altrimenti il record viene considerato
   * non modificabile, la timestamp dell'ultima modifica non deve essere
   * cambiata altrimenti vuol dire che un altro utente ha gia' modificato
   * il record.
   * @param obj l'oggetto peer da salvare
   * @throws Exception
   * @deprecated usare la funzione salva(..,userID,...) perchè rientrante
   */
  public void salva(Record obj)
     throws Exception;

  /**
   * Salva un oggetto sul database applicando una serie di controlli
   * se stiamo modificando un record gia' esistente.
   * Lo stato_rec deve essere 0, altrimenti il record viene considerato
   * non modificabile, la timestamp dell'ultima modifica non deve essere
   * cambiata altrimenti vuol dire che un altro utente ha gia' modificato
   * il record.
   * @param obj l'oggetto peer da salvare
   * @param statoRecNew lo stato del record da impostare
   * @throws Exception
   * @deprecated usare la funzione salva(..,userID,...) perchè rientrante
   */
  public void salva(Record obj, int statoRecNew)
     throws Exception;

  /**
   * Salva un oggetto sul database applicando una serie di controlli
   * se stiamo modificando un record gia' esistente.
   * Lo stato_rec deve essere 0, altrimenti il record viene considerato
   * non modificabile, la timestamp dell'ultima modifica non deve essere
   * cambiata altrimenti vuol dire che un altro utente ha gia' modificato
   * il record.
   * @param obj l'oggetto peer da salvare
   * @throws Exception
   * @deprecated usare la funzione salva(..,userID,...) perchè rientrante
   */
  public void salva(Record obj, Connection dbCon, int statoRecNew)
     throws Exception;

  /**
   * Salva un oggetto sul database applicando una serie di controlli
   * se stiamo modificando un record gia' esistente.
   * Lo stato_rec deve essere 0, altrimenti il record viene considerato
   * non modificabile, la timestamp dell'ultima modifica non deve essere
   * cambiata altrimenti vuol dire che un altro utente ha gia' modificato
   * il record.
   * Viene allocata una transazione e il salvataggio eseguito all'interno della transazione.
   * @param obj l'oggetto peer da salvare
   * @param userID identificativo dell'utente
   * @param statoRecNew lo stato del record da impostare
   * @param writeLevel livello di scrittura dell'utente
   * @throws Exception
   */
  public void salva(Record obj, int userID, int statoRecNew, int writeLevel)
     throws Exception;

  /**
   * Salva un oggetto sul database applicando una serie di controlli
   * se stiamo modificando un record gia' esistente.
   * Lo stato_rec deve essere 0, altrimenti il record viene considerato
   * non modificabile, la timestamp dell'ultima modifica non deve essere
   * cambiata altrimenti vuol dire che un altro utente ha gia' modificato
   * il record.
   * @param obj l'oggetto peer da salvare
   * @param dbCon connessione al db SQL
   * @param userID identificativo dell'utente
   * @param statoRecNew lo stato del record da impostare
   * @param writeLevel livello di scrittura dell'utente
   * @throws Exception
   */
  public void salva(Record obj, Connection dbCon, int userID, int statoRecNew, int writeLevel)
     throws Exception;

  /**
   * Pulizia per nuovo oggetto.
   * Se l'oggetto possiede id_azienda, id_user, ult_modif, ecc.
   * questi campi vengono azzerati o posti a null.
   * Al successivo salvataggio verranno impostati correttamente da saveObject().
   * @param obj
   * @throws Exception
   */
  public void clearNewObject(Record obj)
     throws Exception;
}
