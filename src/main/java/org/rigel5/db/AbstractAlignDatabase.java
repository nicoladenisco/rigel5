/*
 * Copyright (C) 2017 Nicola De Nisco
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
package org.rigel5.db;

import com.workingdogs.village.Column;
import com.workingdogs.village.TableDataSet;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.torque.map.ColumnMap;
import org.commonlib5.exec.ExecHelper;
import org.commonlib5.utils.ArrayMap;
import org.commonlib5.utils.ClassOper;
import org.commonlib5.utils.FileScanner;
import org.commonlib5.utils.OsIdent;
import org.commonlib5.utils.StringOper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.rigel5.db.torque.TableMapHelper;

/**
 * Allinea il database leggendo il file updpilot.xml.
 * Classe base da specializzare in classi derivate.
 *
 * @author Nicola De Nisco
 */
abstract public class AbstractAlignDatabase
{
  private final static Log log = LogFactory.getLog(AbstractAlignDatabase.class);

  public static class ItemUpdate
  {
    String name;
    public Element element;
    public int order = 0;
    public boolean uniqueTransaction = false;
    public String target;
  }

  public static class StepUpdate
  {
    public final List<ItemUpdate> lsItem = new ArrayList<>();
    public boolean uniqueTransaction = false;
  }

  public File dirScripts = null;
  public int anno = 0, settimana = 0;
  public int updAnno = 0, updSettimana = 0;
  public boolean updated = false;
  public boolean verbose = false;
  public boolean veryverbose = false;
  public boolean forceLast = false;
  public boolean forceFrom = false;
  public int forceAnno = 0, forceSettimana = 0;
  protected Connection con = null;
  protected String adapter;
  protected List<File> buildScripts = new ArrayList<>();
  protected String clobType = "CLOB";
  protected boolean alterFkCorrectPostgres = false;

