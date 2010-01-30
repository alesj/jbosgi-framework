/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.framework.resolver.internal.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.metadata.Capability;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.classloading.OSGiPackageRequirement;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.resolver.AbstractResolver;
import org.jboss.osgi.framework.resolver.ExportPackage;
import org.jboss.osgi.framework.resolver.ResolverBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * The BundleResolver wires BundleRequirements to their corresponding BundleCapability.
 * 
 * @author thomas.diesler@jboss.com
 * @since 10-Sep-2009
 */
public class BasicResolverImpl extends AbstractResolver 
{
   /** The log */
   private static final Logger log = Logger.getLogger(BasicResolverImpl.class);

   private Map<OSGiBundleState, List<BundleCapability>> bundleCapabilitiesMap = new ConcurrentHashMap<OSGiBundleState, List<BundleCapability>>();
   private Map<OSGiBundleState, List<BundleRequirement>> bundleRequirementsMap = new ConcurrentHashMap<OSGiBundleState, List<BundleRequirement>>();

   public BasicResolverImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   @Override
   public ResolverBundle addBundle(Bundle bundle)
   {
      // Ignore the system bundle and fragments
      AbstractBundleState bundleState = AbstractBundleState.assertBundleState(bundle);
      if (bundleState instanceof OSGiBundleState == false)
         return null;
      
      return super.addBundle(bundle);
   }

   public ResolverBundle removeBundle(Bundle bundle)
   {
      ResolverBundle removedBundle = super.removeBundle(bundle);
      
      AbstractDeployedBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      bundleCapabilitiesMap.remove(bundleState);

      List<BundleRequirement> bundleRequirements = bundleRequirementsMap.remove(bundleState);
      if (bundleRequirements != null)
      {
         for (BundleRequirement requirement : bundleRequirements)
         {
            requirement.unwireCapability();
         }
      }
      return removedBundle;
   }

   public List<ResolverBundle> resolve(List<Bundle> bundles)
   {
      List<ResolverBundle> resolvedBundles = new ArrayList<ResolverBundle>();
      for (AbstractDeployedBundleState aux : resolveBundles(bundles))
      {
         ResolverBundle resBundle = getBundle(aux);
         if (resBundle == null)
            throw new IllegalStateException("Cannot obtain bundle for: " + aux);
         resolvedBundles.add(resBundle);
      }
      return Collections.unmodifiableList(resolvedBundles);
   }

   /**
    * Resolve the given list of bundles.
    * 
    * This is an iterative process that tries to wire BundleRequirements to BundleCapabilities.
    * 
    * @param bundles the bundles to resolve
    * @return The list of resolved bundles in the resolve order or an empty list
    */
   private List<OSGiBundleState> resolveBundles(List<Bundle> bundles)
   {
      if (bundles == null)
         throw new IllegalArgumentException("Null bundles");

      // Normalize to OSGiBundleState instances
      List<OSGiBundleState> unresolvedBundles = new ArrayList<OSGiBundleState>();
      for (Bundle aux : bundles)
      {
         if (aux.getBundleId() != 0)
            unresolvedBundles.add(OSGiBundleState.assertBundleState(aux));
      }

      int resolved = 1;
      int resolveRounds = 0;

      // Get the list of all capabilities
      List<BundleCapability> allCapabilities = new ArrayList<BundleCapability>();
      for (List<BundleCapability> list : bundleCapabilitiesMap.values())
      {
         allCapabilities.addAll(list);
      }

      List<OSGiBundleState> resolvedBundles = new ArrayList<OSGiBundleState>();
      while (resolved > 0 && unresolvedBundles.isEmpty() == false)
      {
         resolveRounds++;

         log.debug("#" + resolveRounds + " *****************************************************************");
         log.debug("Unresolved bundles: " + unresolvedBundles);

         resolved = 0;
         Iterator<OSGiBundleState> it = unresolvedBundles.iterator();
         while (it.hasNext())
         {
            OSGiBundleState bundleState = it.next();
            log.debug("Resolving: " + bundleState);
            if (resolveBundle(allCapabilities, bundleState))
            {
               resolvedBundles.add(bundleState);
               it.remove();
               resolved++;
            }
         }
      }

      log.debug("END *****************************************************************");

      // Log the unresolved bundles
      for (AbstractDeployedBundleState bundle : unresolvedBundles)
      {
         StringBuffer message = new StringBuffer("Unresolved bundle: " + bundle);
         message.append("\n  Cannot find exporter for");
         List<BundleRequirement> bundleRequirements = getBundleRequirements(bundle);
         for (BundleRequirement requirement : bundleRequirements)
         {
            PackageRequirement packreq = requirement.getPackageRequirement();
            BundleCapability bestMatch = findBestMatch(allCapabilities, requirement);
            if (bestMatch == null && packreq.isOptional() == false && packreq.isDynamic() == false)
            {
               message.append("\n    " + packreq.getName() + ";version=" + packreq.getVersionRange());
            }
         }
         log.debug(message);
      }

      return resolvedBundles;
   }

