/*
 * Copyright (C) 2022 Nicola De Nisco
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
package com.workingdogs.village;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.commonlib5.utils.OsIdent;

/**
 * Gestione di un piccolo db in Derby per eseguire i test.
 *
 * @author Nicola De Nisco
 */
public class DerbyTestEnvironment implements Closeable
{
  private File temp, dbFile;
  private String connectionURL;
  private Connection conn;
  private static final DerbyTestEnvironment theInstance = new DerbyTestEnvironment();

  private DerbyTestEnvironment()
  {
  }

  public static DerbyTestEnvironment getInstance()
  {
    return theInstance;
  }

  public boolean isOpen()
  {
    return conn != null;
  }

  public void open()
     throws ClassNotFoundException, SQLException
  {
    temp = OsIdent.getSystemTemp();
    dbFile = new File(temp, "rigel5test.derby");
    Logger.getLogger(DerbyTestEnvironment.class.getName()).log(Level.INFO,
       "Database di test creato in " + dbFile.getAbsolutePath());

    Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
    connectionURL = "jdbc:derby:" + dbFile.getAbsolutePath() + ";create=true";
    conn = DriverManager.getConnection(connectionURL);
    createTestDatabase();
  }

  @Override
  public void close()
     throws IOException
  {
    try
    {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    }
    catch(SQLException ex)
    {
      if(ex.getMessage().equals("Derby system shutdown."))
        return;

      throw new IOException(ex);
    }
  }

  public void createTestDatabase()
     throws SQLException
  {
    String sSQL1
       = "CREATE TABLE mic_batteri\n"
       + "(\n"
       + "    idbatteri integer NOT NULL,\n"
       + "    codice character varying(10) ,\n"
       + "    descrizione character varying(255) ,\n"
       + "    tiporecord integer DEFAULT 0,\n"
       + "    stato_rec integer DEFAULT 0,\n"
       + "    id_user integer DEFAULT 0,\n"
       + "    id_ucrea integer DEFAULT 0,\n"
       + "    ult_modif timestamp,\n"
       + "    creazione timestamp,\n"
       + "    codiceregionale character varying(50) ,\n"
       + "    CONSTRAINT mic_batteri_pkey PRIMARY KEY (idbatteri),\n"
       + "    CONSTRAINT idx_mic_batteri_1 UNIQUE (codice)\n"
       + ")";

    executeStatement(sSQL1);

    String sSQL2
       = "CREATE TABLE mic_antibiotici\n"
       + "(\n"
       + "    idantibiotici integer NOT NULL,\n"
       + "    codice character varying(50) ,\n"
       + "    descrizione character varying(255) ,\n"
       + "    micdose1 character varying(50) ,\n"
       + "    micdose2 character varying(50) ,\n"
       + "    farmaco character varying(50) ,\n"
       + "    tiporecord integer DEFAULT 0,\n"
       + "    stato_rec integer DEFAULT 0,\n"
       + "    id_user integer DEFAULT 0,\n"
       + "    id_ucrea integer DEFAULT 0,\n"
       + "    ult_modif timestamp,\n"
       + "    creazione timestamp,\n"
       + "    codiceregionale character varying(50) ,\n"
       + "    CONSTRAINT mic_antibiotici_pkey PRIMARY KEY (idantibiotici),\n"
       + "    CONSTRAINT idx_mic_antibiotici_1 UNIQUE (codice)\n"
       + ")";

    executeStatement(sSQL2);

    executeStatement("CREATE SCHEMA stp");

    String sSQL3
       = "CREATE TABLE stp.transcode\n"
       + "(\n"
       + "    app character varying(16)  NOT NULL,\n"
       + "    tipo character varying(16)  NOT NULL,\n"
       + "    codice_caleido character varying(64)  NOT NULL,\n"
       + "    codice_app character varying(64)  NOT NULL,\n"
       + "    CONSTRAINT transcode_pkey PRIMARY KEY (app, tipo, codice_caleido)\n"
       + ")";

    executeStatement(sSQL3);

    String sSQL4
       = "CREATE INDEX idx_transcode_1\n"
       + "    ON stp.transcode (app, codice_app)\n"
       + "";

    executeStatement(sSQL4);

    String sSQL5
       = "CREATE TABLE tblPosts (\n"
       + "  nId INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,\n"
       + "  strContent VARCHAR(140) NOT NULL,\n"
       + "  strLink VARCHAR(200),\n"
       + "  strImage VARCHAR(200)\n"
       + ")";

    executeStatement(sSQL5);
  }

  public void executeStatement(String sSQL2)
     throws SQLException
  {
    try(Statement stm = conn.createStatement())
    {
      stm.executeUpdate(sSQL2);
    }
    catch(SQLException ex)
    {
      // ignora errore di tabella già esistente
      // Table/View 'MIC_BATTERI' already exists in Schema 'APP'.
      if(!ex.getMessage().contains("already exists"))
        throw ex;
    }
  }

  public String getConnectionURL()
  {
    return connectionURL;
  }

  public Connection getConn()
  {
    return conn;
  }
}
