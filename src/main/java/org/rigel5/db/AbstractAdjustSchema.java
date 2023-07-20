/*
 * AdjustSchema.java
 *
 * Created on 21 marzo 2006, 18.26
 */
package org.rigel5.db;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.commonlib5.utils.LongOptExt;
import org.commonlib5.utils.StringOper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Prende un file xml con dentro i nodi table, scende di un livello e considera i nodi "column", "index", "foreignkey".
 * Per questi nodi mette in uppercase tutti i valori dei tag name e mette gli underscore ("_") a tutti i valori
 * dei tag name che non li hanno.
 *
 * TEST:
 * -R -d torque/dtd torque/src/schema/pcsdm-schema.xml
 *
 * @author Nicola De Nisco
 */
public class AbstractAdjustSchema extends ParseSchemaBase
{
  private static final Log log = LogFactory.getLog(AbstractAdjustSchema.class);

  public static class TableInfo
  {
    public String idMethod = null;
    public boolean haveIdMethod = false;
    public int numCol = 0, numColPrimary = 0;
    public boolean haveStatoRec = false;
    public boolean haveMultiplePrimary = false;
    public Element uniquePrimaryKeyColumn = null;
    public String uniquePrimaryKeyColumnName = null;
    public String uniquePrimaryKeyColumnType = null;
  }

  public static class UniqueInfo
  {
    public String tableName, constraintName;
    public ArrayList<String> arColumns = new ArrayList<>();
  }

  // se è true, sostituisce il punto all'underscore nei nomi delle tabelle
  public boolean pointVsUndScore = false, forcePrimary = false, forceForeign = false,
     forceIDUndScore = false, forceTableJavaName = false,
     usePostgresql = false, useSchema = false, forcePrimaryIDtoNative = false;
  public String inputFile = null;
  public String infoFile = null;
  public String descFile = null;
  public ArrayList<String> vStatoRec = new ArrayList<>();
  public HashMap<String, Element> htElemTables = new HashMap<>();
  public HashMap<String, Element> htNameTables = new HashMap<>();
  public HashMap<String, String> htSchemas = new HashMap<>();
  public HashMap<String, TableInfo> htInfo = new HashMap<>();
  public HashMap<String, String> htTblDescr = new HashMap<>();
  public ArrayList<String> vDefZero = new ArrayList<>();
  public ArrayList<String> vSetZero = new ArrayList<>();
  public ArrayList<UniqueInfo> vUnique = new ArrayList<>();
  public boolean addStatoRec = false;
  public int minColStatoRec = 4;
  public String omBaseClass = null;
  public String omBasePeer = null;
  public Namespace ns;

  public AbstractAdjustSchema()
  {
  }

  public void readTableDescription()
     throws Exception
  {
    SAXBuilder builder = new SAXBuilder();
    Document dot = builder.build(new File(descFile));
    List<Element> tables = dot.getRootElement().getChildren("table", ns);
    for(Element e : tables)
    {
      String n = e.getAttributeValue("name");
      String d = e.getAttributeValue("description");

      if(n != null && d != null)
        htTblDescr.put(n.toUpperCase(), d);
    }
  }

  public String pVsu(String name)
  {
    int pos;
    if(pointVsUndScore && (pos = name.indexOf('.')) != -1)
      name = name.substring(0, pos) + "." + name.substring(pos + 1);
    return name;
  }

  public String chID(String name)
  {
    if(name.startsWith("ID") && !name.startsWith("ID_"))
      return "ID_" + name.substring(2);

    return name;
  }

  public String javaName(String name)
  {
    String[] aa = name.split("\\.|_");
    StringBuilder sb = new StringBuilder();
    for(String s : aa)
      sb.append(s.substring(0, 1).toUpperCase()).append(s.substring(1).toLowerCase());
    return sb.toString();
  }

