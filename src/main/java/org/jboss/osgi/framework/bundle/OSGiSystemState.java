/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.osgi.framework.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.spi.NotImplementedException;
import org.jboss.util.collection.ConcurrentSet;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * OSGiSystemBundle.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class OSGiSystemState extends AbstractBundleState
{
   /** The registred contexts */
   private Set<ControllerContext> registered = new ConcurrentSet<ControllerContext>();

   /**
    * Create a new OSGiSystemBundle.
    * @param osgiMetaData the metadata for the system bundle
    */
   public OSGiSystemState(OSGiMetaData osgiMetaData)
   {
      super(osgiMetaData);
   }

   protected Set<ControllerContext> getRegisteredContexts()
   {
      return registered;
   }

   @Override
   protected void afterServiceRegistration(OSGiServiceState service)
   {
      registered.add(service);
   }

   @Override
   protected void beforeServiceUnregistration(OSGiServiceState service)
   {
      registered.remove(service);
   }

   public long getBundleId()
   {
      return 0;
   }
   
   public String getLocation()
   {
      return Constants.SYSTEM_BUNDLE_LOCATION;
   }

   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      // [JBOSGI-138] Proper system BundleContext implementation
      return getClass().getClassLoader().loadClass(name);
   }
   
   public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse)
   {
      log.warn("[JBOSGI-138] findEntries(" + path + "," + filePattern + "," + recurse + ")");
      return null;

      // [Bug-1472] Clarify the semantic of resource API when called on the system bundle
      // https://www.osgi.org/members/bugzilla/show_bug.cgi?id=1472
      /*
      Vector<URL> entryList = new Vector<URL>();
      for (Bundle bundle : getBundleManager().getBundles())
      {
         if (bundle != this)
         {
            Enumeration<URL> bundleEntries = bundle.findEntries(path, filePattern, recurse);
            if (bundleEntries != null)
            {
               while(bundleEntries.hasMoreElements())
               {
                  URL next = bundleEntries.nextElement();
                  entryList.add(next);
               }
            }
         }
      }
      return (entryList.size() > 0 ? entryList.elements() : null);
      */
   }

   public URL getEntry(String path)
   {
      log.warn("[JBOSGI-138] getEntry(" + path + ")");
      return null;
   }

   @SuppressWarnings("unchecked")
   public Enumeration getEntryPaths(String path)
   {
      log.warn("[JBOSGI-138] getEntryPaths(" + path + ")");
      return null;
   }

   public URL getResource(String name)
   {
      log.warn("[JBOSGI-138] getResource(" + name + ")");
      return null;
   }

   @SuppressWarnings("unchecked")
   public Enumeration getResources(String name) throws IOException
   {
      log.warn("[JBOSGI-138] getResources(" + name + ")");
      return null;
   }

   public void start(int options) throws BundleException
   {
      // [JBOSGI-138] Proper system BundleContext implementation
      throw new NotImplementedException();
   }

   public void stop(int options) throws BundleException
   {
      final OSGiBundleManager bundleManager = getBundleManager();
      bundleManager.getExecutor().execute(new Runnable()
      {
         public void run()
         {
            bundleManager.stopFramework();
         }
      });
   }

   @Override
   public void update() throws BundleException
   {
      final OSGiBundleManager bundleManager = getBundleManager();
      bundleManager.getExecutor().execute(new Runnable()
      {
         public void run()
         {
            bundleManager.restartFramework();
         }
      });
   }

   public void update(InputStream in) throws BundleException
   {
      throw new BundleException("The system bundle cannot be updated from a stream");
   }

   public void uninstall() throws BundleException
   {
      throw new BundleException("The system bundle cannot be uninstalled");
   }
}
