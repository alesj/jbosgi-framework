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
package org.jboss.osgi.framework.deployers;

// $Id: $

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.NativeLibraryProvider;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;
import org.jboss.osgi.framework.metadata.NativeLibrary;
import org.jboss.osgi.framework.metadata.NativeLibraryMetaData;
import org.jboss.osgi.framework.plugins.BundleStoragePlugin;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

/**
 * A deployer that takes care of loading native code libraries.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 19-Dec-2009
 */
public class OSGiNativeCodePolicyDeployer extends AbstractRealDeployer
{
   public OSGiNativeCodePolicyDeployer()
   {
      setInput(ClassLoaderFactory.class);
      addInput(ClassLoaderPolicy.class);
      addInput(OSGiBundleState.class);
      setStage(DeploymentStages.CLASSLOADER);
      setTopLevelOnly(true);
   }

   @Override
   protected void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      AbstractBundleState absBundleState = unit.getAttachment(AbstractBundleState.class);
      if (absBundleState == null)
         throw new IllegalStateException("No bundle state");

      OSGiClassLoadingMetaData classLoadingMetaData = (OSGiClassLoadingMetaData)unit.getAttachment(ClassLoadingMetaData.class);
      NativeLibraryMetaData libMetaData = classLoadingMetaData.getNativeLibraries();
      if (libMetaData == null || libMetaData.getNativeLibraries() == null)
         return;

      // Add the native library mappings to the OSGiClassLoaderPolicy
      OSGiBundleState bundleState = (OSGiBundleState)absBundleState;
      ClassLoaderPolicy policy = (ClassLoaderPolicy)unit.getAttachment(ClassLoaderPolicy.class);
      for (NativeLibrary library : libMetaData.getNativeLibraries())
      {
         String libpath = library.getLibraryPath();
         String libfile = new File(libpath).getName();
         String libname = libfile.substring(0, libfile.lastIndexOf('.'));
         
         // Add the library provider to the policy
         NativeLibraryProvider libProvider = new OSGiNativeLibraryProvider(bundleState, libname, libpath);
         policy.addNativeLibrary(libProvider);
         
         // [TODO] why does the TCK use 'Native' to mean 'libNative' ? 
         if (libname.startsWith("lib"))
         {
            libname = libname.substring(3);
            libProvider = new OSGiNativeLibraryProvider(bundleState, libname, libpath);
            policy.addNativeLibrary(libProvider);
         }
      }
   }
   
   static class OSGiNativeLibraryProvider implements NativeLibraryProvider
   {
      private OSGiBundleState bundleState;
      private String libpath;
      private String libname;
      private File libraryFile;
      
      OSGiNativeLibraryProvider(OSGiBundleState bundleState, String libname, String libpath)
      {
         this.bundleState = bundleState;
         this.libpath = libpath;
         this.libname = libname;
         
         // If a native code library in a selected native code clause cannot be found
         // within the bundle then the bundle must fail to resolve
         URL entryURL = bundleState.getEntry(libpath);
         if (entryURL == null)
            throw new IllegalStateException("Cannot find native library: " + libpath);
      }
      
      public String getLibraryName()
      {
         return libname;
      }

      public String getLibraryPath()
      {
         return libpath;
      }
      
      public File getLibraryLocation() throws IOException
      {
         if (libraryFile == null)
         {
            // Get the virtual file for entry for the library
            VirtualFile fileSource = bundleState.getRoot().getChild(libpath);
            
            // Create a unique local file location
            libraryFile = getUniqueLibraryFile(bundleState, libpath);
            libraryFile.deleteOnExit();
            
            // Copy the native library to the bundle storage area
            FileOutputStream fos = new FileOutputStream(libraryFile);
            VFSUtils.copyStream(fileSource.openStream(), fos);
            fos.close();
         }
         return libraryFile;
      }

      private File getUniqueLibraryFile(final OSGiBundleState bundleState, final String libpath)
      {
         OSGiBundleManager bundleManager = bundleState.getBundleManager();
         String timestamp = new SimpleDateFormat("-yyyyMMdd-HHmmssSSS").format(new Date(bundleState.getLastModified()));
         String uniquePath = new StringBuffer(libpath).insert(libpath.lastIndexOf("."), timestamp).toString();
         BundleStoragePlugin plugin = bundleManager.getPlugin(BundleStoragePlugin.class);
         return plugin.getDataFile(bundleState, uniquePath);
      }
   }
}