  /**
   * Corregge l'albero XML modificando opportunamente i nomi
   * delle tabelle.
   * Tutte le modifiche conseguenti (foreign_key, ecc) sono
   * applicate.
   * @throws Exception
   */
  public void adjustNames()
     throws Exception
  {
    final List<Element> eleTables = doc.getRootElement().getChildren("table", ns);

    for(Element table : eleTables)
    {
      String tableName = pVsu(StringOper.okStr(table.getAttributeValue("name").toUpperCase()));

      String sname = null;
      String tname = tableName;
      int pos = tableName.indexOf('.');
      if(pos != -1)
      {
        sname = tableName.substring(0, pos);
        tname = tableName.substring(pos + 1);
      }

      // verifica per flag schema obbligatorio
      if(useSchema)
        if(pos == -1)
          throw new Exception("Tabella " + tableName + " deve avere uno schema.");

      // verifica per tabella duplicata
      Element tdup;
      if((tdup = htElemTables.get(tableName)) != null)
        throw new Exception("Tabella " + tableName + " duplicata.");

      if((tdup = htNameTables.get(tname)) != null)
        throw new Exception("Tabella " + tname + " duplicata (" + tdup.getAttributeValue("name") + ").");

      // imposta nome corretto nell'elemento
      table.setAttribute("name", tableName);
      htElemTables.put(tableName, table);
      htNameTables.put(tname, table);

      if(sname != null)
        if(htSchemas.get(sname) == null)
          htSchemas.put(sname, sname);

      if(forceTableJavaName)
        table.setAttribute("javaName", javaName(tableName));
      if(omBaseClass != null)
        table.setAttribute("baseClass", omBaseClass);
      if(omBasePeer != null)
        table.setAttribute("basePeer", omBasePeer);

      TableInfo ti = new TableInfo();

      // verifica se generazione ID
      ti.idMethod = table.getAttributeValue("idMethod");
      ti.haveIdMethod = StringOper.isOkStr(ti.idMethod);

      // aggiusta descrizione
      String sDesc = StringOper.okStrNull(table.getAttributeValue("description"));
      if(sDesc != null)
        table.setAttribute("description", adjDescrizione(sDesc));

      // Iteratore sul tag column
      final List<Element> eleColumns = table.getChildren("column", ns);
      for(Element column : eleColumns)
      {
        String columnName = chID(StringOper.okStr(column.getAttributeValue("name")).toUpperCase());
        String type = StringOper.okStr(column.getAttributeValue("type")).toUpperCase();
        String defval = StringOper.okStr(column.getAttributeValue("default")).toUpperCase();

        // imposta nome corretto della colonna
        column.setAttribute("name", columnName);

        // aggiusta descrizione
        String cDesc = StringOper.okStrNull(column.getAttributeValue("description"));
        if(cDesc != null)
          column.setAttribute("description", adjDescrizione(cDesc));

        if(columnName.equalsIgnoreCase("stato_rec"))
          ti.haveStatoRec = true;

        boolean isAutoIncrement = false;
        boolean isPrimaryKey = false;

        String s;
        if((s = column.getAttributeValue("autoIncrement")) != null)
          isAutoIncrement = StringOper.checkTrueFalse(s.trim(), false);
        if((s = column.getAttributeValue("primaryKey")) != null)
          isPrimaryKey = StringOper.checkTrueFalse(s.trim(), false);

        if(isAutoIncrement && !ti.haveIdMethod)
        {
          if(forcePrimaryIDtoNative)
          {
            table.setAttribute("idMethod", "native");
            ti.haveIdMethod = true;
          }
          else
            throw new Exception("Tabella " + tname + " colonna " + columnName
               + " auto incremento ma la tabella non ha una dichiarazione di idMethod.");
        }

        if(isPrimaryKey)
        {
          // conteggia i campi primary key
          if(ti.numColPrimary++ == 0)
          {
            ti.uniquePrimaryKeyColumn = column;
            ti.uniquePrimaryKeyColumnName = column.getAttributeValue("name");
            ti.uniquePrimaryKeyColumnType = column.getAttributeValue("type");
          }
        }
        else if(type.equals("INTEGER") && defval.isEmpty())
        {
          // aggiunge un default=0 a tutti i campi integer non primary key
          column.setAttribute("default", "0");
          vDefZero.add("ALTER TABLE ONLY " + tableName + " ALTER COLUMN " + columnName + " SET DEFAULT 0;");
          vSetZero.add("UPDATE " + tableName + " SET " + columnName + "=0 WHERE " + columnName + " IS NULL;");
        }

        if(!defval.isEmpty())
        {
          vDefZero.add("ALTER TABLE ONLY " + tableName + " ALTER COLUMN " + columnName + " SET DEFAULT " + defval + ";");
          vSetZero.add("UPDATE " + tableName + " SET " + columnName + "=" + defval + " WHERE " + columnName + " IS NULL;");
        }

        ti.numCol++;
      }

      // aggiunge campo stato rec se non presente
      if(!ti.haveStatoRec && addStatoRec && ti.numCol >= minColStatoRec)
      {
        Element srCol = new Element("column");
        srCol.setAttribute("name", "STATO_REC");
        srCol.setAttribute("type", "INTEGER");
        table.addContent(srCol);
      }

      if(ti.numColPrimary == 0)
        throw new Exception("La tabella " + tableName + " non ha una chiave primaria definita.");

      if(ti.numColPrimary > 1)
      {
        ti.haveMultiplePrimary = true;
        ti.uniquePrimaryKeyColumn = null;
        ti.uniquePrimaryKeyColumnName = null;
      }
      else if(forcePrimary)
      {
        String pkName = creaNomePrimaryKey(tableName);
        ti.uniquePrimaryKeyColumn.setAttribute("name", pkName);
      }

      if(forcePrimaryIDtoNative && !ti.haveIdMethod && ti.numColPrimary == 1 && isNumeric(ti.uniquePrimaryKeyColumnType))
      {
        table.setAttribute("idMethod", "native");
        ti.uniquePrimaryKeyColumn.setAttribute("autoIncrement", "true");
      }

      htInfo.put(tableName, ti);
    }

    // aggiusta nomi di foreignkey, indici
    for(Element table : eleTables)
    {
      String tableName = table.getAttributeValue("name");
      log.debug("Aggiusta nomi: " + tableName);

      // Iteratore sul tag foreign-key
      Iterator iterFk = table.getChildren("foreign-key", ns).iterator();
      while(iterFk.hasNext())
      {
        Element foreignKey = (Element) (iterFk.next());
        String foreignTableName = pVsu(foreignKey.getAttributeValue("foreignTable").toUpperCase());

        if(StringOper.isEqu(tableName, foreignTableName))
          throw new Exception("Tabella " + tableName + ": riferimento circolare a se stessa.");

        // recupera info sulla foreign table
        Element eleFTable = getTable(foreignTableName);
        if(eleFTable == null)
          throw new Exception("Tabella " + tableName + ": riferimento a tabella " + foreignTableName + " inesitente.");

        TableInfo ti = htInfo.get(foreignTableName);
        if(ti == null)
          throw new Exception("Impossibile recuperare le informazioni sulla tabella " + tableName);

        // imposta nome corretto della foreign table
        foreignTableName = eleFTable.getAttributeValue("name");
        foreignKey.setAttribute("foreignTable", foreignTableName);

        Iterator itRef = foreignKey.getChildren("reference", ns).iterator();
        while(itRef.hasNext())
        {
          Element ref = (Element) itRef.next();

          String local = chID(ref.getAttributeValue("local").toUpperCase());
          String foreign = chID(ref.getAttributeValue("foreign").toUpperCase());

          // forza la foreign al valore di chiave primaria della foreignTableName
          if(forceForeign && !ti.haveMultiplePrimary && !local.contains("UUID") && !foreign.contains("UUID"))
            foreign = ti.uniquePrimaryKeyColumnName;

          // modifica il campo locale usando il tipo di quello remoto
          checkTypesLocalForeign(table, eleFTable, local, foreign);

          ref.setAttribute("local", local);
          ref.setAttribute("foreign", foreign);
        }
      }

      int idxCounter = 1;
      String tname = tableName.replace('.', '_');

      // aggiustaggio tag index per inidici UNIQUE
      final List<Element> eleUniques = table.getChildren("unique", ns);
      for(Element unique : eleUniques)
      {
        UniqueInfo ui = new UniqueInfo();
        ui.tableName = tname;
        ui.constraintName = "IDX_" + tname + "_" + idxCounter;

        unique.setAttribute("name", ui.constraintName);
        idxCounter++;

        for(Element uniqueColumn : unique.getChildren("unique-column", ns))
        {
          String columnName = chID(StringOper.okStr(uniqueColumn.getAttributeValue("name").toUpperCase()));
          getColumn(table, columnName);
          uniqueColumn.setAttribute("name", columnName);
          ui.arColumns.add(columnName);
        }

        vUnique.add(ui);
      }

      // aggiustaggio tag index per inidici INDEX
      for(Element index : table.getChildren("index", ns))
      {
        index.setAttribute("name", "IDX_" + tname + "_" + idxCounter);
        idxCounter++;

        for(Element indexColumn : index.getChildren("index-column", ns))
        {
          String columnName = chID(StringOper.okStr(indexColumn.getAttributeValue("name").toUpperCase()));
          getColumn(table, columnName);
          indexColumn.setAttribute("name", columnName);
        }
      }

      // nomi sequenza per postgresql
      if(usePostgresql)
      {
        TableInfo ti = htInfo.get(tableName);
        if(!ti.haveMultiplePrimary && "native".equalsIgnoreCase(ti.idMethod))
        {
          // inserisce o aggiorna     <id-method-parameter name="seqName" value="APP.CODA_CODA_ID_SEQ"/>
          Element eidm = table.getChild("id-method-parameter", ns);
          if(eidm == null)
          {
            eidm = new Element("id-method-parameter", ns);
            table.addContent(eidm);
          }

          String seqName = tableName + "_" + ti.uniquePrimaryKeyColumnName + "_SEQ";
          if(seqName.length() > 26)
            log.debug("Possibile incompatibilita vecchie versioni: nome sequenza " + seqName + " maggiore di 26 caratteri.");

          eidm.setAttribute("name", "seqName");
          eidm.setAttribute("value", seqName.toUpperCase());
        }
      }
    }
  }

