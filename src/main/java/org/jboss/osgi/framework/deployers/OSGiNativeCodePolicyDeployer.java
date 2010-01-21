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

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.NativeLibraryProvider;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.NativeLibrary;
import org.jboss.classloading.spi.metadata.NativeLibraryMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
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

      ClassLoadingMetaData classLoadingMetaData = unit.getAttachment(ClassLoadingMetaData.class);
      NativeLibraryMetaData libMetaData = classLoadingMetaData.getNativeLibraries();
      if (libMetaData == null || libMetaData.getNativeLibraries() == null)
         return;
         
      final OSGiBundleState bundleState = (OSGiBundleState)absBundleState;
      final OSGiBundleManager bundleManager = bundleState.getBundleManager();

      // Add the native library mappings to the OSGiClassLoaderPolicy
      ClassLoaderPolicy policy = unit.getAttachment(ClassLoaderPolicy.class);
      for (NativeLibrary library : libMetaData.getNativeLibraries())
      {
         final String libpath = library.getLibraryPath();

         NativeLibraryProvider provider = new NativeLibraryProvider()
         {
            private File libraryFile;
            
            public String getLibraryPath()
            {
               return libpath;
            }
            
            public File getLibraryLocation() throws IOException
            {
               if (libraryFile == null)
               {
                  URL entryURL = bundleState.getEntry(libpath);

                  // If a native code library in a selected native code clause cannot be found
                  // within the bundle then the bundle must fail to resolve
                  if (entryURL == null)
                     throw new IOException("Cannot find native library: " + libpath);

                  // Copy the native library to the bundle storage area
                  VirtualFile nativeVirtualFile = bundleState.getRoot().getChild(libpath);
                  BundleStoragePlugin plugin = bundleManager.getPlugin(BundleStoragePlugin.class);
                  libraryFile = plugin.getDataFile(bundleState, libpath);
                  FileOutputStream fos = new FileOutputStream(libraryFile);
                  VFSUtils.copyStream(nativeVirtualFile.openStream(), fos);
                  fos.close();
               }
               return libraryFile;
            }
         };
         
         // Add the library provider to the policy
         String libfile = new File(libpath).getName();
         String libname = libfile.substring(0, libfile.lastIndexOf('.'));
         policy.addNativeLibrary(libname, provider);
      }
   }
}