   public ExportPackage getExporter(Bundle bundle, String importPackage)
   {
      AbstractDeployedBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      BundleCapability match = getMatchingCapability(bundleState, importPackage);
      if (match == null)
         return null;

      AbstractDeployedBundleState exportingBundle = match.getExportingBundle();
      ResolverBundle resolverBundle = getBundle(exportingBundle);
      return resolverBundle.getExportPackage(importPackage);
   }

   private BundleCapability getMatchingCapability(AbstractDeployedBundleState bundle, String importPackage)
   {
      List<BundleRequirement> requirements = bundleRequirementsMap.get(bundle);
      if (requirements == null)
         return null;

      BundleCapability result = null;
      for (BundleRequirement aux : requirements)
      {
         String auxName = aux.getPackageRequirement().getName();
         if (auxName.equals(importPackage))
         {
            result = aux.getWiredCapability();
            break;
         }
      }

      return result;
   }

   private boolean resolveBundle(List<BundleCapability> allCapabilities, OSGiBundleState bundle)
   {
      List<BundleCapability> bundleCapabilities = getBundleCapabilities(bundle);
      List<BundleRequirement> bundleRequirements = getBundleRequirements(bundle);

      List<BundleCapability> futureCapabilities = new ArrayList<BundleCapability>(allCapabilities);
      futureCapabilities.addAll(bundleCapabilities);

      for (BundleRequirement requirement : bundleRequirements)
      {
         PackageRequirement packreq = requirement.getPackageRequirement();

         BundleCapability bestMatch = findBestMatch(futureCapabilities, requirement);
         if (bestMatch == null && packreq.isOptional() == false && packreq.isDynamic() == false)
            return false;

         requirement.wireCapability(bestMatch);
      }

      // Remove optional or dynamic requirements that don't have a wire 
      Iterator<BundleRequirement> it = bundleRequirements.iterator();
      while (it.hasNext())
      {
         if (it.next().getWiredCapability() == null)
            it.remove();
      }

      if (processRequiredBundle(bundle, bundleCapabilities, bundleRequirements) == false)
         return false;

      allCapabilities.addAll(bundleCapabilities);
      bundleCapabilitiesMap.put(bundle, bundleCapabilities);
      bundleRequirementsMap.put(bundle, bundleRequirements);

      logResolvedBundleInfo(bundle, bundleCapabilities, bundleRequirements);

      return true;
   }

   /**
    * Logs information about a resolved bundle
    */
   private void logResolvedBundleInfo(AbstractDeployedBundleState bundle, List<BundleCapability> bundleCapabilities, List<BundleRequirement> bundleRequirements)
   {
      // Log the package wiring information
      StringBuffer message = new StringBuffer("Resolved: " + bundle);

      // Log the exports
      int nameLengthMax = 0;
      for (BundleCapability capability : bundleCapabilities)
      {
         PackageCapability packcap = capability.getPackageCapability();
         String packNameVersion = packcap.getName() + ";version=" + packcap.getVersion();
         nameLengthMax = Math.max(nameLengthMax, packNameVersion.length());
      }
      if (bundleCapabilities.isEmpty() == false)
      {
         message.append("\n  Exports");
         List<String> lines = new ArrayList<String>();
         for (BundleCapability capability : bundleCapabilities)
         {
            PackageCapability packcap = capability.getPackageCapability();
            String packNameVersion = packcap.getName() + ";version=" + packcap.getVersion();
            lines.add("\n    " + packNameVersion);
         }
         Collections.sort(lines);
         for (String line : lines)
            message.append(line);
      }

      // Log the imports
      nameLengthMax = 0;
      for (BundleRequirement requirement : bundleRequirements)
      {
         PackageRequirement packreq = requirement.getPackageRequirement();
         String packNameVersion = packreq.getName() + ";version=" + packreq.getVersionRange();
         nameLengthMax = Math.max(nameLengthMax, packNameVersion.length());
      }
      if (bundleRequirements.isEmpty() == false)
      {
         message.append("\n  Imports");
         List<String> lines = new ArrayList<String>();
         for (BundleRequirement requirement : bundleRequirements)
         {
            PackageRequirement packreq = requirement.getPackageRequirement();
            String packNameVersion = packreq.getName() + ";version=" + packreq.getVersionRange();
            StringBuffer line = new StringBuffer("\n    " + packNameVersion);
            for (int i = 0; i < (nameLengthMax - packNameVersion.length()); i++)
               line.append(" ");

            BundleCapability wire = requirement.getWiredCapability();
            if (wire == null)
            {
               line.append(" <= null");
            }
            else
            {
               Bundle wireBundle = wire.getExportingBundle();
               PackageCapability wireCap = wire.getPackageCapability();
               packNameVersion = wireCap.getName() + ";version=" + wireCap.getVersion();
               line.append(" <= " + wireBundle + " " + packNameVersion);
            }
            lines.add(line.toString());
         }
         Collections.sort(lines);
         for (String line : lines)
            message.append(line);
      }
      log.debug(message);
   }

