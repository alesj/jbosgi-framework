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

import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;

/**
 * An implementation of the {@link ExportedPackage} that is backed by a {@link PackageCapability}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Feb-2010
 */
class CapabilityExportedPackage extends AbstractExportedPackage
{
   CapabilityExportedPackage(AbstractBundleState bundle, PackageCapability capability)
   {
      super(bundle, capability.getName());
      setVersion(Version.parseVersion(capability.getVersion().toString()));
   }
}