  /**
   * Rintraccia la tabella ignorando eventualmente il nome
   * dello schema SQL che la contiente.
   * @param tblName
   * @return
   */
  public Element getTable(String tblName)
  {
    Element eleFTable = (Element) htElemTables.get(tblName);
    if(eleFTable != null)
      return eleFTable;

    return (Element) htNameTables.get(tblName);
  }

  /**
   * Cambia la colonna locale uniformandola al tipo della colonna remota.
   * @param tLocal
   * @param tForeing
   * @param cnameLocal
   * @param cnameForeign
   * @throws Exception
   */
  public void checkTypesLocalForeign(Element tLocal, Element tForeing, String cnameLocal, String cnameForeign)
     throws Exception
  {
    Element colLocal = getColumn(tLocal, cnameLocal);
    Element colForeign = getColumn(tForeing, cnameForeign);

    String ltype = colLocal.getAttributeValue("type");
    String ftype = colForeign.getAttributeValue("type");

    if(!StringOper.isEqu(ltype, ftype))
      throw new Exception(String.format("I tipi delle colonne %s (%s) -> %s (%s) sono differenti.",
         cnameLocal, ltype, cnameForeign, ftype));

    if(ltype.equalsIgnoreCase("VARCHAR") || ltype.equalsIgnoreCase("CHAR"))
    {
      String lsize = colLocal.getAttributeValue("size");
      String fsize = colForeign.getAttributeValue("size");

      if(!StringOper.isEqu(lsize, fsize))
        throw new Exception(String.format("Dimensioni CHAR/VARCHAR delle colonne %s (%s) -> %s (%s) sono differenti.",
           cnameLocal, lsize, cnameForeign, fsize));
    }
  }

