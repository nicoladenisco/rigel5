/*
 * Copyright (C) 2026 Nicola De Nisco
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

import com.workingdogs.village.QueryDataSet;
import java.sql.Connection;
import java.sql.Statement;
import org.apache.torque.util.TorqueConnection;
import org.apache.torque.util.Transaction;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Test per PeerReadOnlyHelper.
 *
 * @author Nicola De Nisco
 */
public class PeerReadOnlyHelperTest
{
  public final TorqueTestHelper torque = new TorqueTestHelper();

  public PeerReadOnlyHelperTest()
  {
  }

  @BeforeClass
  public static void setUpClass()
  {
  }

  @AfterClass
  public static void tearDownClass()
  {
  }

  @Before
  public void setUp()
     throws Exception
  {
    torque.init();
    torque.checkAndBuildDb1();
  }

  @After
  public void tearDown()
     throws Exception
  {
    torque.shutdown();
  }

  public void test1()
     throws Exception
  {
    System.out.println("test1");
    try(PeerReadOnlyHelper instance = new PeerReadOnlyHelper())
    {
      Connection con = instance.getReadOnlyConnection();

      String sSQL
         = "UPDATE stp.transcode"
         + "   SET codice_caleido='AAAAAA'"
         + " WHERE codice_caleido='C0001'";
      try(Statement st = con.createStatement())
      {
        st.executeUpdate(sSQL);
      }
    }

    // verifica che il cambiamento al db non sia avvenuto
    try(TorqueConnection con = Transaction.begin())
    {
      String sSQL
         = "SELECT * FROM stp.transcode WHERE codice_caleido='AAAAAA'";

      assertEquals(0, QueryDataSet.fetchAllRecords(con, sSQL).size());
      //Transaction.commit(con);
    }
  }
}
