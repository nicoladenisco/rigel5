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
package org.rigel5.table.peer.html;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;
import org.apache.torque.criteria.Criteria;
import org.apache.torque.criteria.SqlEnum;
import org.apache.torque.sql.OrderBy;
import org.apache.torque.util.UniqueList;
import org.rigel5.db.torque.PeerTransactAgent;
import org.rigel5.table.FiltroListe;
import org.rigel5.table.html.CommonPager;
import org.rigel5.table.html.RigelHtmlPage;

/**
 * Description: Paginatore di liste di peer.
 * <p>
 * Questo oggetto funge da coordinatore fra un oggetto PeerTableModel
 * ed un oggetto hTable per consentire la visualizzazione paginata dei
 * dati su una tabella di database.
 * </p>
 * <pre>
 * Modo d'uso:
 *
 * private PeerPager ble = new PeerPager();
 * private AlternateColorTable table = new AlternateColorTable();
 * private PeerTableModel ptm = new PeerTableModel();
 * ...
 * ...
 * ...
 * ptm.addColumn("CODICE", "cliforid", 100);
 * ptm.addColumn("DESCRIZIONE", "descrizione", 300, true);
 * ptm.init(new Clifor());
 * table.setModel(ptm);
 * ble.setHTable(table);
 * ble.setBaseSelfUrl("ListaClienti1.jsp");
 * ...
 * ...
 * ...
 * public String getHtml(HttpServletRequest request, HttpSession sessione)
 *    throws Exception
 * {
 *    return ble.getHtml(SU.getParMap(request), sessione);
 * }
 * </pre>
 * @author Nicola De Nisco
 * @version 1.0
 */
public class PeerPager extends CommonPager
{
  private static int idCounter = 0;
  protected Method doSelectM = null;

  public PeerPager()
  {
    super("PeerListaElem" + (idCounter++));
  }

  public PeerPager(String id)
  {
    super(id);
  }

  /**
   * Ritorna l'html per la pagina richiesta.
   * Dal db vengono prelevati solo i record necessari.
   * @param page the value of page
   * @throws java.lang.Exception
   */
  @Override
  protected void getHtmlTable(RigelHtmlPage page)
     throws Exception
  {
    cSelezione.setOffset((int) start);
    cSelezione.setLimit((int) limit);
    cSelezione.setIgnoreCase(true);

    List v = getRecords(cSelezione);
    if(!v.isEmpty())
    {
      ((PeerTableModel) (getTableModel())).init(v);
      ((PeerTableModel) (getTableModel())).attach(table);
      table.doHtml(page);
    }
  }

  /**
   * Ritorna il gruppo di record (sotto forma di oggetti OM)
   * da visualizzare nella pagina
   * in base la filtro richiesto.
   * @param fl filtro per la selezione dei record
   * @return vettore dei record (oggetti OM)
   * @throws Exception
   */
  public List getRecords(FiltroListe fl)
     throws Exception
  {
    Criteria c;
    if(fl.getOggFiltro() == null)
      c = new Criteria();
    else
      c = (Criteria) (((Criteria) (fl.getOggFiltro())).clone());

    c.setLimit(fl.getLimit());
    c.setOffset(fl.getOffset());
    c.setIgnoreCase(fl.isIgnoreCase());
    return getRecords(c);
  }

  /**
   * Restituisce i records del database secondo il FiltroListe fornito.
   * Questo metodo è utilizzata quando questa classe è usata
   * senza un wrapper che la ingloba. Nella sua derivata PeerPagerAppMaint
   * che usa un wrapper la getRecords è ridefinita per utilizzare quella del wrapper.
   * @param c Critria per la selezione dei record
   * @return vettore dei record (oggetti OM)
   * @throws Exception
   */
  public List getRecords(Criteria c)
     throws Exception
  {
    PeerTableModel ptm = (PeerTableModel) getTableModel();

    // estrae metodo doSelect dalla class del peer (es.: MagartPeer)
    if(doSelectM == null)
      doSelectM = ptm.getBeanPeerClass().getMethod("doSelect", Criteria.class, Connection.class);

    addExtraFilter(c);

    // usa ordinamento di default per chiave primaria
    if((c.getOrderByColumns() == null || c.getOrderByColumns().isEmpty()) && ptm.getDefaultOrderCriteria() != null)
    {
      // copia le colonne di ordinamento dal criteria di default a quello corrente
      UniqueList<OrderBy> oc = ptm.getDefaultOrderCriteria().getOrderByColumns();
      for(OrderBy ob : oc)
      {
        if(ob.getOrder().equals(SqlEnum.ASC))
          c.addAscendingOrderByColumn(ob.getColumn());
        else
          c.addDescendingOrderByColumn(ob.getColumn());
      }
    }

    return (List) PeerTransactAgent.executeReturnReadonly((con) -> doSelectM.invoke(null, c, con));
  }

  @Override
  public long getTotalRecords()
     throws Exception
  {
    Criteria c;
    if(cSelezione.getOggFiltro() == null)
      c = new Criteria();
    else
      c = (Criteria) (((Criteria) (cSelezione.getOggFiltro())).clone());

    addExtraFilter(c);

    if(c.getTopLevelCriterion() == null)
      return getTableModel().getTotalRecords();

    return ((PeerTableModel) (getTableModel())).getTotalRecords(c);
  }

  /**
   * Imposta dei filtri supplementari sul criterio di selezione.
   * Puo' essere ridefinita per implementare dei filtri ulteriori o
   * un ordinamento di default.
   * <code>
   * private PeerPager bleCli = new PeerPager("Clifor")
   * {
   * protected void addExtraFilter(Criteria c)
   * throws Exception
   * {
   * if(!isFiltro())
   * c.addAscendingOrderByColumn(CliforPeer.DESCRIZIONE);
   *
   * if(isAgente)
   * {
   * String agente = getUser(session).getFirstName();
   * c.add(CliforPeer.ID_AGENTE1, agente);
   * }
   * }
   * };
   * </code>
   * @param c Criterio di selezione
   * @throws Exception
   */
  protected void addExtraFilter(Criteria c)
     throws Exception
  {
  }
}
