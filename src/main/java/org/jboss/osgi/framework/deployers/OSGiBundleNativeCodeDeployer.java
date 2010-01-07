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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.classloading.OSGiClassLoaderPolicy;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.plugins.BundleStoragePlugin;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.Constants;

/**
 * A deployer that takes care of loading native code libraries.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 19-Dec-2009
 */
public class OSGiBundleNativeCodeDeployer extends AbstractRealDeployer
{
   /** The log */
   private static final Logger log = Logger.getLogger(OSGiBundleNativeCodeDeployer.class);

   /** Maps an alias to an OSGi processor name */
   private static Map<String, String> processorAlias = new HashMap<String, String>();
   static
   {
      processorAlias.put("pentium", "x86");
      processorAlias.put("i386", "x86");
      processorAlias.put("i486", "x86");
      processorAlias.put("i586", "x86");
      processorAlias.put("i686", "x86");
      processorAlias.put("amd64", "x86-64");
      processorAlias.put("em64t", "x86-64");
      processorAlias.put("x86_64", "x86-64");
   }
   
   /** Maps an alias to an OSGi osname */
   private static Map<String, String> osAlias = new HashMap<String, String>();
   static
   {
      osAlias.put("SymbianOS", "Epoc32");
      osAlias.put("hp-ux", "HPUX");
      osAlias.put("Mac OS", "MacOS");
      osAlias.put("Mac OS X", "MacOSX");
      osAlias.put("OS/2", "OS2");
      osAlias.put("procnto", "QNX");
      osAlias.put("Win95", "Windows95");
      osAlias.put("Windows 95", "Windows95");
      osAlias.put("Win32", "Windows95");
      osAlias.put("Win98", "Windows98");
      osAlias.put("Windows 98", "Windows98");
      osAlias.put("Win32", "Windows98");
      osAlias.put("WinNT", "WindowsNT");
      osAlias.put("Windows NT", "WindowsNT");
      osAlias.put("Win32", "WindowsNT");
      osAlias.put("WinCE", "WindowsCE");
      osAlias.put("Windows CE", "WindowsCE");
      osAlias.put("Win2000", "Windows2000");
      osAlias.put("Windows 2000", "Windows2000");
      osAlias.put("Win32", "Windows2000");
      osAlias.put("Win2003", "Windows2003");
      osAlias.put("Windows 2003", "Windows2003");
      osAlias.put("Win32", "Windows2003");
      osAlias.put("Windows Server 2003", "Windows2003");
      osAlias.put("WinXP", "WindowsXP");
      osAlias.put("Windows XP", "WindowsXP");
      osAlias.put("Win32", "WindowsXP");
      osAlias.put("WinVista", "WindowsVista");
      osAlias.put("Windows Vista", "WindowsVista");
      osAlias.put("Win32", "WindowsVista");
   }
   
   public OSGiBundleNativeCodeDeployer()
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
      
      if ((absBundleState instanceof OSGiBundleState) == false)
         return;
      
      OSGiBundleState bundleState = (OSGiBundleState)absBundleState;
      OSGiMetaData osgiMetaData = bundleState.getOSGiMetaData();
      List<ParameterizedAttribute> nativeCodeParams = osgiMetaData.getBundleNativeCode();
      if (nativeCodeParams == null)
         return;
      
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      String fwOSName = bundleManager.getProperty(Constants.FRAMEWORK_OS_NAME);
      String fwProcessor = bundleManager.getProperty(Constants.FRAMEWORK_PROCESSOR);
      //String fwOSVersion = bundleManager.getProperty(Constants.FRAMEWORK_OS_VERSION);
      
