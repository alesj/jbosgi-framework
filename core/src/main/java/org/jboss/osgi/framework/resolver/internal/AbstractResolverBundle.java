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
package org.jboss.osgi.framework.resolver.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.resolver.ExportPackage;
import org.jboss.osgi.framework.resolver.ImportPackage;
import org.jboss.osgi.framework.resolver.RequiredBundle;
import org.jboss.osgi.framework.resolver.ResolverBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * An abstract ResolverBundle.
 * 
 * @author thomas.diesler@jboss.com
 * @since 20-Nov-2009
 */
public abstract class AbstractResolverBundle implements ResolverBundle
{
   protected AbstractBundleState bundleState;
   private boolean resolved;

   protected Map<String, ExportPackage> exportedPackages = new LinkedHashMap<String, ExportPackage>();
   protected Map<String, ImportPackage> importedPackages = new LinkedHashMap<String, ImportPackage>();
   protected Map<String, RequiredBundle> requiredBundles = new LinkedHashMap<String, RequiredBundle>();

   public AbstractResolverBundle(Bundle bundle)
   {
      this.bundleState = AbstractBundleState.assertBundleState(bundle);
   }

   public Bundle getBundle()
   {
      return bundleState.getBundleInternal();
   }

   public long getBundleId()
   {
      return bundleState.getBundleId();
   }

   public String getSymbolicName()
   {
      return bundleState.getSymbolicName();
   }

   public Version getVersion()
   {
      return bundleState.getVersion();
   }

   public int getState()
   {
      return bundleState.getState();
   }

   public ExportPackage getExportPackage(String packageName)
   {
      return exportedPackages.get(packageName);
   }

   public List<ExportPackage> getExportPackages()
   {
      List<ExportPackage> values = new ArrayList<ExportPackage>(exportedPackages.values());
      return Collections.unmodifiableList(values);
   }

   public ImportPackage getImportPackage(String packageName)
   {
      return importedPackages.get(packageName);
   }

   public List<ImportPackage> getImportPackages()
   {
      List<ImportPackage> values = new ArrayList<ImportPackage>(importedPackages.values());
      return Collections.unmodifiableList(values);
   }

   public List<RequiredBundle> getRequiredBundles()
   {
      List<RequiredBundle> values = new ArrayList<RequiredBundle>(requiredBundles.values());
      return Collections.unmodifiableList(values);
   }

   public RequiredBundle getRequiredBundle(String symbolicName)
   {
      return requiredBundles.get(symbolicName);
   }

   public boolean isResolved()
   {
      return resolved;
   }

   public void markResolved()
   {
      this.resolved = true;
   }

   public String toShortString()
   {
      return getSymbolicName() + "-" + getVersion();
   }

   @Override
   public String toString()
   {
      return "Bundle[" + toShortString() + "]";
   }
}