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
package org.jboss.osgi.framework.packageadmin;

//$Id: StartLevelImpl.java 93118 2009-09-02 08:24:44Z thomas.diesler@jboss.com $

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;

/**
 * An implementation of the {@link ExportedPackage}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Feb-2010
 */
abstract class AbstractExportedPackage implements ExportedPackage
{
   /** The log */
   private static final Logger log = Logger.getLogger(AbstractExportedPackage.class);
   
   private AbstractBundleState bundle;
   private String packageName;
   private Version version;

   AbstractExportedPackage(AbstractBundleState bundle, String packageName)
   {
      this.bundle = bundle;
      this.packageName = packageName;
      this.version = Version.emptyVersion;
   }

   void setVersion(Version version)
   {
      this.version = version;
   }

   public Bundle getExportingBundle()
   {
      return bundle;
   }

   public Bundle[] getImportingBundles()
   {
      // [TODO] Not implemented getImportingBundles
      log.info("Not implemented getImportingBundles");
      return null;
   }

   public String getName()
   {
      return packageName;
   }

   public String getSpecificationVersion()
   {
      // [TODO] Not implemented getSpecificationVersion
      log.info("Not implemented getSpecificationVersion");
      return null;
   }

   public Version getVersion()
   {
      return version;
   }

   public boolean isRemovalPending()
   {
      // [TODO] Not implemented isRemovalPending
      log.info("Not implemented isRemovalPending");
      return false;
   }
}