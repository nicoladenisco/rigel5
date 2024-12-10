/*
 * Copyright (C) 2024 Nicola De Nisco
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

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.MappedPropertyDescriptor;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.torque.TorqueException;
import org.apache.torque.TorqueRuntimeException;
import org.apache.torque.dsfactory.AbstractDataSourceFactory;

/**
 * Versione modificata di JndiDataSourceFactory di torque.
 *
 * @author Nicola De Nisco
 */
public class RigelJndiDataSourceFactory extends AbstractDataSourceFactory
{
  /**
   * Key for the configuration which contains jndi properties.
   */
  public static final String JNDI_KEY = "jndi";

  /**
   * Key for the configuration property which contains the jndi path.
   */
  public static final String PATH_KEY = "path";

  /**
   * Key for the configuration property which contains the
   * time between two jndi lookups.
   */
  public static final String TIME_BETWEEN_LOOKUPS_KEY = "ttl";

  /**
   * Key for the configuration which contains properties for a DataSource
   * which should be bound into jndi.
   */
  public static final String DATASOURCE_KEY = "datasource";

  /**
   * Key for the configuration property which contains the class name
   * of the datasource to be bound into jndi.
   */
  public static final String CLASSNAME_KEY = "classname";

  /** The log. */
  private static final Logger log = LogManager.getLogger(RigelJndiDataSourceFactory.class);

  /** The path to get the resource from. */
  private String path;
  /** The context to get the resource from. */
  private Context ctx;

  /** A locally cached copy of the DataSource */
  private DataSource ds = null;

  /** Time of last actual lookup action */
  private long lastLookup = 0;

  /** Time between two lookups */
  private long ttl = 0; // ms

  /**
   * @see org.apache.torque.dsfactory.DataSourceFactory#getDataSource
   */
  @Override
  public DataSource getDataSource()
     throws TorqueException
  {
    long time = System.currentTimeMillis();

    if(ds == null || time - lastLookup > ttl)
    {
      try
      {
        synchronized(ctx)
        {
          ds = ((DataSource) ctx.lookup(path));
        }
        lastLookup = time;
      }
      catch(Exception e)
      {
        throw new TorqueException(e);
      }
    }

    return ds;
  }

  /**
   * @see org.apache.torque.dsfactory.DataSourceFactory#initialize
   */
  @Override
  public void initialize(Configuration configuration)
     throws TorqueException
  {
    initJNDI(configuration);
    initDataSource(configuration);
  }

  /**
   * Initializes JNDI.
   *
   * @param configuration where to read the settings from
   * @throws TorqueException if a property set fails
   */
  private void initJNDI(Configuration configuration)
     throws TorqueException
  {
    log.debug("Starting initJNDI");

    Configuration c = configuration.subset(JNDI_KEY);
    if(c == null || c.isEmpty())
    {
      throw new TorqueException(
         "JndiDataSourceFactory requires a jndi "
         + "path property to lookup the DataSource in JNDI.");
    }

    try
    {
      String initPath = c.getString("initPath", "java:comp/env");
      Context initCtx = new InitialContext();
      ctx = (Context) initCtx.lookup(initPath);
      path = c.getString("path");

      log.debug("Creato context environment.");
      debugCtx(ctx);
    }
    catch(Exception e)
    {
      log.error("", e);
      throw new TorqueException(e);
    }
  }

  /**
   * Initializes the DataSource.
   *
   * @param configuration where to read the settings from
   * @throws TorqueException if a property set fails
   */
  private void initDataSource(Configuration configuration)
     throws TorqueException
  {
    log.debug("Starting initDataSource");
    try
    {
      Object dataSource = null;

      Configuration c = configuration.subset(DATASOURCE_KEY);
      if(c != null)
      {
        for(Iterator<?> i = c.getKeys(); i.hasNext();)
        {
          String key = (String) i.next();
          if(key.equals(CLASSNAME_KEY))
          {
            String classname = c.getString(key);
            log.debug("Datasource class: {}", classname);

            Class<?> dsClass = Class.forName(classname);
            dataSource = dsClass.newInstance();
          }
          else
          {
            if(dataSource != null)
            {
              log.debug("Setting datasource property: {}", key);
              setProperty(key, c, dataSource);
            }
            else
            {
              log.error("Tried to set property {} without Datasource definition!", key);
            }
          }
        }
      }

      if(dataSource != null)
      {
        synchronized(ctx)
        {
          bindDStoJndi(ctx, path, dataSource);
        }
      }
    }
    catch(Exception e)
    {
      log.error("", e);
      throw new TorqueException(e);
    }
  }

