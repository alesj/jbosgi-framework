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
package org.jboss.test.osgi.classloader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.Loader;
import org.jboss.osgi.framework.testing.AbstractFrameworkTest;
import org.jboss.osgi.spi.util.ConstantsHelper;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;

/**
 * Test OSGi classloader domain
 * 
 * @author thomas.diesler@jboss.com
 * @since 29-Apr-2010
 */
public class ClassLoaderDomainTestCase extends AbstractFrameworkTest
{
   @Test
   public void testDomainStructure() throws BundleException
   {
      ClassLoaderSystem classLoaderSystem = getClassLoaderSystem();
      assertNotNull("ClassLoaderSystem not null", classLoaderSystem);
      
      ClassLoaderDomain classLoaderDomain = getClassLoaderDomain();
      assertNotNull("ClassLoaderDomain not null", classLoaderDomain);
      
      ClassLoaderDomain defaultDomain = classLoaderSystem.getDefaultDomain();
      assertNotNull("Default domain not null", defaultDomain);
      
      Loader parentDomain = classLoaderDomain.getParent();
      assertSame(defaultDomain, parentDomain);
   }

   @Test
   public void testSystemBundleLoad() throws Exception
   {
      // Load a class that is included in the system classpath by default
      Class<?> loadedClass = getFramework().loadClass(StartLevel.class.getName());
      assertNotNull("Class loaded: " + StartLevel.class.getName(), loadedClass);
      
      // Load a class that is included in the system classpath explicitly
      loadedClass = getFramework().loadClass(ConstantsHelper.class.getName());
      assertNotNull("Class loaded: " + ConstantsHelper.class.getName(), loadedClass);
      
      // Attempt to load a javax.* class that is not part of the JDK  
      loadedClass = getFramework().loadClass(Inject.class.getName());
      assertNull("Cannot load class: " + Inject.class.getName(), loadedClass);
      
      // Attempt to load a javax.* class that is part of the JDK  
      loadedClass = getFramework().loadClass(SwingUtilities.class.getName());
      assertNull("Cannot load class: " + SwingUtilities.class.getName(), loadedClass);
   }
   
   @Test
   public void testDefaultDomainClassLoad() throws BundleException
   {
      ClassLoaderSystem classLoaderSystem = getClassLoaderSystem();
      ClassLoaderDomain defaultDomain = classLoaderSystem.getDefaultDomain();
      assertNotNull("Default domain not null", defaultDomain);
      
      // Load a class that is included in the system classpath by default
      Class<?> loadedClass = defaultDomain.loadClass(StartLevel.class.getName());
      assertNotNull("Class loaded: " + StartLevel.class.getName(), loadedClass);
      
      // Load a class that is included in the system classpath explicitly
      loadedClass = defaultDomain.loadClass(ConstantsHelper.class.getName());
      assertNotNull("Class loaded: " + ConstantsHelper.class.getName(), loadedClass);
      
      // Attempt to load a javax.* class that is not part of the JDK  
      loadedClass = defaultDomain.loadClass(Inject.class.getName());
      assertNull("Cannot load class: " + Inject.class.getName(), loadedClass);
      
      // Attempt to load a javax.* class that is part of the JDK  
      loadedClass = defaultDomain.loadClass(SwingUtilities.class.getName());
      assertNull("Cannot load class: " + SwingUtilities.class.getName(), loadedClass);
   }
}