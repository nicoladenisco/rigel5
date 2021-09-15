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
package org.rigel5.glue.pager;

import java.util.*;
import org.apache.torque.criteria.Criteria;
import org.rigel5.table.peer.html.PeerPager;
import org.rigel5.table.peer.html.PeerWrapperListaHtml;

/**
 * Paginatore di liste Peer per sola visualizzazione.
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerPagerAppMaint extends PeerPager
{
  private PeerWrapperListaHtml wl = null;

  public PeerPagerAppMaint()
  {
  }

  @Override
  public List getRecords(Criteria c)
     throws Exception
  {
    return wl.getRecords(c);
  }

  public PeerWrapperListaHtml getWl()
  {
    return wl;
  }

  public void setWl(PeerWrapperListaHtml wl)
  {
    this.wl = wl;

    // imposta numero per pagina
    if(wl.getNumPerPage() != 0)
      limit = wl.getNumPerPage();
  }
}
