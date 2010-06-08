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
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.OSGiManifestMetaData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * OSGiSystemBundle.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class OSGiSystemState extends AbstractBundleState
{
   /** The osgi metadata */
   private OSGiMetaData osgiMetaData;

   /**
    * Create a new OSGiSystemBundle.
    */
   public OSGiSystemState(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
      
      // Initialize basic metadata
      Manifest manifest = new Manifest();
      Attributes attributes = manifest.getMainAttributes();
      attributes.put(new Name(Constants.BUNDLE_SYMBOLICNAME), Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
      osgiMetaData = new OSGiManifestMetaData(manifest);
   }

   /**
    * Assert that the given bundle is an instance of OSGiSystemState
    * @throws IllegalArgumentException if the given bundle is not an instance of OSGiSystemState
    */
   public static OSGiSystemState assertBundleState(Bundle bundle)
   {
      bundle = AbstractBundleState.assertBundleState(bundle);
      if (bundle instanceof OSGiSystemState == false)
         throw new IllegalArgumentException("Not an OSGiSystemState: " + bundle);

      return (OSGiSystemState)bundle;
   }

   @Override
   public OSGiMetaData getOSGiMetaData()
   {
      return osgiMetaData;
   }

   @Override
   public boolean isFragment()
   {
      return false;
   }
   
   @Override
   public boolean isPersistentlyStarted()
   {
      return false;
   }

   @Override
   public long getBundleId()
   {
      return 0;
   }
   
   @Override
   public String getLocation()
   {
      return Constants.SYSTEM_BUNDLE_LOCATION;
   }

   @Override
   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      ClassLoaderDomain domain = getBundleManager().getClassLoaderDomain();
      return domain.loadClass(name);
   }
   
   @Override
   public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse)
   {
      System.out.println("FIXME [JBOSGI-138] findEntries(" + path + "," + filePattern + "," + recurse + ")");
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

   @Override
   public URL getEntry(String path)
   {
      System.out.println("FIXME [JBOSGI-138] getEntry(" + path + ")");
      return null;
   }

   @Override
   @SuppressWarnings({ "rawtypes" })
   public Enumeration getEntryPaths(String path)
   {
      System.out.println("FIXME [JBOSGI-138] getEntryPaths(" + path + ")");
      return null;
   }

   @Override
   public URL getResource(String name)
   {
      System.out.println("FIXME [JBOSGI-138] getResource(" + name + ")");
      return null;
   }

   @Override
   @SuppressWarnings({ "rawtypes" })
   public Enumeration getResources(String name) throws IOException
   {
      System.out.println("FIXME [JBOSGI-138] getResources(" + name + ")");
      return null;
   }

   @Override
   public void start(int options) throws BundleException
   {
      createBundleContext();
   }

   @Override
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

   @Override
   public void update(InputStream in) throws BundleException
   {
      throw new BundleException("The system bundle cannot be updated from a stream");
   }

   @Override
   public void uninstall() throws BundleException
   {
      throw new BundleException("The system bundle cannot be uninstalled");
   }
}