   private BundleCapability findBestMatch(List<BundleCapability> capabilities, BundleRequirement requirement)
   {
      BundleCapability result = null;
      for (BundleCapability capability : capabilities)
      {
         if (capability.matches(requirement))
         {
            if (result == null)
            {
               result = capability;
            }
            else
            {
               // [TODO] handle multiple matches
            }
         }
      }
      return result;
   }

   /**
    * Get the set of bundle capabilities
    */
   private List<BundleCapability> getBundleCapabilities(AbstractDeployedBundleState bundle)
   {
      List<BundleCapability> result = new ArrayList<BundleCapability>();

      AbstractDeployedBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      DeploymentUnit unit = bundleState.getDeploymentUnit();
      ClassLoadingMetaData metadata = unit.getAttachment(ClassLoadingMetaData.class);

      List<Capability> capabilities = metadata.getCapabilities().getCapabilities();
      if (capabilities != null)
      {
         for (Capability capability : capabilities)
         {
            if (capability instanceof PackageCapability)
            {
               PackageCapability packageCapability = (PackageCapability)capability;
               result.add(new BundleCapability(bundle, packageCapability));
            }
         }
      }
      return result;
   }

   /**
    * Get the set of bundle requirements
    */
   private List<BundleRequirement> getBundleRequirements(AbstractDeployedBundleState bundle)
   {
      List<BundleRequirement> result = new ArrayList<BundleRequirement>();

      AbstractDeployedBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      DeploymentUnit unit = bundleState.getDeploymentUnit();
      ClassLoadingMetaData classloadingMetaData = unit.getAttachment(ClassLoadingMetaData.class);

      List<Requirement> requirements = classloadingMetaData.getRequirements().getRequirements();
      if (requirements != null)
      {
         for (Requirement requirement : requirements)
         {
            if (requirement instanceof PackageRequirement)
            {
               PackageRequirement packageRequirement = (PackageRequirement)requirement;
               result.add(new BundleRequirement(bundle, packageRequirement));
            }
         }
      }

      return result;
   }

   private boolean processRequiredBundle(AbstractDeployedBundleState bundle, List<BundleCapability> bundleCapabilities, List<BundleRequirement> bundleRequirements)
   {
      // The Require-Bundle header specifies that all exported packages from
      // another bundle must be imported, effectively requiring the public interface
      // of another bundle
      OSGiMetaData osgiMetaData = bundle.getOSGiMetaData();
      List<ParameterizedAttribute> requireBundles = osgiMetaData.getRequireBundles();
      if (requireBundles != null)
      {
         for (ParameterizedAttribute attr : requireBundles)
         {
            String requiredBundleName = attr.getAttribute();
            if (requiredBundleName == null)
               throw new IllegalStateException("Cannot obtain value for: " + Constants.REQUIRE_BUNDLE);

            String visibility = attr.getDirectiveValue(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_PRIVATE, String.class);

            String resolution = attr.getDirectiveValue(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_MANDATORY, String.class);

            // [TODO] bundle-version
            //String bundleVersion = attr.getAttributeValue(Constants.BUNDLE_VERSION_ATTRIBUTE, String.class);

            if (Constants.RESOLUTION_MANDATORY.equals(resolution))
            {
               Bundle requiredBundle = getRequiredBundle(requiredBundleName);
               if (requiredBundle == null)
               {
                  log.debug("Cannot find " + resolution + " required bundle: " + requiredBundleName);
                  return false;
               }

               List<BundleCapability> otherCapabilities = bundleCapabilitiesMap.get(requiredBundle);
               for (BundleCapability otherCapability : otherCapabilities)
               {
                  PackageCapability otherPackage = otherCapability.getPackageCapability();
                  String packageName = otherPackage.getName();
                  Object version = otherPackage.getVersion();
                  VersionRange versionRange = new VersionRange(version, true, version, true);

                  OSGiPackageRequirement newPackageRequirement = new OSGiPackageRequirement(bundle, packageName, versionRange, null);
                  BundleRequirement newBundleRequirement = new BundleRequirement(bundle, newPackageRequirement);
                  newBundleRequirement.wireCapability(otherCapability);
                  bundleRequirements.add(newBundleRequirement);

                  if (Constants.VISIBILITY_REEXPORT.equals(visibility))
                  {
                     BundleCapability newBundleCapability = new BundleCapability(bundle, otherPackage);
                     bundleCapabilities.add(newBundleCapability);
                  }
               }
            }
            else if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
            {
               // [TODO] resolution=optional
            }
         }
      }

      return true;
   }

   private Bundle getRequiredBundle(String requiredBundle)
   {
      for (Bundle bundle : bundleCapabilitiesMap.keySet())
      {
         if (bundle.getSymbolicName().equals(requiredBundle))
            return bundle;
      }
      return null;
   }
}