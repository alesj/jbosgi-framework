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
package org.jboss.osgi.framework.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.spi.MutableMetaData;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.plugins.StartLevelPlugin;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class OSGiBundleStateTest
{
   @Test
   public void testStart() throws Exception
   {
      StartLevelPlugin sls = mock(StartLevelPlugin.class);
      stub(sls.getStartLevel()).toReturn(1);

      // Set up the OSGiBundleState
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      stub(bm.getPlugin(StartLevelPlugin.class)).toReturn(sls);
      OSGiBundleState bs = new OSGiBundleState(bm, mockDeploymentUnit());

      // Initialise the Bundle State to be installed
      bs.changeState(Bundle.INSTALLED);
      
      assertFalse("Precondition failed", bs.isPersistentlyStarted());
      bs.start(0);
      assertTrue(bs.isPersistentlyStarted());
      verify(bm).startBundle(bs);
   }

   @Test
   public void testNoStart() throws Exception
   {
      StartLevelPlugin sls = mock(StartLevelPlugin.class);
      stub(sls.getStartLevel()).toReturn(1);

      // Set up the OSGiBundleState
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      stub(bm.getPlugin(StartLevelPlugin.class)).toReturn(sls);
      OSGiBundleState bs = new OSGiBundleState(bm, mockDeploymentUnit());

      // Initialise the Bundle State to be installed
      bs.changeState(Bundle.INSTALLED);
      bs.setStartLevel(5); // higher than the system start level

      assertFalse("Precondition failed", bs.isPersistentlyStarted());
      bs.start(0);
      assertTrue(bs.isPersistentlyStarted());
      verify(bm, never()).startBundle((DeployedBundleState)any());
   }

   @Test
   public void testStop() throws Exception
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      OSGiBundleState obs = new OSGiBundleState(bm, mockDeploymentUnit());
      obs.changeState(Bundle.INSTALLED);
      obs.setPersistentlyStarted(true);

      assertTrue("Precondition failed", obs.isPersistentlyStarted());
      obs.stop(0);

      verify(bm).stopBundle(obs);
      assertFalse(obs.isPersistentlyStarted());
   }

   @Test
   public void testStopTransient() throws Exception
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      OSGiBundleState obs = new OSGiBundleState(bm, mockDeploymentUnit());
      obs.changeState(Bundle.INSTALLED);
      obs.setPersistentlyStarted(true);

      assertTrue("Precondition failed", obs.isPersistentlyStarted());
      obs.stop(Bundle.STOP_TRANSIENT);

      verify(bm).stopBundle(obs);
      assertTrue("Stopped transiently, should not have changed the persistence.",
            obs.isPersistentlyStarted());
   }

   @Test
   public void testConstructor()
   {
      StartLevelPlugin sls = mock(StartLevelPlugin.class);
      stub(sls.getInitialBundleStartLevel()).toReturn(42);

      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      stub(bm.getOptionalPlugin(StartLevelPlugin.class)).toReturn(sls);
      OSGiBundleState obs = new OSGiBundleState(bm, mockDeploymentUnit());

      assertEquals(42, obs.getStartLevel());
   }

   @Test
   public void testStartLevel()
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      OSGiBundleState obs = new OSGiBundleState(bm, mockDeploymentUnit());
      assertEquals(1, obs.getStartLevel());

      obs.setStartLevel(15);
      assertEquals(15, obs.getStartLevel());
   }

   @Test
   public void testPersistentlyStarted()
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      OSGiBundleState obs = new OSGiBundleState(bm, mockDeploymentUnit());

      obs.setPersistentlyStarted(true);
      assertTrue(obs.isPersistentlyStarted());

      obs.setPersistentlyStarted(false);
      assertFalse(obs.isPersistentlyStarted());
   }

   private VFSDeploymentUnit mockDeploymentUnit()
   {
      VFSDeploymentUnit du = mock(VFSDeploymentUnit.class);
      MutableMetaData mmd = mock(MutableMetaData.class);
      stub(du.getMutableMetaData()).toReturn(mmd);
      OSGiMetaData omd = mock(OSGiMetaData.class);
      stub(du.getAttachment(OSGiMetaData.class)).toReturn(omd);
      return du;
   }
}
