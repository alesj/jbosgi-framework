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
package org.jboss.test.osgi.core.jbosgi326;

// $Id: $

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Register a simple service
 * 
 * @author thomas.diesler@jboss.com
 * @since 07-May-2010
 */
public class OSGi326Activator implements BundleActivator
{
   @Override
   public void start(BundleContext context) throws Exception
   {
      context.registerService(SomeService.class.getName(), new SomeServiceA(), null);
   }

   @Override
   public void stop(BundleContext context) throws Exception
   {
      // nothing to do
   }
}
