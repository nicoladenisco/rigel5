/*
 *  Copyright (C) 2019 Informatica Medica s.r.l.
 *
 *  Questo software è proprietà di Informatica Medica s.r.l.
 *  Tutti gli usi non esplicitimante autorizzati sono da
 *  considerarsi tutelati ai sensi di legge.
 *
 *  Informatica Medica s.r.l.
 *  Viale dei Tigli, 19
 *  Casalnuovo di Napoli (NA)
 *
 *  Creato il Mar 6, 2019, 9:04:14 AM
 */
package org.rigel5.db.torque;

import java.sql.Connection;
import java.util.List;
import java.util.Stack;
import org.apache.torque.om.ObjectKey;
import org.apache.torque.om.Persistent;
import org.apache.torque.util.Criteria;
import org.commonlib5.lambda.ConsumerThrowException;

/**
 * Caricatore tabelle per conversione peer in bean.
 * Viene utilizzata per fornire una interfaccia verso il database
 * alla funzione di conversione getBeanDeep().
 *
 * @author Nicola De Nisco
 */
public interface TorqueTableFetcher
{
  /** Si può procedere nell'analisi dei bean collegati. */
  public static final int DEEP_GRANT = 0;
  /** Non si può procedere nell'analisi dei bean collegati. */
  public static final int DEEP_DENY = 1;
  /** Si può convertire il bean e le relazioni senza caricare liste di bean collegati. */
  public static final int DEEP_GRANT_NO_DEEP = 2;
  /** Si può convertire il bean e le relazioni senza caricare liste di bean collegati. */
  public static final int DEEP_GRANT_NO_RECURSE = 3;

  @FunctionalInterface
  public static interface SelectBeanInterface
  {
    public List doSelectJoinAllForBeans(Criteria criteria, Stack<String> ignoreTableName, Connection conn)
       throws Exception;
  }

  /**
   * Preleva record da una tabella collegata.
   * @param tableNameFrom tabella origine della relazione (cardinalita uno nella join)
   * @param tableNameTarget tabella destinazione della relazione (cardinalita molti nella join)
   * @param c criteria per la selezione dei record nella tabella destinazione
   * @param ignoreTableName eventuale lista di tabelle da ignorare
   * @param conn connessione al db
   * @param sb funzione per l'accesso alla tabella target
   * @param funAddDettail funzione per aggiungere un record alla tabella origine
   * @throws Exception
   */
  public void fetchTable(String tableNameFrom, String tableNameTarget, Criteria c, Stack<String> ignoreTableName, Connection conn,
     SelectBeanInterface sb,
     ConsumerThrowException funAddDettail)
     throws Exception;

  /**
   * Recupera un bean se è stato già creato.
   * @param record oggetto peer di cui si richiede il bean
   * @return eventuale bean corrispondente o null
   * @throws java.lang.Exception
   */
  public Object getCreatedBeen(Persistent record)
     throws Exception;

  /**
   * Recupera un bean se è stato già creato.
   * @param tableName nome della tabella
   * @param pk chiave primaria del record corrispondente
   * @return il bean se già creato oppure null
   * @throws Exception
   */
  Object getCreatedBeen(String tableName, ObjectKey pk)
     throws Exception;

  /**
   * Recupera un bean se è stato già creato.
   * @param tableName nome della tabella
   * @param pk chiave primaria del record corrispondente
   * @return il bean se già creato oppure null
   * @throws Exception
   */
  Object getCreatedBeen(String tableName, int pk)
     throws Exception;

  /**
   * Salva peer e bean per scopi successivi.
   * @param record oggetto peer
   * @param bean bean relativo
   * @throws java.lang.Exception
   */
  public void putCreatedBean(Persistent record, Object bean)
     throws Exception;

  /**
   * Ritorna stato di completamento del bean.
   * @param bean bean in caricamento
   * @return vero se il caricamento è completato
   */
  public boolean isBeanCompleted(Object bean);

  /**
   * Imposta la condizione di bean completato.
   * Durante il caricamento può verificarsi un completamento parziale.
   * Questa funzione indica se il caricamento è stato completato.
   * @param bean bean in caricamento
   * @param completed lo stato di completamento
   */
  public void setBeanCompleted(Object bean, boolean completed);

  /**
   * Recupera stack delle tabelle da ignorare.
   * @return stack di nomi tabella
   */
  public Stack<String> getIgnoreTables();

  /**
   * Strategia generale da applicare per la tabella durante la conversione dei bean.
   * @param tableNameTarget tabella destinazione della relazione (cardinalita molti nella join)
   * @return una delle costanti DEEP_...
   */
  public int getDeepStrategy(String tableNameTarget);

  /**
   * Dettaglio di strategia da applicare nella relazione fra due tabelle.
   * @param tableNameFrom tabella origine della relazione (cardinalita uno nella join)
   * @param tableNameTarget tabella destinazione della relazione (cardinalita molti nella join)
   * @return una delle costanti DEEP_...
   */
  public int getDeepStrategy(String tableNameFrom, String tableNameTarget);
}