  /**
   * Does nothing. We do not want to close a dataSource retrieved from Jndi,
   * because other applications might use it as well.
   */
  @Override
  public void close()
  {
    // do nothing
  }

  /**
   *
   * @param ctx the context
   * @throws NamingException
   */
  private void debugCtx(Context ctx)
     throws NamingException
  {
    log.debug("InitialContext -------------------------------");
    Map<?, ?> env = ctx.getEnvironment();
    log.debug("Environment properties: {}", env.size());
    env.forEach((key, value) -> log.debug("    {}: {}", key, value));
    log.debug("----------------------------------------------");
  }

  /**
   *
   * @param ctx
   * @param path
   * @param ds
   * @throws Exception
   */
  private void bindDStoJndi(Context ctx, String path, Object ds)
     throws Exception
  {
    debugCtx(ctx);

    // add subcontexts, if not added already
    int start = path.indexOf(':') + 1;
    if(start > 0)
    {
      path = path.substring(start);
    }
    StringTokenizer st = new StringTokenizer(path, "/");
    while(st.hasMoreTokens())
    {
      String subctx = st.nextToken();
      if(st.hasMoreTokens())
      {
        try
        {
          ctx.createSubcontext(subctx);
          log.debug("Added sub context: {}", subctx);
        }
        catch(NameAlreadyBoundException nabe)
        {
          // ignore
          log.debug("Sub context {} already exists", subctx);
        }
        catch(NamingException ne)
        {
          log.debug("Naming exception caught when creating subcontext {}",
             subctx,
             ne);
          // even though there is a specific exception
          // for this condition, some implementations
          // throw the more general one.
          /*
                     *                      if (ne.getMessage().indexOf("already bound") == -1 )
                     *                      {
                     *                      throw ne;
                     *                      }
           */
          // ignore
        }
        ctx = (Context) ctx.lookup(subctx);
      }
      else
      {
        // not really a subctx, it is the ds name
        ctx.bind(subctx, ds);
      }
    }
  }

  /**
   * Encapsulates setting configuration properties on
   * <code>DataSource</code> objects.
   *
   * Ridefinita dalla classe base per emettere warning e non error
   * se DataSouce non accetta una chiave di setup.
   *
   * @param property the property to read from the configuration
   * @param c the configuration to read the property from
   * @param ds the <code>DataSource</code> instance to write the property to
   * @throws Exception if anything goes wrong
   */
  @Override
  protected void setProperty(String property, final Configuration c, final Object ds)
     throws Exception
  {
    if(c == null || c.isEmpty())
    {
      return;
    }

    String key = property;
    Class<?> dsClass = ds.getClass();
    int dot = property.indexOf('.');
    try
    {
      if(dot > 0)
      {
        property = property.substring(0, dot);

        MappedPropertyDescriptor mappedPD = new MappedPropertyDescriptor(property, dsClass);
        Class<?> propertyType = mappedPD.getMappedPropertyType();
        Configuration subProps = c.subset(property);
        // use reflection to set properties
        Iterator<?> j = subProps.getKeys();
        while(j.hasNext())
        {
          String subProp = (String) j.next();
          String propVal = subProps.getString(subProp);
          Object value = ConvertUtils.convert(propVal, propertyType);
          PropertyUtils.setMappedProperty(ds, property, subProp, value);

          log.debug("setMappedProperty({}, {}, {}, {})", ds, property, subProp, value);
        }
      }
      else
      {
        if("password".equals(key))
        {
          // do not log value of password
          // for this, ConvertUtils.convert cannot be used
          // as it also logs the value of the converted property
          // so it is assumed here that the password is a String
          String value = c.getString(property);
          PropertyUtils.setSimpleProperty(ds, property, value);

          log.debug("setSimpleProperty({}, {}, (value not logged))", ds, property);
        }
        else
        {
          Class<?> propertyType = PropertyUtils.getPropertyType(ds, property);
          Object value = ConvertUtils.convert(c.getString(property), propertyType);
          PropertyUtils.setSimpleProperty(ds, property, value);

          log.debug("setSimpleProperty({}, {}, {})", ds, property, value);
        }
      }
    }
    catch(RuntimeException e)
    {
      throw new TorqueRuntimeException(
         "Runtime error setting property " + property, e);
    }
    catch(Exception e)
    {
      // la modifica riguarda solo questa linea: da error a warn
      log.warn("Property: {}  value: {}  is not supported by DataSource: {}",
         property, c.getString(key), ds.getClass().getName());
    }
  }
}