  public static final int osType = OsIdent.checkOStype();
  public static final int SB_STATEMENT_SIZE = 1024;
  public static final Pattern alterPostgres = Pattern.compile(
     "ALTER TABLE (.+) +ADD CONSTRAINT +.+\\.(.+) +FOREIGN KEY +(.+)",
     Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  /**
   * Imposta connesione da utilizzare per l'aggiornamento del db.
   * @param con
   * @param adapter nome dell'adapter nella configurazione torque (postgresql, oracle, mysql, ecc.)
   */
  public void setConnection(Connection con, String adapter)
  {
    this.con = con;
    this.adapter = adapter;
    if("postgresql".equals(adapter))
    {
      this.clobType = "TEXT";
      this.alterFkCorrectPostgres = true;
    }
  }

  /**
   * Aggiunge un file al pacchetto di script per la crezione del db.
   * @param script file per la crezione del db
   */
  public void addBuildScript(File script)
  {
    buildScripts.add(script);
  }

  /**
   * Imposta lo step di aggiornamento attuale.
   * Verrà determinato leggendo la tabella version.
   * @param major
   * @param minor
   */
  public void setUpdateStep(int major, int minor)
  {
    anno = major;
    settimana = minor;
  }

  public void parsingSqlAggiornamento(int annoInstall, int settimanaInstall, String fileXmlUpdate)
     throws Exception
  {
    parsingSqlAggiornamento(annoInstall, settimanaInstall, new File(fileXmlUpdate));
  }

  public void parsingSqlAggiornamento(int annoInstall, int settimanaInstall, File fileXmlUpdate)
     throws Exception
  {
    if(forceFrom)
    {
      // abbassa il livello di aggiornamento letto, affinchè vengano riapplicati tutti gli step successivi a force
      annoInstall = forceAnno;
      settimanaInstall = forceSettimana - 1;
    }

    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(fileXmlUpdate);

    Element webapp = doc.getRootElement();

    Element elemento = null;
    List<Element> elementi = webapp.getChildren("step");
    Iterator el = elementi.iterator();
    while(el.hasNext())
    {
      elemento = (Element) el.next();
      int major = StringOper.parse(elemento.getAttributeValue("major"), 0);
      int minor = StringOper.parse(elemento.getAttributeValue("minor"), 0);

      if(major > annoInstall
         || (major == annoInstall && minor > settimanaInstall))
      {
        if(verbose)
          log.info("Esecuzione modifiche major=" + major + " minor=" + minor);

        StepUpdate step = parseStep(elemento);
        executeStep(elemento, step);

        updAnno = major;
        updSettimana = minor;
        updated = true;
        elemento = null;
      }
      else if(verbose)
        log.info("Gia aggiornato major=" + major + " minor=" + minor);
    }

    if(forceLast && !forceFrom && elemento != null)
    {
      int major = StringOper.parse(elemento.getAttributeValue("major"), 0);
      int minor = StringOper.parse(elemento.getAttributeValue("minor"), 0);
      if(verbose)
        log.info("Riapplicazione ultimo step di modifica major=" + major + " minor=" + minor);

      StepUpdate step = parseStep(elemento);
      executeStep(elemento, step);

      updAnno = major;
      updSettimana = minor;
      updated = true;
    }
  }

  protected StepUpdate parseStep(Element elemento)
     throws Exception
  {
    StepUpdate rv = new StepUpdate();
    List<Element> lsEle = elemento.getChildren();
    for(Element e : lsEle)
    {
      if(!StringOper.isEquAny(e.getName(), "file-sql", "file-csv", "execute", "statement"))
      {
        log.info("Elemento " + e.getName() + " non valido: viene ignorato.");
        continue;
      }

      ItemUpdate iu = new ItemUpdate();
      iu.name = e.getName();
      iu.order = StringOper.parse(e.getAttributeValue("order"), 0);
      iu.element = e;
      if(StringOper.checkTrue(e.getAttributeValue("transaction")))
        iu.uniqueTransaction = true;
      iu.target = StringOper.okStr(e.getAttributeValue("target"), "all");

      // se l'adapter è specificato scarta gli step con target differente
      if(adapter != null)
      {
        if(!StringOper.isEquAny(iu.target, adapter, "all"))
          continue;
      }

      rv.lsItem.add(iu);
    }

    rv.lsItem.sort((o1, o2) -> o1.order - o2.order);
    return rv;
  }

  protected void executeStep(Element elemento, StepUpdate step)
     throws Exception
  {
    File toRead;

    for(ItemUpdate iu : step.lsItem)
    {
      switch(iu.name)
      {
        case "file-sql":
          if((toRead = testFileModify(iu)) == null)
            continue;

          executeFileSqlIgnoraErrori(iu.element, toRead);
          break;

        case "file-csv":
          if((toRead = testFileModify(iu)) == null)
            continue;

          executeBranchCsv(iu.element, toRead);
          break;

        case "execute":
          executeBranchCommand(iu.element);
          break;

        case "statement":
          executeStatement(iu.element);
          break;
      }
    }
  }

  protected File testFileModify(ItemUpdate iu)
     throws Exception
  {
    String sFile = null;
    File toRead = null;

    if((sFile = StringOper.okStrNull(iu.element.getAttributeValue("modify"))) == null)
      throw new Exception("Step non dichiarato correttamente: manca modify.");

    toRead = new File(dirScripts, sFile);
    if(!toRead.canRead())
    {
      log.info("WARNING: Non riesco a leggere " + toRead.getAbsolutePath());
      return null;
    }

    // lo step è valido e può essere eseguito
    return toRead;
  }

  protected void executeFileSql(Element e, File fileSql)
     throws Exception
  {
    log.info("=== Elaborazione " + fileSql.getAbsolutePath() + " ===");

    try(InputStreamReader rd = new InputStreamReader(new FileInputStream(fileSql), "UTF-8"))
    {
      executeStreamSql(rd);
    }
  }

  protected void executeStreamSql(Reader rd)
     throws Exception
  {
    try(Statement st = con.createStatement();
       BufferedReader in = new BufferedReader(rd))
    {
      String str;
      StringBuilder sb = new StringBuilder(SB_STATEMENT_SIZE);

      while((str = in.readLine()) != null)
      {
        // trim della stringa
        str = str.trim();

        sb.append(str).append("\n ");
        if(str.endsWith(";"))
        {
          String sSQL = StringOper.okStrNull(sb);
          if(sSQL == null)
            continue;

          sSQL = StringOper.left(sSQL, -1);

          if(veryverbose)
            log.info("sSQL=" + sSQL);
          else
            log.info('!');

          if(!executeMacro(sSQL))
          {
            // esegue la modifica
            executeStatement(st, sSQL);
          }

          // ricrea il buffer per il comando SQL
          sb = new StringBuilder(SB_STATEMENT_SIZE);
        }
      }
    }
  }

  protected void executeFileSqlIgnoraErrori(Element e, File fileSql)
  {
    log.info("=== Elaborazione " + fileSql.getAbsolutePath() + " ===");

    try(InputStreamReader rd = new InputStreamReader(new FileInputStream(fileSql), "UTF-8"))
    {
      executeStreamSqlIgnoraErrori(rd);
    }
    catch(IOException ex)
    {
      System.err.println("SEVERE ERROR: " + ex.getMessage());
    }
  }

  public void executeFileSQL(File fileSQL, boolean ignoraErrori)
     throws Exception
  {
    try(InputStreamReader fr = new InputStreamReader(
       new FileInputStream(fileSQL), StandardCharsets.UTF_8))
    {
      if(ignoraErrori)
        executeStreamSqlIgnoraErrori(fr);
      else
        executeStreamSql(fr);
    }
  }

  protected void executeStreamSqlIgnoraErrori(Reader rd)
  {
    try
    {
      try(BufferedReader in = new BufferedReader(rd))
      {
        String str, sSQL;
        StringBuilder sb = new StringBuilder(SB_STATEMENT_SIZE);

        while((str = in.readLine()) != null)
        {
          // trim della stringa
          str = str.trim();
          sb.append(str).append("\n ");

          if(str.endsWith(";"))
          {
            if((sSQL = StringOper.okStrNull(sb)) == null)
              continue;

            executeInternalNoError(sSQL);
            sb = new StringBuilder(SB_STATEMENT_SIZE);
          }
        }

        // eventuale residuo non terminato da ';'
        if(sb.length() > 0)
        {
          if((sSQL = StringOper.okStrNull(sb)) != null)
            executeInternalNoError(sSQL + ";");
        }
      }

      if(!veryverbose)
        log.info('!');
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  private void executeInternalNoError(String sSQL)
  {
    try
    {
      if(veryverbose)
        log.info("sSQL=" + sSQL);

      if(!executeMacro(sSQL))
      {
        // esegue la modifica
        try(Statement st = con.createStatement())
        {
          if(sSQL.endsWith(";"))
            sSQL = StringOper.left(sSQL, -1);

          executeStatement(st, sSQL);
        }
      }

      if(!veryverbose)
        System.out.print('.');
    }
    catch(SQLException se)
    {
      // in caso di errore SQL visualizza il messaggio ma continua
      // se non siamo in veryverbose scrive qui la stringa sql
      if(!veryverbose)
        log.info("\nsSQL=" + sSQL);

      log.info(se.getMessage());
    }
    catch(Exception se)
    {
      // in caso di errore generico visualizza stack ma continua
      se.printStackTrace();
    }
  }

  public int executeStatement(Statement st, String sSQL)
     throws Exception
  {
    if(alterFkCorrectPostgres)
    {
      // correzione per generazione bacata di Torque
      // ALTER TABLE DVM.SAT_DEVICES ADD CONSTRAINT DVM.SAT_DEVICES_FK_1 FOREIGN KEY (ID_SAT_HOSTS) REFERENCES DVM.SAT_HOSTS (SAT_HOSTS_ID)
      // diventa
      // ALTER TABLE DVM.SAT_DEVICES ADD CONSTRAINT SAT_DEVICES_FK_1 FOREIGN KEY (ID_SAT_HOSTS) REFERENCES DVM.SAT_HOSTS (SAT_HOSTS_ID)
      // ovvero il nome del constraint non deve avere lo schema (viene ricavato dalla tabella)
      // altrimenti Postgres solleva eccezione per errore di sintassi
      if(sSQL.toUpperCase().startsWith("ALTER"))
      {
        String s = sSQL.replace("\r\n", " ").replace("\n", " ");

        Matcher m = alterPostgres.matcher(s);
        if(m.find())
        {
          sSQL = "ALTER TABLE " + m.group(1) + " ADD CONSTRAINT " + m.group(2) + " FOREIGN KEY " + m.group(3);
        }
      }
    }

    return st.executeUpdate(sSQL);
  }

  static final Pattern pMacro = Pattern.compile("macro_(.+)\\((.+)\\);", Pattern.CASE_INSENSITIVE);
  static final Pattern pIgnore = Pattern.compile("ERROR: relation .+ already exists", Pattern.CASE_INSENSITIVE);
  static final Pattern pDup = Pattern.compile("Key \\((.+)\\)=\\((.*)\\) is duplicated", Pattern.CASE_INSENSITIVE);

  protected boolean executeMacro(String sSQL)
     throws Exception
  {
    Matcher m = pMacro.matcher(sSQL);
    if(!m.find())
      return false;

    if(m.groupCount() != 2)
      throw new Exception("Errore nella macro " + sSQL);

    String mName = "macro_" + m.group(1).toLowerCase();

    try
    {
      Method mm = getClass().getMethod(mName, String.class);

      try
      {
        mm.invoke(this, m.group(2));
        return true;
      }
      catch(Exception ex)
      {
        if(ex instanceof InvocationTargetException)
        {
          Exception t = (Exception) ((InvocationTargetException) ex).getCause();
          if(t != null)
            throw t;
        }

        throw ex;
      }

    }
    catch(NoSuchMethodException e1)
    {
      throw new Exception("La macro '" + mName + "' non è definita.");
    }
  }

  /**
   * Macro per la creazione di un indice unico.
   * Questa macro crea un indice unico per la tabella element il campo specificato
   * verificando che l'indice non esista gia. In questo caso non solleva errore.
   * ATTENZIONE: se i valori non sono compatibili l'indice non viene creato.<br>
   * Sintassi: macro_createunique(TABELLA; NOME_INDICE; COLONNA1, COLONNA2, ...).<br>
   * @param params parametri della macro
   * @throws Exception
   */
  public void macro_createunique(String params)
     throws Exception
  {
    String[] arParams = params.split(";");
    if(arParams.length != 3)
      throw new Exception("Errore di sintassi: attesi 3 parametri (separatore ;) [" + params + "].");

    String tabella = StringOper.okStrNull(arParams[0]);
    String indice = StringOper.okStrNull(arParams[1]);
    String colonne = StringOper.okStrNull(arParams[2]);

    if(tabella == null || indice == null || colonne == null)
      throw new Exception("Dati errati: " + params);

    if(testIndice(tabella, indice))
    {
      if(verbose)
        log.info("L'indice " + indice + " già esiste; comando ignorato.");

      return;
    }

    try
    {
      try(Statement st = con.createStatement())
      {
        st.executeUpdate(
           "ALTER TABLE " + tabella
           + " ADD CONSTRAINT " + indice
           + " UNIQUE(" + colonne + ");"
        );
      }
    }
    catch(Exception e)
    {
      if(pIgnore.matcher(e.getMessage()).find())
      {
        if(verbose)
          log.info("L'indice " + indice + " già esiste; comando ignorato.");

        return;
      }

      // risolleva l'eccezione
      throw e;
    }
  }

  /**
   * Macro per la creazione di un indice unico.
   * Come macro_createunique ma con controllo sui valori compatibili con l'indice.
   * Se la tabella contiene dei duplicati per l'indice unico richiesto, pone a NULL
   * tutti i valori successivi al primo in modo da poter applicare con successo
   * il vincolo di univocità.<br>
   * Sintassi: macro_createuniquepurge(TABELLA; COL_PRIMARY1, COL_PRIMARY2, ...; NOME_INDICE; COLONNA1, COLONNA2,
   * ...).<br>
   * @param params parametri della macro
   * @throws Exception
   */
  public void macro_createuniquepurge(String params)
     throws Exception
  {
    String[] arParams = params.split(";");
    if(arParams.length != 4)
      throw new Exception("Errore di sintassi: attesi 4 parametri (separatore ;) [" + params + "].");

    String tabella = StringOper.okStrNull(arParams[0]);
    String primary = StringOper.okStrNull(arParams[1]);
    String indice = StringOper.okStrNull(arParams[2]);
    String colonne = StringOper.okStrNull(arParams[3]);

    if(tabella == null || indice == null || colonne == null)
      throw new Exception("Dati errati: " + params);

    if(testIndice(tabella, indice))
    {
      if(verbose)
        log.info("L'indice " + indice + " già esiste; comando ignorato.");

      return;
    }

    // rimuove eventuali duplicati dell'indice che stiamo creando
    eliminaDuplicati(tabella, primary, colonne);

    while(true)
    {
      try
      {
        String sSQL = "ALTER TABLE " + tabella
           + " ADD CONSTRAINT " + indice
           + " UNIQUE(" + colonne + ");";

        try(Statement st = con.createStatement())
        {
          st.executeUpdate(sSQL);
        }

        if(verbose)
          log.info("OK: " + sSQL);

        return;
      }
      catch(Exception e)
      {
        //
        // Gestione di sicurezza: se ci sono ancora dei duplicati
        // li intercetta attraverso il messaggio di errore di PotsgreSQL
        // element quindi tenta di rimuoverli a posteriori.
        //

        if(pIgnore.matcher(e.getMessage()).find())
        {
          if(verbose)
            log.info("L'indice " + indice + " già esiste; comando ignorato.");

          return;
        }

        Matcher mdup = pDup.matcher(e.getMessage());
        if(mdup.find())
        {
          // rimuove la chiave conflittuale
          String colonnaRimuovere = mdup.group(1);
          String valoreRimuovere = mdup.group(2);

          eliminaDuplicati(tabella, primary, colonnaRimuovere, valoreRimuovere);

          if(verbose)
            log.info("CA=Rimosso il valore " + valoreRimuovere + " dalla colonna " + colonnaRimuovere);

          continue;
        }

        // risolleva l'eccezione
        throw e;
      }
    }
  }

  /**
   * Macro per la cancellazione di tutte le viste.
   * @param params
   * @throws Exception
   */
  public void macro_dropallviews(String params)
     throws Exception
  {
    Map<String, String> options = StringOper.string2Map(StringOper.okStr(params), " ,;", true);
    boolean optionVerbose = StringOper.checkTrueFalse(options.get("verbose"), false);
    boolean optionShowsql = StringOper.checkTrueFalse(options.get("showsql"), false);

    DatabaseMetaData databaseMetaData = con.getMetaData();
    ArrayList<String> viewNames = new ArrayList<String>();
    try(ResultSet rSet = databaseMetaData.getTables(null, null, null, DbUtils.VIEWS_FILTER))
    {
      while(rSet.next())
      {
        if(rSet.getString("TABLE_TYPE").equals("VIEW"))
        {
          String schemaName = StringOper.okStrNull(rSet.getString("TABLE_SCHEM"));
          String tableName = StringOper.okStrNull(rSet.getString("TABLE_NAME"));

          if(tableName == null)
            continue;

          if(schemaName == null)
            viewNames.add(tableName);
          else
            viewNames.add(schemaName + "." + tableName);
        }
      }
    }

    log.info("Dropping " + viewNames.size() + " views.");

    for(String vn : viewNames)
    {
      try
      {
        String sSQL = "DROP VIEW " + vn + ";";

        if("postgresql".equals(adapter))
          sSQL = "DROP VIEW " + vn + " CASCADE;";

        if(optionShowsql)
          log.info("SQL=" + sSQL);

        try(Statement st = con.createStatement())
        {
          st.executeUpdate(sSQL);
        }

        if(verbose || optionVerbose)
          log.info("OK: " + sSQL);
      }
      catch(SQLException ex)
      {
        if(verbose || optionVerbose)
          log.info("ERROR: " + ex.getMessage());
      }
    }
  }

  /**
   * Macro per la creazione sicura delle foreign keys.
   * Sintassi: macro_createforeign(TABELLA; NOME INDICE; COL_LOCAL1, ...; TAB_FOREIGN; COL_FOREIGN1, ...)<br>
   * Esempio: macro_createforeign(SYS_UTENTI_FILTRI; SYS_UTENTI_FILTRI_FK_1; IDUTENTI; DB_UTENTI; IDUTENTI)
   * @param params
   * @throws Exception
   */
  public void macro_createforeign(String params)
     throws Exception
  {
    String[] arParams = params.split(";");
    if(arParams.length != 5)
      throw new Exception("Errore di sintassi: attesi 5 parametri (separatore ;) [" + params + "].");

    String tabella = StringOper.okStrNull(arParams[0]);
    String indice = StringOper.okStrNull(arParams[1]);
    String colonne = StringOper.okStrNull(arParams[2]);
    String ftabella = StringOper.okStrNull(arParams[3]);
    String fcolonne = StringOper.okStrNull(arParams[4]);

    if(tabella == null || indice == null || colonne == null)
      throw new Exception("Dati errati: " + params);

    if(testIndice(tabella, indice))
    {
      if(verbose)
        log.info("L'indice " + indice + " già esiste; comando ignorato.");

      return;
    }

    try(Statement st = con.createStatement())
    {
      // rimuove i valori che vanno in conflitto con la chiave esterna
      String sSQL
         = "UPDATE " + tabella
         + "   SET " + colonne + "=NULL"
         + " WHERE " + colonne + " NOT IN (SELECT " + fcolonne + " FROM " + ftabella + ")";

      st.executeUpdate(sSQL);

      if(verbose)
        log.info("macro_createforeign: " + sSQL);

      // applica la chiave esterna
      sSQL
         = "ALTER TABLE " + tabella
         + "  ADD CONSTRAINT " + indice + " FOREIGN KEY (" + colonne + ")"
         + "  REFERENCES " + ftabella + " (" + fcolonne + ")";

      st.executeUpdate(sSQL);

      if(verbose)
        log.info("macro_createforeign: " + sSQL);
    }
    catch(SQLException ex)
    {
      if(verbose)
        log.info("ERROR: " + ex.getMessage());
    }
  }

  /**
   * Macro per l'inserimento di un record 0 in una tabella.
   * Sintassi: macro_createzero(NOME TABELLA)<br>
   * Esempio: macro_createzero(TURBINE_USER)
   * @param params
   * @throws Exception
   */
  public void macro_createzero(String params)
     throws Exception
  {
    String sSQL = DbUtils.costruisciSQLzero(con, params);

    if(verbose)
      log.info("macro_createzero: " + sSQL);

    try(Statement st = con.createStatement())
    {
      st.executeUpdate(sSQL);
    }
    catch(SQLException ex)
    {
      if(verbose)
        log.info("ERROR: " + ex.getMessage());
    }
  }

  /**
   * Verifica se un indice esiste già nel db.
   * @param nomeTabella nome della tabella
   * @param nomeIndice nome dell'indice
   * @return vero se già esiste
   * @throws Exception
   */
  protected boolean testIndice(String nomeTabella, String nomeIndice)
     throws Exception
  {
    String nomeSchema = null;
    int pos = nomeTabella.indexOf('.');
    if(pos != -1)
    {
      nomeSchema = nomeTabella.substring(0, pos);
      nomeTabella = nomeTabella.substring(pos + 1);
    }

    DatabaseMetaData dbMeta = con.getMetaData();
    try(ResultSet rs = dbMeta.getIndexInfo(null, nomeSchema, nomeTabella, false, false))
    {
      if(testResultset(rs, nomeIndice))
        return true;
    }

    if(nomeSchema != null)
      nomeSchema = nomeSchema.toLowerCase();

    try(ResultSet rs = dbMeta.getIndexInfo(null, nomeSchema, nomeTabella.toLowerCase(), false, false))
    {
      if(testResultset(rs, nomeIndice))
        return true;
    }

    return false;
  }

  protected boolean testResultset(ResultSet rs, String nomeIndice)
     throws SQLException
  {
    while(rs.next())
    {
      String idxName = rs.getString(6);
      if(StringOper.isEquNocase(nomeIndice, idxName))
        return true;
    }
    return false;
  }

  protected void eliminaDuplicati(String nomeTabella, String primary, String colonne, String valori)
     throws Exception
  {
    String sSQL = "SELECT " + primary
       + " FROM " + nomeTabella
       + " WHERE ";

    String car[] = colonne.split(",");
    String var[] = valori.split(",");

    if(car.length != var.length)
      throw new Exception("Errore interno inaspettato: lista colonne collisione diversa da lista valori.");

    for(int i = 0; i < var.length; i++)
    {
      if(i != 0)
        sSQL += " AND ";

      if(isNumeric(nomeTabella, car[i]))
        sSQL += car[i] + "=" + var[i];
      else
        sSQL += car[i] + "='" + var[i] + "'";
    }

    sSQL += " ORDER BY " + primary;

    // recupera tutte le chiavi primarie in collisione
    ArrayList<String> arPrimary = new ArrayList<String>();

    try(Statement st = con.createStatement();
       ResultSet rs = st.executeQuery(sSQL))
    {
      while(rs.next())
      {
        arPrimary.add(rs.getString(1));
      }
    }

    if(arPrimary.isEmpty())
      return;

    boolean isPrimaryNumeric = isNumeric(nomeTabella, primary);

    // imposta a NULL tutti i record successivi al primo
    annullaDuplicati(nomeTabella, var, car, isPrimaryNumeric, primary, StringOper.toArray(arPrimary), 1, arPrimary.size());
  }

  public void annullaDuplicati(String nomeTabella, String[] var, String[] car,
     boolean isPrimaryNumeric, String primary, String[] arPrimary, int min, int max)
     throws SQLException
  {
    if((max - min) > 50)
    {
      int middle = (max + min) / 2;
      annullaDuplicati(nomeTabella, var, car, isPrimaryNumeric, primary, arPrimary, min, middle);
      annullaDuplicati(nomeTabella, var, car, isPrimaryNumeric, primary, arPrimary, middle, max);
      return;
    }

    String uSQL = "UPDATE " + nomeTabella + " SET ";

    for(int i = 0; i < var.length; i++)
    {
      if(i != 0)
        uSQL += ",";

      uSQL += car[i] + "=NULL";
    }

    if(isPrimaryNumeric)
      uSQL += " WHERE " + primary + " IN (" + StringOper.join(arPrimary, ',', min, max) + ")";
    else
      uSQL += " WHERE " + primary + " IN (" + StringOper.join(arPrimary, ',', '\'', min, max) + ")";

    if(verbose)
      log.info("CL=" + uSQL);

    try(Statement su = con.createStatement())
    {
      su.executeUpdate(uSQL);
    }
  }

  protected void eliminaDuplicati(String nomeTabella, String primary, String colonne)
     throws Exception
  {
    String sSQL, where;
    String car[] = colonne.split(",");

    // query preliminare per impostare a null tutti i valori stringa vuota
    sSQL = "UPDATE " + nomeTabella + " SET ";
    where = " WHERE ";

    for(int i = 0; i < car.length; i++)
    {
      String nomeColonna = car[i];
      if(!isNumeric(nomeTabella, nomeColonna))
      {
        String uSQL = sSQL + nomeColonna + "=NULL" + where + nomeColonna + "=''";

        try(Statement su = con.createStatement())
        {
          if(veryverbose)
            log.info("uSQL=" + uSQL);

          su.executeUpdate(uSQL);
        }
      }
    }

    // estrae i duplicati
    sSQL = "SELECT " + colonne + ", COUNT(*)"
       + " FROM " + nomeTabella
       + " GROUP BY " + colonne
       + " HAVING COUNT(*) > 1";
    for(int i = 0; i < car.length; i++)
    {
      sSQL += " AND " + car[i] + " IS NOT NULL";
    }

    if(verbose)
      log.info("CD=" + sSQL);

    try(Statement st = con.createStatement();
       ResultSet rs = st.executeQuery(sSQL))
    {
      while(rs.next())
      {
        String val = "";
        for(int i = 0; i < car.length; i++)
        {
          if(i > 0)
            val += ",";

          val += StringOper.okStr(rs.getString(car[i]));
        }

        eliminaDuplicati(nomeTabella, primary, colonne, val);
      }
    }
  }

  public void macro_populateuuid(String params)
     throws Exception
  {
    String[] arParams = params.split(";");
    if(arParams.length != 3)
      throw new Exception("Errore di sintassi: attesi 3 parametri (separatore ;) [" + params + "].");

    String nomeTabella = arParams[0];
    String nomePrimary = arParams[1];
    String nomeCampo = arParams[2];

    boolean pnum = DbUtils.isNumeric(con, nomeTabella, nomePrimary);

    String sSQL
       = "SELECT " + nomePrimary + "," + nomeCampo
       + "  FROM " + nomeTabella
       + " WHERE " + nomeCampo + " IS NULL";

    try(Statement sq = con.createStatement())
    {
      try(ResultSet rs = sq.executeQuery(sSQL))
      {
        String sUPD;
        while(rs.next())
        {
          if(pnum)
          {
            sUPD = "UPDATE " + nomeTabella
               + "   SET " + nomeCampo + "='" + UUID.randomUUID() + "'"
               + " WHERE " + nomePrimary + "=" + rs.getLong(1);
          }
          else
          {
            sUPD = "UPDATE " + nomeTabella
               + "   SET " + nomeCampo + "='" + UUID.randomUUID() + "'"
               + " WHERE " + nomePrimary + "='" + rs.getLong(1) + "'";
          }

          try(Statement su = con.createStatement())
          {
            su.executeUpdate(sUPD);
          }
        }
      }
    }
  }

  public void macro_build(String params)
     throws Exception
  {
    if(buildScripts.isEmpty())
    {
      log.info("ERROR: build scripts empty in build macro.");
      return;
    }

    for(File fileSQL : buildScripts)
    {
      if(verbose)
        log.info("Build " + fileSQL.getAbsolutePath());

      executeFileSQL(fileSQL, true);
    }
  }

  public void macro_campoclob(String params)
     throws Exception
  {
    String[] arParams = params.split(";");
    if(arParams.length < 1)
      throw new Exception("Errore di sintassi: attesi almento 1 parametro (separatore ;) [" + params + "].");

    String nomeTabella = arParams[0];
    String campi[] = null;

    if(arParams.length > 1)
      campi = Arrays.copyOfRange(arParams, 1, arParams.length);

    TableMapHelper tm = new TableMapHelper(nomeTabella);

    if(campi == null)
    {
      ColumnMap[] columns = tm.getColumns();
      for(int i = 0; i < columns.length; i++)
      {
        ColumnMap c = columns[i];
        if("CLOB".equalsIgnoreCase(c.getTorqueType()))
        {
          testConvertCampoToClob(tm.getNomeTabella(), c);
        }
      }
    }
    else
    {
      for(int i = 0; i < campi.length; i++)
      {
        String nomeCampo = campi[i];
        ColumnMap campo = tm.getCampo(nomeCampo);

        if(campo == null)
        {
          if(verbose)
            log.info("Tabella " + nomeTabella + " campo " + nomeCampo + " scartato perchè non trovato.");
          continue;
        }

        testConvertCampoToClob(nomeTabella, campo);
      }
    }
  }

  private void testConvertCampoToClob(String nomeTabella, ColumnMap cTo)
     throws Exception
  {
    String nomeCampo = cTo.getColumnName();
    TableDataSet tds = new TableDataSet(con, nomeTabella);
    Column cAct = tds.schema().findInSchemaIgnoreCaseQuiet(nomeCampo);
    Column cOld = tds.schema().findInSchemaIgnoreCaseQuiet(nomeCampo + "OLD");

    if(cAct == null)
    {
      String sSQL
         = "ALTER TABLE " + nomeTabella + "\n"
         + "    ADD COLUMN " + nomeCampo + " " + clobType + ";\n";

      if(cOld != null)
      {
        sSQL += "UPDATE " + nomeTabella + "\n"
           + "	SET " + nomeCampo + "=" + nomeCampo + "OLD;\n"
           + "";
      }

      StringReader sr = new StringReader(sSQL);
      executeStreamSqlIgnoraErrori(sr);
    }
    else
    {
      if(cAct.typeEnum() == Types.CLOB || "TEXT".equalsIgnoreCase(cAct.typeName()))
      {
        if(verbose)
          log.info("Tabella " + nomeTabella + " campo " + nomeCampo + " scartato perchè già CLOB.");
        return;
      }

      String sSQL
         = "ALTER TABLE " + nomeTabella + "\n"
         + "    RENAME " + nomeCampo + " TO " + nomeCampo + "OLD;\n"
         + "ALTER TABLE " + nomeTabella + "\n"
         + "    ADD COLUMN " + nomeCampo + " " + clobType + ";\n"
         + "UPDATE " + nomeTabella + "\n"
         + "	SET " + nomeCampo + "=" + nomeCampo + "OLD;\n"
         + "";

      StringReader sr = new StringReader(sSQL);
      executeStreamSqlIgnoraErrori(sr);
    }

    if(verbose)
      log.info("Tabella " + nomeTabella + " campo " + nomeCampo + " convertito in CLOB.");
  }

  /**
   * Ritorna il tipo per una colonna di tabella.
   * @param nomeTabella nome della tabella
   * @param nomeColonna nome della colonna
   * @return int {@code =>} SQL type from java.sql.Types 0=non trovato
   * @throws Exception
   */
  protected int getTipoColonna(String nomeTabella, String nomeColonna)
     throws Exception
  {
    String nomeSchema = null;
    int pos = nomeTabella.indexOf('.');
    if(pos != -1)
    {
      nomeSchema = nomeTabella.substring(0, pos);
      nomeTabella = nomeTabella.substring(pos + 1);
    }

    try(ResultSet rs = con.getMetaData().getColumns(null, nomeSchema, nomeTabella, null))
    {
      while(rs.next())
      {
        String cn = rs.getString("COLUMN_NAME");
        if(StringOper.isEquNocase(cn, nomeColonna))
        {
          return rs.getInt("DATA_TYPE");
        }
      }
    }

    if(nomeSchema != null)
      nomeSchema = nomeSchema.toLowerCase();

    try(ResultSet rs = con.getMetaData().getColumns(null, nomeSchema, nomeTabella.toLowerCase(), null))
    {
      while(rs.next())
      {
        String cn = rs.getString("COLUMN_NAME");
        if(StringOper.isEquNocase(cn, nomeColonna))
        {
          return rs.getInt("DATA_TYPE");
        }
      }
    }

    return 0;
  }

  protected boolean isNumeric(String nomeTabella, String nomeColonna)
     throws Exception
  {
    switch(getTipoColonna(nomeTabella, nomeColonna))
    {
      case Types.BIT:
      case Types.TINYINT:
      case Types.SMALLINT:
      case Types.INTEGER:
      case Types.BIGINT:
      case Types.FLOAT:
      case Types.REAL:
      case Types.DOUBLE:
      case Types.NUMERIC:
      case Types.DECIMAL:
        return true;
    }

    return false;
  }

  protected void executeBranchCommand(Element fileCmd)
     throws Exception
  {
    String comando = null;
    switch(osType)
    {
      case OsIdent.OS_MACOSX:
      case OsIdent.OS_LINUX:
        comando = StringOper.okStrNull(fileCmd.getAttributeValue("linux"));
        break;
      case OsIdent.OS_WINDOWS:
        comando = StringOper.okStrNull(fileCmd.getAttributeValue("win"));
        break;
    }

    if(comando == null)
    {
      log.info("WARNING: script non dichiarato per la piattaforma.");
      return;
    }

    if(!comando.isEmpty())
    {
      if(verbose)
        log.info("Eseguo: " + comando);

      try
      {
        ExecHelper eh = ExecHelper.execUsingShell(comando);
        log.info("SDOUT:\n" + eh.getOutput());
        log.info("SDERR:\n" + eh.getError());
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  protected void executeBranchCsv(Element fileCsv, File csvFile)
     throws Exception
  {
    String sTable = StringOper.okStrNull(fileCsv.getAttributeValue("table"));
    if(sTable == null)
      throw new Exception("Step non dichiarato correttamente: manca table.");

    String sFile = StringOper.okStrNull(fileCsv.getAttributeValue("modify"));
    if(sFile == null)
      throw new Exception("Step non dichiarato correttamente: manca modify.");

    if(verbose)
      log.info("Eseguo: " + csvFile.getAbsolutePath());

    try
    {
      String chiave = fileCsv.getAttributeValue("key");
      executeFileCsv(sTable, chiave, csvFile, fileCsv);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  protected void executeStatement(Element statSQL)
  {
    String sSQL = statSQL.getText();
    StringReader sr = new StringReader(sSQL);
    executeStreamSqlIgnoraErrori(sr);
  }

  public static class Info
  {
    String nomeCampo;
    int indiceCampo, sqlType;
  }

  protected String purgeNome(String nomeCampo)
  {
    String s = StringOper.okStrNull(nomeCampo);
    if(s == null)
      throw new RuntimeException("Errore di sintassi: campo vuoto nell'header.");

    if(s.startsWith("\""))
    {
      s = s.substring(1);
      if(s.endsWith("\""))
        return s.substring(0, s.length() - 1);

      throw new RuntimeException("Errore di sintassi: campo " + nomeCampo + " errato nell'header.");
    }

    if(s.startsWith("\'"))
    {
      s = s.substring(1);
      if(s.endsWith("\'"))
        return s.substring(0, s.length() - 1);

      throw new RuntimeException("Errore di sintassi: campo " + nomeCampo + " errato nell'header.");
    }

    return s;
  }

  protected void executeFileCsv(String tabella, String chiave, File csvFile, Element fileCsv)
     throws Exception
  {
    try(Reader in = new InputStreamReader(new FileInputStream(csvFile), "UTF-8"))
    {
      CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

      Map<String, Integer> headers = parser.getHeaderMap();
      int numColonne = headers.size();

      StringBuilder sb1 = new StringBuilder();
      StringBuilder sb2 = new StringBuilder();
      sb1.append("INSERT INTO ").append(tabella).append("(");
      sb2.append(") VALUES (");
      StringBuilder sb3 = new StringBuilder();
      sb3.append("UPDATE ").append(tabella).append(" SET ");

      ArrayList<Info> arInfo = new ArrayList<>();

      {
        int c1 = 0, c2 = 0;
        for(Map.Entry<String, Integer> entry : headers.entrySet())
        {
          Info i = new Info();

          i.nomeCampo = purgeNome(entry.getKey());
          i.indiceCampo = entry.getValue();

          if(c1 > 0)
          {
            sb1.append(",");
            sb2.append(",");
          }

          sb1.append(i.nomeCampo);
          sb2.append("?");

          if(!StringOper.isEquNocase(chiave, i.nomeCampo))
          {
            if(c2 > 0)
              sb3.append(",");
            sb3.append(i.nomeCampo).append("=?");
            c2++;
          }

          c1++;
          arInfo.add(i);
        }
      }

      sb1.append(sb2).append(")");
      sb3.append(" WHERE ").append(chiave).append("=?");

      PreparedStatement psIns = con.prepareStatement(sb1.toString());
      PreparedStatement psUpd = con.prepareStatement(sb3.toString());

      //ArrayMap<String, Integer> colMap = DbUtils.getTipiColonne(con, tabella);
      ArrayMap<String, Integer> colMap = DbUtils.getTipiColonneQuery(con, "SELECT * FROM " + tabella);

      if(colMap == null)
      {
        log.info("Tabella " + tabella + " non trovata nel database.");
        return;
      }

      arInfo.forEach((i) -> i.sqlType = colMap.getIgnoreCase(i.nomeCampo));

      for(CSVRecord csvRecord : parser)
      {
        // scarta i record che non hanno lo stesso numero di colonne
        if(numColonne != csvRecord.size())
        {
          log.info(String.format("Record %d scartato: attesi %d campi, trovati %d campi.",
             csvRecord.getRecordNumber(), numColonne, csvRecord.size()));
          continue;
        }

        int c1 = 1, c2 = 1, tipoChiave = 0;
        String valoreChiave = null;
        boolean updateRecord = false;

        for(Info i : arInfo)
        {
          String valoreCampo = manipolaCampo(i, StringOper.okStrNull(csvRecord.get(i.indiceCampo)));

          psIns.setObject(c1++, valoreCampo, i.sqlType);

          if(StringOper.isEquNocase(chiave, i.nomeCampo))
          {
            tipoChiave = i.sqlType;
            valoreChiave = valoreCampo;
          }
          else
            psUpd.setObject(c2++, valoreCampo, i.sqlType);
        }

        if(valoreChiave != null)
        {
          psUpd.setObject(c2++, valoreChiave, tipoChiave);
          updateRecord = psUpd.executeUpdate() != 0;
        }

        if(!updateRecord)
        {
          psIns.executeUpdate();
        }
      }
    }
  }

  protected String manipolaCampo(Info i, String valore)
     throws Exception
  {
    if(valore == null)
      return null;

    if(valore.startsWith("\""))
      valore = valore.substring(1);

    if(valore.endsWith("\""))
      valore = valore.substring(0, valore.length() - 1);

    return valore;
  }

  protected void parseForceFrom(String s)
  {
    String[] ss = s.split("/");
    if(ss.length != 2)
      throw new RuntimeException("Esprimere lo step di partenza con yyyy/ss (ES: 2014/21)");

    forceAnno = StringOper.parse(ss[0], 0);
    forceSettimana = StringOper.parse(ss[1], 0);

    if(forceAnno == 0 || forceSettimana == 0)
      throw new RuntimeException("Valori non consentiti per lo step di partenza.");
  }

  public void rigeneraViste(File dirSql)
     throws Exception
  {
    List<File> lsFiles = FileScanner.scan(dirSql, 99, "*-viste.sql");
    if(lsFiles.isEmpty())
      return;

    for(File fvista : lsFiles)
    {
      executeFileSQL(fvista, true);
    }
  }

  public void loadTablesFromSchemaDir(File dirSchema, String baseClass)
     throws Exception
  {
    List<File> files = FileScanner.scan(dirSchema, 10, "*-schema.xml");

    for(File f : files)
      loadTablesFromSchemaFile(f, baseClass);
  }

  public void loadTablesFromSchemaFile(File fxml, String baseClass)
     throws Exception
  {
    if(!fxml.exists())
      return;

    log.info("Leggo struttura db XML dal file " + fxml.getAbsolutePath());
    SAXBuilder builder = new SAXBuilder();
    Document d = builder.build(fxml);
    Element root = d.getRootElement();
    Namespace ns = d.getRootElement().getNamespace();

    List<Element> tables = root.getChildren("table", ns);

    for(Element table : tables)
    {
      // primo tentativo nome completo
      String tableName = table.getAttributeValue("name");
      caricaTableMap(tableName, baseClass);
    }
  }

  public void loadTablesFromResources(String nomeRisorsa, String baseClass)
     throws Exception
  {
    log.info("Leggo struttura db XML da risorsa " + nomeRisorsa);
    try(InputStream is = AbstractAlignDatabase.class.getResourceAsStream(nomeRisorsa))
    {
      loadTablesFromSchemaStream(is, baseClass);
    }
  }

  public void loadTablesFromSchemaStream(InputStream is, String baseClass)
     throws Exception
  {
    SAXBuilder builder = new SAXBuilder();
    Document d = builder.build(is);
    Element root = d.getRootElement();
    Namespace ns = d.getRootElement().getNamespace();

    List<Element> tables = root.getChildren("table", ns);

    for(Element table : tables)
    {
      String tableName = table.getAttributeValue("name");
      caricaTableMap(tableName, baseClass);
    }
  }

  private void caricaTableMap(String tableName, String baseClass)
  {
    // primo tentativo nome completo
    if(!caricaClassePeer(tableName, baseClass))
    {
      // secondo tentativo senza nome dello schema
      int pos = tableName.indexOf('.');
      if(pos != -1)
      {
        if(caricaClassePeer(tableName.substring(pos + 1), baseClass))
        {
          log.debug("OK TableMap per tabella " + tableName);
          return;
        }
      }

      log.error("Caricamento TableMap per tabella " + tableName + " fallito.");
    }

    log.debug("OK TableMap per tabella " + tableName);
  }

  private boolean caricaClassePeer(String tableName, String baseClass)
  {
    String pname = javaName(tableName) + "Peer";
    Class cz = ClassOper.loadClass(pname, baseClass, null);
    if(cz != null)
    {
      log.debug("Caricata classe " + cz.getName());
      return true;
    }

    return false;
  }

  public String javaName(String name)
  {
    String[] aa = name.split("\\.|_");
    StringBuilder sb = new StringBuilder();
    for(String s : aa)
      sb.append(s.substring(0, 1).toUpperCase()).append(s.substring(1).toLowerCase());
    return sb.toString();
  }
}
