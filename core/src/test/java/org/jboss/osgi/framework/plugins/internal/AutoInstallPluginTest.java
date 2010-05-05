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
package org.jboss.osgi.framework.plugins.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

/**
 * Unit tests for the org.jboss.osgi.framework.plugins.internal.AutoInstallPluginImpl class.
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class AutoInstallPluginTest
{
   @Test
   public void testAutoInstallPlugin() throws Exception
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      when(bm.installBundle((URL)anyObject())).thenAnswer(new Answer<AbstractBundleState>()
      {
         public AbstractBundleState answer(InvocationOnMock invocation) throws Throwable
         {
            return mock(AbstractBundleState.class);
         }
      });

      AutoInstallPluginImpl ai = new AutoInstallPluginImpl(bm);

      List<URL> install = new ArrayList<URL>(Arrays.asList(new URL("http://localhost:9999/ai1")));
      List<URL> start = new ArrayList<URL>(Arrays.asList(new URL("http://localhost:9999/as1"), new URL("http://localhost:9999/as2")));

      ai.setAutoInstall(install);
      ai.setAutoStart(start);

      ai.installBundles();
      assertEquals(3, ai.autoBundles.size());

      List<URL> locations = new ArrayList<URL>();
      locations.add(new URL("http://localhost:9999/ai1"));
      locations.add(new URL("http://localhost:9999/as1"));
      locations.add(new URL("http://localhost:9999/as2"));

      for (URL u : ai.autoBundles.keySet())
      {
         locations.remove(u);
      }
      assertEquals("All locations should be in the map", 0, locations.size());

      ai.startBundles();

      Bundle ai1b = ai.autoBundles.get(new URL("http://localhost:9999/ai1"));
      verifyZeroInteractions(ai1b);
      Bundle as1b = ai.autoBundles.get(new URL("http://localhost:9999/as1"));
      verify(as1b).start();
      verifyNoMoreInteractions(as1b);
      Bundle as2b = ai.autoBundles.get(new URL("http://localhost:9999/as2"));
      verify(as2b).start();
      verifyNoMoreInteractions(as2b);
   }

   @Test
   public void testAutoInstallPluginNoBundles() throws Exception
   {
      OSGiBundleManager bm = mock(OSGiBundleManager.class);
      AutoInstallPluginImpl ai = new AutoInstallPluginImpl(bm);
      ai.installBundles();
      ai.startBundles();
   }
}
