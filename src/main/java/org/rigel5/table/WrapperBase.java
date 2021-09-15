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
package org.rigel5.table;

import java.util.*;
import javax.servlet.http.*;
import org.jdom2.Element;
import org.rigel5.RigelXmlSetupInterface;
import org.rigel5.table.html.wrapper.CustomButtonInfo;
import org.rigel5.table.html.wrapper.EditInfo;
import org.rigel5.table.html.wrapper.ForeignInfo;
import org.rigel5.table.html.wrapper.MasterDetailInfo;
import org.rigel5.table.html.wrapper.ParametriFiltroListe;
import org.rigel5.table.html.wrapper.ParametroListe;

/**
 * <p>
 * Classe base di tutti i Wrapper.</p>
 * <p>
 * Vedi le classi derivate per maggiori dettagli.</p>
 */
abstract public class WrapperBase implements RigelXmlSetupInterface
{
  protected String nome;
  protected String header;
  protected String titolo;
  protected String customScript;
  protected RigelTableModel ptm = null;
  protected Vector<String> sortColumns = new Vector<String>(16, 16);
  protected int numPerPage = 10;
  protected String nomeTabella;
  protected int numColonne;
  protected Hashtable attr = new Hashtable();
  protected BloccoPermessi permessi;
  protected String tableStatement;
  protected ForeignInfo foInfo = null;
  protected MasterDetailInfo mdInfo = null;
  protected EditInfo edInfo = null;
  protected EditInfo prInfo = null;
  protected Element eleXml = null;
  protected boolean nosize = false;
  protected boolean editEnabled = true;
  protected boolean saveEnabled = true;
  protected boolean newEnabled = true;
  protected boolean customColumnsEnabled = false;
  protected AbstractTablePager pager = null;
  protected Vector<CustomButtonInfo> headerButtons = new Vector<CustomButtonInfo>();
  protected ParametriFiltroListe filtro = new ParametriFiltroListe();
  //
  // costanti
  public static final String VAR_MACRO = "@@@";

  /////////////////////////////////////////////////////////////////
  /**
   * Salva i dati del form nell'oggetto peer associato.
   * @param params
   * @param sessione
   * @throws Exception
   */
  abstract public void salvaDatiForm(Map params, HttpSession sessione)
     throws Exception;

  /////////////////////////////////////////////////////////////////
  public List<String> getSortColumns()
  {
    return sortColumns;
  }

  public void addSortColumn(String columnName)
  {
    sortColumns.add(columnName);
  }

  public AbstractTablePager getPager()
  {
    return pager;
  }

  public void setPager(AbstractTablePager lista)
  {
    this.pager = lista;
  }

  //////////////////////////////////////////////////////////////////
  public Hashtable getAttributes()
  {
    return attr;
  }

  public Object getAttribute(String nome)
  {
    return attr.get(nome);
  }

  public void setAttribute(String nome, Object val)
  {
    if(val == null)
      attr.remove(nome);
    else
      attr.put(nome, val);
  }

  public void removeAttribute(String nome)
  {
    attr.remove(nome);
  }

  public void setHeader(String header)
  {
    this.header = header;
  }

  public String getHeader()
  {
    return header;
  }

  public void setTitolo(String titolo)
  {
    this.titolo = titolo;
  }

  public String getTitolo()
  {
    return titolo;
  }

  public void setPtm(RigelTableModel ptm)
  {
    this.ptm = ptm;
  }

  public RigelTableModel getPtm()
  {
    return ptm;
  }

  public void setNumPerPage(int numPerPage)
  {
    this.numPerPage = numPerPage;
  }

  public int getNumPerPage()
  {
    return numPerPage;
  }

  public void setNomeTabella(String nomeTabella)
  {
    this.nomeTabella = nomeTabella;
  }

  public String getNomeTabella()
  {
    return nomeTabella;
  }

  public void setNumColonne(int numColonne)
  {
    this.numColonne = numColonne;
  }

  public int getNumColonne()
  {
    return numColonne;
  }

  public void setNome(String nome)
  {
    this.nome = nome;
  }

  public String getNome()
  {
    return nome;
  }

  public void setPermessi(BloccoPermessi permessi)
  {
    this.permessi = permessi;
  }

  public BloccoPermessi getPermessi()
  {
    return permessi;
  }

  public void setTableStatement(String tableStatement)
  {
    this.tableStatement = tableStatement;
  }

  public String getTableStatement()
  {
    return tableStatement;
  }

  public boolean isEditEnabled()
  {
    return editEnabled;
  }

  public void setEditEnabled(boolean editEnabled)
  {
    this.editEnabled = editEnabled;
  }

  public boolean isSaveEnabled()
  {
    return saveEnabled;
  }

  public void setSaveEnabled(boolean saveEnabled)
  {
    this.saveEnabled = saveEnabled;
  }

  public boolean isNewEnabled()
  {
    return newEnabled;
  }

  public void setNewEnabled(boolean newEnabled)
  {
    this.newEnabled = newEnabled;
  }

  public boolean isCustomColumnsEnabled()
  {
    return customColumnsEnabled;
  }

  public void setCustomColumnsEnabled(boolean customColumnsEnabled)
  {
    this.customColumnsEnabled = customColumnsEnabled;
  }

  public ForeignInfo getFoInfo()
  {
    return foInfo;
  }

  public void setFoInfo(ForeignInfo foInfo)
  {
    this.foInfo = foInfo;
  }

  public MasterDetailInfo getMdInfo()
  {
    return mdInfo;
  }

  public void setMdInfo(MasterDetailInfo mdInfo)
  {
    this.mdInfo = mdInfo;
  }

  public EditInfo getEdInfo()
  {
    return edInfo;
  }

  public void setEdInfo(EditInfo edInfo)
  {
    this.edInfo = edInfo;
  }

  public EditInfo getPrInfo()
  {
    return prInfo;
  }

  public void setPrInfo(EditInfo prInfo)
  {
    this.prInfo = prInfo;
  }

  @Override
  public Element getEleXml()
  {
    return eleXml;
  }

  @Override
  public void setEleXml(Element eleXml)
  {
    this.eleXml = eleXml;
  }

  /**
   * Aggiunge un pulsante personalizzato a questa lista
   * @param ci un descrittore del pulsante (icona) personalizzato
   */
  public void addHeaderButton(CustomButtonInfo ci)
  {
    headerButtons.add(ci);
  }

  public boolean isHeaderButton()
  {
    return !headerButtons.isEmpty();
  }

  public int getNumHeaderButtons()
  {
    return headerButtons.size();
  }

  public CustomButtonInfo getHeaderButton(int button)
  {
    return headerButtons.get(button);
  }

  public String getCustomScript()
  {
    return customScript;
  }

  public void setCustomScript(String customScript)
  {
    this.customScript = customScript;
  }

  public ParametriFiltroListe getFiltro()
  {
    return filtro;
  }

  public void setFiltro(ParametriFiltroListe filtro)
  {
    this.filtro = filtro;
  }

  public void addParametri(ParametroListe par)
  {
    filtro.addParametri(par);
  }

  public void setParametro(String nomeParam, String valParam)
  {
    filtro.setParametro(nomeParam, valParam);
  }

  public boolean populateParametri(Map params)
  {
    if(filtro.haveParametri())
      return filtro.populateParametri(params);
    return false;
  }

  public void clearParametri()
  {
    filtro.clearParametri();
  }

  public boolean haveParametri()
  {
    return filtro.haveParametri();
  }

  public boolean isNosize()
  {
    return nosize;
  }

  public void setNosize(boolean nosize)
  {
    this.nosize = nosize;
  }
}
