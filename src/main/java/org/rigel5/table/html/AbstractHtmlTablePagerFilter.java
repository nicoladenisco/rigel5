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
package org.rigel5.table.html;

import java.util.*;
import javax.servlet.http.*;
import org.commonlib5.utils.StringOper;
import org.rigel5.table.FiltroListe;
import org.rigel5.table.MascheraRicercaGenerica;
import org.rigel5.table.RigelColumnDescriptor;
import org.rigel5.table.RigelTableModel;

/**
 * <p>
 * Title: Paginatore per tabelle con filtro (ricerca avanzata).</p>
 * <p>
 * Description:
 * Implementazione standard di un paginatore per tabelle html.
 * Particolarmente indicato per tabelle molto lunghe, in quanto
 * recupera da database solo i record visualizzati nella pagina corrente.
 * Consente di implementare dei filtri molto flessibili sulla visualizzazione.
 * </p>
 *
 * @author Nicola De Nisco
 * @version 1.0
 */
abstract public class AbstractHtmlTablePagerFilter extends AbstractHtmlTablePager
{
  public static final int FILTRO_NORMALE = 0;
  public static final int FILTRO_MACHERA = 1;
  public static final int FILTRO_APPLICA = 2;
  public static final int FILTRO_ANNULLA = 3;
  protected String emptyHtml = "";
  protected int filtro = 0;
  protected FiltroListe cSelezione = null;
  protected MascheraRicercaGenerica mgr = null;
  private String preTable = "";
  private String postTable = "";
  private String idPager = "default";

  public AbstractHtmlTablePagerFilter(String id)
  {
    idPager = id;
    emptyHtml = "<table width=100%><tr><td bgcolor=white>"
       + i18n.msg("Nessun elemento trovato!")
       + "</td></tr></table>";
  }

  /**
   * Html da visualizzare quando nessun elemento viene selezionato dal filtro.
   *
   * @param newEmptyHtml codice HTML
   */
  public void setEmptyHtml(String newEmptyHtml)
  {
    emptyHtml = newEmptyHtml;
  }

  /**
   * Html da visualizzare quando nessun elemento viene selezionato dal filtro.
   *
   * @return codice HTML
   */
  public String getEmptyHtml()
  {
    return emptyHtml;
  }

  /**
   * HTML da visualizzare prima della tabella.
   *
   * @param newPreTable codice HTML
   */
  public void setPreTable(String newPreTable)
  {
    preTable = newPreTable;
  }

  /**
   * HTML da visualizzare prima della tabella.
   *
   * @return codice HTML
   */
  public String getPreTable()
  {
    return preTable;
  }

  /**
   * HTML da visualizzare dopo della tabella.
   *
   * @param newPostTable codice HTML
   */
  public void setPostTable(String newPostTable)
  {
    postTable = newPostTable;
  }

  /**
   * HTML da visualizzare dopo della tabella.
   *
   * @return codice HTML
   */
  public String getPostTable()
  {
    return postTable;
  }

  /**
   * Identificatore di istanza di paginatore.
   * Viene utilizzare per memorizzare una istanza dei dati filtro
   * all'interno dei dati di sessione.
   *
   * @param newIdPager stringa univoca per questo paginatore
   */
  public void setIdPager(String newIdPager)
  {
    idPager = newIdPager;
  }

  /**
   * Identificatore di istanza di paginatore.
   * Viene utilizzare per memorizzare una istanza dei dati filtro
   * all'interno dei dati di sessione.
   *
   * @return stringa univoca per questo paginatore
   */
  public String getIdPager()
  {
    return idPager;
  }

  public MascheraRicercaGenerica getMascheraRicerca()
  {
    return mgr;
  }

  public void setMascheraRicerca(MascheraRicercaGenerica mgr)
  {
    this.mgr = mgr;
  }

  public RigelTableModel getTableModel()
     throws Exception
  {
    if(table == null)
      throw new Exception(i18n.msg("Tabella e' null: tabella non inizializzata correttamente (setHTable)."));

    if(table.getModel() == null)
      throw new Exception(i18n.msg("TableModel e' null: tabella non inizializzata correttamente (setModel oppure attach)."));

    if(table.getModel() instanceof RigelTableModel)
      return ((RigelTableModel) (table.getModel()));

    throw new Exception(i18n.msg("Questa classe puo' essere utilizzata solo all'interno del rigel."));
  }

  @Override
  public long getTotalRecords()
     throws Exception
  {
    return getTableModel().getTotalRecords(cSelezione);
  }

  /**
   * Ritorna l'html per la pagina richiesta.
   * Dal db vengono prelevati solo i record necessari.
   * @param page the value of page
   * @throws java.lang.Exception
   */
  @Override
  abstract protected void getHtmlTable(RigelHtmlPage page)
     throws Exception;

  /**
   * In base alle impostazioni utente crea il FiltroListe con
   * all'interno gli opportuni filtri necessari.
   * @param params
   * @return
   * @throws java.lang.Exception
   */
  protected FiltroListe creaFiltro(Map params)
     throws Exception
  {
    if(!getTableModel().isInitalized())
      throw new Exception(i18n.msg("Oggetto table model non inizializzato."));

    if(mgr == null)
      mgr = getTableModel().getMascheraRG(i18n);

    FiltroListe fl = new FiltroListe();
    fl.setOggFiltro(mgr.buildCriteriaSafe(params));
    fl.salvaInfoColonne(getTableModel());
    return fl;
  }

