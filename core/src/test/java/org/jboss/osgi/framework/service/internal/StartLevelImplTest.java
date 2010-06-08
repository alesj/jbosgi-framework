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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.concurrent.Executor;

import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleWrapper;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.startlevel.StartLevel;


/**
 * Unit tests for the org.jboss.osgi.framework.service.internal.StartLevelImpl class.
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class StartLevelImplTest
{
   @Test
   public void testInitialStartLevel()
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      StartLevel sl = new StartLevelImpl(bm);
      assertEquals(1, sl.getInitialBundleStartLevel());

      sl.setInitialBundleStartLevel(42);
      assertEquals(42, sl.getInitialBundleStartLevel());
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testStartStopService()
   {
      ServiceRegistration sr = mock(ServiceRegistration.class);
      BundleContext sc = mock(BundleContext.class);
      stub(sc.registerService((String)any(), any(), (Dictionary)any())).toReturn(sr);

      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      stub(bm.getSystemContext()).toReturn(sc);
      StartLevelImpl sl = new StartLevelImpl(bm);

      sl.startService();
      verify(sc).registerService(StartLevel.class.getName(), sl, null);
      verifyNoMoreInteractions(sc);
      verifyZeroInteractions(sr);

      sl.stopService();
      verifyNoMoreInteractions(sc);
      verify(sr).unregister();
      verifyNoMoreInteractions(sr);
   }

   @Test
   public void testIsBundleStartLevel()
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      StartLevel sl = new StartLevelImpl(bm);

      OSGiBundleState b = mock(OSGiBundleState.class);
      stub(b.getStartLevel()).toReturn(17);

      assertEquals(17, sl.getBundleStartLevel(b));
   }

   @Test
   public void testIncreaseDecreaseStartLevel() throws Exception
   {
      OSGiBundleState b1 = mock(OSGiBundleState.class);
      stub(b1.isPersistentlyStarted()).toReturn(false);
      stub(b1.getStartLevel()).toReturn(1);
      
      OSGiBundleState b2 = mock(OSGiBundleState.class);
      stub(b2.isPersistentlyStarted()).toReturn(true);
      stub(b2.getStartLevel()).toReturn(1);
      
      OSGiBundleState b3 = mock(OSGiBundleState.class);
      stub(b3.isPersistentlyStarted()).toReturn(true);
      stub(b3.getStartLevel()).toReturn(3);

      OSGiBundleState b4 = mock(OSGiBundleState.class);
      stub(b4.isPersistentlyStarted()).toReturn(true);
      stub(b4.getStartLevel()).toReturn(4);

      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      Collection<AbstractBundleState> bundles = Arrays.<AbstractBundleState> asList(b1, b2, b3, b4);
      stub(bm.getBundles()).toReturn(bundles);
      StartLevelImpl sl = new StartLevelImpl(bm);
      
      assertEquals("Precondition failed", 0, sl.getStartLevel());
      sl.increaseStartLevel(3);
      verify(b1, never()).start(anyInt());
      verify(b2).start(Bundle.START_TRANSIENT);
      verify(b3).start(Bundle.START_TRANSIENT);
      verify(b4, never()).start(anyInt());
      verify(b1, never()).stop(anyInt());
      verify(b2, never()).stop(anyInt());
      verify(b3, never()).stop(anyInt());
      verify(b4, never()).stop(anyInt());
      assertEquals(3, sl.getStartLevel());

      sl.increaseStartLevel(4);
      verify(b1, never()).start(anyInt());
      verify(b2).start(Bundle.START_TRANSIENT);
      verify(b3).start(Bundle.START_TRANSIENT);
      verify(b4).start(Bundle.START_TRANSIENT);
      verify(b1, never()).stop(anyInt());
      verify(b2, never()).stop(anyInt());
      verify(b3, never()).stop(anyInt());
      verify(b4, never()).stop(anyInt());
      assertEquals(4, sl.getStartLevel());

      sl.decreaseStartLevel(2);
      verify(b1, never()).start(anyInt());
      verify(b2).start(Bundle.START_TRANSIENT);
      verify(b3).start(Bundle.START_TRANSIENT);
      verify(b4).start(Bundle.START_TRANSIENT);
      verify(b1, never()).stop(anyInt());
      verify(b2, never()).stop(anyInt());
      verify(b3).stop(Bundle.STOP_TRANSIENT);
      verify(b4).stop(Bundle.STOP_TRANSIENT);
      assertEquals(2, sl.getStartLevel());
   }

   @Test
   public void testSetStartLevel()
   {
      final StringBuffer trace = new StringBuffer();

      Bundle sb = mock(Bundle.class);
      BundleContext sbc = mock(BundleContext.class);
      stub(sbc.getBundle()).toReturn(sb);

      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      StartLevelImpl sl = new StartLevelImpl(bm)
      {
         @Override
         public synchronized void increaseStartLevel(int sl)
         {
            trace.append("increased");
            super.increaseStartLevel(sl);
         }

         @Override
         void decreaseStartLevel(int sl)
         {
            trace.append("decreased");
            super.decreaseStartLevel(sl);
         }
      };
      stub(sl.getSystemContext()).toReturn(sbc);

      sl.executor = new SameThreadTestExecutor();

      FrameworkEventsPlugin ep = mock(FrameworkEventsPlugin.class);
      sl.eventsPlugin = ep;

      assertEquals("Precondition failed", 0, sl.getStartLevel());
      assertEquals("Precondition failed", "", trace.toString());
      verify(ep, never()).fireFrameworkEvent(sb, FrameworkEvent.STARTLEVEL_CHANGED, null);
      sl.setStartLevel(2);
      assertEquals("increased", trace.toString());
      assertEquals(2, sl.getStartLevel());
      verify(ep, times(1)).fireFrameworkEvent(sb, FrameworkEvent.STARTLEVEL_CHANGED, null);
      sl.setStartLevel(1);
      assertEquals("increaseddecreased", trace.toString());
      assertEquals(1, sl.getStartLevel());
      verify(ep, times(2)).fireFrameworkEvent(sb, FrameworkEvent.STARTLEVEL_CHANGED, null);
   }

   @Test
   public void testIsBundlePersistentlyStarted()
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      StartLevel sl = new StartLevelImpl(bm);
      
      AbstractBundleState abs1 = mock(OSGiBundleState.class);
      stub(abs1.isPersistentlyStarted()).toReturn(true);
      assertTrue(sl.isBundlePersistentlyStarted(abs1));

      AbstractBundleState abs2 = mock(OSGiBundleState.class);
      stub(abs2.isPersistentlyStarted()).toReturn(false);
      assertFalse(sl.isBundlePersistentlyStarted(abs2));
      
      AbstractBundleState abs3 = mock(OSGiBundleState.class);
      stub(abs3.isPersistentlyStarted()).toReturn(true);
      OSGiBundleWrapper bw = new OSGiBundleWrapper(abs3);
      assertTrue(sl.isBundlePersistentlyStarted(bw));
   }

   @Test
   public void testSetBundleStartLevel() throws Exception
   {
      Bundle sb = mock(Bundle.class);
      BundleContext sbc = mock(BundleContext.class);
      stub(sbc.getBundle()).toReturn(sb);

      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      StartLevelImpl sl = new StartLevelImpl(bm);
      sl.executor = new SameThreadTestExecutor();
      sl.eventsPlugin = mock(FrameworkEventsPlugin.class);
      stub(sl.getSystemContext()).toReturn(sbc);

      sl.setStartLevel(5);

      OSGiBundleState abs = mock(OSGiBundleState.class);
      verify(abs, never()).start(anyInt());
      sl.setBundleStartLevel(abs, 2);
      verify(abs).start(Bundle.START_TRANSIENT);
      verify(abs).setStartLevel(2);

      verify(abs, never()).stop(anyInt());
      sl.setBundleStartLevel(abs, 8);
      verify(abs).stop(Bundle.STOP_TRANSIENT);
      verify(abs).setStartLevel(8);
   }

   private static final class SameThreadTestExecutor implements Executor
   {
      @Override
      public void execute(Runnable command)
      {
         command.run();
      }
   }
}
