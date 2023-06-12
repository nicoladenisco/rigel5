/*
 *  ModifyMonitorListener.java
 *  Creato il May 4, 2018, 10:50:26 AM
 *
 *  Copyright (C) 2018 Informatica Medica s.r.l.
 *
 *  Questo software è proprietà di Informatica Medica s.r.l.
 *  Tutti gli usi non esplicitimante autorizzati sono da
 *  considerarsi tutelati ai sensi di legge.
 *
 *  Informatica Medica s.r.l.
 *  Viale dei Tigli, 19
 *  Casalnuovo di Napoli (NA)
 */
package org.rigel5.db.torque;

import java.sql.Connection;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.om.ObjectKey;

/**
 * Ascoltatore cambiamenti su tabella.
 *
 * @author Nicola De Nisco
 */
public interface ModifyMonitorListener
{
  /**
   * Notifica inserimento record.
   * Viene chiamata dopo l'inserimento di un nuovo record in tabella.
   * @param dbName nome del database
   * @param tableName nome della tabella
   * @param primaryKey primary key del record appena creato
   * @param criteria valori inseriti nel record
   * @param con connessione sql (può essere null)
   * @throws TorqueException
   */
  public void doInsert(String dbName, String tableName, ObjectKey primaryKey, Criteria criteria, Connection con)
     throws TorqueException;

  /**
   * Notifica aggiornamento record.
   * Viene chiamata dopo l'aggiornamento di un record in tabella.
   * @param dbName nome del database
   * @param tableName nome della tabella
   * @param selectCriteria valori per selezione record
   * @param criteria valori aggiornati nel record
   * @param con connessione sql (può essere null)
   * @throws TorqueException
   */
  public void doUpdate(String dbName, String tableName, Criteria selectCriteria, Criteria criteria, Connection con)
     throws TorqueException;

  /**
   * Notifica cancellazione record.
   * Viene chiamata dopo la cancellazione del record.
   * @param dbName nome del database
   * @param tableName nome della tabella
   * @param selectCriteria valori per selezione record
   * @param con connessione sql (può essere null)
   * @throws TorqueException
   */
  public void doDelete(String dbName, String tableName, Criteria selectCriteria, Connection con)
     throws TorqueException;
}
