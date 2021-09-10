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
package org.rigel2.db;

import com.workingdogs.village.DataSetException;
import com.workingdogs.village.QueryDataSet;
import com.workingdogs.village.Record;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.torque.TorqueException;
import org.apache.torque.criteria.CriteriaInterface;
import org.apache.torque.om.mapper.RecordMapper;

/**
 * Adattaore generico per la creazione di record village.
 *
 * @author Nicola De Nisco
 */
public class VillageRecordMapper implements RecordMapper<Record>
{
  private QueryDataSet qds;

  @Override
  public Record processRow(ResultSet resultSet, int rowOffset, CriteriaInterface<?> criteria)
     throws TorqueException
  {
    try
    {
      if(qds == null)
        qds = new QueryDataSet(resultSet);

      return new Record(qds);
    }
    catch(SQLException | DataSetException ex)
    {
      throw new TorqueException(ex);
    }
  }
}
