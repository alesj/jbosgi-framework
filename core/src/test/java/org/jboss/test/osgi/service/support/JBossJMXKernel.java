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
package org.jboss.test.osgi.service.support;

import org.jboss.beans.metadata.api.annotations.Constructor;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.basic.KernelConstants;

/**
 * Sets up MBeanServer impl.
 *
 * JDK = org.jboss.jmx.mbeanserver.JBossMBeanServerBuilder
 * JBossMX = org.jboss.mx.server.MBeanServerBuilderImpl
 *
 * by default we use JDK's.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class JBossJMXKernel extends JMXKernel
{
   private static final String MBEAN_SERVER_BUILDER_CLASS_PROPERTY = "javax.management.builder.initial";
   private static final String DEFAULT_MBEAN_SERVER_BUILDER_CLASS = "org.jboss.jmx.mbeanserver.JBossMBeanServerBuilder";
   private String builderClass;

   @Constructor
   public JBossJMXKernel(@Inject(bean = KernelConstants.KERNEL_NAME) Kernel kernel, @Inject ClassLoaderSystem system)
   {
      super(kernel, system);
   }

   protected void addProperties()
   {
      super.addProperties();
      if (builderClass == null)
         builderClass = DEFAULT_MBEAN_SERVER_BUILDER_CLASS;
      System.setProperty(MBEAN_SERVER_BUILDER_CLASS_PROPERTY, builderClass);
   }

   protected void removeProperties()
   {
      System.clearProperty(MBEAN_SERVER_BUILDER_CLASS_PROPERTY);
      super.removeProperties();
   }

   public void setBuilderClass(String builderClass)
   {
      this.builderClass = builderClass;
   }
}
