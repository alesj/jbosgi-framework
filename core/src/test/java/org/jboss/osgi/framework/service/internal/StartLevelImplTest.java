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
package org.jboss.osgi.framework.service.internal;

import static junit.framework.Assert.assertEquals;

import org.jboss.deployers.plugins.main.MainDeployerImpl;
import org.jboss.kernel.Kernel;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.plugins.FrameworkProperties;
import org.junit.Test;
import org.osgi.service.startlevel.StartLevel;


/**
 * Unit tests for the org.jboss.osgi.framework.service.internal.StartLevelImpl class.
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class StartLevelImplTest
{
   @Test
   public void testInitialStartLevel() {
      // david TODO use mock object for the OSGiBundleManager
      OSGiBundleManager bm = new OSGiBundleManager(new Kernel(), new MainDeployerImpl(), new FrameworkProperties(null));
      StartLevel sl = new StartLevelImpl(bm);
      assertEquals(1, sl.getInitialBundleStartLevel());
      
      sl.setInitialBundleStartLevel(42);
      assertEquals(42, sl.getInitialBundleStartLevel());
   }

}