  /**
   * Modifica i nomi delle chiavi primarie delle tabelle in autoincremento
   * portandoli alla definizione standard NOMETABELLA_ID.
   * Tutti i riferimenti secondari (foreign_keys, ecc.) sono aggiornati.
   * @throws Exception
   */
  public void adjustPrimaryKeys()
     throws Exception
  {
    HashMap<String, String> htCambiamenti = new HashMap<>();

    List<Element> lsTables = doc.getRootElement().getChildren("table", ns);

    for(Element table : lsTables)
    {
      String tableName = table.getAttributeValue("name");
      log.debug("NOME TABELLA: " + table.getAttributeValue("name"));

      TableInfo ti = htInfo.get(tableName);
      if(ti == null)
        throw new Exception("Impossibile recuperare le informazioni sulla tabella " + tableName);

      // Iteratore sul tag column
      boolean isStatoRec = false;
      Iterator itertab = table.getChildren("column", ns).iterator();
      while(itertab.hasNext())
      {
        Element column = (Element) (itertab.next());

        boolean isAutoIncrement = false;
        boolean isPrimaryKey = false;

        String s;
        if((s = column.getAttributeValue("autoIncrement")) != null)
          isAutoIncrement = StringOper.checkTrueFalse(s.trim(), false);
        if((s = column.getAttributeValue("primaryKey")) != null)
          isPrimaryKey = StringOper.checkTrueFalse(s.trim(), false);

        boolean attributoDaCambiare = isAutoIncrement && isPrimaryKey && !ti.haveMultiplePrimary;

        if(attributoDaCambiare)
        {
          String prevName = column.getAttributeValue("name").toUpperCase();
          String columnName = creaNomePrimaryKey(tableName).toUpperCase();
          if(columnName != null && !StringOper.isEqu(prevName, columnName))
          {
            column.setAttribute("name", columnName);
            if(ti.uniquePrimaryKeyColumn != null)
            {
              ti.uniquePrimaryKeyColumn.setAttribute("name", columnName);
              ti.uniquePrimaryKeyColumnName = columnName;
            }
            htCambiamenti.put(tableName + "/" + prevName, columnName);
          }
        }

        String nomeColonna = column.getAttributeValue("name");
        if(nomeColonna.equalsIgnoreCase("STATO_REC"))
          isStatoRec = true;
      }

      if(!isStatoRec)
        vStatoRec.add(tableName);
    }

    if(htCambiamenti.isEmpty())
      return;

    for(Element table : lsTables)
    {
      List<Element> lsForeigns = table.getChildren("foreign-key", ns);

      for(Element foreignKey : lsForeigns)
      {
        String tableNameForeign = foreignKey.getAttributeValue("foreignTable");

        List<Element> childList = foreignKey.getChildren("reference", ns);
        if(childList.size() == 1)
        {
          Element ref = childList.get(0);
          String prevNameForeign = ref.getAttributeValue("foreign").toUpperCase();

          String columnNameForeign;
          if((columnNameForeign = htCambiamenti.get(tableNameForeign + "/" + prevNameForeign)) != null)
          {
            ref.setAttribute("foreign", columnNameForeign.toUpperCase());
          }
        }
      }
    }
  }

