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

// $Id$

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;
import org.jboss.osgi.framework.metadata.NativeLibrary;
import org.jboss.osgi.framework.metadata.NativeLibraryMetaData;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.osgi.framework.Constants;

/**
 * A deployer that takes care of loading native code libraries.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 19-Dec-2009
 */
public class OSGiNativeCodeMetaDataDeployer extends AbstractRealDeployer
{
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

   public OSGiNativeCodeMetaDataDeployer()
   {
      setInput(ClassLoadingMetaData.class);
      setStage(DeploymentStages.POST_PARSE);
      setTopLevelOnly(true);
   }

   @SuppressWarnings("unchecked")
   @Override
   protected void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      AbstractBundleState absBundleState = unit.getAttachment(AbstractBundleState.class);
      if ((absBundleState instanceof OSGiBundleState) == false)
         return;

      OSGiClassLoadingMetaData classLoadingMetaData = (OSGiClassLoadingMetaData)unit.getAttachment(ClassLoadingMetaData.class);
      if (classLoadingMetaData == null)
         throw new IllegalStateException("No ClassLoadingMetaData");

      OSGiBundleState bundleState = (OSGiBundleState)absBundleState;
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      OSGiMetaData osgiMetaData = bundleState.getOSGiMetaData();
      List<ParameterizedAttribute> nativeCodeParams = osgiMetaData.getBundleNativeCode();
      if (nativeCodeParams == null)
         return;

      // Find the matching parameters
      List<ParameterizedAttribute> matchedParams = new ArrayList<ParameterizedAttribute>();
      for (ParameterizedAttribute param : nativeCodeParams)
      {
         if (matchParameter(bundleManager, param))
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

      NativeLibraryMetaData nativeLibraries = classLoadingMetaData.getNativeLibraries();
      for (ParameterizedAttribute param : matchedParams)
      {
         Parameter osnameParam = param.getAttribute(Constants.BUNDLE_NATIVECODE_OSNAME);
         Parameter procParam = param.getAttribute(Constants.BUNDLE_NATIVECODE_PROCESSOR);
         //Parameter osversionParam = param.getAttribute(Constants.BUNDLE_NATIVECODE_OSVERSION);

         List<String> osNames;
         if (osnameParam.isCollection())
            osNames = (List<String>)osnameParam.getValue();
         else
            osNames = Collections.singletonList((String)osnameParam.getValue());

         String libpath = param.getAttribute();
         String libsource = bundleState.getCanonicalName();

         NativeLibrary library = new NativeLibrary(osNames, libpath, libsource);
         
         // Processors
         if (procParam != null)
         {
            List<String> processors;
            if (procParam.isCollection())
               processors = (List<String>)procParam.getValue();
            else
               processors = Collections.singletonList((String)procParam.getValue());
            
            library.setProcessors(processors);
         }
         
         // [TODO] osVersions, languages, selectionFilter, optional
         // library.setOsVersions(osVersions);
         // library.setLanguages(languages);
         // library.setSelectionFilter(selectionFilter);
         // library.setOptional(optional);
         
         nativeLibraries.addNativeLibrary(library);
      }
   }

   @SuppressWarnings("unchecked")
   private boolean matchParameter(OSGiBundleManager bundleManager, ParameterizedAttribute param)
   {
      String fwOSName = bundleManager.getProperty(Constants.FRAMEWORK_OS_NAME);
      String fwProcessor = bundleManager.getProperty(Constants.FRAMEWORK_PROCESSOR);
      //String fwOSVersion = bundleManager.getProperty(Constants.FRAMEWORK_OS_VERSION);

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
      if (match == true && osnameParam != null)
      {
         List<String> osNames;
         if (osnameParam.isCollection())
            osNames = (List<String>)osnameParam.getValue();
         else
            osNames = Collections.singletonList((String)osnameParam.getValue());

         boolean osmatch = false;
         for (String osname : osNames)
         {
            osmatch = (osname.equals(fwOSName) || osname.equals(osAlias.get(fwOSName)));
            if (osmatch == true)
               break;
         }

         match &= osmatch;
      }

      // processor ~= [org.osgi.framework.processor]
      match &= (procParam != null);
      if (match && procParam != null)
      {
         List<String> processors;
         if (procParam.isCollection())
            processors = (List<String>)procParam.getValue();
         else
            processors = Collections.singletonList((String)procParam.getValue());

         boolean procmatch = false;
         for (String proc : processors)
         {
            procmatch = (proc.equals(fwProcessor) || proc.equals(processorAlias.get(fwProcessor)));
            if (procmatch == true)
               break;
         }

         match &= procmatch;
      }

      // [TODO] osversion range includes [org.osgi.framework.os.version] or osversion is not specified
      // [TODO] language ~= [org.osgi.framework.language] or language is not specified
      // [TODO] selection-filter evaluates to true when using the values of the system properties or selection-filter is not specified
      return match;
   }
}
