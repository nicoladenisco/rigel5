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
package org.rigel2.glue.validators;

import java.sql.Connection;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.apache.torque.om.Persistent;
import org.commonlib.utils.StringOper;
import org.jdom2.*;
import org.rigel2.RigelCacheManager;
import org.rigel2.RigelI18nInterface;
import org.rigel2.SetupHolder;
import org.rigel2.table.RigelTableModel;
import org.rigel2.table.html.hEditTable;

/**
 * Funzioni per la validazione dei forms.
 *
 * @author Nicola De Nisco
 */
public class Validator
{
  public static class ValidatorCacheBean
  {
    public final Hashtable<String, PostParseValidator> cachePost = new Hashtable<String, PostParseValidator>();
    public final Hashtable<String, PreParseValidator> cachePre = new Hashtable<String, PreParseValidator>();
    public final Hashtable<String, SaveMasterDetailValidator> cacheSave
       = new Hashtable<String, SaveMasterDetailValidator>();
    public final Hashtable<String, PostSaveAction> cacheAction = new Hashtable<String, PostSaveAction>();
  }

  public static class StopParse
  {
    public boolean stopParsing;
  }

  private static ValidatorCacheBean getCacheBean()
  {
    RigelCacheManager cm = SetupHolder.getCacheManager();
    ValidatorCacheBean bean = (ValidatorCacheBean) cm.getGenericCachedData("Rigel_ValidatorCacheBean");
    if(bean == null)
    {
      bean = new ValidatorCacheBean();
      cm.putGenericCachedData("Rigel_ValidatorCacheBean", bean);
    }
    return bean;
  }

  /**
   * Validazione del record dopo il parsing dei dati di input.Viene chiamata immediatamente prima del
   * salvataggio di un oggetto.Questa funzione cerca
   * nella definizione XML della lista le sezioni post-parse-validator
   * che identificano validatori finali dei dati.
   * I validatori
   * devono implementare l'interfaccia PostParseValidator.
   * <pre>
   * ES.:
   * [post-parse-validator]
   * [class]StoffertePPValidator[/class]
   * [/post-parse-validator]
   * </pre>
   *
   * @param eleXml
   * @param obj oggetto peer da validare
   * @param tableModel the value of tableModel
   * @param table
   * @param row the value of row
   * @param session sessione HTTP
   * @param param parametri del form di input
   * @param i18n
   * @param con connessione al database (puo' essere null)
   * @param custom un oggetto a piacere da passare al validatore (puo' essere null)
   * @return true per indicare dati validi
   * @throws Exception
   */
  public static boolean postParseValidate(Element eleXml, Persistent obj,
     RigelTableModel tableModel, hEditTable table, int row, HttpSession session, Map param,
     RigelI18nInterface i18n, Connection con, Object custom)
     throws Exception
  {
    if(eleXml != null)
    {
      Iterator itrPPV = eleXml.getChildren("post-parse-validator").iterator();
      while(itrPPV.hasNext())
      {
        Element elePPV = (Element) (itrPPV.next());

        String classPPV = StringOper.okStrNull(elePPV.getChildText("class"));
        if(classPPV != null)
        {
          PostParseValidator ppv;
          ValidatorCacheBean vb = getCacheBean();
          synchronized(vb.cachePost)
          {
            if((ppv = vb.cachePost.get(classPPV)) == null)
            {
              ppv = ValidatorsFactory.getInstance().getPostParseValidator(classPPV);
              vb.cachePost.put(classPPV, ppv);
            }
          }

          synchronized(ppv)
          {
            ppv.init(elePPV);
            return ppv.validate(obj, tableModel, table, row, session, param, i18n, con, custom);
          }
        }
      }
    }

    // per default il record e' valido
    return true;
  }