  /**
   * Corregge la descrizione.
   * Rimuove gli apostrofi e filtra caratteri strani.
   * @param descr descrizione da correggere
   * @return descrizione corretta
   * @throws Exception
   */
  public String adjDescrizione(String descr)
     throws Exception
  {
    descr = descr.replaceAll("\\'\\'", "'");
    return StringOper.CvtSQLstring(descr);
  }

  /**
   * Ordina le tabelle in ordine alfabetico.
   * @throws Exception
   */
  public void sortTables()
     throws Exception
  {
    // costruisce un array dei nomi
    Element database = doc.getRootElement();
    String[] arTables = (String[]) htElemTables.keySet().toArray(new String[htElemTables.size()]);

    // ordina i nomi tabella in ordine alfabetico crescente
    Arrays.sort(arTables, (String o1, String o2) -> StringOper.compare(o1, o2));

    // rimuove le tabelle e le riaccoda nell'ordine corretto
    Comparator cmp = new TableItemComparator();
    database.removeChildren("table");
    for(int j = 0; j < arTables.length; j++)
    {
      String tableName = arTables[j];
      Element table = (Element) htElemTables.get(tableName);
      sortElements(table, cmp);
      database.removeContent(table);
      database.addContent(table);
    }
  }

  protected static class TableItemComparator implements Comparator
  {
    @Override
    public int compare(Object o1, Object o2)
    {
      return ((Element) o1).getName().compareTo(((Element) o2).getName());
    }
  }

