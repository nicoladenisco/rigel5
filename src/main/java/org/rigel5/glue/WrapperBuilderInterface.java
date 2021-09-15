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
package org.rigel5.glue;

import org.rigel5.table.peer.html.PeerWrapperEditHtml;
import org.rigel5.table.peer.html.PeerWrapperFormHtml;
import org.rigel5.table.peer.html.PeerWrapperListaHtml;
import org.rigel5.table.peer.xml.PeerWrapperListaXml;
import org.rigel5.table.sql.html.SqlWrapperFormHtml;
import org.rigel5.table.sql.html.SqlWrapperListaHtml;
import org.rigel5.table.sql.xml.SqlWrapperListaXml;

/**
 * Interfaccia per definire un generatore di Wrapper.
 * Questa interfaccia viene implemetata nell'applicazione ospite
 * per creare un generatore di wrapper utilizzato dalla cache
 * dei wrapper.
 *
 * @author Nicola De Nisco
 */
public interface WrapperBuilderInterface
{
  /**
   * Ritorna una lista basata su oggetti Peer (Torque).
   * @param nomeLista nome della lista (sezione sul file XML)
   * @return wrapper inizializzato con la lista richiesta.
   * @throws Exception
   */
  public PeerWrapperListaHtml getListaPeer(String nomeLista) throws Exception;

  /**
   * Ritorna una lista di edit basata su oggetti Peer (Torque).
   * @param nomeLista nome della lista (sezione sul file XML)
   * @return wrapper inizializzato con la lista richiesta.
   * @throws Exception
   */
  public PeerWrapperEditHtml getListaEditPeer(String nomeLista) throws Exception;

  /**
   * Ritorna un form basato su query libere (SQL).
   * @param nomeForm nome del form (sezione sul file XML)
   * @return wrapper inizializzato con il form richiesto.
   * @throws Exception
   */
  public PeerWrapperFormHtml getFormPeer(String nomeForm) throws Exception;

  /**
   * Ritorna una lista specializzata per la generazione di XML.
   * @param nomeLista nome della lista (sezione sul file XML)
   * @return wrapper inizializzato con la lista richiesta.
   * @throws Exception
   */
  public PeerWrapperListaXml getListaXmlPeer(String nomeLista) throws Exception;

  /**
   * Ritorna una lista basata su query libera (SQL).
   * @param nomeLista nome della lista (sezione sul file XML)
   * @return wrapper inizializzato con la lista richiesta.
   * @throws Exception
   */
  public SqlWrapperListaHtml getListaSql(String nomeLista) throws Exception;

  /**
   * Ritorna un form basato su query libere (SQL).
   * @param nomeForm nome del form (sezione sul file XML)
   * @return wrapper inizializzato con il form richiesto.
   * @throws Exception
   */
  public SqlWrapperFormHtml getFormSql(String nomeForm) throws Exception;

  /**
   * Ritorna una lista specializzata per la generazione di XML.
   * @param nomeLista nome della lista (sezione sul file XML)
   * @return wrapper inizializzato con la lista richiesta.
   * @throws Exception
   */
  public SqlWrapperListaXml getListaXmlSql(String nomeLista) throws Exception;

  /**
   * Ritorna una lista basata sulla tablemap (Torque).
   * @param nomeTabella nome della tabella di cui generare la lista.
   * @return wrapper inizializzato con la lista richiesta.
   * @throws Exception
   */
  public PeerWrapperListaHtml getListaTmap(String nomeTabella) throws Exception;

  /**
   * Ritorna una lista di edit basata sulla tablemap (Torque).
   * @param nomeTabella nome della tabella di cui generare la lista.
   * @return wrapper inizializzato con la lista richiesta.
   * @throws Exception
   */
  public PeerWrapperEditHtml getListaEditTmap(String nomeTabella) throws Exception;

  /**
   * Ritorna un form basato sulla tablemap (Torque).
   * @param nomeTabella nome della tabella di cui generare il form.
   * @return wrapper inizializzato con il form richiesto.
   * @throws Exception
   */
  public PeerWrapperFormHtml getFormTmap(String nomeTabella) throws Exception;
}