  /**
   * Validazione del record prima del parsing dei dati
   * di input.Viene chiamata immediatamente prima che
   * dati presenti nella map vengano caricati nell'oggetto.Questa funzione cerca
   * nella definizione XML della lista le sezioni pre-parse-validator
   * che identificano validatori finali dei dati.I validatori
   * devono implementare l'interfaccia PreParseValidator.<pre>
   * ES.:
   * [pre-parse-validator]
   * [class]StoffertePPValidator[/class]
   * [/pre-parse-validator]
   * </pre>
   *
   * @param eleXml
   * @param obj oggetto peer da validare
   * @param tableModel the value of tableModel
   * @param table
   * @param row the value of row
   * @param session
   * @param param
   * @param i18n
   * @param custom
   * @param stopParsing
   * @return true per indicare dati validi
   * @throws Exception
   */
  public static boolean preParseValidate(Element eleXml, Persistent obj,
     RigelTableModel tableModel, hEditTable table, int row, HttpSession session, Map param,
     RigelI18nInterface i18n, Object custom, StopParse stopParsing)
     throws Exception
  {
    if(eleXml != null)
    {
      Iterator itrPPV = eleXml.getChildren("pre-parse-validator").iterator();
      while(itrPPV.hasNext())
      {
        Element elePPV = (Element) (itrPPV.next());

        String classPPV = StringOper.okStrNull(elePPV.getChildText("class"));
        if(classPPV != null)
        {
          PreParseValidator ppv;
          ValidatorCacheBean vb = getCacheBean();
          synchronized(vb.cachePre)
          {
            if((ppv = vb.cachePre.get(classPPV)) == null)
            {
              ppv = ValidatorsFactory.getInstance().getPreParseValidator(classPPV);
              vb.cachePre.put(classPPV, ppv);
            }
          }

          synchronized(ppv)
          {
            ppv.init(elePPV);
            boolean rv = ppv.validate(obj, tableModel, table, row, session, param, i18n, custom);
            stopParsing.stopParsing = ppv.isStopParsing();
            return rv;
          }
        }
      }
    }

    // per default il record e' valido
    return true;
  }

  public static boolean postSaveMasterDetail(
     Element eleXml, Persistent obj, RigelTableModel tableModelMaster, hEditTable tableMaster, int rowMaster,
     List<Persistent> detail, RigelTableModel tableModelDetail, hEditTable tableDetail,
     HttpSession session, Map param, RigelI18nInterface i18n, Connection dbCon, Object custom)
     throws Exception
  {
    if(eleXml != null)
    {
      Iterator itrPPV = eleXml.getChildren("post-save-validator").iterator();
      while(itrPPV.hasNext())
      {
        Element elePPV = (Element) (itrPPV.next());

        String classPPV = StringOper.okStrNull(elePPV.getChildText("class"));
        if(classPPV != null)
        {
          SaveMasterDetailValidator ppv;
          ValidatorCacheBean vb = getCacheBean();
          synchronized(vb.cacheSave)
          {
            if((ppv = vb.cacheSave.get(classPPV)) == null)
            {
              ppv = ValidatorsFactory.getInstance().getSaveMasterDetailValidator(classPPV);
              vb.cacheSave.put(classPPV, ppv);
            }
          }

          synchronized(ppv)
          {
            ppv.init(elePPV);
            return ppv.validate(obj,
               tableModelMaster, tableMaster, rowMaster,
               detail, tableModelDetail, tableDetail,
               session, param, i18n, dbCon, custom);
          }
        }
      }
    }

    // per default il record e' valido
    return true;
  }

  public static boolean postSaveAction(
     Element eleXml, Persistent obj, RigelTableModel tableModel, hEditTable table, int row,
     HttpSession session, Map param, RigelI18nInterface i18n, Object custom)
     throws Exception
  {
    if(eleXml != null)
    {
      Iterator itrPPV = eleXml.getChildren("post-save-action").iterator();
      while(itrPPV.hasNext())
      {
        Element elePPV = (Element) (itrPPV.next());

        String classPPV = StringOper.okStrNull(elePPV.getChildText("class"));
        if(classPPV != null)
        {
          PostSaveAction ppv;
          ValidatorCacheBean vb = getCacheBean();
          synchronized(vb.cacheAction)
          {
            if((ppv = vb.cacheAction.get(classPPV)) == null)
            {
              ppv = ValidatorsFactory.getInstance().getPostSaveAction(classPPV);
              vb.cacheAction.put(classPPV, ppv);
            }
          }

          synchronized(ppv)
          {
            ppv.init(elePPV);
            return ppv.action(obj, tableModel, table, row, session, param, i18n, custom);
          }
        }
      }
    }

    // per default il record e' valido
    return true;
  }
}