  /**
   * <p>
   * Sorts the child elements, using the specified comparator.Any other intervening content (Text, Comments, etc.) are
   * not moved.(Note that this means that the elements will now be in a different
   * order with respect to any comments, which may cause a problem
   * if the comments describe the elements.)</p>
   * <p>
   * This method overcomes two problems with the standard Collections.sort():
   * <ul>
   * <li>Collections.sort() doesn't bother to remove an item from its old
   * location before placing it in its new location, which causes JDOM to
   * complain that the item has been added twice.
   * <li>This method will sort the child Elements without moving other
   * content, such as formatting text nodes (newlines, indents, etc.)
   * Otherwise, all the formatting whitespace would move to the beginning
   * or end of the content list.
   * </ul>
   * </p>
   * @param parent
   * @param c
   */
  public static void sortElements(Element parent, Comparator c)
  {
    // Create a new, static list of child elements, and sort it.
    List children = new ArrayList(parent.getChildren());
    Collections.sort(children, c);
    ListIterator childrenIter = children.listIterator();

    // Create a new, static list of all content items.
    List content = new ArrayList(parent.getContent());
    ListIterator contentIter = content.listIterator();

    // Loop through the content items, and whenever we find an Element,
    // we'll insert the next ordered Element in its place. Because the
    // content list is not live, it won't complain about an Element being
    // added twice.
    while(contentIter.hasNext())
    {
      Object obj = contentIter.next();
      if(obj instanceof Element)
        contentIter.set(childrenIter.next());
    }

    // Finally, we set the content list back into the parent Element.
    parent.setContent(content);
  }

  /**
   * Genera un nome colonna Primary Key coerente con le regole.
   * @param nomeTabella
   * @return
   */
  public String creaNomePrimaryKey(String nomeTabella)
  {
    String columnName = nomeTabella;
    if(!columnName.endsWith("_ID"))
    {
      int pos = columnName.indexOf('.');
      if(pos == -1)
        pos = columnName.indexOf('_');
      if(pos != -1)
        columnName = columnName.substring(pos + 1);

      columnName += "_ID";
    }
    return columnName;
  }

  public String getNomeChiaveDaNomeTabella(String nomeTabella)
  {
    // cerca la tabella foreign
    Element table = getTable(nomeTabella);
    if(table == null)
      return null;

    String columnName = creaNomePrimaryKey(nomeTabella);

    // verifica l'effettiva esistenza della colonna nella tabella foreign
    Iterator iterColumn = (table.getChildren("column", ns)).iterator();
    while(iterColumn.hasNext())
    {
      Element column = (Element) (iterColumn.next());
      String nomeCol = column.getAttributeValue("name");
      if(nomeCol.equals(columnName))
        return columnName;
    }

    return null;
  }

  public Element getColumn(String tabella, String colonna)
     throws Exception
  {
    Element table = getTable(tabella);
    if(table == null)
      throw new Exception("Tabella " + tabella + " inesistente.");

    return getColumn(table, colonna);
  }

  public Element getColumn(Element table, String colonna)
     throws Exception
  {
    String nomeTable = table.getAttributeValue("name");
    Iterator iterColumn = (table.getChildren("column", ns)).iterator();
    while(iterColumn.hasNext())
    {
      Element column = (Element) (iterColumn.next());
      String nomeCol = column.getAttributeValue("name");
      if(nomeCol.equals(colonna))
        return column;
    }
    throw new Exception("Colonna " + nomeTable + "." + colonna + " inesistente.");
  }