  /**
   * Funzione generale per la restituzione della pagina html.
   * Dai parametri recupera il tipo di funzione richiesta (filtro)
   * e il record di start della visualizzazione (start).
   * Dalla sessione recupera il FiltroListe con i filtri impostati.
   * @param params the value of params
   * @param sessione the value of sessione
   * @param page the value of page
   * @throws java.lang.Exception
   */
  @Override
  public synchronized void getHtml(Map params, HttpSession sessione, RigelHtmlPage page)
     throws Exception
  {
    // imposta il nome del form all'interno del tablemodel
    getTableModel().setFormName(formName);

    // recupera parametri dalla richiesta html
    filtro = StringOper.parse(params.get("filtro"), 0);
    start = StringOper.parse(params.get(sRstart), start);

    // recupera criterio di selezione record dai dati di sessione
    cSelezione = getSelezione(sessione);
    if(cSelezione == null)
      cSelezione = new FiltroListe();

    cSelezione.recuperaInfoColonne(getTableModel());

    switch(filtro)
    {
      case FILTRO_ANNULLA: // pulisci filtro impostato
        start = 0;
        cSelezione = new FiltroListe();
        setSelezione(sessione, cSelezione);
        for(int i = 0; i < getTableModel().getColumnCount(); i++)
        {
          RigelColumnDescriptor cd = getTableModel().getColumn(i);
          cd.setFiltroSort(0);
          cd.setFiltroTipo(0);
          cd.setFiltroValore(null);
        }
        break;
      case FILTRO_NORMALE:
      case FILTRO_MACHERA: // selezione del tipo filtro
        break;
      case FILTRO_APPLICA: // elementi con filtro applicato
        start = 0;
        cSelezione = creaFiltro(params);
        setSelezione(sessione, cSelezione);
    }

    // recupera html per la pagina
    // ATTENZIONE: questa va prima di getHtmlFiltro/getSimpleSearch
    // altrimenti l'html dei filtri potrebbe essere incompleto (combo-auto)
    getHtmlTable(page);

    // recupera filtro per la pagina
    getHtmlFiltro(page);

    // recupera filtro semplificato per la pagina
    getSimpleSearch(sessione, 20, page);

    // recupera componenti di navigazione
    long totalRecords = getTotalRecords();
    if(totalRecords > limit)
      getHtmlNavRecord(totalRecords, sessione, page);
  }

  /**
   * Recupera il filtro corrente dai dati di sessione.
   *
   * @param sessione
   * @return
   */
  public FiltroListe getSelezione(HttpSession sessione)
  {
    return (FiltroListe) (sessione.getAttribute("FiltroListeFilter:" + idPager));
  }

  /**
   * Salva il filtro corrente nei dati di sessione.
   *
   * @param sessione
   * @param c
   */
  public void setSelezione(HttpSession sessione, FiltroListe c)
  {
    sessione.setAttribute("FiltroListeFilter:" + idPager, c);
  }

  /**
   * Ritorna stato del filtro.
   *
   * @return true se il filtro e' attivo.
   */
  public boolean isFiltro()
  {
    return cSelezione == null ? false : !cSelezione.isEmpty();
  }

  /**
   * Ritorna stato del filtro per ordinamento.
   *
   * @return true se il filtro e' attivo e contiene parametri per ordinamento.
   */
  public boolean isFiltroSort()
  {
    return cSelezione == null ? false : cSelezione.isOrdinamento();
  }

  /**
   * Ritorna il filtro di selezione attualmente impostato
   *
   * @return filtro di selezione
   */
  public FiltroListe getCSelezione()
  {
    return cSelezione;
  }

  /**
   * Restituisce una pagina html per l'impostazione dei filtri sui dati.
   * @param page pagina che riceve la maschera
   * @throws java.lang.Exception
   */
  protected void getHtmlFiltro(RigelHtmlPage page)
     throws Exception
  {
    if(mgr == null)
    {
      if(getTableModel().isInitalized())
        mgr = getTableModel().getMascheraRG(i18n);
    }

    if(mgr != null)
      mgr.buildHtmlRicerca(formName, page);
  }

  /**
   * Ritorna la maschera di ricerca semplice.
   * ATTENZIONE: va sempre chiamata dopo getHtml.
   *
   * @param sessione
   * @param size
   * @param page the value of page
   * @throws Exception
   */
  public synchronized void getSimpleSearch(HttpSession sessione, int size, RigelHtmlPage page)
     throws Exception
  {
    if(mgr == null)
    {
      if(getTableModel().isInitalized())
        mgr = getTableModel().getMascheraRG(i18n);
    }

    if(mgr != null)
      mgr.buildHtmlRicercaSemplice(formName, size, isFiltro(), page);
  }

  /**
   * Ritorna il valore corrente di filtro, ovvero
   * se stiamo visualizzando la maschera di ricerca
   * o l'elenco dei records.
   * Vedi le costanti FILTRO_xxx.
   *
   * @return valore di filtro.
   */
  public int getCurrFiltro()
  {
    return filtro;
  }
}
