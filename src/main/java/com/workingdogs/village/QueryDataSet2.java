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
package com.workingdogs.village;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Evoluzione query data set.
 *
 * @author Nicola De Nisco
 */
public class QueryDataSet2 extends QueryDataSet
{
  public QueryDataSet2()
     throws SQLException, DataSetException
  {
  }

  public QueryDataSet2(Connection conn, String selectStmt)
     throws SQLException, DataSetException
  {
    super(conn, selectStmt);
  }

  public QueryDataSet2(ResultSet resultSet)
     throws SQLException, DataSetException
  {
    super(resultSet);
  }

  /**
   * Returns numberOfResults records in a QueryDataSet as a List
   * of Record objects. Starting at record start. Used for
   * functionality like util.LargeSelect.
   *
   * @param start The index from which to start retrieving
   * <code>Record</code> objects from the data set.
   * @param numberOfResults The number of results to return (or
   * <code> -1</code> for all results).
   * @return A <code>List</code> of <code>Record</code> objects.
   * @exception Exception
   */
  public List<Record> getSelectResults(int start, int numberOfResults)
     throws Exception
  {
    List<Record> results = null;

    if(numberOfResults < 0)
    {
      results = new ArrayList<>();
      fetchRecords();
    }
    else
    {
      results = new ArrayList<>(numberOfResults);
      fetchRecords(start, numberOfResults);
    }

    int startRecord = 0;

    //Offset the correct number of records
    if(start > 0 && numberOfResults <= 0)
    {
      startRecord = start;
    }

    // Return a List of Record objects.
    for(int i = startRecord; i < size(); i++)
    {
      Record rec = getRecord(i);
      results.add(rec);
    }

    return results;
  }

  public static List<Record> fetchAllRecords(Connection dbCon, String sSQL)
     throws Exception
  {
    try (QueryDataSet2 qs = new QueryDataSet2(dbCon, sSQL))
    {
      return qs.fetchAllRecords();
    }
  }
}