  /**
   * Verifica per tipo numerico.
   *
   * @param sqlType tipo sql (Types)
   * @return vero se la colonna è numerica
   */
  public boolean isNumeric(String sqlType)
  {
    switch(sqlType)
    {
      case "BIT":
      case "TINYINT":
      case "SMALLINT":
      case "INTEGER":
      case "BIGINT":
      case "FLOAT":
      case "REAL":
      case "DOUBLE":
      case "NUMERIC":
      case "DECIMAL":
        return true;
    }

    return false;
  }

  /**
   * Produce la stampa del documento XML.
   * @param docOutput
   * @throws Exception
   */
  public void print(Document docOutput)
     throws Exception
  {
    // output del documento
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat(Format.getPrettyFormat());

    if(ouputFile != null)
    {
      try (OutputStreamWriter out = new FileWriter(ouputFile))
      {
        xout.output(docOutput, out);
        out.flush();
      }
    }
    else
    {
      xout.output(docOutput, System.out);
    }

    if(infoFile != null)
    {
      try (OutputStreamWriter out = new FileWriter(infoFile))
      {

        out.write("<!--\nTabelle senza STATO_REC:\n");
        for(int i = 0; i < vStatoRec.size(); i++)
        {
          String string = (String) vStatoRec.get(i);
          out.write(string + "\n");
        }

        // costruisce array con i nomi degli schemi
        String[] schemaArray = htSchemas.keySet().stream()
           .sorted().toArray(String[]::new);

        out.write("==== SCHEMI ====\n");
        for(int j = 0; j < schemaArray.length; j++)
        {
          String sname = schemaArray[j];

          out.write(
             "CREATE SCHEMA " + sname + ";\n"
             + "GRANT ALL ON SCHEMA " + sname + " TO pcsdm;\n"
             + "GRANT ALL ON SCHEMA " + sname + " TO public;\n"
             + "COMMENT ON SCHEMA " + sname + " IS 'Inserire descrizione'\n");
        }

        out.write("==== INDICI UNIVOCI ====\n");
        for(int j = 0; j < vUnique.size(); j++)
        {
          UniqueInfo ui = vUnique.get(j);
          out.write(
             "ALTER TABLE " + ui.tableName
             + " ADD CONSTRAINT " + ui.constraintName
             + " UNIQUE(" + StringOper.join(ui.arColumns.iterator(), ',') + ");\n"
          );
        }

        for(int j = 0; j < vUnique.size(); j++)
        {
          UniqueInfo ui = vUnique.get(j);
          TableInfo ti = htInfo.get(ui.tableName);

          if(ti == null)
            continue;

          if(ti.haveMultiplePrimary)
          {
            out.write(
               "-- chiavi primarie multiple: la rimozione record duplicati è manuale\n"
               + "macro_createunique(" + ui.tableName
               + ";" + ui.constraintName
               + ";" + StringOper.join(ui.arColumns.iterator(), ',') + ");\n"
            );
          }
          else
          {
            out.write(
               "macro_createuniquepurge(" + ui.tableName
               + ";" + ti.uniquePrimaryKeyColumn.getAttributeValue("name")
               + ";" + ui.constraintName
               + ";" + StringOper.join(ui.arColumns.iterator(), ',') + ");\n"
            );
          }
        }

        // inserisce script per impostare i default zero per campi interi
        if(!vDefZero.isEmpty())
        {
          out.write("==== DEFAULT CAMPI INTERI ====\n");
          for(int i = 0; i < vDefZero.size(); i++)
          {
            out.write(vDefZero.get(i));
            out.write('\n');
          }
        }

        // inserisce script per impostare a zero i campi null interi
        if(!vSetZero.isEmpty())
        {
          out.write("==== IMPOSTA ZERO CAMPI INTERI ====\n");
          for(int i = 0; i < vSetZero.size(); i++)
          {
            out.write(vSetZero.get(i));
            out.write('\n');
          }
        }

        out.write("-->\n");
        out.flush();
      }
    }
  }

