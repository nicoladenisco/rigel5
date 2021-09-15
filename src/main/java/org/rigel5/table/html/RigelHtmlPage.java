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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.commonlib5.utils.StringOper;

/**
 * Rappresentazione di una pagina in Rigel.
 * Una pagina è una collezione di componeti che verranno assemblati
 * dall'UI manager (@see RigelUIManager) per costruire la pagina finale.
 *
 * @author Nicola De Nisco
 */
public class RigelHtmlPage extends ArrayList<RigelHtmlPageComponent>
{
  /** una eventuale sezione di appartenenza (può essere null) */
  private String section = null;

  public String getSection()
  {
    return section;
  }

  public void setSection(String section)
  {
    this.section = section;
  }

  @Override
  public boolean add(RigelHtmlPageComponent e)
  {
    e.setSection(section);
    return super.add(e);
  }

  @Override
  public void add(int index, RigelHtmlPageComponent element)
  {
    element.setSection(section);
    super.add(index, element);
  }

  @Override
  public boolean addAll(Collection<? extends RigelHtmlPageComponent> c)
  {
    for(RigelHtmlPageComponent e : c)
      e.setSection(section);
    return super.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends RigelHtmlPageComponent> c)
  {
    for(RigelHtmlPageComponent e : c)
      e.setSection(section);
    return super.addAll(index, c);
  }

  /**
   * Restituisce solo i componenti del tipo indicato.
   * @param type tipo di componente richiesto
   * @return lista dei componenti
   */
  public List<RigelHtmlPageComponent> findComponents(PageComponentType type)
  {
    return stream()
       .filter((c) -> type == c.getType())
       .collect(Collectors.toList());
  }

  /**
   * Restituisce solo i componenti del tipo indicato.
   * @param type tipo di componente richiesto
   * @param group gruppo richiesto
   * @return lista dei componenti
   */
  public List<RigelHtmlPageComponent> findComponents(PageComponentType type, String group)
  {
    if(group == null)
      return findComponents(type);

    return stream()
       .filter((c) -> type == c.getType() && StringOper.isEqu(group, c.getGroup()))
       .collect(Collectors.toList());
  }

  /**
   * Restituisce solo i componenti del tipo indicato.
   * @param type tipo di componente richiesto
   * @param group gruppo richiesto
   * @param section sezione richiesta
   * @return lista dei componenti
   */
  public List<RigelHtmlPageComponent> findComponents(PageComponentType type, String group, String section)
  {
    if(section == null)
      return findComponents(type, group);

    return stream()
       .filter((c) -> type == c.getType() && StringOper.isEqu(group, c.getGroup()) && StringOper.isEqu(section, c.getSection()))
       .collect(Collectors.toList());
  }

  /**
   * Assembla un gruppo di componenti.
   * @param sb accumulo del contenuto dei componenti
   * @param lsComps lista componenti da assemblare
   * @param pre da inserire prima del gruppo componenti
   * @param post da inserire dopo del gruppo componenti
   * @return lista di componenti utilizzati
   * @throws Exception
   */
  public List<RigelHtmlPageComponent> buildPart(StringBuilder sb, List<RigelHtmlPageComponent> lsComps, String pre, String post)
     throws Exception
  {
    if(!lsComps.isEmpty())
    {
      sb.append(pre).append("\r\n");
      sb.append(StringOper.join(lsComps.iterator(), "\r\n", null));
      sb.append(post).append("\r\n");
    }
    return lsComps;
  }

  /**
   * Cerca e assembla un gruppo di componenti.
   * @param sb accumulo del contenuto dei componenti
   * @param type tipo di componente richiesto
   * @param pre da inserire prima del gruppo componenti
   * @param post da inserire dopo del gruppo componenti
   * @return lista di componenti utilizzati
   * @throws Exception
   */
  public List<RigelHtmlPageComponent> buildPart(StringBuilder sb, PageComponentType type, String pre, String post)
     throws Exception
  {
    return buildPart(sb, findComponents(type), pre, post);
  }

  /**
   * Assembla un gruppo di componenti.
   * @param sb accumulo del contenuto dei componenti
   * @param type tipo di componente richiesto
   * @param group gruppo richiesto
   * @param pre da inserire prima del gruppo componenti
   * @param post da inserire dopo del gruppo componenti
   * @return lista di componenti utilizzati
   * @throws Exception
   */
  public List<RigelHtmlPageComponent> buildPart(StringBuilder sb,
     PageComponentType type, String group,
     String pre, String post)
     throws Exception
  {
    return buildPart(sb, findComponents(type, group), pre, post);
  }

  /**
   * Assembla un gruppo di componenti.
   * @param sb accumulo del contenuto dei componenti
   * @param type tipo di componente richiesto
   * @param group gruppo richiesto
   * @param section la sezione richiesta
   * @param pre da inserire prima del gruppo componenti
   * @param post da inserire dopo del gruppo componenti
   * @return lista di componenti utilizzati
   * @throws Exception
   */
  public List<RigelHtmlPageComponent> buildPart(StringBuilder sb,
     PageComponentType type, String group, String section,
     String pre, String post)
     throws Exception
  {
    return buildPart(sb, findComponents(type, group, section), pre, post);
  }

  public String getAllJavascript()
     throws Exception
  {
    StringBuilder sb = new StringBuilder(128);
    buildPart(sb, PageComponentType.JAVASCRIPT, "<SCRIPT LANGUAGE=\"JavaScript\">", "</SCRIPT>");
    return sb.toString();
  }

  public String getAllJavascriptNotag()
     throws Exception
  {
    StringBuilder sb = new StringBuilder(128);
    buildPart(sb, PageComponentType.JAVASCRIPT, "\n", "\n");
    return sb.toString();
  }

  public String getAllJavascriptPart()
     throws Exception
  {
    StringBuilder sb = new StringBuilder(128);
    buildPart(sb, PageComponentType.JAVASCRIPT_PART, "\n", "\n");
    return sb.toString();
  }

  public String getHtml(String group)
     throws Exception
  {
    return getHtml(group, null);
  }

  public String getHtml(String group, String section)
     throws Exception
  {
    return getGeneric(PageComponentType.HTML, group, section);
  }

  public String getGeneric(int type, String group, String section)
     throws Exception
  {
    StringBuilder sb = new StringBuilder(128);
    buildPart(sb, PageComponentType.fromInteger(type), StringOper.okStrNull(group), StringOper.okStrNull(section), "\n", "\n");
    return sb.toString();
  }

  public String getGeneric(PageComponentType type, String group, String section)
     throws Exception
  {
    StringBuilder sb = new StringBuilder(128);
    buildPart(sb, type, StringOper.okStrNull(group), StringOper.okStrNull(section), "\n", "\n");
    return sb.toString();
  }
}