      List<ParameterizedAttribute> matchedParams = new ArrayList<ParameterizedAttribute>();
      for (ParameterizedAttribute param : nativeCodeParams)
      {
         // Only select the native code clauses for which the following expressions all evaluate to true
         //  * osname ~= [org.osgi.framework.os.name]
         //  * processor ~= [org.osgi.framework.processor]
         //  * osversion range includes [org.osgi.framework.os.version] or osversion is not specified
         //  * language ~= [org.osgi.framework.language] or language is not specified
         //  * selection-filter evaluates to true when using the values of the system properties or selection-filter is not specified

         Parameter osnameParam = param.getAttribute(Constants.BUNDLE_NATIVECODE_OSNAME);
         Parameter procParam = param.getAttribute(Constants.BUNDLE_NATIVECODE_PROCESSOR);
         //Parameter osversionParam = param.getAttribute(Constants.BUNDLE_NATIVECODE_OSVERSION);
         
         boolean match = (osnameParam != null);
         
         // osname ~= [org.osgi.framework.os.name]
         if (match && osnameParam != null)
         {
            String osname = (String)osnameParam.getValue();
            match = (osname.equals(fwOSName) || osname.equals(osAlias.get(fwOSName)));
         }
         
         // processor ~= [org.osgi.framework.processor]
         match &= (procParam != null);
         if (match && procParam != null)
         {
            String processor = (String)procParam.getValue();
            match = (processor.equals(fwProcessor) || processor.equals(processorAlias.get(fwProcessor)));
         }
         
         // [TODO] osversion range includes [org.osgi.framework.os.version] or osversion is not specified
         // [TODO] language ~= [org.osgi.framework.language] or language is not specified
         // [TODO] selection-filter evaluates to true when using the values of the system properties or selection-filter is not specified
         
         if (match == true)
            matchedParams.add(param);
      }
      
      // If no native clauses were selected in step 1, this algorithm is terminated
      // and a BundleException is thrown if the optional clause is not present
      if (matchedParams.size() == 0)
      {
         // [TODO] optional
         throw new DeploymentException("No native clauses selected from: " + nativeCodeParams);
      }
      
      // The selected clauses are now sorted in the following priority order:
      //  * osversion: floor of the osversion range in descending order, osversion not specified
      //  * language: language specified, language not specified
      //  * Position in the Bundle-NativeCode manifest header: lexical left to right
      if (matchedParams.size() > 1)
      {
         // [TODO] selected clauses are now sorted
      }

      // The first clause of the sorted clauses from step 3 must be used as the selected native code clause
      ParameterizedAttribute selectedParams = matchedParams.get(0);
      log.debug("Selected native code clause: " + selectedParams);
      
      String nativeLib = selectedParams.getAttribute();
      URL entryURL = bundleState.getEntry(nativeLib);
      
      // If a native code library in a selected native code clause cannot be found
      // within the bundle then the bundle must fail to resolve
      if (entryURL == null)
         throw new DeploymentException("Cannot find native library: " + nativeLib);

      // Copy the native library to the bundle storage area
      File nativeFileCopy;
      try
      {
         VirtualFile nativeVirtualFile = bundleState.getRoot().getChild(nativeLib);
         BundleStoragePlugin plugin = bundleManager.getPlugin(BundleStoragePlugin.class);
         nativeFileCopy = plugin.getDataFile(bundleState, nativeLib);
         FileOutputStream fos = new FileOutputStream(nativeFileCopy);
         VFSUtils.copyStream(nativeVirtualFile.openStream(), fos);
         fos.close();
      }
      catch (IOException ex)
      {
         throw new DeploymentException("Cannot copy native library: " + nativeLib, ex);
      }
      
      // Generate the key for the library mapping
      String libfile = new File(nativeLib).getName();
      String libname = libfile.substring(0, libfile.lastIndexOf('.'));
      
      // Add the native library mapping to the OSGiClassLoaderPolicy
      OSGiClassLoaderPolicy policy = (OSGiClassLoaderPolicy)unit.getAttachment(ClassLoaderPolicy.class);
      policy.addLibraryMapping(libname, nativeFileCopy);
   }
}