  /**
   * Esegue la seguenza di operazioni di trasformazione.
   * @throws Exception
   */
  public void run()
     throws Exception
  {
    if(descFile != null)
      readTableDescription();

    // carica XML in memoria
    parseXml();

    ns = doc.getRootElement().getNamespace();

    // aggiusta nomi tabelle
    adjustNames();

    // aggiusta nomi delle chiavi primarie
    adjustPrimaryKeys();

    // ordina le tabelle
    sortTables();
  }

  public static void configuraLog4j2()
  {
    ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

    AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
    builder.add(console);

    FilterComponentBuilder flow = builder.newFilter("MarkerFilter", Filter.Result.ACCEPT, Filter.Result.DENY);
    flow.addAttribute("marker", "FLOW");
    console.add(flow);

    LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
    standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");
    console.add(standard);

    RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ALL);
    rootLogger.add(builder.newAppenderRef("stdout"));
    builder.add(rootLogger);

    Configurator.initialize(builder.build());
    System.out.println("Log4j2 configurato per output su console.");
  }

  public static void main(String[] args)
  {
    try
    {
      configuraLog4j2();
      AbstractAdjustSchema gl = new AbstractAdjustSchema();

      int c;
      LongOptExt[] longopts = new LongOptExt[]
      {
        new LongOptExt("help", LongOpt.NO_ARGUMENT, null, 'h', "show usage"),
        new LongOptExt("outputfile", LongOpt.REQUIRED_ARGUMENT, null, 'o', "the output file"),
        new LongOptExt("schema", LongOpt.NO_ARGUMENT, null, 's', "force use schema for table"),
        new LongOptExt("dtd", LongOpt.REQUIRED_ARGUMENT, null, 'd', "set directory for DTD"),
        new LongOptExt("stato-rec", LongOpt.NO_ARGUMENT, null, 'R', "add STATO_REC to tables"),
        new LongOptExt("force-primary", LongOpt.NO_ARGUMENT, null, 'p', "force rewrite of primary key"),
        new LongOptExt("force-foreign", LongOpt.NO_ARGUMENT, null, 'f', "force rewrite of foreign key"),
        new LongOptExt("descfile", LongOpt.REQUIRED_ARGUMENT, null, 'D', "read table description from file"),
        new LongOptExt("info", LongOpt.REQUIRED_ARGUMENT, null, 'I', "output info file"),
        new LongOptExt("postgres", LongOpt.NO_ARGUMENT, null, 'P', "PostgreSQL optimization"),
      };

      Getopt g = new Getopt("ADJUSTSCHEMA", args, LongOptExt.getOptstring(longopts), longopts);
      g.setOpterr(false); // We'll do our own error handling

      while((c = g.getopt()) != -1)
      {
        switch(c)
        {
          case 'd':
            gl.dtdDir = new File(g.getOptarg());
            if(!gl.dtdDir.exists())
            {
              System.err.println("La directory " + gl.dtdDir.getAbsolutePath() + " non esiste.");
              System.exit(-1);
            }

            if(!gl.dtdDir.isDirectory())
            {
              System.err.println(gl.dtdDir.getAbsolutePath() + " esiste ma non e' una directory.");
              System.exit(-1);
            }

            break;
          case 'h':
            log.debug(
               "[-s] [-o outputfile] inputfile");
            for(LongOptExt o : longopts)
              log.debug(o.getHelpMsg());
            return;

          case 's':
            gl.pointVsUndScore = true;
            break;

          case 'o':
            gl.ouputFile = g.getOptarg();
            break;

          case 'R':
            gl.addStatoRec = true;
            break;

          case 'p':
            gl.forcePrimary = true;
            break;

          case 'f':
            gl.forceForeign = true;
            break;

          case 'P':
            gl.usePostgresql = true;
            break;

          case 'D':
            gl.descFile = g.getOptarg();
            break;

          case 'I':
            gl.infoFile = g.getOptarg();
            break;
        }
      }

      if(g.getOptind() < args.length)
        gl.inputFile = args[g.getOptind()];

      gl.run();
      gl.print(gl.doc);
    }
    catch(Exception ex)
    {
      log.error("", ex);
    }
  }
}
