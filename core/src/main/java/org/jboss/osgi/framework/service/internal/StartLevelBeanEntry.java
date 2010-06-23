/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.osgi.framework.service.internal;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
@XmlType(name = "startLevelBundleEntry", propOrder = {})
public class StartLevelBeanEntry
{
   private int startLevel = 1;
   private String bundleSymbolicName;
   private String version = "0.0.0";

   public int getStartLevel()
   {
      return startLevel;
   }

   @XmlAttribute(name = "start-level")
   public void setStartLevel(int sl)
   {
      startLevel = sl;
   }

   public String getSymbolicName()
   {
      return bundleSymbolicName;
   }

   @XmlAttribute(name = "symbolic-name", required = true)
   public void setSymbolicName(String sn)
   {
      bundleSymbolicName = sn;
   }

   public String getVersion()
   {
      return version;
   }

   @XmlAttribute(name = "version")
   public void setVersion(String v)
   {
      version = v;
   }

   @Override
   public String toString()
   {
      return "bundle symbolic name=" + bundleSymbolicName +
            " version=" + version +
            " start level=" + startLevel;
   }
}
